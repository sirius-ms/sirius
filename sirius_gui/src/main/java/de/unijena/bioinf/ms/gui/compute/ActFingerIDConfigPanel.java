package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;

public class ActFingerIDConfigPanel extends ActivatableConfigPanel<FingerIDConfigPanel> {
    public ActFingerIDConfigPanel(final JCheckBoxList<String> sourceIonization) {
        super("CSI:FingerID", Icons.FINGER_32, true, () -> new FingerIDConfigPanel(sourceIonization));
    }

    @Override
    protected void setComponentsEnabled(final boolean enabled) {
        super.setComponentsEnabled(enabled);
        content.searchDBList.setEnabled(enabled);
        content.adductOptions.setEnabled(enabled);
    }

    @Override
    protected void setButtonEnabled(boolean enabled) {
        setButtonEnabled(enabled, enabled ? "Enable CSI:FingerID search" : "Can't connect to CSI:FingerID server!");
    }
}
