package de.blafoo.mandelbrot.core;

/// Record für die Render-Parameter eines Mandelbrot-Bildes.
///
/// Stellt die zentralen Transformations-Helfer bereit
/// ([#withZoomAt], [#withPanByPixels], [#withCenter], ...), damit alle
/// UI-Module dieselbe (getestete) Zoom-/Pan-Mathematik nutzen.
public record RenderParams(
        int width,
        int height,
        double centerX,
        double centerY,
        double zoom,
        int maxIterations,
        ColorScheme colorScheme
) {
    /// Standardwerte: Mandelbrot zentriert bei (-0.5, 0), Zoom 1.0,
    /// 300 Iterationen, Default-Farbschema.
    public static final double DEFAULT_CENTER_X   = -0.5;
    public static final double DEFAULT_CENTER_Y   =  0.0;
    public static final double DEFAULT_ZOOM       =  1.0;
    public static final int    DEFAULT_ITERATIONS =  300;

    /// Skalierungsfaktor für die Umrechnung Pixel → komplexe Ebene.
    public double scale() {
        double size = Math.min(width, height);
        return 3.0 / (size * zoom);
    }

    /// Wandelt Pixel-Koordinaten in eine komplexe Zahl um.
    public Complex pixelToComplex(int px, int py) {
        double re = centerX + (px - width / 2.0) * scale();
        double im = centerY + (py - height / 2.0) * scale();
        return new Complex(re, im);
    }

    // ------------------------------------------------------------------
    //  Wither-Methoden – immutable Updates der Render-Parameter.
    // ------------------------------------------------------------------

    public RenderParams withCenter(double cx, double cy) {
        return new RenderParams(width, height, cx, cy, zoom, maxIterations, colorScheme);
    }

    /// Zoomt um den Faktor `factor` um den Bildpixel `(px, py)`, sodass dieser
    /// Punkt unter dem Cursor stehen bleibt.
    public RenderParams withZoomAt(int px, int py, double factor) {
        var c = pixelToComplex(px, py);
        double newCenterX = c.re() + (centerX - c.re()) / factor;
        double newCenterY = c.im() + (centerY - c.im()) / factor;
        return new RenderParams(width, height, newCenterX, newCenterY,
                zoom * factor, maxIterations, colorScheme);
    }

    /// Verschiebt den Bildausschnitt um `(dxPx, dyPx)` Bildschirm-Pixel.
    /// Positive Werte → der angezeigte Ausschnitt verschiebt sich nach links/oben.
    public RenderParams withPanByPixels(double dxPx, double dyPx) {
        double s = scale();
        return new RenderParams(width, height,
                centerX - dxPx * s, centerY - dyPx * s,
                zoom, maxIterations, colorScheme);
    }
}
