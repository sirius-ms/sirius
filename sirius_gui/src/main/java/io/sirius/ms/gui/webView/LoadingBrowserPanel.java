package io.sirius.ms.gui.webView;

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * Browser panel that stands in for the web view it shows while that view is being created.
 * <p>
 * Creating a web view starts a browser and loads a web application, which takes seconds. This panel
 * exists immediately, shows a loading animation and creates the real web view in the background, so
 * the event dispatch thread stays responsive and the user sees something right away.
 * <p>
 * It is a {@link BrowserPanel} itself and forwards to the created one, so users of a web view do not
 * have to care whether it is there yet. Data updates submitted too early are applied as soon as it
 * is, see {@link #submitDataUpdate(String)}.
 */
@Slf4j
public class LoadingBrowserPanel extends BrowserPanel implements Loadable {

    private final LoadablePanel loadablePanel;

    /**
     * Holds the web view once it has been created. Shown by {@link #loadablePanel} instead of the
     * loading animation from then on.
     */
    private final JPanel contentContainer;

    private final Object lock = new Object();

    /**
     * The web view this panel stands in for, {@code null} until it has been created. Guarded by
     * {@link #lock}, since it is written on the EDT but read from wherever a data update comes from.
     */
    private @Nullable BrowserPanel delegate;

    /**
     * Whether the browser resources have been released already. A web view that is created after
     * that is released right away, since nothing else would ever do it. Guarded by {@link #lock}.
     */
    private boolean released = false;

    /**
     * Data update that was submitted before the web view was there. Only the most recent one is
     * kept: the same reasoning as for the replaceable updates of a live web view, where a newer
     * update makes an older one pointless. Guarded by {@link #lock}.
     */
    private @Nullable String pendingDataUpdate;

    /**
     * @param webViewFactory creates the web view to show. Called on a background thread, so it may
     *                       block for as long as the web view needs to start.
     */
    public LoadingBrowserPanel(@NotNull Supplier<? extends BrowserPanel> webViewFactory) {
        setLayout(new BorderLayout());

        contentContainer = new JPanel(new BorderLayout());
        loadablePanel = new LoadablePanel(contentContainer);
        loadablePanel.setLoading(true, true); //the web view is never there before this panel is shown
        add(loadablePanel, BorderLayout.CENTER);

        load(webViewFactory);
    }

    private void load(@NotNull Supplier<? extends BrowserPanel> webViewFactory) {
        Jobs.runInBackground(() -> {
            final BrowserPanel webView;
            try {
                webView = webViewFactory.get();
            } catch (Exception e) {
                log.error("Error when creating web view.", e);
                Jobs.runEDTLater(this::showError);
                return;
            }
            Jobs.runEDTLater(() -> showWebView(webView));
        });
    }

    /**
     * Shows the created web view and applies the data update it may have missed. If this panel has
     * been cleaned up while the web view was still being created, the web view is closed instead: it
     * never became part of a window, so its {@link #removeNotify()} would never release it.
     */
    private void showWebView(@NotNull BrowserPanel webView) {
        final String missedDataUpdate;
        synchronized (lock) {
            if (released) {
                webView.cleanupResources();
                return;
            }
            delegate = webView;
            missedDataUpdate = pendingDataUpdate;
            pendingDataUpdate = null;
        }

        contentContainer.add(webView, BorderLayout.CENTER);
        setLoading(false, true);

        if (missedDataUpdate != null)
            webView.submitDataUpdate(missedDataUpdate);
    }

    /**
     * Replaces the loading animation with a message, so a web view that cannot be created does not
     * leave the user waiting forever. Details are in the log.
     */
    private void showError() {
        JLabel message = new JLabel("Could not load this view. See log for details.", SwingConstants.CENTER);
        contentContainer.add(message, BorderLayout.CENTER);
        setLoading(false, true);
    }

    /**
     * Submits the given data update to the web view, or applies it as soon as the web view is there.
     * A later update replaces one that is still waiting, since it makes it pointless.
     */
    @Override
    public void submitDataUpdate(String javascript) {
        final BrowserPanel webView;
        synchronized (lock) {
            webView = delegate;
            if (webView == null) {
                pendingDataUpdate = javascript;
                return;
            }
        }
        webView.submitDataUpdate(javascript);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code null} if the web view is not there yet, since there is nothing to execute the
     * javascript on and this call cannot wait for it without blocking its caller.
     */
    @Override
    public <T> T executeJavaScript(String javascript) {
        final BrowserPanel webView;
        synchronized (lock) {
            webView = delegate;
        }
        if (webView == null) {
            log.warn("Cannot execute javascript before the web view has been created. Ignoring: {}", javascript);
            return null;
        }
        return webView.executeJavaScript(javascript);
    }

    @Override
    public void cleanupResources() {
        final BrowserPanel webView;
        synchronized (lock) {
            released = true;
            webView = delegate;
        }
        if (webView != null)
            webView.cleanupResources();
    }

    @Override
    public void showDevTools() {
        final BrowserPanel webView;
        synchronized (lock) {
            webView = delegate;
        }
        if (webView != null)
            webView.showDevTools();
    }

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
        return loadablePanel.setLoading(loading, absolute);
    }
}
