package de.blafoo.mandelbrot.javafx;

import de.blafoo.mandelbrot.core.ColorScheme;
import de.blafoo.mandelbrot.core.MandelbrotEngine;
import de.blafoo.mandelbrot.core.PngWriter;
import de.blafoo.mandelbrot.core.RenderParams;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/// JavaFX-basierter Mandelbrot-Viewer.
/// Nutzt den plattformunabhängigen `MandelbrotEngine` aus dem `core`-Modul
/// und Virtual Threads für paralleles CPU-Rendering.
public class MandelbrotFxApp extends Application {

    private static final int INITIAL_WIDTH = 1000;
    private static final int INITIAL_HEIGHT = 750;

    private @Nullable Stage stage;
    private @Nullable Canvas canvas;
    private @Nullable WritableImage image;
    private @Nullable PixelBuffer<IntBuffer> pixelBuffer;
    private int[] pixels = new int[0];

    private double centerX = -0.5, centerY = 0.0;
    private double zoom = 1.0;
    private int maxIterations = 300;
    private ColorScheme colorScheme = ColorScheme.defaultScheme();

    private double dragStartX, dragStartY;
    private double dragStartCenterX, dragStartCenterY;

    private final Label status = new Label("Bereit");
    private final ExecutorService renderPool = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicLong currentRenderId = new AtomicLong();

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        canvas = new Canvas(INITIAL_WIDTH, INITIAL_HEIGHT);
        BorderPane root = new BorderPane();
        root.setTop(buildToolBar());
        root.setCenter(canvas);
        root.setBottom(buildStatusBar());

        canvas.widthProperty().addListener((obs, o, n) -> rerender());
        canvas.heightProperty().addListener((obs, o, n) -> rerender());

        // Canvas größenänderbar machen
        Region center = new Region();
        center.setMinSize(0, 0);
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty().subtract(80));

        installMouseHandlers();

        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
        stage.setTitle("Mandelbrot Viewer · JavaFX");
        stage.setScene(scene);
        stage.setOnCloseRequest(_ -> renderPool.shutdownNow());
        stage.show();

        rerender();
    }

    private ToolBar buildToolBar() {
        ChoiceBox<ColorScheme> schemes = new ChoiceBox<>();
        schemes.getItems().addAll(ColorScheme.all());
        schemes.setValue(colorScheme);
        schemes.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(@Nullable ColorScheme cs) {
                return cs == null ? "" : cs.displayName();
            }
            @Override public @Nullable ColorScheme fromString(@Nullable String s) { return null; }
        });
        schemes.valueProperty().addListener((obs, o, n) -> {
            if (n != null) { colorScheme = n; rerender(); }
        });

        Spinner<Integer> iterSpinner = new Spinner<>(50, 10000, maxIterations, 50);
        iterSpinner.setEditable(true);
        iterSpinner.setPrefWidth(110);
        iterSpinner.valueProperty().addListener((obs, o, n) -> {
            maxIterations = n; rerender();
        });

        Button reset = new Button("Reset");
        reset.setOnAction(_ -> { centerX = -0.5; centerY = 0; zoom = 1.0; rerender(); });

        Button save = new Button("💾 Speichern");
        save.setOnAction(_ -> saveCurrentImage());

        return new ToolBar(
                new Label("Farbschema:"), schemes,
                new Label("  Iterationen:"), iterSpinner,
                reset, save
        );
    }

    /// Speichert das aktuell angezeigte Bild als PNG
    private void saveCurrentImage() {
        if (pixels.length == 0 || canvas == null) return;
        var chooser = new FileChooser();
        chooser.setTitle("Mandelbrot speichern");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG-Bild", "*.png"));
        chooser.setInitialFileName("mandelbrot_%.6f_%.6f_z%.2fx.png"
                .formatted(centerX, centerY, zoom));
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            int w = (int) canvas.getWidth();
            int h = (int) canvas.getHeight();
            PngWriter.writeFile(pixels, w, h, file);
        } catch (RuntimeException ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Fehler beim Speichern: " + ex.getMessage()).showAndWait();
        }
    }

    private HBox buildStatusBar() {
        HBox box = new HBox(status);
        box.setPadding(new Insets(4, 8, 4, 8));
        HBox.setHgrow(status, Priority.ALWAYS);
        return box;
    }

    private void installMouseHandlers() {
        canvas.setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
            dragStartCenterX = centerX;
            dragStartCenterY = centerY;
        });
        canvas.setOnMouseDragged(e -> {
            // Zentrale Pan-Mathematik (RenderParams.withPanByPixels).
            var p = currentParams()
                    .withCenter(dragStartCenterX, dragStartCenterY)
                    .withPanByPixels(e.getX() - dragStartX, e.getY() - dragStartY);
            centerX = p.centerX();
            centerY = p.centerY();
            rerender();
        });
        canvas.setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 1.2 : 1 / 1.2;
            // Zentrale Zoom-am-Cursor-Mathematik (RenderParams.withZoomAt).
            var p = currentParams().withZoomAt((int) e.getX(), (int) e.getY(), factor);
            centerX = p.centerX();
            centerY = p.centerY();
            zoom    = p.zoom();
            rerender();
        });
        canvas.setOnMouseMoved(e -> updateStatus(e.getX(), e.getY()));
    }

    private double currentScale() {
        return currentParams().scale();
    }

    private RenderParams currentParams() {
        int w = (int) canvas.getWidth();
        int h = (int) canvas.getHeight();
        return new RenderParams(Math.max(1, w), Math.max(1, h),
                centerX, centerY, zoom, maxIterations, colorScheme);
    }

    private void updateStatus(double px, double py) {
        double scale = currentScale();
        double re = centerX + (px - canvas.getWidth() / 2) * scale;
        double im = centerY + (py - canvas.getHeight() / 2) * scale;
        status.setText("Re=%.6f  Im=%.6f  Zoom=%.2fx  Iter=%d"
                .formatted(re, im, zoom, maxIterations));
    }

    private void rerender() {
        int w = (int) canvas.getWidth();
        int h = (int) canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        if (pixels.length != w * h) {
            pixels = new int[w * h];
            IntBuffer buf = IntBuffer.wrap(pixels);
            pixelBuffer = new PixelBuffer<>(w, h, buf, PixelFormat.getIntArgbPreInstance());
            image = new WritableImage(pixelBuffer);
        }

        long renderId = currentRenderId.incrementAndGet();
        var params = new RenderParams(w, h, centerX, centerY, zoom, maxIterations, colorScheme);

        renderPool.submit(() -> {
            int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
            int strip   = Math.max(1, Math.ceilDiv(h, threads));
            var palette = colorScheme.generatePalette(maxIterations);

            // Phase 1: Coarse-Pass für sofortige Vorschau
            MandelbrotEngine.renderStripCoarse(pixels, params, palette, 0, h, 4,
                    () -> currentRenderId.get() != renderId);
            if (currentRenderId.get() != renderId) return;
            publish(renderId);

            // Phase 2: Voller Render parallel
            Thread[] workers = new Thread[threads];
            for (int t = 0; t < threads; t++) {
                int startY = t * strip;
                int endY   = (t == threads - 1) ? h : startY + strip;
                workers[t] = Thread.ofVirtual().start(() ->
                        MandelbrotEngine.renderStrip(pixels, params, palette,
                                startY, endY,
                                () -> currentRenderId.get() != renderId));
            }
            for (var w2 : workers) {
                try { w2.join(); } catch (InterruptedException _) {
                    Thread.currentThread().interrupt(); return;
                }
            }
            if (currentRenderId.get() != renderId) return;
            publish(renderId);
        });
    }

    private void publish(long renderId) {
        Platform.runLater(() -> {
            if (currentRenderId.get() != renderId) return;
            pixelBuffer.updateBuffer(_ -> null);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.drawImage(image, 0, 0);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}

