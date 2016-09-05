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

package de.unijena.bioinf.sirius.gui.fingerid;

import java.util.HashSet;
import java.util.Set;

/**
 * This is a copy of DatasourceService in ChemDb. We should fix that at some point;
 */
public class DatasourceService2 {

    public enum Sources {

        PUBCHEM("PubChem", 0,   "SELECT compound_id FROM ref.pubchem WHERE inchi_key_1 = ?", "https://pubchem.ncbi.nlm.nih.gov/compound/%s"),
        MESH("MeSH", 4,         "SELECT name FROM ref.mesh WHERE inchi_key_1 = ?", "http://www.ncbi.nlm.nih.gov/mesh/%s"),
        HMDB("HMDB", 8,         "SELECT hmdb_id FROM ref.hmdb WHERE inchi_key_1 = ?", "http://www.hmdb.ca/metabolites/%s"),
        KNAPSACK("KNApSAcK",16, "SELECT knapsack_id FROM ref.knapsack WHERE inchi_key_1 = ?", "http://kanaya.naist.jp/knapsack_jsp/information.jsp?sname=C_ID&word=%s"),
        CHEBI("CHEBI",32,       "SELECT chebi_id FROM ref.chebi WHERE inchi_key_1 = ?", "https://www.ebi.ac.uk/chebi/searchId.do?chebiId=%s"),
        PUBMED("PubMed", 64,    null,null),
        BIO("biological", 128,  null,null),
        KEGG("KEGG", 256,       "SELECT kegg_id FROM ref.kegg WHERE inchi_key_1 = ?", "http://www.kegg.jp/dbget-bin/www_bget?cpd:%s"),
        HSDB("HSDB", 512,       "SELECT cas FROM ref.hsdb WHERE inchi_key_1 = ?", null),
        MACONDA("Maconda", 1024,"SELECT maconda_id FROM ref.maconda WHERE inchi_key_1 = ?", "http://www.maconda.bham.ac.uk/contaminant.php?id="),
        METACYC("Biocyc", 2048,"SELECT metacyc_id FROM ref.metacyc WHERE inchi_key_1 = ?", "http://metacyc.org/META/new-image?object=%s"),
        GNPS("GNPS", 4096,      "SELECT id FROM ref.gnps WHERE inchi_key_1 = ?", "https://gnps.ucsd.edu/ProteoSAFe/gnpslibraryspectrum.jsp?SpectrumID=%s"),
        ZINCBIO("ZINC bio", 8192,"SELECT zinc_id FROM ref.zincbio WHERE inchi_key_1 = ?", "http://zinc.docking.org/substance/%s"),
        TRAIN("training set", 16384, null,null),
        UNDP("Natural Products", 32768, "SELECT undp_id FROM ref.undp WHERE inchi_key_1 = ?", null),

        CUSTOM("custom", 1073741824, null, null);

        public final int flag; public final String name; public final String sqlQuery;
        public final String URI;
        private Sources(String name, int flag, String sqlQuery, String uri) {
            this.name = name;
            this.flag = flag;
            this.sqlQuery = sqlQuery;
            this.URI = uri;
        }

    }

    public static DatasourceService2.Sources getFromName(String name) {
        for (Sources s : Sources.values())
            if (s.name.equalsIgnoreCase(name)) return s;
        return null;
    }

    public static Set<String> getDataSourcesFromBitFlags(int flags) {
        final HashSet<String> set = new HashSet<>();
        return getDataSourcesFromBitFlags(set, flags);
    }

    public static Set<String> getDataSourcesFromBitFlags(Set<String> set, int flags) {
        for (Sources s : Sources.values()) {
            if ((flags & s.flag) == s.flag) {
                set.add(s.name);
            }
        }
        return set;
    }

}