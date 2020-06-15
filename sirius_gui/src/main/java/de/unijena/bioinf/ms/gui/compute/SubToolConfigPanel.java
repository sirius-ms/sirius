package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import javax.swing.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class SubToolConfigPanel<C> extends ConfigPanel {
    //todo if we decide to use the GUI longer we could think about creating panel from annotated classes
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

        return Stream.concat(
                Stream.of(annotatedClass.getMethods()).filter(m -> m.isAnnotationPresent(CommandLine.Option.class)).map(m -> m.getAnnotation(CommandLine.Option.class)),
                Stream.of(annotatedClass.getFields()).filter(f -> f.isAnnotationPresent(CommandLine.Option.class)).map(f -> f.getAnnotation(CommandLine.Option.class))
        ).filter(opt -> Arrays.asList(opt.names()).contains(fname)).findFirst();
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
}
