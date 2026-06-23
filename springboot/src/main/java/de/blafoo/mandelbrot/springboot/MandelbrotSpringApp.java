package de.blafoo.mandelbrot.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring-Boot-4-Anwendung, die die Mandelbrot-Menge als
 * Web-Service (PNG-Endpoint) und als interaktive HTML-Seite ausliefert.
 *
 * Startbar via:
 * <pre>
 *   mvn -pl springboot spring-boot:run
 * </pre>
 * oder
 * <pre>
 *   java -jar springboot/target/mandelbrot-springboot-1.0-SNAPSHOT.jar
 * </pre>
 *
 * UI:        http://localhost:8080/
 * Bild-API:  http://localhost:8080/api/mandelbrot.png?width=800&amp;height=600&amp;centerX=-0.5&amp;centerY=0&amp;zoom=1
 * Health:    http://localhost:8080/actuator/health
 */
@SpringBootApplication
public class MandelbrotSpringApp {

    void main(String[] args) {
        SpringApplication.run(MandelbrotSpringApp.class, args);
    }
}

