package de.blafoo.mandelbrot.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/// Parallele CPU-Render-Pipeline mit optionaler Cancellation und progressivem
/// Coarse-Pass.
///
/// Die Klasse hält **keinen State** außer dem Executor und der laufenden
/// Render-Generation (für `renderProgressive`). `renderBlocking` ist
/// reentrant und Thread-safe.
public final class MandelbrotRenderer {

    private final ExecutorService executor;
    private final int strips;
    private final AtomicLong generation = new AtomicLong();

    public MandelbrotRenderer(ExecutorService executor) {
        this(executor, Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    public MandelbrotRenderer(ExecutorService executor, int strips) {
        this.executor = executor;
        this.strips   = Math.max(1, strips);
    }

    /// Blockierender Voll-Render. Liefert das fertige ARGB-Pixel-Array
    /// (Länge `width*height`)
    public int[] renderBlocking(RenderParams params) {
        int w = params.width(), h = params.height();
        int[] pixels  = new int[w * h];
        int[] palette = params.colorScheme().generatePalette(params.maxIterations());
        renderParallel(pixels, params, palette, () -> false);
        return pixels;
    }

    // ------------------------------------------------------------------
    //  Interne parallele Render-Schleife.
    // ------------------------------------------------------------------

    private void renderParallel(int[] pixels, RenderParams params,
                                int[] palette, BooleanSupplier cancelled) {
        int h = params.height();
        int rowsPerStrip = Math.max(1, Math.ceilDiv(h, strips));
        Future<?>[] tasks = new Future<?>[strips];
        for (int t = 0; t < strips; t++) {
            int sy = t * rowsPerStrip;
            int ey = Math.min(sy + rowsPerStrip, h);
            if (sy >= ey) { tasks[t] = null; continue; }
            tasks[t] = executor.submit(() ->
                    MandelbrotEngine.renderStrip(pixels, params, palette,
                            sy, ey, cancelled));
        }
        for (Future<?> f : tasks) {
            if (f == null) continue;
            try {
                f.get();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                throw new RenderException("Render-Strip fehlgeschlagen", e);
            }
        }
    }

    /// Callback für progressives Rendering. `phase` ist `0` für Coarse-Pass, `1` für Voll-Render.
    @FunctionalInterface
    public interface PixelCallback {
        void accept(int[] pixels, int phase);
    }

    /// RuntimeException für Renderfehler.
    public static final class RenderException extends RuntimeException {
        public RenderException(String msg, Throwable cause) { super(msg, cause); }
    }
}

