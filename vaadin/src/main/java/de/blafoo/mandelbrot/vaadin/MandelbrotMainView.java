package de.blafoo.mandelbrot.vaadin;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import de.blafoo.mandelbrot.core.ColorScheme;
import de.blafoo.mandelbrot.core.RenderParams;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.UUID;


/// Bedienung (analog zu Desktop / JavaFX / Spring-Boot-Web-UI):
/// * **Linksklick** auf das Bild → 2× hineinzoomen am Klickpunkt
/// * **Rechtsklick** auf das Bild → 2× herauszoomen am Klickpunkt
/// * **Mausrad** über dem Bild → Zoom in/out am Cursor (1,25×)
/// * **Shift + Drag** → Bildausschnitt verschieben (Pan)
/// * Buttons → herauszoomen, schwenken, zurücksetzen
/// * ComboBox → Farbschema wechseln
/// * IntegerField → maximale Iterationen anpassen
@Route("")
@PageTitle("Mandelbrot Viewer · Vaadin")
public class MandelbrotMainView extends VerticalLayout {

    private static final int IMG_WIDTH = 800;
    private static final int IMG_HEIGHT = 600;

    private final MandelbrotRenderService renderService;

    private double centerX = -0.5;
    private double centerY = 0.0;
    private double zoom = 1.0;
    private int maxIterations = 300;
    private ColorScheme colorScheme = ColorScheme.defaultScheme();

    private final Image image = new Image();
    private final Span status = new Span();

    /// Letzter gerenderter PNG-Inhalt – für den Save-Button.
    private volatile byte @Nullable [] lastPng;

    public MandelbrotMainView(MandelbrotRenderService renderService) {
        this.renderService = renderService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(FlexComponent.Alignment.CENTER);

        add(new H2("Mandelbrot Viewer · Vaadin"));
        add(buildToolbar());
        add(buildImagePanel());
        add(status);

        refresh();
    }

    private HorizontalLayout buildToolbar() {
        var schemeBox = new ComboBox<ColorScheme>("Farbschema");
        schemeBox.setItems(ColorScheme.all());
        schemeBox.setItemLabelGenerator(ColorScheme::displayName);
        schemeBox.setValue(colorScheme);
        schemeBox.setAllowCustomValue(false);
        schemeBox.addValueChangeListener(e -> {
            colorScheme = e.getValue();
            refresh();
        });

        var iterField = new IntegerField("max. Iterationen");
        iterField.setValue(maxIterations);
        iterField.setMin(50);
        iterField.setMax(5000);
        iterField.setStep(50);
        iterField.setStepButtonsVisible(true);
        iterField.addValueChangeListener(e -> {
            Integer v = e.getValue();
            if (v != null) {
                // Java 21+: Math.clamp ersetzt eigenen Bereichscheck
                maxIterations = Math.clamp(v, 50, 5000);
                refresh();
            }
        });

        var zoomOut = new Button("Zoom −", VaadinIcon.MINUS.create(), _ -> {
            zoom /= 2.0;
            refresh();
        });
        var panLeft  = new Button(VaadinIcon.ARROW_LEFT.create(),  _ -> { centerX -= 0.5 / zoom; refresh(); });
        var panRight = new Button(VaadinIcon.ARROW_RIGHT.create(), _ -> { centerX += 0.5 / zoom; refresh(); });
        var panUp    = new Button(VaadinIcon.ARROW_UP.create(),    _ -> { centerY -= 0.5 / zoom; refresh(); });
        var panDown  = new Button(VaadinIcon.ARROW_DOWN.create(),  _ -> { centerY += 0.5 / zoom; refresh(); });

        var reset = new Button("Reset", VaadinIcon.REFRESH.create(), _ -> {
            centerX = -0.5;
            centerY = 0.0;
            zoom = 1.0;
            maxIterations = 300;
            iterField.setValue(maxIterations);
            refresh();
        });
        reset.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Save-Button: liefert das aktuell angezeigte PNG als Download.
        // DownloadHandler (Vaadin 24.8+) liefert pro Klick ein frisches PNG mit
        // sinnvollem Dateinamen wie "mandelbrot_-0.745000_0.110000_z4.00x.png".
        var saveButton = new Button("Speichern", VaadinIcon.DOWNLOAD.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        var saveAnchor = new Anchor();
        saveAnchor.setHref(buildSaveDownloadHandler());
        saveAnchor.add(saveButton);
        saveAnchor.setTarget(AnchorTarget.BLANK);

        var toolbar = new HorizontalLayout(schemeBox, iterField,
                zoomOut, panLeft, panRight, panUp, panDown, reset, saveAnchor);
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.setSpacing(true);
        return toolbar;
    }

    /// Liefert bei jedem Browser-Request das **aktuell** angezeigte PNG.
    /// `lastPng` wird bei jedem `refresh()` aktualisiert; ein noch nicht
    /// gerendertes Bild liefert HTTP 404.
    private DownloadHandler buildSaveDownloadHandler() {
        return DownloadHandler.fromInputStream(_ -> {
            byte[] png = lastPng;
            if (png == null) {
                return DownloadResponse.error(404, "Noch kein Bild gerendert");
            }
            var name = "mandelbrot_%.6f_%.6f_z%.2fx.png".formatted(centerX, centerY, zoom);
            return new DownloadResponse(new ByteArrayInputStream(png),
                    name, "image/png", png.length);
        });
    }

    private Image buildImagePanel() {
        image.setWidth(IMG_WIDTH + "px");
        image.setHeight(IMG_HEIGHT + "px");
        image.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("cursor", "crosshair")
                .set("background", "#000");
        image.setAlt("Mandelbrot-Menge");
        // Browser-eigenes Bild-Ziehen unterdrücken (sonst kollidiert es mit Shift+Drag)
        image.getElement().setAttribute("draggable", "false");
        return image;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Native DOM-Listener registrieren – alle Maus-Interaktionen rufen
        // @ClientCallable-Methoden auf der View auf.
        //   - Linksklick           → zoomAt(2.0)
        //   - Rechtsklick          → zoomAt(0.5) + preventDefault
        //   - Mausrad              → zoomAt(1.25 bzw. 0.8) + preventDefault
        //   - Shift + Drag (links) → panBy(dx, dy) beim Loslassen
        // `this.$server` referenziert die View, weil executeJs auf getElement() läuft.
        getElement().executeJs("""
                const img = $0;
                const srv = this.$server;
                img.addEventListener('click', e => {
                    srv.zoomAt(e.offsetX, e.offsetY, 2.0);
                });
                img.addEventListener('contextmenu', e => {
                    e.preventDefault();
                    srv.zoomAt(e.offsetX, e.offsetY, 0.5);
                });
                img.addEventListener('wheel', e => {
                    e.preventDefault();
                    const r = img.getBoundingClientRect();
                    const ox = e.clientX - r.left;
                    const oy = e.clientY - r.top;
                    srv.zoomAt(ox, oy, e.deltaY < 0 ? 1.25 : 0.8);
                }, { passive: false });
                let drag = null;
                img.addEventListener('mousedown', e => {
                    if (e.button !== 0 || !e.shiftKey) return;
                    drag = { x: e.clientX, y: e.clientY, dx: 0, dy: 0 };
                    e.preventDefault();
                });
                window.addEventListener('mousemove', e => {
                    if (!drag) return;
                    drag.dx = e.clientX - drag.x;
                    drag.dy = e.clientY - drag.y;
                });
                window.addEventListener('mouseup', () => {
                    if (!drag) return;
                    if (drag.dx !== 0 || drag.dy !== 0) {
                        srv.panBy(drag.dx, drag.dy);
                    }
                    drag = null;
                });
                img.addEventListener('dragstart', e => e.preventDefault());
                """, image.getElement());
    }

    /// Vom Browser aufgerufener Server-Callback: zoomt um `factor` am Bildpunkt
    /// `(px, py)` (CSS-Pixel relativ zum Bild).
    @ClientCallable
    public void zoomAt(double px, double py, double factor) {
        zoomAtPixel((int) Math.round(px), (int) Math.round(py), factor);
    }

    /// Vom Browser aufgerufener Server-Callback: verschiebt den Bildausschnitt
    /// um `(dxPx, dyPx)` CSS-Pixel (positive Werte → Bild zieht nach rechts/unten).
    @ClientCallable
    public void panBy(double dxPx, double dyPx) {
        var p = currentParams().withPanByPixels(dxPx, dyPx);
        centerX = p.centerX();
        centerY = p.centerY();
        refresh();
    }

    private void zoomAtPixel(int px, int py, double factor) {
        // Zentrale Zoom-Mathematik aus dem Core-Modul (RenderParams.withZoomAt),
        // damit alle UI-Module dieselbe Formel nutzen.
        var p = currentParams().withZoomAt(px, py, factor);
        centerX = p.centerX();
        centerY = p.centerY();
        zoom    = p.zoom();
        refresh();
    }

    private RenderParams currentParams() {
        return new RenderParams(IMG_WIDTH, IMG_HEIGHT,
                centerX, centerY, zoom, maxIterations, colorScheme);
    }

    private void refresh() {
        long t0 = System.nanoTime();
        var params = currentParams();
        byte[] png;
        try {
            png = renderService.renderPng(params);
        } catch (RuntimeException ex) {
            Notification.show("Render-Fehler: " + ex.getMessage());
            return;
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        // Für den Save-Button zwischenspeichern.
        lastPng = png;

        // DownloadHandler (Vaadin 24.8+) ersetzt das veraltete StreamResource.
        // Eindeutiger Dateiname pro Render erzwingt Browser-Reload des Bildes.
        var name = "mandelbrot-" + UUID.randomUUID() + ".png";
        var handler = DownloadHandler.fromInputStream(_ ->
                new DownloadResponse(new ByteArrayInputStream(png),
                        name, "image/png", png.length));
        image.setSrc(handler);

        status.setText("Center=(%.6f, %.6f)  Zoom=%.2fx  Iter=%d  Schema=%s  Render=%d ms"
                .formatted(centerX, centerY, zoom, maxIterations,
                        colorScheme.displayName(), ms));
    }
}




