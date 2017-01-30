package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import de.unijena.bioinf.sirius.gui.compute.BatchComputeDialog;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ComputeAction extends AbstractAction {
    @Override
    public void actionPerformed(ActionEvent e) {
        List<ExperimentContainer> ecs = MF.getCompoundView().getSelectedValuesList();
        if (ecs != null && !ecs.isEmpty()) {
            new BatchComputeDialog(MF, ecs); //todo no check button should not be active
        }
    }
}