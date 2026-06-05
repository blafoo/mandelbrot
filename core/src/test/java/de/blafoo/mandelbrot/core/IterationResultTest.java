package de.blafoo.mandelbrot.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/// Unit-Tests für das sealed Interface [IterationResult] und seine Records.
/// Demonstriert **Pattern Matching for switch** + **Record Patterns** (Java 21+).
class IterationResultTest {

    @Test
    @DisplayName("inMandelbrotSet() erzeugt InSet-Record")
    void factoryInSet() {
        var result = IterationResult.inMandelbrotSet(500);
        assertInstanceOf(IterationResult.InSet.class, result);
        assertEquals(500, ((IterationResult.InSet) result).maxIterations());
    }

    @Test
    @DisplayName("escaped() erzeugt Escaped-Record")
    void factoryEscaped() {
        var result = IterationResult.escaped(42, 42.7);
        assertInstanceOf(IterationResult.Escaped.class, result);
        var escaped = (IterationResult.Escaped) result;
        assertEquals(42, escaped.iterations());
        assertEquals(42.7, escaped.smoothValue());
    }

    @Test
    @DisplayName("Pattern Matching for switch: InSet vs. Escaped")
    void patternMatchingSwitch() {
        IterationResult inSet = IterationResult.inMandelbrotSet(100);
        IterationResult escaped = IterationResult.escaped(7, 7.5);

        var inSetText = switch (inSet) {
            case IterationResult.InSet _a -> "in";
            case IterationResult.Escaped b -> "out";
        };
        var escapedText = switch (escaped) {
            case IterationResult.InSet a -> "in";
            case IterationResult.Escaped(int i, double s) -> "out:" + i + "," + s;
        };

        assertEquals("in", inSetText);
        assertEquals("out:7,7.5", escapedText);
    }

    @Test
    @DisplayName("Sealed Interface: nur InSet und Escaped sind erlaubt")
    void sealedHierarchy() {
        var subclasses = IterationResult.class.getPermittedSubclasses();
        assertEquals(2, subclasses.length);
        var names = java.util.Arrays.stream(subclasses)
                .map(Class::getSimpleName)
                .sorted()
                .toList();
        assertEquals(java.util.List.of("Escaped", "InSet"), names);
    }
}

