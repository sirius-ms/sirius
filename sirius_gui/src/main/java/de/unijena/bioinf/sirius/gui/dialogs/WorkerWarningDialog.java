package de.unijena.bioinf.sirius.gui.dialogs;

import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.net.ConnectionCheckPanel;

import javax.swing.*;
import java.awt.*;

public class WorkerWarningDialog extends WarningDialog {

    public static final String NEVER_ASK_AGAIN_KEY = PropertyManager.PROPERTY_BASE + ".sirius.dialog.check_worker_action.never_ask_again";


    public WorkerWarningDialog(Window owner) {
        this(owner, false);
    }

    public WorkerWarningDialog(Window owner, final boolean noWorkerInfoError) {
        super(owner
                , "<html>" + (
                        noWorkerInfoError
                                ? ConnectionCheckPanel.WORKER_INFO_MISSING_MESSAGE
                                : ConnectionCheckPanel.WORKER_WARNING_MESSAGE
                ) + "<br><br> See the Webservice information for details.</html>"
                , NEVER_ASK_AGAIN_KEY
        );
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
        details.setIcon(Icons.NET_16);
        boxedButtonPanel.add(details);
    }
}
