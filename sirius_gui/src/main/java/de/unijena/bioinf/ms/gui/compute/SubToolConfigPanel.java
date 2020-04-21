package de.unijena.bioinf.ms.gui.compute;

import picocli.CommandLine;

public abstract class SubToolConfigPanel<C> extends ConfigPanel {
    //todo if we decide to use the GUI longer we could think about creating panel from annotated classes
    private final CommandLine.Command command;

    public SubToolConfigPanel(Class<C> annotatedObject) {
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
}
