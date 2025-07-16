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

import ca.odell.glazedlists.impl.matchers.OrMatcher;
import ca.odell.glazedlists.impl.matchers.TrueMatcher;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import de.unijena.bioinf.ms.gui.fingerid.DBFilterPanel;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;

public class DatabaseFilterMatcherEditor extends AbstractMatcherEditor<FingerprintCandidateBean> {

    public DatabaseFilterMatcherEditor(DBFilterPanel panel) {
        panel.addFilterChangeListener((filterSet, sketched) -> {
            Matcher<FingerprintCandidateBean> combinedMatcher;
            if (filterSet > 0 && sketched) {
                combinedMatcher = new OrMatcher<>(new DatabaseMatcher(filterSet), new SketchedMatcher());
            } else if (filterSet == 0 && sketched) {
                combinedMatcher = new SketchedMatcher();
            } else if (filterSet > 0) {
                combinedMatcher = new DatabaseMatcher(filterSet);
            } else {
                combinedMatcher = TrueMatcher.getInstance();
            }
            fireChanged(combinedMatcher);
        });
    }

    public static class DatabaseMatcher implements Matcher<FingerprintCandidateBean> {
        final long filterSet;

        public DatabaseMatcher(long filterSet) {
            this.filterSet = filterSet;
        }

        @Override
        public boolean matches(FingerprintCandidateBean candidate) {
            return (filterSet & candidate.getMergedDBFlags()) != 0;
        }
    }

    public static class SketchedMatcher implements Matcher<FingerprintCandidateBean> {
        @Override
        public boolean matches(FingerprintCandidateBean candidate) {
            return candidate.isSketched();
        }
    }
}
