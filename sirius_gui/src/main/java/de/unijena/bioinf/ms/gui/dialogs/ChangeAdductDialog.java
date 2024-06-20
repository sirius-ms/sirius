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

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.gui.utils.PrecursorIonTypeSelector;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class ChangeAdductDialog extends Dialog {
    private static final String TITLE = "Set Adduct Type";
    private PrecursorIonTypeSelector adductSelector;
    private ReturnValue rv;


    public ChangeAdductDialog(Frame owner) {
        super(owner, TITLE, true);
        init();
    }

    public ChangeAdductDialog(Dialog owner) {
        super(owner, TITLE, true);
        init();
    }

    public ChangeAdductDialog(Window owner) {
        super(owner, TITLE, ModalityType.APPLICATION_MODAL);
        init();
    }

    protected void init(){
        setLayout(new BorderLayout());
        adductSelector = new PrecursorIonTypeSelector();
        TwoColumnPanel center = new TwoColumnPanel();
        center.addNamed("Adduct", adductSelector);


        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        south.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        south.add(Box.createHorizontalGlue());

        final JButton ok = new JButton("Save");
        ok.addActionListener(e -> {
            rv = ReturnValue.Success;
            dispose();
        });


        final JButton abort = new JButton("Cancel");
        abort.addActionListener(e -> {
            rv = ReturnValue.Cancel;
            dispose();
        });

        south.add(ok);
        south.add(abort);
        add(south, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        this.pack();
        this.setResizable(false);
        setLocationRelativeTo(getParent());
        setVisible(true);
    }


    public Optional<PrecursorIonType> getSelectedAdduct(){
        if (rv ==ReturnValue.Cancel)
            return Optional.empty();
        return adductSelector.getSelectedAdduct();
    }
}
