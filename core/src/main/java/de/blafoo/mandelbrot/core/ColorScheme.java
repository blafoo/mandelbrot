package de.blafoo.mandelbrot.core;

import java.util.List;

/// Sealed Interface für Farbschemata (Sealed Types + Records).
/// Die fünf konkreten Schemata werden als `record` implementiert.
public sealed interface ColorScheme {

    /// Anzeigename des Farbschemas (für UI-Auswahl).
    String displayName();

    /// Liefert die ARGB-Farbe für einen Wert `t ∈ [0, 1]`.
    int colorFor(double t);

    // --- Die fünf konkreten Farbschemata als Records ---

    record Classic() implements ColorScheme {
        @Override public String displayName() { return "Klassisch"; }

        @Override
        public int colorFor(double t) {
            var angle = t * 5.0 * Math.PI;
            var r = (int) (127.5 * (1 + Math.sin(angle)));
            var g = (int) (127.5 * (1 + Math.sin(angle + 2.094)));
            var b = (int) (127.5 * (1 + Math.sin(angle + 4.189)));
            return clampRGB(r, g, b);
        }
    }

    record Fire() implements ColorScheme {
        @Override public String displayName() { return "Feuer"; }

        @Override
        public int colorFor(double t) {
            if (t < 0.33) {
                var s = t / 0.33;
                return clampRGB((int) (255 * s), 0, 0);
            } else if (t < 0.66) {
                var s = (t - 0.33) / 0.33;
                return clampRGB(255, (int) (255 * s), 0);
            } else {
                var s = (t - 0.66) / 0.34;
                return clampRGB(255, 255, (int) (255 * s));
            }
        }
    }

    record Ocean() implements ColorScheme {
        @Override public String displayName() { return "Ozean"; }

        @Override
        public int colorFor(double t) {
            var angle = t * 4.0 * Math.PI;
            var r = (int) (80 * (1 + Math.sin(angle + 4.0)));
            var g = (int) (100 + 155 * Math.pow(Math.sin(angle * 0.5), 2));
            var b = (int) (150 + 105 * Math.sin(angle));
            return clampRGB(r, g, b);
        }
    }

    record Neon() implements ColorScheme {
        @Override public String displayName() { return "Neon"; }

        @Override
        public int colorFor(double t) {
            var angle = t * 6.0 * Math.PI;
            var r = (int) (127.5 * (1 + Math.sin(angle)));
            var g = (int) (127.5 * (1 + Math.sin(angle + 2.5)));
            var b = (int) (200 + 55 * Math.sin(angle + 5.0));
            return clampRGB(r, g, b);
        }
    }

    record Grayscale() implements ColorScheme {
        @Override public String displayName() { return "Graustufen"; }

        @Override
        public int colorFor(double t) {
            var v = (int) (255 * Math.pow(t, 0.5));
            return clampRGB(v, v, v);
        }
    }

    // --- Hilfsmethoden ---

    /// Alle verfügbaren Schemata als unveränderliche [SequencedCollection][java.util.SequencedCollection]
    /// (Java 21+). Liefert garantiert die gleiche Reihenfolge bei jedem Aufruf.
    static List<ColorScheme> all() {
        return List.of(
                new Classic(), new Fire(), new Ocean(), new Neon(), new Grayscale()
        );
    }

    /// Default-Schema = erstes Element der Liste (nutzt [List#getFirst] aus JEP 431).
    static ColorScheme defaultScheme() {
        return all().getFirst();
    }

    /// Generiert eine Farbpalette für Smooth Coloring.
    default int[] generatePalette(int maxIterations) {
        var palette = new int[maxIterations + 1];
        for (int i = 0; i <= maxIterations; i++) {
            var t = (double) i / maxIterations;
            palette[i] = colorFor(t);
        }
        return palette;
    }

    private static int clampRGB(int r, int g, int b) {
        r = Math.clamp(r, 0, 255);
        g = Math.clamp(g, 0, 255);
        b = Math.clamp(b, 0, 255);
        return (r << 16) | (g << 8) | b;
    }
}
