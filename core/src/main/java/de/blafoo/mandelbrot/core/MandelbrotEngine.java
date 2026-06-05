package de.blafoo.mandelbrot.core;

import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

/// Optimierungen im Inner-Loop:
/// - **Cardioid-/Period-2-Bulb-Test** schließt die zwei größten In-Set-Regionen sofort aus.
/// - **Periodicity Detection**: Speichert alle 20 Iterationen einen Checkpoint und
///   bricht ab, sobald die Bahn in einen Zyklus läuft (typisch 2–5× schneller bei
///   tiefen Zooms in In-Set-Bereichen).
/// - **Primitive `double`**: Keine [Complex]-Allokationen pro Pixel.
///
/// Thread-safe: `renderStrip()` darf parallel aus mehreren Threads aufgerufen werden.
public final class MandelbrotEngine {

    private static final double BAILOUT_SQ            = 256.0;
    private static final int    PERIOD_CHECK_INTERVAL = 20;
    private static final double PERIOD_EPS            = 1.0e-14;
    private static final double INV_LOG2              = 1.0 / Math.log(2.0);
    private static final int    BLACK_ARGB            = 0xFF000000;

    private MandelbrotEngine() {}

    /// Berechnet die Iteration für einen Punkt (für Statusanzeige).
    public static IterationResult computePixel(Complex c, int maxIter) {
        int iter = iterate(c.re(), c.im(), maxIter);
        if (iter == maxIter) return IterationResult.inMandelbrotSet(maxIter);
        return IterationResult.escaped(iter, smoothValue(c.re(), c.im(), iter));
    }

    /// Rendert einen horizontalen Streifen.
    public static void renderStrip(int[] pixels, RenderParams params,
                                   int[] palette, int startY, int endY) {
        renderStrip(pixels, params, palette, startY, endY, null);
    }

    /// Wie [#renderStrip(int[], RenderParams, int[], int, int)], aber mit Cancel-Hook.
    public static void renderStrip(int[] pixels, RenderParams params,
                                   int[] palette, int startY, int endY,
                                   @Nullable BooleanSupplier cancelled) {
        final int width   = params.width();
        final int maxIter = params.maxIterations();
        final double scale = params.scale();
        final double cx0  = params.centerX() - width / 2.0 * scale;
        final double cy0  = params.centerY() - params.height() / 2.0 * scale;
        final int paletteLast = palette.length - 1;

        for (int y = startY; y < endY; y++) {
            if (cancelled != null && cancelled.getAsBoolean()) return;
            final double cIm = cy0 + y * scale;
            final int rowBase = y * width;
            for (int x = 0; x < width; x++) {
                pixels[rowBase + x] = computeColor(cx0 + x * scale, cIm,
                        maxIter, palette, paletteLast);
            }
        }
    }

    /// Coarse Rendering mit Pixel-Verdopplung (für progressive Vorschau).
    public static void renderStripCoarse(int[] pixels, RenderParams params,
                                         int[] palette, int startY, int endY, int step,
                                         @Nullable BooleanSupplier cancelled) {
        if (step <= 1) {
            renderStrip(pixels, params, palette, startY, endY, cancelled);
            return;
        }
        final int width   = params.width();
        final int maxIter = params.maxIterations();
        final double scale = params.scale();
        final double cx0  = params.centerX() - width / 2.0 * scale;
        final double cy0  = params.centerY() - params.height() / 2.0 * scale;
        final int paletteLast = palette.length - 1;

        int sy = startY - (startY % step);
        if (sy < startY) sy += step;

        for (int y = sy; y < endY; y += step) {
            if (cancelled != null && cancelled.getAsBoolean()) return;
            final double cIm = cy0 + y * scale;
            final int blockH = Math.min(step, endY - y);
            for (int x = 0; x < width; x += step) {
                final int color  = computeColor(cx0 + x * scale, cIm,
                        maxIter, palette, paletteLast);
                final int blockW = Math.min(step, width - x);
                for (int dy = 0; dy < blockH; dy++) {
                    final int row = (y + dy) * width + x;
                    for (int dx = 0; dx < blockW; dx++) {
                        pixels[row + dx] = color;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    //  Hot Path
    // ------------------------------------------------------------------

    /// Berechnet die fertige ARGB-Farbe für einen Punkt – Hotpath.
    private static int computeColor(double cRe, double cIm, int maxIter,
                                    int[] palette, int paletteLast) {
        // Cardioid-Test
        final double imSq = cIm * cIm;
        final double xMq  = cRe - 0.25;
        final double q    = xMq * xMq + imSq;
        if (q * (q + xMq) <= 0.25 * imSq) return BLACK_ARGB;

        // Period-2-Bulb-Test
        final double rePlus1 = cRe + 1.0;
        if (rePlus1 * rePlus1 + imSq <= 0.0625) return BLACK_ARGB;

        // Iteration mit Periodicity Detection
        double zr = 0.0, zi = 0.0, zr2 = 0.0, zi2 = 0.0;
        double cpZr = 0.0, cpZi = 0.0;
        int checkpoint = PERIOD_CHECK_INTERVAL;
        int iter = 0;

        while (zr2 + zi2 <= BAILOUT_SQ && iter < maxIter) {
            zi  = 2.0 * zr * zi + cIm;
            zr  = zr2 - zi2 + cRe;
            zr2 = zr * zr;
            zi2 = zi * zi;

            if (Math.abs(zr - cpZr) < PERIOD_EPS && Math.abs(zi - cpZi) < PERIOD_EPS) {
                return BLACK_ARGB;
            }
            if (--checkpoint == 0) {
                cpZr = zr;
                cpZi = zi;
                checkpoint = PERIOD_CHECK_INTERVAL;
            }
            iter++;
        }

        if (iter == maxIter) return BLACK_ARGB;

        final double smooth = iter + 1 - Math.log(0.5 * Math.log(zr2 + zi2)) * INV_LOG2;
        return BLACK_ARGB | interpolateColor(palette, paletteLast, smooth);
    }

    private static int iterate(double cRe, double cIm, int maxIter) {
        final double imSq = cIm * cIm;
        final double xMq  = cRe - 0.25;
        final double q    = xMq * xMq + imSq;
        if (q * (q + xMq) <= 0.25 * imSq) return maxIter;
        final double rePlus1 = cRe + 1.0;
        if (rePlus1 * rePlus1 + imSq <= 0.0625) return maxIter;

        double zr = 0.0, zi = 0.0, zr2 = 0.0, zi2 = 0.0;
        double cpZr = 0.0, cpZi = 0.0;
        int checkpoint = PERIOD_CHECK_INTERVAL;
        int iter = 0;
        while (zr2 + zi2 <= BAILOUT_SQ && iter < maxIter) {
            zi  = 2.0 * zr * zi + cIm;
            zr  = zr2 - zi2 + cRe;
            zr2 = zr * zr;
            zi2 = zi * zi;
            if (Math.abs(zr - cpZr) < PERIOD_EPS && Math.abs(zi - cpZi) < PERIOD_EPS) {
                return maxIter;
            }
            if (--checkpoint == 0) {
                cpZr = zr; cpZi = zi; checkpoint = PERIOD_CHECK_INTERVAL;
            }
            iter++;
        }
        return iter;
    }

    private static double smoothValue(double cRe, double cIm, int iter) {
        double zr = 0.0, zi = 0.0;
        for (int i = 0; i < iter + 1; i++) {
            double nzr = zr * zr - zi * zi + cRe;
            zi = 2.0 * zr * zi + cIm;
            zr = nzr;
        }
        double modSq = zr * zr + zi * zi;
        return iter + 1 - Math.log(0.5 * Math.log(modSq)) * INV_LOG2;
    }

    private static int interpolateColor(int[] palette, int paletteLast, double smoothIter) {
        final int idx  = (int) smoothIter;
        final double frac = smoothIter - idx;
        final int i1 = Math.min(idx, paletteLast);
        final int i2 = Math.min(idx + 1, paletteLast);
        final int c1 = palette[i1];
        final int c2 = palette[i2];

        final int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        final int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;

        final int r = (int) (r1 + frac * (r2 - r1));
        final int g = (int) (g1 + frac * (g2 - g1));
        final int b = (int) (b1 + frac * (b2 - b1));
        return (r << 16) | (g << 8) | b;
    }
}

