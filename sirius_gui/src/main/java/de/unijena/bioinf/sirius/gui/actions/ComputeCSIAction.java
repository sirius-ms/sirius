package de.unijena.bioinf.sirius.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 29.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.fingerid.FingerIdDialog;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListChangeListener;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ComputeCSIAction extends AbstractAction implements PropertyChangeListener {

    public ComputeCSIAction() {
        super("CSI:FingerID");
        putValue(Action.SMALL_ICON, Icons.FINGER_32);
        putValue(Action.SHORT_DESCRIPTION, "Search computed compounds with CSI:FingerID");

        proofCSI(((CheckConnectionAction) SiriusActions.CHECK_CONNECTION.getInstance()).isActive.get());

        MF.getExperimentList().addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<ExperimentContainer> event, DefaultEventSelectionModel<ExperimentContainer> selection) {
                if (MF.getCsiFingerId().isEnabled()) {
                    for (ExperimentContainer container : event.getSourceList()) {
                        if (container.isComputed()) {
                            setEnabled(true);
                            return;
                        }
                    }
                    setEnabled(false);
                } else {
                    setEnabled(false);
                }
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<ExperimentContainer> selection) {
            }
        });

        MF.getCsiFingerId().addPropertyChangeListener("enabled", evt -> setEnabled(proofCSI(((CheckConnectionAction) SiriusActions.CHECK_CONNECTION.getInstance()).isActive.get())));

        SiriusActions.CHECK_CONNECTION.getInstance().addPropertyChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CheckConnectionAction checker = (CheckConnectionAction) SiriusActions.CHECK_CONNECTION.getInstance();
        checker.actionPerformed(null);
        if (!checker.isActive.get()) return;


        final FingerIdDialog dialog = new FingerIdDialog(MF, MF.getCsiFingerId(), true, false);
        final int returnState = dialog.run();
        final SearchableDatabase db = dialog.getSearchDb();
        if (returnState == FingerIdDialog.COMPUTE_ALL) {
            //MF.getCsiFingerId().computeAll(MF.getCompounds(), db);
            MF.getCsiFingerId().computeAll(MF.getCompounds(), db);
        } else if (returnState == FingerIdDialog.COMPUTE) {
            //MF.getCsiFingerId().computeAll(MF.getCompoundListSelectionModel().getSelected(), db);
            MF.getCsiFingerId().computeAll(MF.getCompoundListSelectionModel().getSelected(), db);
        }
    }


    protected boolean proofCSI(final boolean network) {
        setEnabled(false);
        if (MF.getCsiFingerId().isEnabled() && MF.getCompounds().size() > 0) {
            if (network) {
                for (ExperimentContainer container : MF.getCompounds()) {
                    if (container.isComputed())
                        return true;
                    setEnabled(true);
                    break;
                }
            }
        }
        return false;
    }


    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("net")) {
            boolean value = (boolean) evt.getNewValue();
            setEnabled(proofCSI(value));
        }
    }
}
