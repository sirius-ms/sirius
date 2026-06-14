package io.sirius.ms.gui.webView.jxbrowser;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.callback.CreatePopupCallback;
import com.teamdev.jxbrowser.browser.event.BrowserClosing;
import com.teamdev.jxbrowser.browser.event.ConsoleMessageReceived;
import com.teamdev.jxbrowser.browser.event.TitleChanged;
import com.teamdev.jxbrowser.js.ConsoleMessage;
import com.teamdev.jxbrowser.navigation.callback.StartNavigationCallback;
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished;
import com.teamdev.jxbrowser.view.swing.BrowserView;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.sirius.ms.gui.webView.BrowserPanel;
import io.sirius.ms.gui.webView.LinkInterception;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JxBrowserPanel extends BrowserPanel {

    private final @NotNull Browser browser;

    // Top-level windows hosting JS popups (window.open) spawned from this browser.
    // Tracked so they can be disposed when this panel is cleaned up. Only touched on the EDT.
    private final List<Popup> popups = new ArrayList<>();

    public JxBrowserPanel(String url, @NotNull Browser browser, LinkInterception linkInterception) {
        super();
        setLayout(new BorderLayout());
        this.browser = browser;
        this.linkInterception = linkInterception;
        initialize(url);
        addDevToolsKeybinding();
    }

    private void initialize(@NotNull String url) {
        setupLinkInterception();
        setupPopupHandling();
        setupLoadingListener();
        setupConsoleListener();
        add(BrowserView.newInstance(browser), BorderLayout.CENTER);
        browser.navigation().loadUrlAndWait(url);
    }

    private static final int POPUP_DEFAULT_WIDTH = 1280;
    private static final int POPUP_DEFAULT_HEIGHT = 800;

    /**
     * Hosts JavaScript popups (e.g. {@code window.open(...)}) in their own Swing window.
     * <p>
     * This panel is often shown inside an application-modal {@link java.awt.Dialog} (e.g. the
     * custom-database transformation dialog). A modal dialog blocks every other window in the app
     * <em>except those it owns</em>, so a plain top-level popup window flashes up and is then forced
     * behind the dialog and cannot be focused or raised - not even from the taskbar. We therefore
     * suppress JxBrowser's own popup and load the target URL into a non-modal window <em>owned by
     * this panel's window ancestor</em>, which exempts it from the modal block.
     */
    private void setupPopupHandling() {
        browser.set(CreatePopupCallback.class, params -> {
            final String targetUrl = params.targetUrl();
            if (targetUrl != null && !targetUrl.isBlank() && !"about:blank".equals(targetUrl)) {
                final String targetName = params.targetName();
                SwingUtilities.invokeLater(() -> openOrFocusPopupWindow(targetName, targetUrl));
                return CreatePopupCallback.Response.suppress();
            }
            // Without a concrete URL we cannot recreate the popup ourselves; fall back to default.
            return CreatePopupCallback.Response.create();
        });
    }

    private void openOrFocusPopupWindow(String name, @NotNull String url) {
        // A named target reuses its window (like the browser's window.open semantics): if one is
        // already open, navigate it to the new URL and raise it instead of stacking duplicates.
        if (name != null && !name.isBlank()) {
            for (Popup existing : popups) {
                if (name.equals(existing.name())) {
                    if (!url.equals(existing.browser().url()))
                        existing.browser().navigation().loadUrl(url);
                    bringToFront(existing.frame());
                    return;
                }
            }
        }
        openPopupWindow(name, url);
    }

    private void openPopupWindow(String name, @NotNull String url) {
        final Browser popupBrowser = browser.engine().newBrowser();

        // Own the popup with this panel's window ancestor. When that ancestor is an application-modal
        // dialog, an owned (and itself non-modal) window is exempt from the modal block and can be
        // focused and raised; a plain top-level window would be blocked behind the dialog.
        final Window owner = SwingUtilities.getWindowAncestor(this);
        final JDialog popupDialog = new JDialog(owner);
        popupDialog.setModalityType(Dialog.ModalityType.MODELESS);
        popupDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        popupDialog.add(BrowserView.newInstance(popupBrowser), BorderLayout.CENTER);
        popupDialog.setSize(POPUP_DEFAULT_WIDTH, POPUP_DEFAULT_HEIGHT);
        popupDialog.setLocationRelativeTo(owner);

        applyPopupTitle(popupDialog, popupBrowser.title());
        popupBrowser.on(TitleChanged.class, e -> {
            final String title = e.title();
            SwingUtilities.invokeLater(() -> applyPopupTitle(popupDialog, title));
        });

        final Popup popup = new Popup(name, popupDialog, popupBrowser);
        popups.add(popup);

        // The page closed itself (window.close()) -> dispose the window.
        popupBrowser.on(BrowserClosing.class, e -> SwingUtilities.invokeLater(() -> {
            popups.remove(popup);
            popupDialog.dispose();
        }));
        // The user closed the window -> close the underlying popup browser.
        popupDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                popups.remove(popup);
                if (!popupBrowser.isClosed())
                    popupBrowser.close();
            }
        });

        popupBrowser.navigation().loadUrl(url);

        popupDialog.setVisible(true);
        bringToFront(popupDialog);
    }

    private static void bringToFront(@NotNull Window window) {
        window.toFront();
        window.requestFocus();
    }

    private static void applyPopupTitle(@NotNull JDialog dialog, String title) {
        dialog.setTitle(title == null || title.isBlank() ? "SIRIUS" : title);
    }

    private record Popup(String name, @NotNull JDialog frame, @NotNull Browser browser) {
    }

    private void setupConsoleListener() {
        browser.on(ConsoleMessageReceived.class, event -> {
            ConsoleMessage consoleMessage = event.consoleMessage();
            switch (consoleMessage.level()) {
                case DEBUG -> log.debug(consoleMessage.message());
                case WARNING -> log.warn(consoleMessage.message());
                case LEVEL_ERROR -> log.error(consoleMessage.message());
                default -> log.info(consoleMessage.message());
            }
        });
    }

    public void setupLoadingListener() {
        browser.navigation().on(FrameLoadFinished.class, event -> {
            if (event.frame().isMain()) {
                if (!event.navigation().isLoading()) {
                    executeReplaceableDataUpdate();
                }
            }
        });
    }

    private void setupLinkInterception() {
        // Set the StartNavigationCallback to intercept all navigations
        browser.navigation().set(StartNavigationCallback.class, params -> {
            // The isUserGesture() method checks if the navigation was initiated
            // by a user action, such as a mouse click or a key press.

            //todo verify user interaction needed?
//            if (params.isUserGesture()) {
            if (params.isMainFrame()) {
                String url = params.url();

                // Ignore about:blank and similar
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    // Open URL in system browser
                    try {
                        URI targetURL = URI.create(url);
                        URI currentURL = browser.url() != null ? URI.create(browser.url()) : null;
                        if (linkInterception == LinkInterception.ALL || currentURL == null || !currentURL.getHost().equals(targetURL.getHost())) {
                            GuiUtils.openURL(targetURL, null, true);
                            // Ignore the navigation in JxBrowser
                            return StartNavigationCallback.Response.ignore();
                        }
                    } catch (Exception e) {
                        log.error("Error when loading external link!", e);
                    }
                }
            }
            // Allow all other navigations (e.g., initial page load, programmatic navigation)
            return StartNavigationCallback.Response.start();
        });
    }

    public <T> T executeJavaScript(String javascript) {
        return browser.mainFrame().map(frame -> (T) frame.executeJavaScript(javascript)).orElse(null);
    }

    // we use replaceable calls to ensure that during fast selection changes,
    // we do not stack data loading tasks in the browser process.
    private final Object dataUpdateLock = new Object();
    private Runnable replaceableDataUpdate = null;


    public void executeReplaceableDataUpdate() {
        synchronized (dataUpdateLock) {
            if (replaceableDataUpdate != null) {
                replaceableDataUpdate.run();
                replaceableDataUpdate = null;
            }
        }
    }

    public void submitReplaceableDataUpdate(String javascript) {
        synchronized (dataUpdateLock) {
            replaceableDataUpdate = () -> executeJavaScript(javascript);
            if (!browser.navigation().isLoading())
                executeReplaceableDataUpdate();
        }
    }

    @Override
    public void submitDataUpdate(String javascript) {
        submitReplaceableDataUpdate(javascript);
    }

    @Override
    public void cleanupResources() {
        // Close popup windows (and their browsers) opened from this panel before closing ourselves.
        for (Popup popup : new ArrayList<>(popups)) {
            if (!popup.browser().isClosed())
                popup.browser().close();
            popup.frame().dispose();
        }
        popups.clear();

        if (browser != null && !browser.isClosed())
            browser.close();
    }

    @Override
    public void showDevTools() {
        browser.devTools().show();
    }

    private void addDevToolsKeybinding() {
        if (PropertyManager.getBoolean("io.sirius-ms.browser.devtools.allow", false)) {
            // Create the action to be performed
            Action f12Action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showDevTools();
                }
            };

            // Get the InputMap and ActionMap for the panel
            InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = getActionMap();

            // Define a name for the action
            String actionKey = "doF12Action";

            // Link the KeyStroke to the action name in the InputMap
            inputMap.put(KeyStroke.getKeyStroke("F12"), actionKey);

            // Link the action name to the actual Action in the ActionMap
            actionMap.put(actionKey, f12Action);
        }
    }
}
