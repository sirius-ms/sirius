package io.sirius.ms.gui.webView;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests how a {@link LoadingBrowserPanel} bridges the time until its web view has been created. Uses
 * a web view that only records what it was asked to do, so no browser is needed.
 */
class LoadingBrowserPanelTest {

    private static final long TIMEOUT_MS = 15_000;

    /**
     * Stands in for a created web view.
     */
    private static class FakeWebView extends BrowserPanel {
        private final List<String> dataUpdates = Collections.synchronizedList(new ArrayList<>());
        private volatile boolean cleanedUp = false;

        @Override
        public void submitDataUpdate(String javascript) {
            dataUpdates.add(javascript);
        }

        @Override
        public <T> T executeJavaScript(String javascript) {
            return null;
        }

        @Override
        public void cleanupResources() {
            cleanedUp = true;
        }

        @Override
        public void showDevTools() {
        }
    }

    private static void awaitCondition(String what, BooleanSupplier condition) throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                //let the pending swing work finish, so the panel is in its final state
                SwingUtilities.invokeAndWait(() -> {});
                return;
            }
            Thread.sleep(20);
        }
        fail("Timed out waiting until " + what);
    }

    private static boolean containsErrorMessage(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && label.getText() != null && label.getText().startsWith("Could not load"))
                return true;
            if (component instanceof Container child && containsErrorMessage(child))
                return true;
        }
        return false;
    }

    @Test
    void appliesTheLatestUpdateThatWasSubmittedWhileTheWebViewWasStillBeingCreated() throws Exception {
        CountDownLatch webViewIsWanted = new CountDownLatch(1);
        FakeWebView webView = new FakeWebView();
        LoadingBrowserPanel panel = new LoadingBrowserPanel(() -> {
            try {
                assertTrue(webViewIsWanted.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return webView;
        });

        //submitted before the web view is there, so it has to be remembered
        panel.submitDataUpdate("first");
        panel.submitDataUpdate("second");
        assertTrue(webView.dataUpdates.isEmpty(), "nothing can be applied before the web view is there");

        webViewIsWanted.countDown();
        awaitCondition("the web view got its update", () -> !webView.dataUpdates.isEmpty());

        //only the latest one is worth applying, an older one would just be overwritten by it
        assertEquals(List.of("second"), webView.dataUpdates);
    }

    @Test
    void forwardsUpdatesDirectlyOnceTheWebViewIsThere() throws Exception {
        FakeWebView webView = new FakeWebView();
        LoadingBrowserPanel panel = new LoadingBrowserPanel(() -> webView);

        awaitCondition("the web view is there", () -> panel.executeJavaScript("probe") == null && isShowingWebView(panel, webView));

        panel.submitDataUpdate("afterwards");
        assertEquals(List.of("afterwards"), webView.dataUpdates);
    }

    private static boolean isShowingWebView(Container container, FakeWebView webView) {
        for (Component component : container.getComponents()) {
            if (component == webView)
                return true;
            if (component instanceof Container child && isShowingWebView(child, webView))
                return true;
        }
        return false;
    }

    @Test
    void closesAWebViewThatIsCreatedAfterThePanelWasCleanedUp() throws Exception {
        CountDownLatch webViewIsWanted = new CountDownLatch(1);
        FakeWebView webView = new FakeWebView();
        LoadingBrowserPanel panel = new LoadingBrowserPanel(() -> {
            try {
                assertTrue(webViewIsWanted.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return webView;
        });

        //the window was closed while the web view was still being created
        panel.cleanupResources();
        webViewIsWanted.countDown();

        awaitCondition("the late web view was closed", () -> webView.cleanedUp);
        assertFalse(isShowingWebView(panel, webView), "a web view of a closed panel must not be shown");
    }

    @Test
    void showsAMessageWhenTheWebViewCannotBeCreated() throws Exception {
        LoadingBrowserPanel panel = new LoadingBrowserPanel(() -> {
            throw new IllegalStateException("no browser today");
        });

        awaitCondition("the failure is shown", () -> containsErrorMessage(panel));
    }

    @Test
    void showsAMessageWhenCreatingTheWebViewFailsWithAnError() throws Exception {
        //the web view is created by a native library, so a failure can well be an Error
        LoadingBrowserPanel panel = new LoadingBrowserPanel(() -> {
            throw new UnsatisfiedLinkError("no native browser today");
        });

        awaitCondition("the failure is shown", () -> containsErrorMessage(panel));
    }
}
