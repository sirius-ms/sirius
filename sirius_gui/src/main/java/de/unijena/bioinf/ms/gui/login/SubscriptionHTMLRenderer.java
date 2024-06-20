/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.login;

import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.rest.model.license.Subscription;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.Optional;

/*
 **  Use HTML to format the text
 */
class SubscriptionHTMLRenderer extends DefaultListCellRenderer {
    private static final String START = "<html>";
    private static final String END = "</html>";

    protected Color foreColor = Colors.LIST_ACTIVATED_FOREGROUND;
    protected Color backColor = Colors.LIST_EVEN_BACKGROUND;

    final int maxWidth;

    public SubscriptionHTMLRenderer(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public Component getListCellRendererComponent(JList list, Object value,
                                                  int row, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, row, isSelected, cellHasFocus);

        if (isSelected) {
            backColor = new Color(57, 105, 138);
            foreColor = Colors.LIST_SELECTED_FOREGROUND;
        } else {
            if (row % 2 == 0) backColor = Colors.LIST_EVEN_BACKGROUND;
            else backColor = Colors.LIST_UNEVEN_BACKGROUND;
            foreColor = Colors.LIST_ACTIVATED_FOREGROUND;
        }

        setBackground(backColor);
        setForeground(foreColor);

        Subscription item = (Subscription) value;
        setText(START + "<div WIDTH=" + maxWidth + ">"
                + "<p><b>" + item.getName() + "</b></p>"
                + "<p><i>" + item.getDescription() + "</i></p>"
                + "<p>" + "<b>Count:</b> " + item.isCountQueries() + "&nbsp;&nbsp;&nbsp;&nbsp;<b>Limit:</b> " +
                Optional.ofNullable(item.getCompoundLimit()).map(Objects::toString).orElse("UNLIMITED") + "</p>"
                + "<p>" + "<b>Hosting URL:</b> " + item.getServiceUrl() + "</p>"
                + "</div>" + END);
        return this;
    }
}
