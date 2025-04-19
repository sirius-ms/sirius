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
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import javax.swing.*;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Function;

public abstract class SubToolConfigPanel<C> extends ConfigPanel {
    protected CommandLine.Model.CommandSpec commandSpec;

   public SubToolConfigPanel(Class<C> annotatedObject) {
        super();
        commandSpec = CommandLine.Model.CommandSpec.forAnnotatedObject(annotatedObject);
    }

    public String toolCommand() {
        return commandSpec.name();
    }

    public String[] toolCommandAliases() {
        return commandSpec.aliases();
    }

    public String toolHeaderHeading() {
        return commandSpec.usageMessage().headerHeading();
    }

    public String[] toolHeader() {
        return commandSpec.usageMessage().header();
    }

    public String toolDescriptionHeading() {
        return commandSpec.usageMessage().descriptionHeading();
    }

    public String[] toolDescription() {
        return commandSpec.usageMessage().description();
    }

    public String toolFooterHeading() {
        return commandSpec.usageMessage().footerHeading();
    }

    public String[] toolFooter() {
        return commandSpec.usageMessage().footer();
    }

    public Optional<String[]> getOptionDescriptionByName(String name) {
        return Optional.ofNullable(commandSpec.findOption(name)).map(CommandLine.Model.ArgSpec::description);
    }

    public Optional<String> getOptionDefaultByName(String name) {
        return Optional.ofNullable(commandSpec.findOption(name)).map(CommandLine.Model.ArgSpec::defaultValue);
    }

    protected Optional<String> getOptionTooltip(String name) {
        return getOptionDescriptionByName(name).map(GuiUtils::formatToolTip);
    }

    public JSpinner makeGenericOptionSpinner(@NotNull String name, double value, double minimum, double maximum, double stepSize, Function<SpinnerNumberModel, String> result) {
        JSpinner spinner = makeBindedSpinner(name, value, minimum, maximum, stepSize, result);
        getOptionTooltip(name).ifPresent(spinner::setToolTipText);
        return spinner;
    }

    public <T extends Enum<T>> JComboBox<T> makeGenericOptionComboBox(@NotNull String name, Class<T> enumType) {
        return makeGenericOptionComboBox(name, java.util.List.copyOf(EnumSet.allOf(enumType)), Enum::name);
    }

    public <T> JComboBox<T> makeGenericOptionComboBox(@NotNull String name, java.util.List<T> values, Function<T, String> result) {
        JComboBox<T> box = new JComboBox<>();
        values.forEach(box::addItem);

        getOptionTooltip(name).ifPresent(box::setToolTipText);
        getOptionDefaultByName(name)
                .flatMap(dv -> values.stream().filter(v -> result.apply(v).equalsIgnoreCase(dv)).findFirst())
                .ifPresent(box::setSelectedItem);

        parameterBindings.put(name, () -> result.apply((T) box.getSelectedItem()));
        return box;
    }

    public JCheckBox makeGenericOptionCheckBox(String text, String optionKey) {
        return makeGenericOptionCheckBox(text, optionKey, false);
    }

    public JCheckBox makeGenericOptionCheckBox(String text, String optionKey, boolean selected) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        parameterBindings.put(optionKey, () -> "~" + checkBox.isSelected());
        getOptionTooltip(optionKey).ifPresent(checkBox::setToolTipText);
        return checkBox;
    }
}
