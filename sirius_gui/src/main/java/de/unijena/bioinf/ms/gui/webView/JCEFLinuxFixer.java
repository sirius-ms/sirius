package de.unijena.bioinf.ms.gui.webView;

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import lombok.extern.slf4j.Slf4j;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefRendering;
import org.cef.handler.CefLoadHandlerAdapter;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.concurrent.CountDownLatch;

/**
 * This class collects workarounds to make the JCEF browser work on linux
 */
@Slf4j
public class JCEFLinuxFixer {
    public static void preloadJCef(CefApp cefApp){
        final CountDownLatch waiter = new CountDownLatch(1);
        Jobs.runInBackground(() -> {
            CefClient client = cefApp.createClient();
            client.addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                    if (!isLoading){
                        JFrame f = new JFrame();
                        f.add(browser.getUIComponent());
                        f.pack();
                        f.setVisible(true);
                        f.dispose();
                        client.dispose();
                        waiter.countDown();
                    }
                }
            });

            // OFFSCREEN rendering is mandatory since otherwise focussing is buggy
            CefBrowser browser = client.createBrowser("about:blank", CefRendering.DEFAULT, false);

            // very important to ensure that the JCEF process can be closed correctly without creating a memory leak
            browser.setCloseAllowed();
            // we create the browser instance synchronously because this is the only way to ensure the browser is fully
            // loaded before we do JS call to update the data ids to be shown
            browser.createImmediately();
        });

        try {
            waiter.await();
        } catch (InterruptedException e) {
            log.warn("Waiting for JCEF initialization was interrupted.", e);
        }
    }

    //todo do we need to detect whether the fix is needed? maybe not all linux distros are affected by this?
    private static final boolean MOUSEWHEEL_INVERT = System.getProperty("os.name").toLowerCase().contains("linux");
    private static final boolean MOUSEWHEEL_SPEED_UP = true;
    /**
     * Apply the Linux scroll fix to a component.
     * This method will add a mouse wheel listener that intercepts events before they
     * reach the browser's internal handlers.
     * 
     * @param component The component to fix (usually browser.getUIComponent())
     */
    public static void fixMousewheelScrolling(Component component) {
        // Only apply on Linux
        if (!MOUSEWHEEL_INVERT && !MOUSEWHEEL_SPEED_UP) {
            return;
        }
        
        // Remove any existing mouse wheel listeners to avoid duplicates
        MouseWheelListener[] existingListeners = component.getMouseWheelListeners();
        for (MouseWheelListener listener : existingListeners) {
            component.removeMouseWheelListener(listener);
        }
        
        // Add our interceptor as the first listener
        component.addMouseWheelListener(event -> {
            // Create a modified event with inverted wheel rotation (direction)
            // and amplified value (speed)
           int rotation = event.getWheelRotation();
           if (MOUSEWHEEL_INVERT)
               rotation = -rotation;
           if (MOUSEWHEEL_SPEED_UP)
               rotation *= 7;

            MouseWheelEvent modified = new MouseWheelEvent(
                event.getComponent(),
                event.getID(),
                event.getWhen(),
                event.getModifiersEx(),
                event.getX(),
                event.getY(),
                event.getClickCount(),
                event.isPopupTrigger(),
                event.getScrollType(),
                event.getScrollAmount(),
                rotation
            );
            
            // Re-add the original listeners
            for (MouseWheelListener listener : existingListeners) {
                // Pass our modified event to each listener
                listener.mouseWheelMoved(modified);
            }
            
            // Consume the original event to prevent double processing
            event.consume();
        });
    }

    public static void setupPopupErrorListener(CefClient client, JComponent parent) {
        client.addDialogHandler((dialogBrowser, mode, title, defaultPath, acceptFilters, callback) -> {
            // Native file dialogs don't work on linux, see
            // https://github.com/chromiumembedded/cef/blob/master/libcef/browser/file_dialog_manager.cc#L405
            // https://github.com/JetBrains/jcef/blob/dev/native/context.cpp#L211
            //
            // Implementing a java dialog also doesn't work, results in error "The request is not allowed by the user agent or the platform in the current context"
            // Maybe can be fixed with some chromium flags to allow local system writing
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Native file dialogs from this view are not available on Linux.", null, JOptionPane.ERROR_MESSAGE));
            callback.Cancel();
            return true;
        });
    }
}