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

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ms.gui.compute.ElementsPanel;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;

public class ElementSelectionDialog extends JDialog {

    private ReturnValue rv;
    ElementsPanel elementsPanel;

    public ElementSelectionDialog(Window owner, String title, @NotNull FormulaConstraints constraints) {
        this(owner, title, null, null, constraints, false);
    }


    /**
     * @param owner            see JDialog
     * @param title            Title of the dialog
     */
    public ElementSelectionDialog(Window owner, String title, Collection<Element> possibleDetectable, Collection<Element> enabledDetectable, @NotNull FormulaConstraints constraints, boolean setDetectablesOnly) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());

        rv = ReturnValue.Cancel;

        elementsPanel = new ElementsPanel(this, setDetectablesOnly ? 8 : 4, possibleDetectable, enabledDetectable, constraints, setDetectablesOnly);
        add(elementsPanel, BorderLayout.CENTER);


        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        south.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        final JButton ok = new JButton("Apply");
        ok.addActionListener(e -> {
            rv = ReturnValue.Success;
            dispose();
        });


        final JButton abort = new JButton("Cancel");
        abort.addActionListener(e -> {
            rv = ReturnValue.Cancel;
            dispose();
        });

        south.add(Box.createHorizontalGlue());
        south.add(ok);
        south.add(abort);
        add(south, BorderLayout.SOUTH);


        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    public ReturnValue getReturnValue() {
        return rv;
    }

    public boolean isSuccess() {
        return rv.equals(ReturnValue.Success);
    }

    public boolean isAbort() {
        return rv.equals(ReturnValue.Cancel);
    }

    public ElementsPanel getElementsPanel() {
        return elementsPanel;
    }

    @NotNull
    public FormulaConstraints getConstraints(){
        if (isSuccess())
            return elementsPanel.getElementConstraints();
        return FormulaConstraints.empty();
    }

    public List<Element> getAutoDetect() {
        return elementsPanel.getElementsToAutoDetect();
    }
}
