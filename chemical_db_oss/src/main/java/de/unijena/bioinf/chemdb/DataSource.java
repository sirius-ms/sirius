/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import java.util.Arrays;
import java.util.Locale;

//ATTENTION Do not use `:{}[];` in the String here because that might break parsing
//Names: names should be descriptive and short because they have to be rendered in the GUI
//Flags/BitSets: Every bit that is set in our postgres db should also represented as DataSource here
public enum DataSource {
    ALL("All included DBs", 0, null, null, null),
    ALL_BUT_INSILICO("All DBs, except combinatorial DBs", 2|makeBIOFLAG(), null, null, null),
    PUBCHEM("PubChem", 2, "compound_id","pubchem", "https://pubchem.ncbi.nlm.nih.gov/compound/%s"),
    MESH("MeSH", 4, "compound_id", "hasmesh", "http://www.ncbi.nlm.nih.gov/mesh/%s"),
    HMDB("HMDB", 8, "hmdb_id", "hmdb", "http://www.hmdb.ca/metabolites/HMDB%07d"),
    KNAPSACK("KNApSAcK", 16, "knapsack_id", "knapsack", "http://kanaya.naist.jp/knapsack_jsp/information.jsp?word=C%08d"),
    CHEBI("CHEBI", 32, "chebi_id","chebi", "https://www.ebi.ac.uk/chebi/searchId.do?chebiId=%s"),
    PUBMED("PubMed", 64, null, null, null),
    //the BIO flag is a collection of many bio-like databases. Furthermore, there is a flag 128 in the PSQL structure datagase which was called bio. This is part of the combined bio-like database. Soon, the 'bio' flag 128 will be obsolete and replaced by different flags.
    BIO("Bio Database", makeBIOFLAG(), null, null, null), //todo obsolete?
    KEGG("KEGG", 256, "kegg_id","kegg", "http://www.kegg.jp/dbget-bin/www_bget?cpd:%s"),
    HSDB("HSDB", 512, "cas","hsdb", null),
    MACONDA("Maconda", 1024, "maconda_id","maconda", "http://www.maconda.bham.ac.uk/contaminant.php?id=%d"),
    METACYC("Biocyc", 2048, "unique_id","biocyc", "http://biocyc.org/compound?orgid=META&id=%s"),
    GNPS("GNPS", 4096, "id","gnps", "https://gnps.ucsd.edu/ProteoSAFe/gnpslibraryspectrum.jsp?SpectrumID=%s"),
    ZINCBIO("ZINC bio", 8192, "zinc_id","zincbio", "http://zinc.docking.org/substance/%s"),
    TRAIN("Training Set", 16384, null, null, null), //not part of the PSQL database anymore but assigned for each predictor individually
    UNDP("Natural Products", 32768, "undp_id","undp",  null),
    YMDB("YMDB", 65536, "ymdb_id","ymdb", "http://www.ymdb.ca/compounds/YMDB%d05"),
    PLANTCYC("Plantcyc", 131072, "unique_id","plantcyc",  "http://pmn.plantcyc.org/compound?orgid=PLANT&id=%s"),
    NORMAN("NORMAN", 262144,  null,null, null),
    //this is currently only interesting for internal testing.
    ADDITIONAL("additional", 524288,  null,null,null, 0, false), //proably mostly training structures, but maybe more.
    SUPERNATURAL("SuperNatural", 1048576,  "id", "supernatural", "http://bioinf-applied.charite.de/supernatural_new/index.php?site=compound_search&start=0&supplier=all&tox=any&classification=all&compound_input=true&sn_id=%s"),
    COCONUT("COCONUT", 2097152,  "id", "coconut", null),
    PUBCHEMANNOTATIONBIO("PubChem class - bio and metabolites", 16777216,  null,null,null, 0, false), //2**24; Pubchem Annotations now have a separate flag
    PUBCHEMANNOTATIONDRUG("PubChem class - drug", 33554432,  null,null,null, 0, false),
    PUBCHEMANNOTATIONSAFETYANDTOXIC("PubChem class - safety and toxic", 67108864,  null,null,null, 0, false),
    PUBCHEMANNOTATIONFOOD("PubChem class - food", 134217728,  null,null,null, 0, false),

    //everything with flags greater equal to 2**32 are databases of artificial structures.
    KEGGMINE("KEGG Mine", 8589934592L, null,null, null, 8589934592L | 256L, true),
    ECOCYCMINE("EcoCyc Mine", 17179869184L, null,null, null, 17179869184L | 2048L, true),
    YMDBMINE("YMDB Mine", 34359738368L, null,null, null, 34359738368L | 65536L, true);


    // additional field
    public final long flag;
    public final String realName;
    public final String sqlIdColumn;
    public final String sqlRefTable;
    public final long searchFlag;
    public final String URI;
    public final boolean mines;

    DataSource(String realName, long flag, String sqlIdColumn, String sqlRefTable, String uri) {
        this(realName, flag, sqlIdColumn, sqlRefTable, uri, flag, false);
    }

    DataSource(String realName, long flag, String sqlIdColumn, String sqlRefTable, String uri, long searchFlag, boolean mines) {
        this.realName = realName;
        this.flag = flag;
        this.sqlIdColumn = sqlIdColumn;
        this.sqlRefTable = sqlRefTable;
        this.URI = uri;
        this.searchFlag = searchFlag;
        this.mines = mines;
    }

    public String getLink(String id) {
        if (this.URI == null) return null;
        if (DataSources.NUMPAT.matcher(URI).find()) {
            return String.format(Locale.US, URI, Integer.parseInt(id));
        } else {
            return String.format(Locale.US, URI, id);
        }
    }

    public String realName() {
        return realName;
    }

    public long flag() {
        return flag;
    }

    public static boolean isBioOnly(long flags) {
        return (flags & BIO.flag) != 0;
    }

    public boolean isBioOnly() {
        return isBioOnly(flag);
    }

    public boolean isNotBioOnly() {
        return !isBioOnly(flag);
    }

    public static DataSource[] valuesNoALL() {
        return Arrays.stream(DataSource.values()).filter(it -> it != ALL).toArray(DataSource[]::new);
    }

    public static DataSource[] valuesNoALLNoMINES() {
        return Arrays.stream(DataSource.values()).filter(it -> it != ALL && !it.mines).toArray(DataSource[]::new);
    }

    // 4294959036
    private static long makeBIOFLAG() {
        long bioflag = 0L;
        for (int i = 2; i < 32; ++i) {
            if (i==6 || i==13 || i==19) continue; //PubMed and Zinc_bio should not be included into bio database
            bioflag |= (1L << i);
        }
        return bioflag;
    }
}
