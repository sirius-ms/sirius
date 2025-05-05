package de.unijena.bioinf.ms.gui.utils;

import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import java.awt.*;

public class TextHeaderPanel<Body extends JComponent> extends JPanel{
    @Getter
    private final Body body;

    public TextHeaderPanel(String headerText, Body body, int topGap, int bottomGap) {
        super(new BorderLayout());
        this.body = body;
        JXTitledSeparator titledSeparator = new JXTitledSeparator(headerText);
        titledSeparator.setBorder(BorderFactory.createEmptyBorder(topGap, 0, bottomGap, 0));
        add(titledSeparator, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }


    public static <B extends JComponent> TextHeaderPanel<B> wrap(String headerText, B body){
        return wrap(headerText, body, GuiUtils.MEDIUM_GAP, GuiUtils.SMALL_GAP);
    }
    public static <B extends JComponent> TextHeaderPanel<B> wrap(String headerText, B body, int topGap, int gap){
        return new TextHeaderPanel<>(headerText, body, topGap, gap);
    }
}
