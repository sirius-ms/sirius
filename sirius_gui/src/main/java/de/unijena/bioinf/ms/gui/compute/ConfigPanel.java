/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.RelativeLayout;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;

public abstract class ConfigPanel extends JPanel implements ParameterProvider {
    enum Layout{HORIZONTAL, VERTICAL};

    protected final ParameterBinding parameterBindings;

    @Override
    public ParameterBinding getParameterBinding() {
        return parameterBindings;
    }

    public ConfigPanel() {
        this(Layout.HORIZONTAL);
    }

    public ConfigPanel(@NotNull Layout layout) {
        this(new ParameterBinding(), layout);
    }

    public ConfigPanel(ParameterBinding parameterBindings) {
        this(parameterBindings, Layout.HORIZONTAL);
    }

    public ConfigPanel(ParameterBinding parameterBindings, @NotNull Layout layout) {
        this.parameterBindings = parameterBindings;
        applyDefaultLayout(this, layout);
    }

    protected JPanel applyDefaultLayout(@NotNull final JPanel pToStyle) {
        return applyDefaultLayout(pToStyle, Layout.HORIZONTAL);
    }

    protected JPanel applyDefaultLayout(@NotNull final JPanel pToStyle, @NotNull Layout layout) {
        RelativeLayout rl = new RelativeLayout(layout.ordinal(), GuiUtils.LARGE_GAP);
        rl.setAlignment(RelativeLayout.LEADING);
        pToStyle.setLayout(rl);
        return pToStyle;
    }

    public JSpinner makeIntParameterSpinner(@NotNull String parameterKey, int minimum, int maximum, int stepSize) {
        return makeParameterSpinner(parameterKey, Integer.parseInt(PropertyManager.DEFAULTS.getConfigValue(parameterKey)), minimum, maximum, stepSize, m -> String.valueOf(m.getNumber().intValue()));
    }

    public JSpinner makeDoubleParameterSpinner(@NotNull String parameterKey, double minimum, double maximum, double stepSize) {
        return makeParameterSpinner(parameterKey, Double.parseDouble(PropertyManager.DEFAULTS.getConfigValue(parameterKey)), minimum, maximum, stepSize, m -> String.valueOf(m.getNumber().doubleValue()));
    }

    public JSpinner makeFloatParameterSpinner(@NotNull String parameterKey, float minimum, float maximum, float stepSize) {
        return makeParameterSpinner(parameterKey, Double.parseDouble(PropertyManager.DEFAULTS.getConfigValue(parameterKey)), minimum, maximum, stepSize, m -> String.valueOf(m.getNumber().floatValue()));
    }

    public JSpinner makeParameterSpinner(@NotNull String parameterKey, double value, double minimum, double maximum, double stepSize, Function<SpinnerNumberModel, String> result) {
        JSpinner spinner = makeBindedSpinner(parameterKey, value, minimum, maximum, stepSize, result);
        GuiUtils.assignParameterToolTip(spinner, parameterKey);
        return spinner;
    }

    public JSpinner makeBindedSpinner(@NotNull String parameterKey, double value, double minimum, double maximum, double stepSize, Function<SpinnerNumberModel, String> result) {
        SpinnerNumberModel model = new SpinnerNumberModel(value, minimum, maximum, stepSize);
        JSpinner spinner = new JSpinner(model);
        spinner.setMinimumSize(new Dimension(70, 26));
        spinner.setPreferredSize(new Dimension(95, 26));
        spinner.setMaximumSize(new Dimension(120, 26));
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

    public JTextField makeParameterTextField(@NotNull String parameterKey, int columns) {
        return makeParameterTextField(parameterKey, PropertyManager.DEFAULTS.getConfigValue(parameterKey), columns);
    }

    public JTextField makeParameterTextField(@NotNull String parameterKey, String value, int columns) {
        JTextField textField = new JTextField(value, columns);
        parameterBindings.put(parameterKey, textField::getText);
        return textField;
    }

    /**
     * Changes widget values to the values from the provided preset
     *
     * @param preset config map with values for all parameters
     * @throws UnsupportedOperationException if the parameter values are not compatible with the UI or if this panel does not support setting values from a preset
     */
    public void applyValuesFromPreset(Map<String, String> preset) {
        throw new UnsupportedOperationException("Setting values from preset is not supported for this panel.");
    }
}
