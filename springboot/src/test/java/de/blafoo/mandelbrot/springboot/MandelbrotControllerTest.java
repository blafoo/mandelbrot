package de.blafoo.mandelbrot.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Direkter Controller-Test ohne Spring-Kontext: stellt sicher,
 * dass der Mandelbrot-Renderer ein gültiges PNG liefert.
 */
class MandelbrotControllerTest {

    private final MandelbrotController controller = new MandelbrotController();

    @Test
    void schemesNotEmpty() {
        var schemes = controller.schemes();
        assertThat(schemes).isNotEmpty();
        assertThat(schemes).allMatch(m -> m.containsKey("name"));
    }

    @Test
    void rendersValidPng() {
        ResponseEntity<byte[]> resp = controller.render(64, 48, -0.5, 0.0, 1.0, 50, "Classic", false);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        byte[] bytes = resp.getBody();
        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(8);
        // PNG-Magic 0x89 50 4E 47
        assertThat(bytes[0] & 0xFF).isEqualTo(0x89);
        assertThat(bytes[1]).isEqualTo((byte) 0x50);
        assertThat(bytes[2]).isEqualTo((byte) 0x4E);
        assertThat(bytes[3]).isEqualTo((byte) 0x47);
    }

    @Test
    void unknownSchemeFallsBackToDefault() {
        ResponseEntity<byte[]> resp = controller.render(32, 32, -0.5, 0, 1, 50, "DoesNotExist", false);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotEmpty();
    }

    @Test
    void downloadAddsContentDispositionHeader() {
        ResponseEntity<byte[]> resp = controller.render(32, 32, -0.5, 0, 1, 50, "Classic", true);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getHeaders().getFirst("Content-Disposition"))
                .isNotNull()
                .startsWith("attachment;")
                .contains(".png");
    }
}



