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

import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;

import javax.swing.text.JTextComponent;

/**
 * Created by fleisch on 18.05.17.
 */
public class CandidateStringMatcherEditor extends TextComponentMatcherEditor<FingerprintCandidateBean> {

    public CandidateStringMatcherEditor(JTextComponent textComponent) {
        super(textComponent, (baseList, element) -> {
            baseList.add(element.getMolecularFormula());
            baseList.add(element.getName());
            baseList.add(element.getInChiKey());
            baseList.add(element.getFingerprintCandidate().getInchi().in3D);
            baseList.add(element.getFingerprintCandidate().getSmiles());
        });
    }
}
