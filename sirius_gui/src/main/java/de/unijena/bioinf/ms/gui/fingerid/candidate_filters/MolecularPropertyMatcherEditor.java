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
import de.unijena.bioinf.ms.gui.fingerid.StructureSearcher;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;

/**
 * Created by tkoehl on 19.07.18.
 */
public class MolecularPropertyMatcherEditor extends AbstractMatcherEditor<FingerprintCandidateBean> {
    StructureSearcher structureSearcher;

    public MolecularPropertyMatcherEditor(ToolbarToggleButton filterByMolecularPropertyButton) {
        super();
        filterByMolecularPropertyButton.addActionListener(propertyChangeEvent -> fireChanged(new MolecularPropertyMatcher(filterByMolecularPropertyButton.isSelected(), this.structureSearcher)));
    }

    public void setStructureSearcher(StructureSearcher structureSearcher) {
        this.structureSearcher = structureSearcher;
    }

    public void highlightChanged(boolean buttonIsSelected) {
        fireChanged(new MolecularPropertyMatcher(buttonIsSelected, this.structureSearcher));

    }

    public static class MolecularPropertyMatcher implements Matcher<FingerprintCandidateBean> {
        boolean filterIsActiv;
        int id;

        public MolecularPropertyMatcher(boolean buttonIsSelected, StructureSearcher structureSearcher) {
            if (structureSearcher != null) {
                id = structureSearcher.highlight;
            } else {
                id = 0;
            }
            if (buttonIsSelected && id > 0) {
                filterIsActiv = true;
            } else {
                filterIsActiv = false;
            }
        }

        @Override
        public boolean matches(FingerprintCandidateBean candidate) {
            if (filterIsActiv) {
                return candidate.hasFingerprintIndex(id);
            } else {
                return true;
            }
        }
    }
}
