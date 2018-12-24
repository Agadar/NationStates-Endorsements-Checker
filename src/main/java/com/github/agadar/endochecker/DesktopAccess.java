package com.github.agadar.endochecker;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Assists in using OS-specific functionalities.
 * 
 * @author Agadar (https://github.com/Agadar/)
 *
 */
public class DesktopAccess {

    private final Optional<Desktop> desktopOptional;

    public DesktopAccess() {
        if (Desktop.isDesktopSupported()) {
            desktopOptional = Optional.of(Desktop.getDesktop());
        } else {
            desktopOptional = Optional.empty();
        }
    }

    public boolean isBrowserSupported() {
        return desktopOptional.map(desktop -> desktop.isSupported(Desktop.Action.BROWSE)).orElse(false);
    }

    public void openUrlInBrowser(String url) {
        try {
            URI uri = new URI(url);
            desktopOptional.orElseThrow(() -> new IllegalStateException("Browser not supported!")).browse(uri);
        } catch (URISyntaxException | IllegalStateException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
