package de.blafoo.mandelbrot.core;

/// Record für eine komplexe Zahl `re + im·i`.
public record Complex(double re, double im) {

    /// Konstante `0 + 0i` (Startwert der Mandelbrot-Iteration).
    public static final Complex ZERO = new Complex(0, 0);

    /// Quadrierter Betrag `|z|² = re² + im²` (vermeidet `sqrt` für Schwellwert-Tests).
    public double modulusSquared() {
        return re * re + im * im;
    }

    /// Betrag `|z| = √(re² + im²)`.
    public double modulus() {
        return Math.sqrt(modulusSquared());
    }

    /// Komplexes Quadrat `z² = (re² - im²) + 2·re·im·i`.
    public Complex square() {
        return new Complex(re * re - im * im, 2.0 * re * im);
    }

    /// Komplexe Addition `z + other`.
    public Complex plus(Complex other) {
        return new Complex(re + other.re, im + other.im);
    }

    /// Mandelbrot-Iteration: `z → z² + c`.
    public Complex mandelbrotStep(Complex c) {
        return square().plus(c);
    }
}

