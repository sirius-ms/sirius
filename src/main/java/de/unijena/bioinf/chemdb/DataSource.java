package de.unijena.bioinf.chemdb;

import java.util.Arrays;
import java.util.Locale;

public enum DataSource {
    ALL("all", 0, null, null),
    PUBCHEM("PubChem", 2, "SELECT compound_id FROM ref.pubchem WHERE inchi_key_1 = ?", "https://pubchem.ncbi.nlm.nih.gov/compound/%s"),
    MESH("MeSH", 4, "SELECT compound_id FROM ref.mesh WHERE inchi_key_1 = ?", "http://www.ncbi.nlm.nih.gov/mesh/%s"),
    HMDB("HMDB", 8, "SELECT hmdb_id FROM ref.hmdb WHERE inchi_key_1 = ?", "http://www.hmdb.ca/metabolites/%s"),
    KNAPSACK("KNApSAcK", 16, "SELECT knapsack_id FROM ref.knapsack WHERE inchi_key_1 = ?", "http://kanaya.naist.jp/knapsack_jsp/information.jsp?word=C%08d"),
    CHEBI("CHEBI", 32, "SELECT chebi_id FROM ref.chebi WHERE inchi_key_1 = ?", "https://www.ebi.ac.uk/chebi/searchId.do?chebiId=%s"),
    PUBMED("PubMed", 64, null, null),
    BIO("Bio Database", makeBIOFLAG(), null, null),
    KEGG("KEGG", 256, "SELECT kegg_id FROM ref.kegg WHERE inchi_key_1 = ?", "http://www.kegg.jp/dbget-bin/www_bget?cpd:%s"),
    HSDB("HSDB", 512, "SELECT cas FROM ref.hsdb WHERE inchi_key_1 = ?", null),
    MACONDA("Maconda", 1024, "SELECT maconda_id FROM ref.maconda WHERE inchi_key_1 = ?", "http://www.maconda.bham.ac.uk/contaminant.php?id=%d"),
    METACYC("Biocyc", 2048, "SELECT unique_id FROM ref.biocyc WHERE inchi_key_1 = ?", "http://biocyc.org/compound?orgid=META&id=%s"),
    GNPS("GNPS", 4096, "SELECT id FROM ref.gnps WHERE inchi_key_1 = ?", "https://gnps.ucsd.edu/ProteoSAFe/gnpslibraryspectrum.jsp?SpectrumID=%s"),
    ZINCBIO("ZINC bio", 8192, "SELECT zinc_id FROM ref.zincbio WHERE inchi_key_1 = ?", "http://zinc.docking.org/substance/%s"),
    TRAIN("Training Set", 16384, null, null),
    UNDP("Natural Products", 32768, "SELECT undp_id FROM ref.undp WHERE inchi_key_1 = ?", null),
    PLANTCYC("Plantcyc", 131072, "SELECT unique_id FROM ref.plantcyc WHERE inchi_key_1 = ?", "http://pmn.plantcyc.org/compound?orgid=PLANT&id=%s"),
    YMDB("YMDB", 65536, "SELECT ymdb_id FROM ref.ymdb WHERE inchi_key_1 = ?", "http://www.ymdb.ca/compounds/YMDB%d05"),
    KEGGMINE("KEGG Mine", 8589934592L, null, null, 8589934592L | 256L, true),
    ECOCYCMINE("EcoCyc Mine", 17179869184L, null, null, 17179869184L | 2048L, true),
    YMDBMINE("YMDB Mine", 34359738368L, null, null, 34359738368L | 65536L, true);


    // additional field
    public final long flag;
    public final String realName;
    public final String sqlQuery;
    public final long searchFlag;
    public final String URI;
    public final boolean mines;

    DataSource(String realName, long flag, String sqlQuery, String uri) {
        this(realName, flag, sqlQuery, uri, flag, false);
    }

    DataSource(String realName, long flag, String sqlQuery, String uri, long searchFlag, boolean mines) {
        this.realName = realName;
        this.flag = flag;
        this.sqlQuery = sqlQuery;
        this.URI = uri;
        this.searchFlag = searchFlag;
        this.mines = mines;
    }

    public String getLink(String id) {
        if (this.URI == null) return null;
        if (DatasourceService.NUMPAT.matcher(URI).find()) {
            return String.format(Locale.US, URI, Integer.parseInt(id));
        } else {
            return String.format(Locale.US, URI, id);
        }
    }

    public static boolean isBio(long flags) {
        return (flags & BIOFLAG()) != 0;
    }

    public boolean isBio() {
        return isBio(flag);
    }

    public static DataSource[] valuesNoALL() {
        return Arrays.stream(DataSource.values()).filter(it -> it != ALL).toArray(DataSource[]::new);
    }

    public static DataSource[] valuesNoALLNoMINES() {
        return Arrays.stream(DataSource.values()).filter(it -> it != ALL && !it.mines).toArray(DataSource[]::new);
    }

    // 4294967292
    public static long BIOFLAG() {
        return BIO.flag;
    }

    private static long makeBIOFLAG() {
        long bioflag = 0L;
        for (int i = 2; i < 32; ++i) {
            bioflag |= (1L << i);
        }
        return bioflag;
    }
}
