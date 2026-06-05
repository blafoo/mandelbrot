package de.blafoo.mandelbrot.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import de.blafoo.mandelbrot.core.ColorScheme;
import de.blafoo.mandelbrot.core.Complex;
import de.blafoo.mandelbrot.core.IterationResult;
import de.blafoo.mandelbrot.core.MandelbrotEngine;
import de.blafoo.mandelbrot.core.RenderParams;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Custom Android View für die Mandelbrot-Darstellung.
 *
 * <p>Performance-Strategie:
 * <ul>
 *   <li>Während Pan/Zoom-Gesten wird <b>nicht</b> neu berechnet – das vorhandene Bitmap
 *       wird per {@link Matrix} nur transformiert (instantes UI-Feedback).</li>
 *   <li>Erst beim {@code ACTION_UP} wird ein Re-Render gestartet.</li>
 *   <li>Laufende Renders werden über ein Cancellation-Flag <b>frühzeitig abgebrochen</b>.</li>
 *   <li>Standard-Iterationstiefe 200 (statt 300) – schnellere Erst-Anzeige.</li>
 * </ul>
 */
public class MandelbrotView extends View {

    private double centerX = -0.5;
    private double centerY = 0.0;
    private double zoom = 1.0;
    private int maxIterations = 200;
    private ColorScheme colorScheme = ColorScheme.defaultScheme();

    private @Nullable Bitmap bitmap;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean[] currentCancelFlag = { false };
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final int cores = Math.max(2, Runtime.getRuntime().availableProcessors());

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private @Nullable StatusUpdateListener statusListener;

    // Live-Transformation während Geste (kein Re-Render):
    private final Matrix liveMatrix = new Matrix();
    private boolean gestureActive = false;
    private float liveTranslateX = 0f, liveTranslateY = 0f;
    private float liveScale = 1f;
    private float liveScaleFocusX = 0f, liveScaleFocusY = 0f;
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    // Eingefrorene Transformation, die nach Geste-Ende solange angezeigt wird,
    // bis das neue (korrekt berechnete) Bitmap eintrifft. Verhindert das
    // kurzzeitige Zurückspringen auf das untransformierte Original-Bitmap.
    private final Matrix displayMatrix = new Matrix();
    private boolean hasDisplayTransform = false;

    // Drag-State
    private float lastTouchX, lastTouchY;
    private boolean isDragging = false;

    public interface StatusUpdateListener {
        void onStatusUpdate(double re, double im, double zoom, int iterations);
    }

    public MandelbrotView(Context context) {
        this(context, null);
    }

    public MandelbrotView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                gestureActive = true;
                liveScaleFocusX = detector.getFocusX();
                liveScaleFocusY = detector.getFocusY();
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                liveScale *= detector.getScaleFactor();
                liveScaleFocusX = detector.getFocusX();
                liveScaleFocusY = detector.getFocusY();
                invalidate(); // nur neu zeichnen, NICHT neu berechnen
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                var focusRe = screenToRe(e.getX());
                var focusIm = screenToIm(e.getY());
                var factor = 2.0;
                zoom *= factor;
                centerX = focusRe + (centerX - focusRe) / factor;
                centerY = focusIm + (centerY - focusIm) / factor;
                startRender();
                return true;
            }
        });
    }

    public void setOnStatusUpdateListener(StatusUpdateListener listener) {
        this.statusListener = listener;
    }

    public void setColorScheme(ColorScheme scheme) {
        this.colorScheme = scheme;
        startRender();
    }

    public void adjustIterations(int delta) {
        maxIterations = Math.max(50, maxIterations + delta);
        startRender();
    }

    public void resetView() {
        centerX = -0.5;
        centerY = 0.0;
        zoom = 1.0;
        maxIterations = 200;
        resetLiveTransform();
        startRender();
    }

    private void resetLiveTransform() {
        liveTranslateX = 0f;
        liveTranslateY = 0f;
        liveScale = 1f;
        gestureActive = false;
    }

    private double getViewScale() {
        var size = Math.min(getWidth(), getHeight());
        return 3.0 / (size * zoom);
    }

    private double screenToRe(float sx) {
        return centerX + (sx - getWidth() / 2.0) * getViewScale();
    }

    private double screenToIm(float sy) {
        return centerY + (sy - getHeight() / 2.0) * getViewScale();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);

        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = true;
                updateStatus(event.getX(), event.getY());
            }
            case MotionEvent.ACTION_MOVE -> {
                if (isDragging && !scaleDetector.isInProgress()) {
                    var dx = event.getX() - lastTouchX;
                    var dy = event.getY() - lastTouchY;
                    liveTranslateX += dx;
                    liveTranslateY += dy;
                    gestureActive = true;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    invalidate(); // nur Bitmap verschieben, KEIN Render
                }
            }
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false;
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    performClick();
                }
                if (gestureActive) {
                    commitLiveTransform();
                }
            }
        }
        return true;
    }

    /// Übernimmt die Live-Transformation in centerX/Y/zoom und stößt Render an.
    private void commitLiveTransform() {
        // Transformation einfrieren, damit sie bis zum Eintreffen des neuen
        // Bitmaps weiter angezeigt wird (kein Flash zurück zum Original).
        displayMatrix.reset();
        if (liveScale != 1f) {
            displayMatrix.postScale(liveScale, liveScale, liveScaleFocusX, liveScaleFocusY);
        }
        displayMatrix.postTranslate(liveTranslateX, liveTranslateY);
        hasDisplayTransform = true;

        var scale = getViewScale();
        // Pan: Translation in Bildschirm-Pixeln zurück in komplexe Koordinaten
        centerX -= liveTranslateX * scale;
        centerY -= liveTranslateY * scale;
        // Zoom: Skalierung um Fokuspunkt
        if (liveScale != 1f) {
            var focusRe = screenToRe(liveScaleFocusX);
            var focusIm = screenToIm(liveScaleFocusY);
            zoom *= liveScale;
            centerX = focusRe + (centerX - focusRe) / liveScale;
            centerY = focusIm + (centerY - focusIm) / liveScale;
        }
        resetLiveTransform();
        startRender();
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void updateStatus(float sx, float sy) {
        if (statusListener == null) return;
        var re = screenToRe(sx);
        var im = screenToIm(sy);
        var c = new Complex(re, im);
        var result = MandelbrotEngine.computePixel(c, maxIterations);
        int iter = (result instanceof IterationResult.Escaped escaped)
                ? escaped.iterations() : -1;
        statusListener.onStatusUpdate(re, im, zoom, iter);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            startRender();
        }
    }

    /// Stößt ein Re-Render an. Bricht ein evtl. laufendes Rendering sofort ab.
    /// Strategie: Erst ein Coarse-Pass mit Schrittweite 4 (≈ 16× weniger Pixel)
    /// für sofortiges Feedback, dann der finale Voll-Render.
    public void startRender() {
        var w = getWidth();
        var h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Vorheriges Rendering abbrechen
        currentCancelFlag[0] = true;
        final boolean[] cancelFlag = { false };
        currentCancelFlag = cancelFlag;

        var params = new RenderParams(w, h, centerX, centerY, zoom, maxIterations, colorScheme);
        var palette = colorScheme.generatePalette(maxIterations);

        executor.submit(() -> {
            if (cancelFlag[0]) return;
            var pixels = new int[w * h];

            // --- Phase 1: Coarse-Pass für sofortige Vorschau (single-thread, sehr schnell) ---
            MandelbrotEngine.renderStripCoarse(pixels, params, palette, 0, h, 4,
                    () -> cancelFlag[0]);
            if (cancelFlag[0]) return;
            publishBitmap(pixels, w, h, cancelFlag);

            // --- Phase 2: Voller Render parallelisiert (alle Cores) ---
            var threads = new Thread[cores];
            var rowsPerThread = Math.ceilDiv(h, cores);
            for (int t = 0; t < cores; t++) {
                final int startY = t * rowsPerThread;
                final int endY = Math.min(startY + rowsPerThread, h);
                if (startY >= endY) {
                    threads[t] = null;
                    continue;
                }
                threads[t] = new Thread(() ->
                        MandelbrotEngine.renderStrip(pixels, params, palette, startY, endY,
                                () -> cancelFlag[0]));
                threads[t].setPriority(Thread.NORM_PRIORITY + 1);
                threads[t].start();
            }
            for (var thread : threads) {
                if (thread == null) continue;
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (cancelFlag[0]) return;
            publishBitmap(pixels, w, h, cancelFlag);
        });
    }

    private void publishBitmap(int[] pixels, int w, int h, boolean[] cancelFlag) {
        if (cancelFlag[0]) return;
        var bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bmp.setPixels(pixels, 0, w, 0, 0, w, h);
        mainHandler.post(() -> {
            if (!cancelFlag[0]) {
                bitmap = bmp;
                hasDisplayTransform = false;
                displayMatrix.reset();
                invalidate();
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null) return;

        if (gestureActive && (liveTranslateX != 0 || liveTranslateY != 0 || liveScale != 1f)) {
            // Live-Transformation: nur das vorhandene Bitmap verschieben/skalieren
            liveMatrix.reset();
            if (liveScale != 1f) {
                liveMatrix.postScale(liveScale, liveScale, liveScaleFocusX, liveScaleFocusY);
            }
            liveMatrix.postTranslate(liveTranslateX, liveTranslateY);
            canvas.drawBitmap(bitmap, liveMatrix, bitmapPaint);
        } else if (hasDisplayTransform) {
            // Geste beendet, neues Bild noch nicht fertig -> eingefrorene Transformation
            // weiter darstellen, damit das Bild nicht zurückspringt.
            canvas.drawBitmap(bitmap, displayMatrix, bitmapPaint);
        } else {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
    }
}
