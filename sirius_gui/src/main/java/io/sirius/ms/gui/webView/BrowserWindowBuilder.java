package io.sirius.ms.gui.webView;

import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Creates a window showing a web view, so that opening one does not need an own window class.
 * <p>
 * Obtained from {@link BrowserPanelProvider}, which decides the url and whether a loading animation
 * makes sense for the content:
 * <pre>{@code
 * provider.newReactWindow("/reactionTool", "customdb", databaseId)
 *         .title("Transformation products: " + database.getDisplayName())
 *         .frame()
 *         .owner(parentWindow)
 *         .size(GuiUtils.LARGE_CONTENT_SIZE)
 *         .singleton(new MyKey(databaseId))
 *         .onClosed(() -> reloadDatabases())
 *         .show();
 * }</pre>
 */
public class BrowserWindowBuilder {

    private final BrowserPanelProvider<?> provider;
    private final String url;
    private final LinkInterception linkInterception;

    private boolean loading;
    private String title = "SIRIUS";
    private @Nullable Window owner = null;
    private boolean asFrame = false;
    private @Nullable Dialog.ModalityType modality = null;
    private @Nullable Dimension size = null;
    private boolean resizable = true;
    private @Nullable Object singletonKey = null;
    private @Nullable Runnable onClosed = null;
    private boolean closeOnEscape = true;

    BrowserWindowBuilder(@NotNull BrowserPanelProvider<?> provider, @NotNull String url,
                         @NotNull LinkInterception linkInterception, boolean loading) {
        this.provider = provider;
        this.url = url;
        this.linkInterception = linkInterception;
        this.loading = loading;
    }

    /**
     * Title of the window. Defaults to the application name.
     */
    public BrowserWindowBuilder title(@NotNull String title) {
        this.title = title;
        return this;
    }

    /**
     * Window to center the new window on, and to own it if it is a dialog. May be null, e.g. if
     * there is no window the view belongs to.
     */
    public BrowserWindowBuilder owner(@Nullable Window owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Shows the view in a frame: an independent window with an own entry in the task bar that does
     * not stay in front of its owner. Mutually exclusive with {@link #modality(Dialog.ModalityType)}.
     */
    public BrowserWindowBuilder frame() {
        this.asFrame = true;
        return this;
    }

    /**
     * Shows the view in a dialog with the given modality. Dialogs are modeless by default, since a
     * web view the user works with should not block the rest of the application.
     * <p>
     * Note that {@link #show()} does not return before the window is closed again if the dialog is
     * modal.
     */
    public BrowserWindowBuilder modality(@NotNull Dialog.ModalityType modality) {
        this.modality = modality;
        return this;
    }

    /**
     * Size of the view. A web view has no meaningful size of its own, so this is what defines the
     * size of the window. Without it the window falls back to the default size of a web view.
     */
    public BrowserWindowBuilder size(@NotNull Dimension size) {
        this.size = size;
        return this;
    }

    public BrowserWindowBuilder resizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    /**
     * Makes this window the only one for the given key: showing it again while it is open raises the
     * open window instead of opening a second copy of the same content.
     * <p>
     * The key is also how a window is found again by whoever invalidates what it shows, see
     * {@link BrowserWindowRegistry#dispose(java.util.function.Predicate)}. It needs to be a value
     * object, so a record of what the view shows is a good key.
     */
    public BrowserWindowBuilder singleton(@NotNull Object key) {
        this.singletonKey = key;
        return this;
    }

    /**
     * Called when the window is closed, e.g. because the view may have changed something that has to
     * be reloaded elsewhere.
     * <p>
     * Belongs to the window and is therefore only used when one is actually created: showing an open
     * {@link #singleton(Object)} again keeps the action the window was opened with, instead of
     * stacking one action per time the window was shown.
     */
    public BrowserWindowBuilder onClosed(@Nullable Runnable onClosed) {
        this.onClosed = onClosed;
        return this;
    }

    /**
     * Whether pressing escape closes the window, which it does by default.
     */
    public BrowserWindowBuilder closeOnEscape(boolean closeOnEscape) {
        this.closeOnEscape = closeOnEscape;
        return this;
    }

    /**
     * Overrides whether the window shows a loading animation until its web view is ready. The
     * default depends on the content, see {@link BrowserPanelProvider}.
     */
    public BrowserWindowBuilder loading(boolean loading) {
        this.loading = loading;
        return this;
    }

    /**
     * Creates and shows the window. To be called on the event dispatch thread, since it creates and
     * shows swing components.
     * <p>
     * Does not return before the window is closed again if it is a modal dialog.
     *
     * @return the new window, or the already open one if it is a {@link #singleton(Object)} that is
     * open. An already open window is raised and keeps the settings it was opened with, so only
     * {@link #title(String)}, {@link #size(Dimension)} and the like of the first call are used.
     */
    public BrowserWindow show() {
        if (asFrame && modality != null)
            throw new IllegalStateException("A frame cannot be modal. Use either frame() or modality().");

        if (singletonKey != null) {
            BrowserWindow open = BrowserWindowRegistry.get(singletonKey);
            if (open != null) {
                open.toFrontAndFocus();
                return open;
            }
        }

        BrowserPanel browserPanel = loading
                ? provider.newLoadingBrowserPanel(url, linkInterception)
                : provider.newBrowserPanel(url, linkInterception);

        //the window is sized by its content, and a web view has no meaningful size of its own
        if (size != null)
            browserPanel.setPreferredSize(size);

        final BrowserWindow browserWindow = asFrame
                ? new BrowserFrame(title, browserPanel)
                : new BrowserDialog(owner, title, modality == null ? Dialog.ModalityType.MODELESS : modality, browserPanel);
        final Window window = browserWindow.window();

        if (window instanceof Frame frame)
            frame.setResizable(resizable);
        else if (window instanceof Dialog dialog)
            dialog.setResizable(resizable);

        if (closeOnEscape && window instanceof RootPaneContainer rootPaneContainer)
            GuiUtils.closeOnEscape(window, rootPaneContainer.getRootPane());

        if (singletonKey != null || onClosed != null) {
            final Object key = singletonKey;
            final Runnable closed = onClosed;
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    if (key != null)
                        BrowserWindowRegistry.unregister(key, browserWindow);
                    if (closed != null)
                        closed.run();
                }
            });
        }

        //registered before the window is shown, so that showing the same content again while its web
        //view is still loading raises this window instead of creating a second one
        if (singletonKey != null)
            BrowserWindowRegistry.register(singletonKey, browserWindow);

        GuiUtils.packWithinUsableScreen(window);
        window.setLocationRelativeTo(owner);
        window.setVisible(true);

        return browserWindow;
    }
}
