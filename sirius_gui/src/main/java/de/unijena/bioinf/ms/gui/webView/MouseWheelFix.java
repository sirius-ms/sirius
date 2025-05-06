package de.unijena.bioinf.ms.gui.webView;

import java.awt.Component;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * A simple mouse wheel interceptor that fixes scrolling issues in JCEF.
 * This solution does not use reflection and should work with all JCEF implementations.
 */
public class MouseWheelFix {
    //todo do we need to detect whether the fix is needed? maybe not all linux distros are affected by this?
    private static boolean INVERT = System.getProperty("os.name").toLowerCase().contains("linux");
    private static boolean SPEED_UP = true;
    /**
     * Apply the Linux scroll fix to a component.
     * This method will add a mouse wheel listener that intercepts events before they
     * reach the browser's internal handlers.
     * 
     * @param component The component to fix (usually browser.getUIComponent())
     */
    public static void apply(Component component) {
        // Only apply on Linux
        if (!INVERT && !SPEED_UP) {
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
           if (INVERT)
               rotation = -rotation;
           if (SPEED_UP)
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
}