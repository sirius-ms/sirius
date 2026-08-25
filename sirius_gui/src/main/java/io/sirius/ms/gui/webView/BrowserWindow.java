package io.sirius.ms.gui.webView;

import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * A window showing a web view, created by a {@link BrowserWindowBuilder}.
 * <p>
 * A window is either a frame or a dialog, which have no common base beyond {@link Window}, so this
 * is what the two have in common from the outside.
 */
public interface BrowserWindow {

    /**
     * The window itself, a {@link javax.swing.JFrame} or a {@link javax.swing.JDialog}.
     */
    @NotNull
    Window window();

    /**
     * The web view shown in this window, e.g. to push data updates to it. Note that it may still be
     * loading, see {@link LoadingBrowserPanel}.
     */
    @NotNull
    BrowserPanel browserPanel();

    /**
     * Raises this window and gives it the focus.
     */
    default void toFrontAndFocus() {
        window().toFront();
        window().requestFocus();
    }

    /**
     * Closes this window. Disposing releases the browser resources of the web view, so a window is
     * disposed instead of hidden and showing the view again creates a new window.
     */
    default void dispose() {
        window().dispose();
    }
}
