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

package de.unijena.bioinf.ms.gui.fingerid.candidate_filters;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;

import javax.swing.*;
import java.awt.*;

/**
 * Created by tkoehl on 18.07.18.
 */
public class SmartFilterMatcherEditor extends AbstractMatcherEditor<FingerprintCandidateBean> {

    public SmartFilterMatcherEditor(JTextField textField) {
        super();
        textField.addActionListener(propertyChangeEvent -> fireChanged(new SmartMatcher(textField)));
    }

    public static class SmartMatcher implements Matcher<FingerprintCandidateBean> {
        final SMARTSQueryTool tool;
        boolean isValidSmartString;

        public SmartMatcher(JTextField textField) {
            String smart = textField.getText();
            textField.setForeground(Color.black);
            textField.setToolTipText("");

            tool = new SMARTSQueryTool("CC", DefaultChemObjectBuilder.getInstance());

            if (smart == null) {
                isValidSmartString = false;
                return;
            }
            if (smart.equals("")) {
                isValidSmartString = false;
                return;
            }

            try {
                tool.setSmarts(smart);
                isValidSmartString = true;
                textField.setForeground(Color.green);
            } catch (Exception e) {
                isValidSmartString = false;
                textField.setForeground(Color.red);
                textField.setToolTipText("invalid SMART string.");
            }
        }

        @Override
        public boolean matches(FingerprintCandidateBean candidate) {
            if (isValidSmartString) {
                try {
                    return tool.matches(candidate.getMolecule());
                } catch (Exception e) {
                    return false;
                }
            } else {
                return true;
            }
        }
    }
}
