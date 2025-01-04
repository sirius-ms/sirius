package de.unijena.bioinf.ms.gui.utils;

import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import java.io.IOException;
import java.net.URI;

public class HyperlinkJTextPane extends JTextPane {
    public HyperlinkJTextPane(String text) {
        super();
        // Set the editor kit to handle HTML
        setEditorKit(new HTMLEditorKit());
        setEditable(false); // Not editable
        setContentType("text/html");

        setText(text);  // Set initial text, which will now be interpreted as HTML

        // Add the hyperlink listener
        addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    GuiUtils.openURL(SwingUtilities.getWindowAncestor(this), URI.create(e.getURL().toString()), true);
                } catch (IOException ex) {
                    LoggerFactory.getLogger(this.getClass()).error("Could not open URL: {}", e.getURL().toString(), ex);
                }
            }
        });

    }
}