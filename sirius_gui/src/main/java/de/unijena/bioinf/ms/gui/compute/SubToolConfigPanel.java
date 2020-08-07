/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

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
