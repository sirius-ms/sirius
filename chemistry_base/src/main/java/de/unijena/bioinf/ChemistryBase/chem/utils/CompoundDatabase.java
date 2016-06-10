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

package de.unijena.bioinf.ChemistryBase.chem.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * References to other databases can be stored as 32 or 64 bit sets. This class decodes these
 * bitsets into names.
 */
public enum CompoundDatabase {

        PUBCHEM("PubChem", 0, "https://pubchem.ncbi.nlm.nih.gov/compound/%s"),
        MESH("MeSH", 4, "http://www.ncbi.nlm.nih.gov/mesh/%s"),
        HMDB("HMDB", 8, "http://www.hmdb.ca/metabolites/%s"),
        KNAPSACK("KNApSAcK",16, "http://kanaya.naist.jp/knapsack_jsp/information.jsp?sname=C_ID&word=%s"),
        CHEBI("CHEBI",32, "https://www.ebi.ac.uk/chebi/searchId.do?chebiId=%s"),
        PUBMED("PubMed", 64,null),
        BIO("biological", 128,null),
        KEGG("KEGG", 256, "http://www.kegg.jp/dbget-bin/www_bget?cpd:%s"),
        HSDB("HSDB", 512, null),
        MACONDA("Maconda", 1024, "http://www.maconda.bham.ac.uk/contaminant.php?id="),
        METACYC("Metacyc", 2048, "http://metacyc.org/META/new-image?object=%s"),
        GNPS("GNPS", 4096, "https://gnps.ucsd.edu/ProteoSAFe/gnpslibraryspectrum.jsp?SpectrumID=%s"),
        ZINCBIO("ZINC bio", 8192, "http://zinc.docking.org/substance/%s"),
        TRAIN("training set", 16384,null);

        public final int flag; public final String name;
        public final String URI;
        private CompoundDatabase(String name, int flag, String uri) {
            this.name = name;
            this.flag = flag;
            this.URI = uri;
        }


    public static Set<String> getDataSourcesFromBitFlags(int flags) {
        final HashSet<String> set = new HashSet<>();
        return getDataSourcesFromBitFlags(set, flags);
    }

    public static Set<String> getDataSourcesFromBitFlags(Set<String> set, int flags) {
        for (CompoundDatabase s : CompoundDatabase.values()) {
            if ((flags & s.flag) == s.flag) {
                set.add(s.name);
            }
        }
        return set;
    }

}
