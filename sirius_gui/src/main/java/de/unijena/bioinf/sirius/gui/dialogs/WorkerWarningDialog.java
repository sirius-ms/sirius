package de.unijena.bioinf.sirius.gui.dialogs;

import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;

import javax.swing.*;
import java.awt.*;

public class WorkerWarningDialog extends WarningDialog {

    public static final String MESSAGE =
            "<b>Warning:</b> For some predictors there is currently no worker <br>" +
                    "instance available! Corresponding jobs will need to wait until<br> " +
                    "a new worker instance is started. Please send an error report<br>" +
                    "if a specific predictor stays unavailable for a longer time. <br><br>" +
                    "Pleas see the Webservice information for details.";

    public static final String NEVER_ASK_AGAIN_KEY = PropertyManager.PROPERTY_BASE + ".sirius.dialog.check_worker_action.never_ask_again";


    public WorkerWarningDialog(Window owner) {
        super(owner, MESSAGE, NEVER_ASK_AGAIN_KEY);
    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
        boxedButtonPanel.add(Box.createHorizontalGlue());
        addOpenConnectionDialogButton(boxedButtonPanel);
        addOKButton(boxedButtonPanel);
    }

    protected void addOpenConnectionDialogButton(JPanel boxedButtonPanel) {
        final JButton details = new JButton("Details");
        details.setToolTipText("Open Webservice information for details.");
        details.setAction(SiriusActions.CHECK_CONNECTION.getInstance());
        boxedButtonPanel.add(details);
    }
}
