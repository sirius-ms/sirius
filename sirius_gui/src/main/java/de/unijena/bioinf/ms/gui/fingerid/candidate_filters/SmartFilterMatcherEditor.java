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

package de.unijena.bioinf.ms.gui.fingerid.candidate_filters;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import org.openscience.cdk.smarts.SmartsPattern;

import javax.swing.*;
import java.awt.*;
import java.util.function.BooleanSupplier;


public class SmartFilterMatcherEditor extends AbstractMatcherEditor<FingerprintCandidateBean> {

    private final Color foreground;
    private final Color background;
    private final BooleanSupplier isEnabled;

    public SmartFilterMatcherEditor(JTextField textField) {
        this(textField, () -> true);
    }
    public SmartFilterMatcherEditor(JTextField textField, BooleanSupplier isEnabled) {
        super();
        this.isEnabled = isEnabled;
        foreground = textField.getForeground();
        background = textField.getBackground();
        textField.addActionListener(propertyChangeEvent -> fireChanged(new SmartMatcher(textField)));
    }

    public class SmartMatcher implements Matcher<FingerprintCandidateBean> {
        private SmartsPattern smartsPattern = null;
        boolean isValidSmartString;

        public SmartMatcher(JTextField textField) {
            textField.setForeground(foreground); //set neutral color if smart matching inactive
            textField.setBackground(background);

            if (! isEnabled.getAsBoolean())
                return;

            String smart = textField.getText();

            if (smart == null) {
                isValidSmartString = false;
                return;
            }
            if (smart.equals("")) {
                isValidSmartString = false;
                return;
            }

            try {
                smartsPattern = SmartsPattern.create(smart);
                isValidSmartString = true;
                textField.setBackground(Colors.GOOD_IS_GREEN_PALE);
                textField.setForeground(Color.BLACK);
            } catch (Exception e) {
                isValidSmartString = false;
                textField.setBackground(Colors.TEXT_ERROR);
                textField.setForeground(Color.BLACK);
                textField.setToolTipText("invalid SMART string.");
            }
        }

        @Override
        public boolean matches(FingerprintCandidateBean candidate) {
            if (smartsPattern != null && isValidSmartString) {
                try {
                    return smartsPattern.matches(candidate.getMolecule());
                } catch (Exception e) {
                    return false;
                }
            } else {
                return true;
            }
        }
    }
}
