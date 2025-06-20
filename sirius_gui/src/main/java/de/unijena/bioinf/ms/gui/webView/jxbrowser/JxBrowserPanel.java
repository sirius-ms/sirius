package de.unijena.bioinf.ms.gui.webView.jxbrowser;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.navigation.callback.StartNavigationCallback;
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished;
import com.teamdev.jxbrowser.view.swing.BrowserView;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.webView.BrowserPanel;
import de.unijena.bioinf.ms.gui.webView.LinkInterception;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.net.URI;

@Slf4j
public class JxBrowserPanel extends BrowserPanel {

    private final @NotNull Browser browser;

    public JxBrowserPanel(String url, @NotNull Browser browser, LinkInterception linkInterception) {
        super();
        setLayout(new BorderLayout());
        this.browser = browser;
        this.linkInterception = linkInterception;
        initialize(url);
    }

    private void initialize(@NotNull String url) {
        setupLinkInterception();
        setupLoadingListener();
        add(BrowserView.newInstance(browser), BorderLayout.CENTER);
        browser.navigation().loadUrlAndWait(url);
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
        if (browser != null && !browser.isClosed())
            browser.close();
    }
}
