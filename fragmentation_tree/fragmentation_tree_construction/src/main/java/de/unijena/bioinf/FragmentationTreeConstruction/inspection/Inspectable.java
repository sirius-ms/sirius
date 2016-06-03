/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.inspection;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.JDKDocument;

public interface Inspectable {

    public <G, D, L> void inspect(DataDocument<G, D, L> document, D dictionary);

    public static class Utils {
        private static JDKDocument javaobj = new JDKDocument();
        public static <G, D, L> void keyValues(DataDocument<G, D, L> document, D dictionary, Object... pairs) {
            String key = null;
            for (int i=0; i < pairs.length; ++i) {
                if (i % 2 == 0) {
                    key = (String)pairs[i];
                } else {
                    document.addToDictionary(dictionary, key, DataDocument.transform(javaobj, document, pairs[i]));
                }
            }
        }
    }

}
