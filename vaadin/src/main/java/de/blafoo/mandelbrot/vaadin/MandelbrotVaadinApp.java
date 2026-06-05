package de.blafoo.mandelbrot.vaadin;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@StyleSheet(Aura.STYLESHEET)
public class MandelbrotVaadinApp implements AppShellConfigurator {

    static void main(String[] args) {
        SpringApplication.run(MandelbrotVaadinApp.class, args);
    }
}

