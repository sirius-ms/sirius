package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.projectspace.InstanceBean;

import java.util.Collection;

public class ActFormulaIDConfigPanel extends ActivatableConfigPanel<FormulaIDConfigPanel> {

    public ActFormulaIDConfigPanel(Collection<InstanceBean> ecs) {
        super("SIRIUS", Icons.DB_32, false, () -> new FormulaIDConfigPanel(ecs));
    }

    @Override
    protected void setComponentsEnabled(boolean enabled) {
        super.setComponentsEnabled(enabled);
    }
}
