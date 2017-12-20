package de.unijena.bioinf.sirius.gui.utils.jfxWrapper;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;

public class JFXWrapper<E extends Node> extends JFXPanel {
    public final E jfxNode;

    public static <E extends Node> JFXWrapper<E> wrap(E jfxNodeToWrap) {
        return new JFXWrapper<>(jfxNodeToWrap);
    }

    protected JFXWrapper(E source) {
        this.jfxNode = source;
    }


}
