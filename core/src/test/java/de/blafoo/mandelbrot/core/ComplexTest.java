package de.blafoo.mandelbrot.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-Tests für den {@link Complex} Record.
 */
class ComplexTest {

    private static final double EPSILON = 1e-10;

    @Nested
    @DisplayName("Record-Eigenschaften")
    class RecordProperties {

        @Test
        @DisplayName("Accessors re() und im() liefern korrekte Werte")
        void accessors() {
            var c = new Complex(3.0, -4.0);
            assertEquals(3.0, c.re());
            assertEquals(-4.0, c.im());
        }

        @Test
        @DisplayName("Gleichheit zweier identischer komplexer Zahlen")
        void equality() {
            var a = new Complex(1.5, 2.5);
            var b = new Complex(1.5, 2.5);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Ungleichheit bei verschiedenen Werten")
        void inequality() {
            var a = new Complex(1.0, 2.0);
            var b = new Complex(1.0, 3.0);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("toString() enthält re und im")
        void toStringTest() {
            var c = new Complex(1.0, 2.0);
            var str = c.toString();
            assertTrue(str.contains("1.0"));
            assertTrue(str.contains("2.0"));
        }

        @Test
        @DisplayName("Complex.ZERO ist (0, 0)")
        void zeroConstant() {
            assertEquals(0.0, Complex.ZERO.re());
            assertEquals(0.0, Complex.ZERO.im());
            assertEquals(new Complex(0, 0), Complex.ZERO);
        }
    }

    @Nested
    @DisplayName("Betrag (Modulus)")
    class ModulusTests {

        @Test
        @DisplayName("Betrag der Null ist 0")
        void modulusOfZero() {
            var z = new Complex(0, 0);
            assertEquals(0.0, z.modulus());
            assertEquals(0.0, z.modulusSquared());
        }

        @Test
        @DisplayName("|3 + 4i| = 5")
        void modulus3_4i() {
            var z = new Complex(3, 4);
            assertEquals(25.0, z.modulusSquared(), EPSILON);
            assertEquals(5.0, z.modulus(), EPSILON);
        }

        @Test
        @DisplayName("|1 + 0i| = 1")
        void modulusRealUnit() {
            var z = new Complex(1, 0);
            assertEquals(1.0, z.modulus(), EPSILON);
        }

        @Test
        @DisplayName("|0 + i| = 1")
        void modulusImaginaryUnit() {
            var z = new Complex(0, 1);
            assertEquals(1.0, z.modulus(), EPSILON);
        }

        @ParameterizedTest
        @DisplayName("modulusSquared ist immer nicht-negativ")
        @CsvSource({
                "0.0, 0.0",
                "1.0, -1.0",
                "-3.5, 2.7",
                "100.0, -200.0"
        })
        void modulusSquaredNonNegative(double re, double im) {
            var z = new Complex(re, im);
            assertTrue(z.modulusSquared() >= 0.0);
        }
    }

    @Nested
    @DisplayName("Quadrat (square)")
    class SquareTests {

        @Test
        @DisplayName("0² = 0")
        void squareOfZero() {
            var result = new Complex(0, 0).square();
            assertEquals(0.0, result.re(), EPSILON);
            assertEquals(0.0, result.im(), EPSILON);
        }

        @Test
        @DisplayName("1² = 1")
        void squareOfOne() {
            var result = new Complex(1, 0).square();
            assertEquals(1.0, result.re(), EPSILON);
            assertEquals(0.0, result.im(), EPSILON);
        }

        @Test
        @DisplayName("i² = -1")
        void squareOfI() {
            var result = new Complex(0, 1).square();
            assertEquals(-1.0, result.re(), EPSILON);
            assertEquals(0.0, result.im(), EPSILON);
        }

        @Test
        @DisplayName("(1 + i)² = 2i")
        void squareOf1PlusI() {
            var result = new Complex(1, 1).square();
            assertEquals(0.0, result.re(), EPSILON);
            assertEquals(2.0, result.im(), EPSILON);
        }

        @Test
        @DisplayName("(3 + 4i)² = -7 + 24i")
        void squareOf3Plus4i() {
            // (3+4i)² = 9 + 24i + 16i² = 9 + 24i - 16 = -7 + 24i
            var result = new Complex(3, 4).square();
            assertEquals(-7.0, result.re(), EPSILON);
            assertEquals(24.0, result.im(), EPSILON);
        }

        @Test
        @DisplayName("(-2 + 3i)² = -5 - 12i")
        void squareOfNeg2Plus3i() {
            // (-2+3i)² = 4 - 12i + 9i² = 4 - 12i - 9 = -5 - 12i
            var result = new Complex(-2, 3).square();
            assertEquals(-5.0, result.re(), EPSILON);
            assertEquals(-12.0, result.im(), EPSILON);
        }
    }

    @Nested
    @DisplayName("Addition (plus)")
    class PlusTests {

        @Test
        @DisplayName("z + 0 = z (neutrales Element)")
        void addZero() {
            var z = new Complex(3.5, -2.1);
            var result = z.plus(new Complex(0, 0));
            assertEquals(z, result);
        }

        @Test
        @DisplayName("(1 + 2i) + (3 + 4i) = (4 + 6i)")
        void addTwoComplex() {
            var a = new Complex(1, 2);
            var b = new Complex(3, 4);
            var result = a.plus(b);
            assertEquals(4.0, result.re(), EPSILON);
            assertEquals(6.0, result.im(), EPSILON);
        }

        @Test
        @DisplayName("Addition ist kommutativ: a + b = b + a")
        void addCommutative() {
            var a = new Complex(1.5, -3.7);
            var b = new Complex(-2.3, 4.1);
            assertEquals(a.plus(b), b.plus(a));
        }

        @Test
        @DisplayName("z + (-z) = 0")
        void addInverse() {
            var z = new Complex(5.5, -3.3);
            var negZ = new Complex(-5.5, 3.3);
            var result = z.plus(negZ);
            assertEquals(0.0, result.re(), EPSILON);
            assertEquals(0.0, result.im(), EPSILON);
        }

        @Test
        @DisplayName("Addition ist assoziativ: (a + b) + c = a + (b + c)")
        void addAssociative() {
            var a = new Complex(1, 2);
            var b = new Complex(3, 4);
            var c = new Complex(5, 6);
            var left = a.plus(b).plus(c);
            var right = a.plus(b.plus(c));
            assertEquals(left.re(), right.re(), EPSILON);
            assertEquals(left.im(), right.im(), EPSILON);
        }
    }

    @Nested
    @DisplayName("Mandelbrot-Iteration (mandelbrotStep)")
    class MandelbrotStepTests {

        @Test
        @DisplayName("Erster Schritt ab z=0: z² + c = c")
        void firstStepFromZero() {
            var z = new Complex(0, 0);
            var c = new Complex(0.5, 0.3);
            var result = z.mandelbrotStep(c);
            assertEquals(c.re(), result.re(), EPSILON);
            assertEquals(c.im(), result.im(), EPSILON);
        }

        @Test
        @DisplayName("mandelbrotStep = square().plus(c)")
        void stepEqualsSquarePlusC() {
            var z = new Complex(0.3, 0.7);
            var c = new Complex(-0.5, 0.2);
            var step = z.mandelbrotStep(c);
            var manual = z.square().plus(c);
            assertEquals(manual, step);
        }

        @Test
        @DisplayName("Fixpunkt c=0: z=0 bleibt bei 0")
        void fixpointAtOrigin() {
            var z = new Complex(0, 0);
            var c = new Complex(0, 0);
            var result = z.mandelbrotStep(c);
            assertEquals(0.0, result.re(), EPSILON);
            assertEquals(0.0, result.im(), EPSILON);
        }

        @Test
        @DisplayName("Mehrere Iterationen für c=-1: Sequenz 0, -1, 0, -1, ...")
        void oscillationForCMinus1() {
            var c = new Complex(-1, 0);
            var z = new Complex(0, 0);

            z = z.mandelbrotStep(c); // 0² + (-1) = -1
            assertEquals(-1.0, z.re(), EPSILON);
            assertEquals(0.0, z.im(), EPSILON);

            z = z.mandelbrotStep(c); // (-1)² + (-1) = 0
            assertEquals(0.0, z.re(), EPSILON);
            assertEquals(0.0, z.im(), EPSILON);

            z = z.mandelbrotStep(c); // 0² + (-1) = -1
            assertEquals(-1.0, z.re(), EPSILON);
        }

        @Test
        @DisplayName("c=0.25+0i konvergiert zum Fixpunkt 0.5")
        void convergenceAtQuarter() {
            var c = new Complex(0.25, 0);
            var z = new Complex(0, 0);

            // Viele Iterationen → konvergiert gegen 0.5
            for (int i = 0; i < 1000; i++) {
                z = z.mandelbrotStep(c);
            }
            assertEquals(0.5, z.re(), 1e-2);
            assertEquals(0.0, z.im(), 1e-2);
        }

        @Test
        @DisplayName("c=2+0i divergiert schnell (|z| > 2 nach wenigen Schritten)")
        void divergenceForC2() {
            var c = new Complex(2, 0);
            var z = new Complex(0, 0);

            z = z.mandelbrotStep(c); // 2
            z = z.mandelbrotStep(c); // 4+2 = 6
            assertTrue(z.modulusSquared() > 4.0,
                    "z sollte nach 2 Iterationen divergiert sein, |z|²=" + z.modulusSquared());
        }
    }

    @Nested
    @DisplayName("Spezialfälle & Randbedingungen")
    class EdgeCases {

        @Test
        @DisplayName("Rein reelle Zahl: im = 0")
        void purelyReal() {
            var z = new Complex(42.0, 0);
            assertEquals(42.0, z.re());
            assertEquals(0.0, z.im());
            assertEquals(42.0, z.modulus(), EPSILON);
        }

        @Test
        @DisplayName("Rein imaginäre Zahl: re = 0")
        void purelyImaginary() {
            var z = new Complex(0, -7.0);
            assertEquals(0.0, z.re());
            assertEquals(-7.0, z.im());
            assertEquals(7.0, z.modulus(), EPSILON);
        }

        @Test
        @DisplayName("Sehr kleine Werte nahe Null")
        void verySmallValues() {
            var z = new Complex(1e-15, 1e-15);
            var squared = z.square();
            // (a+bi)² wobei a=b=1e-15: re = a²-b² = 0, im = 2ab = 2e-30
            assertEquals(0.0, squared.re(), 1e-25);
            assertEquals(2e-30, squared.im(), 1e-35);
        }

        @Test
        @DisplayName("Große Werte: Betrag korrekt berechnet")
        void largeValues() {
            var z = new Complex(1e6, 1e6);
            var expected = Math.sqrt(2e12);
            assertEquals(expected, z.modulus(), 1e-2);
        }

        @Test
        @DisplayName("|z²| = |z|²")
        void modulusSquaredProperty() {
            var z = new Complex(2.5, -1.3);
            var modulusZ = z.modulus();
            var modulusZSquared = z.square().modulus();
            assertEquals(modulusZ * modulusZ, modulusZSquared, EPSILON);
        }

        @Test
        @DisplayName("Negative reelle und imaginäre Teile")
        void negativeComponents() {
            var a = new Complex(-3, -4);
            var b = new Complex(-1, -2);
            var sum = a.plus(b);
            assertEquals(-4.0, sum.re(), EPSILON);
            assertEquals(-6.0, sum.im(), EPSILON);
        }
    }
}


