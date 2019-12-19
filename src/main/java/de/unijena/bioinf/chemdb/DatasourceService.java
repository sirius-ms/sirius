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

package de.unijena.bioinf.chemdb;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * References to other databases can be stored as 32 or 64 bit sets. This class decodes these
 * bitsets into names.
 */
public class DatasourceService {


    public final static long BIOFLAG; // 4294967292

    static {
        long bioflag = 0L;
        for (int i=2; i < 32; ++i) {
            bioflag |= (1L<<i);
        }
        BIOFLAG = bioflag;
    }

    public static boolean isBio(long flags) {
        return (flags & BIOFLAG) != 0;
    }

    public enum Sources {

        PUBCHEM("PubChem", 2,   "SELECT compound_id FROM ref.pubchem WHERE inchi_key_1 = ?", "https://pubchem.ncbi.nlm.nih.gov/compound/%s"),
        MESH("MeSH", 4,         "SELECT compound_id FROM ref.mesh WHERE inchi_key_1 = ?", "http://www.ncbi.nlm.nih.gov/mesh/%s"),
        HMDB("HMDB", 8,         "SELECT hmdb_id FROM ref.hmdb WHERE inchi_key_1 = ?", "http://www.hmdb.ca/metabolites/%s"),
        KNAPSACK("KNApSAcK",16, "SELECT knapsack_id FROM ref.knapsack WHERE inchi_key_1 = ?", "http://kanaya.naist.jp/knapsack_jsp/information.jsp?word=C%08d"),
        CHEBI("CHEBI",32,       "SELECT chebi_id FROM ref.chebi WHERE inchi_key_1 = ?", "https://www.ebi.ac.uk/chebi/searchId.do?chebiId=%s"),
        PUBMED("PubMed", 64,    null,null),
        BIO("biological", 128,  null,null, 0), //todo obsolete?
        KEGG("KEGG", 256,       "SELECT kegg_id FROM ref.kegg WHERE inchi_key_1 = ?", "http://www.kegg.jp/dbget-bin/www_bget?cpd:%s"),
        HSDB("HSDB", 512,       "SELECT cas FROM ref.hsdb WHERE inchi_key_1 = ?", null),
        MACONDA("Maconda", 1024,"SELECT maconda_id FROM ref.maconda WHERE inchi_key_1 = ?", "http://www.maconda.bham.ac.uk/contaminant.php?id=%d"),
        METACYC("Biocyc", 2048,"SELECT unique_id FROM ref.biocyc WHERE inchi_key_1 = ?", "http://biocyc.org/compound?orgid=META&id=%s"),
        GNPS("GNPS", 4096,      "SELECT id FROM ref.gnps WHERE inchi_key_1 = ?", "https://gnps.ucsd.edu/ProteoSAFe/gnpslibraryspectrum.jsp?SpectrumID=%s"),
        ZINCBIO("ZINC bio", 8192,"SELECT zinc_id FROM ref.zincbio WHERE inchi_key_1 = ?", "http://zinc.docking.org/substance/%s"),
        TRAIN("training set", 16384, null,null), //todo obsolete?
        UNDP("Natural Products", 32768, "SELECT undp_id FROM ref.undp WHERE inchi_key_1 = ?", null),
        YMDB("YMDB", 65536,         "SELECT ymdb_id FROM ref.ymdb WHERE inchi_key_1 = ?", "http://www.ymdb.ca/compounds/YMDB%d05"),
        PLANTCYC("Plantcyc", 131072, "SELECT unique_id FROM ref.plantcyc WHERE inchi_key_1 = ?", "http://pmn.plantcyc.org/compound?orgid=PLANT&id=%s"),
        NORMAN("NORMAN", 262144,  null,null), //TODO implement importer
        ADDITIONAL("additional", 524288,  null,null, 0), //proably mostly training structures, but maybe more.
        PUBCHEMANNOTATIONBIO("PubChem classifications: bio and metabolites", 16777216,  null,null, 0), //2**24; Pubchem Annotations now have a separate flag
        PUBCHEMANNOTATIONDRUG("PubChem classifications: drug", 33554432,  null,null, 0),
        PUBCHEMANNOTATIONSAFETYANDTOXIC("PubChem classifications: safety and toxic", 67108864,  null,null, 0),
        PUBCHEMANNOTATIONFOOD("PubChem classification: food", 134217728,  null,null, 0),

        KEGGMINE("KEGG Mine", 8589934592L, null,null, 8589934592L | 256L ),
        ECOCYCMINE("EcoCyc Mine", 17179869184L, null,null, 17179869184L | 2048L),
        YMDBMINE("YMDB Mine", 34359738368L, null,null, 34359738368L | 65536L);

        public final long flag; public final String name; public final String sqlQuery;
        public final long searchFlag;
        public final String URI;
        Sources(String name, long flag, String sqlQuery, String uri) {
            this(name,flag,sqlQuery,uri,flag);
        }
        Sources(String name, long flag, String sqlQuery, String uri, long searchFlag) {
            this.name = name;
            this.flag = flag;
            this.sqlQuery = sqlQuery;
            this.URI = uri;
            this.searchFlag = searchFlag;
        }

        protected static Pattern NUMPAT = Pattern.compile("%(?:[0-9 ,+\\-]*)d");
        public String getLink(String id) {
            if (this.URI==null) return null;
            if (NUMPAT.matcher(URI).find()) {
                return String.format(Locale.US, URI, Integer.parseInt(id));
            } else {
                return String.format(Locale.US, URI, id);
            }
        }

    }


    public static Set<String> getDataSourcesFromBitFlags(long flags) {
        final HashSet<String> set = new HashSet<>();
        return getDataSourcesFromBitFlags(set, flags);
    }

    public static Set<String> getDataSourcesFromBitFlags(Set<String> set, long flags) {
        for (Sources s : Sources.values()) {
            if ((flags & s.flag) == s.flag) {
                set.add(s.name);
            }
        }
        return set;
    }

    public static DatasourceService.Sources getFromName(String name) {
        for (Sources s : Sources.values())
            if (s.name.equalsIgnoreCase(name)) return s;
        return null;
    }
}
