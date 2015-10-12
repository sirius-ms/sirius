package de.unijena.bioinf.sirius.gui.dialogs;

import java.awt.*;

public class FilePresentDialog extends QuestionDialog {

    public FilePresentDialog(Frame owner, String name) {
        super(owner, "The file \"" + name + "\" is already present. Override it?");
    }

}