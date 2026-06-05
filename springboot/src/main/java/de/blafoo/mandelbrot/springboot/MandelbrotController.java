package de.blafoo.mandelbrot.springboot;

import de.blafoo.mandelbrot.core.ColorScheme;
import de.blafoo.mandelbrot.core.MandelbrotRenderer;
import de.blafoo.mandelbrot.core.PngWriter;
import de.blafoo.mandelbrot.core.RenderParams;
import jakarta.annotation.PreDestroy;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/// REST-Endpoint, der die Mandelbrot-Menge als PNG ausliefert.
///
/// Beispiele:
/// * `/api/mandelbrot.png?width=800&height=600&centerX=-0.75&centerY=0&zoom=1`
/// * `/api/mandelbrot.png?...&scheme=Fire&maxIterations=500`
/// * `/api/mandelbrot.png?...&download=true` – erzwingt Speichern-Dialog
/// * `/api/schemes` – Liste verfügbarer Farbschemata
@RestController
@RequestMapping("/api")
public class MandelbrotController {

    private static final int MAX_DIMENSION  = 4096;
    private static final int MAX_ITERATIONS = 10_000;

    private final ExecutorService renderPool = Executors.newVirtualThreadPerTaskExecutor();
    private final MandelbrotRenderer renderer = new MandelbrotRenderer(renderPool);

    @GetMapping("/schemes")
    public List<Map<String, String>> schemes() {
        return ColorScheme.all().stream()
                .map(s -> Map.of("name", s.displayName()))
                .toList();
    }

    @GetMapping(value = "/mandelbrot.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> render(
            @RequestParam(defaultValue = "800")    int width,
            @RequestParam(defaultValue = "600")    int height,
            @RequestParam(defaultValue = "-0.5")   double centerX,
            @RequestParam(defaultValue = "0.0")    double centerY,
            @RequestParam(defaultValue = "1.0")    double zoom,
            @RequestParam(defaultValue = "300")    int maxIterations,
            @RequestParam(defaultValue = "Classic") String scheme,
            @RequestParam(defaultValue = "false")  boolean download
    ) {
        // Java 21+: Math.clamp ersetzt eigene clamp-Hilfsfunktion
        width         = Math.clamp(width,  16, MAX_DIMENSION);
        height        = Math.clamp(height, 16, MAX_DIMENSION);
        maxIterations = Math.clamp(maxIterations, 50, MAX_ITERATIONS);
        if (zoom <= 0) zoom = 1.0;

        ColorScheme cs = ColorScheme.all().stream()
                .filter(s -> s.displayName().equalsIgnoreCase(scheme))
                .findFirst()
                .orElse(ColorScheme.defaultScheme());

        var params = new RenderParams(width, height, centerX, centerY, zoom, maxIterations, cs);

        // Gemeinsamer parallelisierter Render aus dem Core + PNG-Encoder.
        int[]  pixels = renderer.renderBlocking(params);
        byte[] png    = PngWriter.toBytes(pixels, width, height);

        var resp = ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic());
        if (download) {
            var name = "mandelbrot_%.6f_%.6f_z%.2fx.png".formatted(centerX, centerY, zoom);
            resp = resp.header("Content-Disposition", "attachment; filename=\"" + name + "\"");
        }
        return resp.body(png);
    }

    @PreDestroy
    void shutdown() {
        renderPool.shutdownNow();
    }
}

