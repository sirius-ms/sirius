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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class SubToolConfigPanel<C> extends ConfigPanel {
    protected final Class<C> annotatedClass;
    private final CommandLine.Command command;

    public SubToolConfigPanel(Class<C> annotatedObject) {
        annotatedClass = annotatedObject;
        command = annotatedObject.getAnnotation(CommandLine.Command.class);

    }

    public String toolCommand() {
        return command.name();
    }

    public String[] toolCommandAliases() {
        return command.aliases();
    }

    public String toolHeaderHeading() {
        return command.headerHeading();
    }

    public String[] toolHeader() {
        return command.header();
    }

    public String toolDescriptionHeading() {
        return command.descriptionHeading();
    }

    public String[] toolDescription() {
        return command.description();
    }

    public String toolFooterHeading() {
        return command.footerHeading();
    }

    public String[] toolFooter() {
        return command.footer();
    }

    public Optional<CommandLine.Option> getOptionByName(String name) {
        final String fname;
        if (!name.startsWith("-")) {
            if (name.length() > 1) {
                fname = "--" + name;
            } else {
                fname = "-" + name;
            }
        } else {
            fname = name;
        }
        return collectOptions(new ArrayList<>(), annotatedClass).stream().filter(opt -> Arrays.asList(opt.names())
                .contains(fname)).findFirst();
    }

    protected List<CommandLine.Option> collectOptions(final List<CommandLine.Option> options, Class<?> annotatedClass) {
        Stream.concat(
                Stream.of(annotatedClass.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(CommandLine.Option.class)).map(m -> m.getAnnotation(CommandLine.Option.class)),
                Stream.of(annotatedClass.getDeclaredFields()).filter(f -> f.isAnnotationPresent(CommandLine.Option.class)).map(f -> f.getAnnotation(CommandLine.Option.class))
        ).forEach(options::add);

        Arrays.stream(annotatedClass.getDeclaredFields()).filter(f -> f.isAnnotationPresent(CommandLine.ArgGroup.class))
                .forEach(field -> {
                    final Type genType = field.getGenericType();
                    if (genType instanceof ParameterizedType) {
                        final Type[] types = ((ParameterizedType) genType).getActualTypeArguments();
                        if (types.length > 0)
                            collectOptions(options, (Class<?>) types[0]);
                        else
                            collectOptions(options, field.getType());
                    } else {
                        collectOptions(options, (Class<?>) genType);
                    }
                });

        return options;
    }

    public Optional<String[]> getOptionDescriptionByName(String name) {
        return getOptionByName(name).map(CommandLine.Option::description);
    }

    public Optional<String> getOptionDefaultByName(String name) {
        return getOptionByName(name).map(CommandLine.Option::defaultValue);
    }

    public JSpinner makeGenericOptionSpinner(@NotNull String name, double value, double minimum, double maximum, double stepSize, Function<SpinnerNumberModel, String> result) {
        JSpinner spinner = makeBindedSpinner(name, value, minimum, maximum, stepSize, result);
        getOptionDescriptionByName(name).ifPresent(des -> spinner.setToolTipText(GuiUtils.formatToolTip(des)));
        return spinner;
    }

    public <T extends Enum<T>> JComboBox<T> makeGenericOptionComboBox(@NotNull String name, Class<T> enumType) {
        return makeParameterComboBox(name, java.util.List.copyOf(EnumSet.allOf(enumType)), Enum::name);
    }

    public <T> JComboBox<T> makeGenericOptionComboBox(@NotNull String name, java.util.List<T> values, Function<T, String> result) {
        JComboBox<T> box = new JComboBox<>();
        values.forEach(box::addItem);


        getOptionDescriptionByName(name).ifPresent(des -> box.setToolTipText(GuiUtils.formatToolTip(des)));
        getOptionDefaultByName(name).ifPresent(box::setSelectedItem);

        parameterBindings.put(name, () -> result.apply((T) box.getSelectedItem()));
        return box;
    }

    public JCheckBox makeGenericOptionCheckBox(String text, String optionKey) {
        return makeGenericOptionCheckBox(text, optionKey, null);
    }

    public JCheckBox makeGenericOptionCheckBox(String text, String optionKey, Boolean selected) {
        final CommandLine.Option o = getOptionByName(optionKey).orElse(getOptionByName(negate(optionKey)).orElse(null));

        JCheckBox checkBox = new JCheckBox(text, selected != null ? selected
                :Optional.ofNullable(o).map(CommandLine.Option::defaultValue).map(Boolean::parseBoolean).orElse(false));

        parameterBindings.put(optionKey, () -> "~" + checkBox.isSelected());

        if (o != null) {
            checkBox.setToolTipText(GuiUtils.formatToolTip(o.description()));
            if (o.negatable())
                parameterBindings.put(negate(optionKey), () -> "~" + !checkBox.isSelected());
        }

        return checkBox;
    }

    private static String negate(String optionKey) {
        if (optionKey.startsWith("no-")) {
            return optionKey.substring(3);
        } else {
            return "no-" + optionKey;
        }
    }
}
