package de.blafoo.mandelbrot.core;

/// Sealed Interface für das Ergebnis einer Mandelbrot-Berechnung.
///
/// Die zwei möglichen Zustände werden als Records modelliert und sind
/// damit für **Record Patterns** in `switch` nutzbar (Java 21+ stable).
public sealed interface IterationResult {

    /// Punkt liegt innerhalb der Mandelbrot-Menge (Iteration konvergiert).
    record InSet(int maxIterations) implements IterationResult {}

    /// Punkt liegt außerhalb (Iteration divergiert nach `iterations` Schritten).
    /// `smoothValue` enthält den geglätteten Wert für Smooth Coloring.
    record Escaped(int iterations, double smoothValue) implements IterationResult {}

    /// Fabrikmethode für Punkte innerhalb der Menge.
    static IterationResult inMandelbrotSet(int maxIter) {
        return new InSet(maxIter);
    }

    /// Fabrikmethode für divergierte Punkte.
    static IterationResult escaped(int iterations, double smoothValue) {
        return new Escaped(iterations, smoothValue);
    }

}
