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
import java.util.regex.Pattern;

// BIO FLAG IS: 4294401852
//ATTENTION Do not use `:{}[];` in the String here because that might break parsing
//Names: names should be descriptive and short because they have to be rendered in the GUI
//Flags/BitSets: Every bit that is set in our postgres db should also represented as DataSource here
public enum DataSource {

    ALL("All included DBs", makeALLFLAG(), null, null, null, null),
    //the BIO flag is a collection of many bio-like databases. Furthermore, there was a flag 128 in the PSQL structure database which was called bio. This is now obsolete and replaced by a combined flags.
    BIO("Bio Database", makeBIOFLAG(), null, null, null, null), //todo make distinction to normal databases more clear
    PUBCHEM("PubChem", 2, "compound_id","pubchem", "https://pubchem.ncbi.nlm.nih.gov/compound/%s", new Publication("Kim S et al., PubChem in 2021: new data content and improved web interfaces. Nucleic Acids Res. 2021", "10.1093/nar/gkaa971")),
    MESH("MeSH", 4, "compound_id", "hasmesh", null, null),
    HMDB("HMDB", 8, "hmdb_id", "hmdb", "http://www.hmdb.ca/metabolites/%s", new Publication("Wishart DS, Guo AC, Oler E, et al., HMDB 5.0: the Human Metabolome Database for 2022. Nucleic Acids Res. 2022", "10.1093/nar/gkab1062")),
    //Knapsack maps via inchikey, now using COCONUT as source // http://www.knapsackfamily.com/knapsack_core/result.php?sname=INCHIKEY&word=JLJLRLWOEMWYQK-BKYUDGNBNA-N
    KNAPSACK("KNApSAcK", 16, "inchi_key", "knapsack", "http://www.knapsackfamily.com/knapsack_core/result.php?sname=INCHIKEY&word=%s", new Publication("Nakamura Y. et al, KNApSAcK Metabolite Activity Database for retrieving the relationships between metabolites and biological activities. Plant Cell Physiol.  2014", "10.1093/pcp/pct176")),
    CHEBI("CHEBI", 32, "chebi_id","chebi", "https://www.ebi.ac.uk/chebi/searchId.do?chebiId=%s", new Publication("Hastings J et al., ChEBI in 2016: Improved services and an expanding collection of metabolites. Nucleic Acids Res. 2016", "10.1093/nar/gkv1031")),
    PUBMED("PubMed", 64, null, null, null, null),
    KEGG("KEGG", 256, "kegg_id","kegg", "http://www.kegg.jp/dbget-bin/www_bget?cpd:%s", new Publication("Kanehisa M and Goto S, KEGG: Kyoto Encyclopedia of Genes and Genomes. Nucleic Acids Res. 2000.", "10.1093/nar/28.1.27")),
    HSDB("HSDB", 512, "cas","hsdb", null, new Publication("Fonger GC et al., The National Library of Medicine's (NLM) Hazardous Substances Data Bank (HSDB): background, recent enhancements and future plans. Toxicology. 2014", "10.1016/j.tox.2014.09.003")),
    MACONDA("Maconda", 1024, "maconda_id","maconda", "http://www.maconda.bham.ac.uk/contaminant.php?id=%d", new Publication("Weber RJM et al., MaConDa: a publicly accessible mass spectrometry contaminants database. Bioinformatics. 2012", "10.1093/bioinformatics/bts527")),
    METACYC("Biocyc", 2048, "unique_id","biocyc", "http://biocyc.org/compound?orgid=META&id=%s", new Publication("Caspi R et al., The MetaCyc database of metabolic pathways and enzymes - a 2019 update, Nucleic Acids Res, 2020", "10.1093/nar/gkz862")),
    GNPS("GNPS", 4096, "id","gnps", "https://gnps.ucsd.edu/ProteoSAFe/gnpslibraryspectrum.jsp?SpectrumID=%s", null), //todo this should be part only of a spectral library search tool.
    TRAIN("Training Set", 16384, null, null, null, null), //not part of the PSQL database anymore but assigned for each predictor individually //todo but we still need that flag, right?
    YMDB("YMDB", 65536, "ymdb_id","ymdb", "http://www.ymdb.ca/compounds/%s", new Publication("Ramirez-Gaona M et al., YMDB 2.0: a significantly expanded version of the yeast metabolome database. Nucleic Acids Res. 2017", "10.1093/nar/gkw1058")),
    PLANTCYC("Plantcyc", 131072, "compound_id","plantcyc",  "http://pmn.plantcyc.org/compound?orgid=PLANT&id=%s", new Publication("Hawkins C et al., Plant Metabolic Network 15: A resource of genome-wide metabolism databases for 126 plants and algae. J Integr Plant Biol. 2021", "10.1111/jipb.13163")),
    NORMAN("NORMAN", 262144,  "norman_susdat_id","norman", null, new Publication("Taha HM et al., The NORMAN Suspect List Exchange (NORMAN-SLE): facilitating European and worldwide collaboration on suspect screening in high resolution mass spectrometry. Environ Sci Eur. 2022", "10.1186/s12302-022-00680-6")),
    SUPERNATURAL("SuperNatural", 1048576,  "id", "supernatural", "http://bioinf-applied.charite.de/supernatural_new/index.php?site=compound_search&start=0&supplier=all&tox=any&classification=all&compound_input=true&sn_id=%s", new Publication("Gallo K et al., SuperNatural 3.0-a database of natural products and natural product-based derivatives. Nucleic Acids Res. 2022.", "10.1093/nar/gkac1008")), //todo this is the new Publication. But dataase version is still SUpernatural II. the link also changed for the new version.
    COCONUT("COCONUT", 2097152,  "id", "coconut", "https://coconut.naturalproducts.net/compound/coconut_id/%s", new Publication("Sorokina M et al., COCONUT online: Collection of Open Natural Products database. J Cheminf. 2021", "10.1186/s13321-020-00478-9")),

    BloodExposome("Blood Exposome", 4194304,  "pubchem_cid", "bloodexposome", "https://bloodexposome.org/#/description?qcid=%s", new Publication("Barupal DK and Fiehn O, Generating the Blood Exposome Database Using a Comprehensive Text Mining and Database Fusion Approach. Environ Health Perspect. 2019", "10.1289/EHP4713")),
    TeroMol("TeroMOL", 8388608,  "mol_id", "teromol", "http://terokit.qmclab.com/molecule.html?MolId=%s", new Publication("Zeng T et al.,Chemotaxonomic Investigation of Plant Terpenoids with an Established Database (TeroMOL). New Phytol. 2022", "10.1111/nph.18133")),

    PUBCHEMANNOTATIONBIO("PubChem class - bio and metabolites", 16777216,  null,null,null, 0, false, new Publication("Kim S et al., PubChem in 2021: new data content and improved web interfaces. Nucleic Acids Res. 2021", "10.1093/nar/gkaa971")), //2**24; Pubchem Annotations now have a separate flag
    PUBCHEMANNOTATIONDRUG("PubChem class - drug", 33554432,  null,null,null, 0, false, new Publication("Kim S et al., PubChem in 2021: new data content and improved web interfaces. Nucleic Acids Res. 2021", "10.1093/nar/gkaa971")),
    PUBCHEMANNOTATIONSAFETYANDTOXIC("PubChem class - safety and toxic", 67108864,  null,null,null, 0, false, new Publication("Kim S et al., PubChem in 2021: new data content and improved web interfaces. Nucleic Acids Res. 2021", "10.1093/nar/gkaa971")),
    PUBCHEMANNOTATIONFOOD("PubChem class - food", 134217728,  null,null,null, 0, false, new Publication("Kim S et al., PubChem in 2021: new data content and improved web interfaces. Nucleic Acids Res. 2021", "10.1093/nar/gkaa971")),

    LOTUS("LOTUS", 268435456,  "id", "lotus", "https://lotus.naturalproducts.net/search/simple/%s", new Publication("Rutz A et al., The LOTUS initiative for open knowledge management in natural products research. eLife. 2022", "10.7554/eLife.70780")),
    FooDB("FooDB", 536870912, "fooddb_id", "foodDB", "https://foodb.ca/compounds/%s", new Publication("www.foodb.ca", null)),//todo not published yet?
    MiMeDB("MiMeDB", 1073741824, "mimeDB_id", "mimeDB", "https://mimedb.org/metabolites/%s", new Publication("Wishart DS et al., MiMeDB: the Human Microbial Metabolome Database. Nucleic Acids Res. 2023", "10.1093/nar/gkac868")),

    LIPIDMAPS("LipidMaps", 2147483648L, "id", "lipidmaps", "https://www.lipidmaps.org/databases/lmsd/%s", new Publication("Sud M et al., LMSD: LIPID MAPS structure database. Nucleic Acids Res. 2006", "10.1093/nar/gkl838")),
    LIPID("Lipid", 4294967296L, null, null, "https://www.lipidmaps.org/databases/lmsd/%s", null), //flag for  El Gordo/Lipid candidates
/*"https://www.lipidmaps.org/rest/compound/abbrev/%s/all/txt"*/ //todo which is the correect query?


    //todo the following flags are burned until we update our PSQL and blob database:  8192, 8589934592L, 17179869184L, 34359738368L.
    // 128, 32768 and 524288 are free again

    //everything with flags greater equal to 2**33 have to be added separately to bio dataSource flag

    //todo we might want to have Massbank and MoNA integrated when providing the remote spectral library feature.
//    MASSBANK("MassBank", 68719476736L, null, null, "https://massbank.eu/MassBank/RecordDisplay?id=%s", null),
    //////////////////////////////////////////


    DSSTox("DSSTox", 137438953472L,  "dsstox_substance_id","dsstox", "https://comptox.epa.gov/dashboard/%s", new Publication("Grulke CM at al., EPA’s DSSTox database: History of development of a curated chemistry resource supporting computational toxicology research. Comput Toxicol. 2019", "10.1016/j.comtox.2019.100096"));


    // additional field
    public final long flag;
    public final String realName;
    public final String sqlIdColumn;
    public final String sqlRefTable;
    public final long searchFlag;
    public final String URI;

    public final Publication publication;
    public final boolean mines;

    DataSource(String realName, long flag, String sqlIdColumn, String sqlRefTable, String uri, Publication publication) {
        this(realName, flag, sqlIdColumn, sqlRefTable, uri, flag, false, publication);
    }

    DataSource(String realName, long flag, String sqlIdColumn, String sqlRefTable, String uri, long searchFlag, boolean mines, Publication publication) {
        this.realName = realName;
        this.flag = flag;
        this.sqlIdColumn = sqlIdColumn;
        this.sqlRefTable = sqlRefTable;
        this.URI = uri;
        this.searchFlag = searchFlag;
        this.mines = mines;
        this.publication = publication;
    }

    private static Pattern NUMPAT = Pattern.compile("%[0-9 ,+\\-]*d");

    public String getLink(String id) {
        if (this.URI == null) return null;
        if (NUMPAT.matcher(URI).find()) {
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
        return flags != 0 && (flags & BIO.flag) != 0;
    }

    public static boolean isInAll(long flags){
        return flags != 0 && (flags & ALL.flag) != 0;
    }

    public boolean isBioOnly() {
        return isBioOnly(flag);
    }

    public boolean isNotBioOnly() {
        return !isBioOnly(flag);
    }

    public static DataSource[] valuesALLBio() {
        return Arrays.stream(DataSource.values()).filter(DataSource::isBioOnly).toArray(DataSource[]::new);
    }

    public static DataSource[] valuesNoALL() {
        return Arrays.stream(DataSource.values()).filter(it -> it != ALL).toArray(DataSource[]::new);
    }


    private final static DataSource[] BIO_DATABASES = new DataSource[] {MESH, HMDB, KNAPSACK,CHEBI,KEGG,HSDB,MACONDA,METACYC,GNPS,TRAIN,YMDB,PLANTCYC,NORMAN,SUPERNATURAL,COCONUT,BloodExposome,TeroMol,PUBCHEMANNOTATIONBIO,PUBCHEMANNOTATIONDRUG,PUBCHEMANNOTATIONSAFETYANDTOXIC,PUBCHEMANNOTATIONFOOD,LOTUS,FooDB,MiMeDB,LIPIDMAPS,LIPID};

    // 4294401852
    private static long makeBIOFLAG() {
        long bioflag = 0L;
        for (int i = 2; i < 32; ++i) {
            if (i==6 || i==7 || i==13 || i==15 || i==19) continue; //excluded PubMed and other flags not included in bio database flag
            bioflag |= (1L << i);
        }
        return bioflag;
    }

    public static long makeALLFLAG(){
        long allflag=0L;
        for(int i = 1; i < 32; i++ ){
            if (i==7 || i==13 || i==15 ||i==19) continue;
            allflag |=(1L << i);
        }
        allflag |=(1L << 37); //This custom adds DSSTox which has a flag >32.
        return allflag;
    }


    /**
     * to reference a publication for a specific data source
     */
    public static class Publication {
        final String citationText;
        final String doi;

        public Publication(String citationText, String doi) {
            this.citationText = citationText;
            this.doi = doi;
        }

        public String getCitationText() {
            return citationText;
        }

        public String getDoi() {
            return doi;
        }
    }
}
