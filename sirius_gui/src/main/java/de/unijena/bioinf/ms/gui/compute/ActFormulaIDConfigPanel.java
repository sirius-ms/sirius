package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.projectspace.InstanceBean;

import java.awt.*;

public class ActFormulaIDConfigPanel extends ActivatableConfigPanel<FormulaIDConfigPanel> {


    public ActFormulaIDConfigPanel(Dialog owner, java.util.List<InstanceBean> ecs) {
        super("SIRIUS", Icons.DB_32, false, () -> new FormulaIDConfigPanel(owner, ecs));
    }

    @Override
    protected void setComponentsEnabled(boolean enabled) {
        super.setComponentsEnabled(enabled);
        content.elementPanel.enableElementSelection(enabled);
    }
}
