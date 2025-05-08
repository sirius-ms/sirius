package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class SoftwareTourInitialDialog extends QuestionDialog{
    protected JCheckBox dontAskAnyTourAgain;

    public SoftwareTourInitialDialog(Window owner, String tourName, String propertyKey) {
        super(owner, "<html><body>Should I give you a quick tour of the <b>"+tourName+"</b>?</body></html>", propertyKey, ReturnValue.Cancel);
    }

    @Override
    protected void decorateBodyPanel(TwoColumnPanel boxedButtonPanel){
        super.decorateBodyPanel(boxedButtonPanel);
        dontAsk.setText("Do not show this tour again.");
        dontAskAnyTourAgain = new JCheckBox("Disable all tours (can be re-enabled in settings).");
        dontAskAnyTourAgain.addActionListener(e -> {
            if (dontAskAnyTourAgain.isSelected()) dontAsk.setSelected(true);
        });
        textContainer.add(dontAsk, 10, false);
        textContainer.add(dontAskAnyTourAgain);
    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
        super.decorateButtonPanel(boxedButtonPanel);
        ok.setText("Start");
        cancel.setText("Skip");

        ok.addActionListener(e -> {
            if (isDontAskAnyTourAgain()) {
                SoftwareTourUtils.disableAllTours();
            }
        });
        cancel.addActionListener(e -> {
            if (isDontAskAnyTourAgain()) {
                SoftwareTourUtils.disableAllTours();
            }
        });
    }

    private boolean isDontAskAnyTourAgain() {
        return dontAskAnyTourAgain != null && dontAskAnyTourAgain.isSelected();
    }
}
