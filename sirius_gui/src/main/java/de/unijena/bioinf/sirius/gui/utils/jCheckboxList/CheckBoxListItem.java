

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Each item in the CheckBxoList will be an instance of this class
 *
 * @author Naveed Quadri
 */
public class CheckBoxListItem<E> extends JCheckBox {

    private E value = null;

    public static <E> CheckBoxListItem<E> getNew(E source) {
        return new CheckBoxListItem<>(source, false);
    }

    public CheckBoxListItem(E itemValue, boolean selected) {
        super(itemValue == null ? "" : "" + itemValue, selected);
        setValue(itemValue);
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                boolean b = isSelected();
                setSelected(!b);
            }
        });
    }

    @Override
    public boolean isSelected() {
        return super.isSelected();
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
    }

    public E getValue() {
        return value;
    }

    /**
     * The value of the JCheckbox label
     *
     * @param value
     */
    public void setValue(E value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
