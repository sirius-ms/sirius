package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.RelativeLayout;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.function.Function;

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

    public JSpinner makeIntParameterSpinner(@NotNull String parameterKey, double minimum, double maximum, double stepSize) {
        return makeParameterSpinner(parameterKey, Integer.parseInt(PropertyManager.DEFAULTS.getConfigValue(parameterKey)), minimum, maximum, stepSize, m -> String.valueOf(m.getNumber().intValue()));
    }

    public JSpinner makeDoubleParameterSpinner(@NotNull String parameterKey, double minimum, double maximum, double stepSize) {
        return makeParameterSpinner(parameterKey, Double.parseDouble(PropertyManager.DEFAULTS.getConfigValue(parameterKey)), minimum, maximum, stepSize, m -> String.valueOf(m.getNumber().doubleValue()));
    }

    public JSpinner makeParameterSpinner(@NotNull String parameterKey, double value, double minimum, double maximum, double stepSize, Function<SpinnerNumberModel, String> result) {
        SpinnerNumberModel model = new SpinnerNumberModel(value, minimum, maximum, stepSize);
        JSpinner spinner = new JSpinner(model);
        spinner.setMinimumSize(new Dimension(70, 26));
        spinner.setPreferredSize(new Dimension(70, 26));
        GuiUtils.assignParameterToolTip(spinner, parameterKey);
        parameterBindings.put(parameterKey, () -> result.apply(model));
        return spinner;
    }

    public JCheckBox makeParameterCheckBox(@NotNull String parameterKey) {
        JCheckBox cb = new JCheckBox();
        cb.setSelected(Boolean.parseBoolean(PropertyManager.DEFAULTS.getConfigValue(parameterKey)));
        GuiUtils.assignParameterToolTip(cb, parameterKey);
        parameterBindings.put(parameterKey, () -> String.valueOf(cb.isSelected()));
        return cb;
    }

    public <T extends Enum<T>> JComboBox<T> makeParameterComboBox(@NotNull String parameterKey, Class<T> enumType) {
        return makeParameterComboBox(parameterKey, java.util.List.copyOf(EnumSet.allOf(enumType)), Enum::name);
    }

    public <T> JComboBox<T> makeParameterComboBox(@NotNull String parameterKey, java.util.List<T> values, Function<T, String> result) {
        JComboBox<T> box = new JComboBox<>();
        values.forEach(box::addItem);
        GuiUtils.assignParameterToolTip(box, parameterKey);
        parameterBindings.put(parameterKey, () -> result.apply((T) box.getSelectedItem()));
        return box;
    }
}
