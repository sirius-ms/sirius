
/*
 *  Copyright (C) 2011 Naveed Quadri
 *  naveedmurtuza@gmail.com
 *  www.naveedmurtuza.blogspot.com
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.unijena.bioinf.sirius.gui.utils.jCheckboxList;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A cell renderer for the CheckBoxList
 *
 * @author Naveed Quadri
 */
class CheckboxCellRenderer extends DefaultListCellRenderer {
    protected static Border emptyBorder = new EmptyBorder(1, 1, 1, 1);

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        if (value instanceof CheckBoxListItem) {
            CheckBoxListItem checkbox = (CheckBoxListItem) value;
            checkbox.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            checkbox.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            checkbox.setEnabled(isEnabled());
            checkbox.setFont(getFont());
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(true);
            checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder")
                    : emptyBorder);

            return checkbox;
        } else {
            return super.getListCellRendererComponent(list, value.getClass().getName(), index,
                    isSelected, cellHasFocus);
        }
    }

}
