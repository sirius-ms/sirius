package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.RelativeLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class ConfigPanel extends JPanel implements ParameterProvider {
    protected final ParameterBinding parameterBindings = new ParameterBinding();

    @Override
    public ParameterBinding getParameterBinding() {
        return parameterBindings;
    }

    public ConfigPanel() {
        applyDefaultLayout(this);
    }


    protected JPanel applyDefaultLayout(@NotNull final JPanel pToStyle) {
        RelativeLayout rl = new RelativeLayout(RelativeLayout.X_AXIS, GuiUtils.LARGE_GAP);
        rl.setAlignment(RelativeLayout.LEADING);
        pToStyle.setLayout(rl);
        return pToStyle;
    }
}
