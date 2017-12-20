package de.unijena.bioinf.sirius.gui.utils.jfxWrapper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import org.controlsfx.control.CheckListView;

import java.util.List;

public class JCheckBoxList<E> extends JFXPanel {
    private final ObservableList<E> elements;
    private final CheckListView<E> checkListView;

    public JCheckBoxList(E... elements) {
        this(FXCollections.observableArrayList(elements));
    }

    public JCheckBoxList(List<E> elements) {
        this(FXCollections.observableArrayList(elements));
    }

    public JCheckBoxList(ObservableList<E> elements) {
        this.elements = elements;
        // Create the CheckListView with the data
        checkListView = new CheckListView<>(elements);
        setScene(checkListView.getScene());
    }

    public ObservableList<E> getElements() {
        return elements;
    }
}
