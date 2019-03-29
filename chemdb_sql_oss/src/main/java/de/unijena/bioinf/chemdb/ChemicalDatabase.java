package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.fingerid.connection_pooling.ConnectionPool;
import de.unijena.bioinf.fingerid.connection_pooling.PoolFunction;
import de.unijena.bioinf.fingerid.connection_pooling.PooledConnection;
import de.unijena.bioinf.fingerid.connection_pooling.PooledDB;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class ChemicalDatabase extends AbstractChemicalDatabase implements PooledDB<Connection> {

    // temporary switch
    public static final boolean USE_NEW_FINGERPRINTS = true;

    private final static String STRUCTURES_TABLE = USE_NEW_FINGERPRINTS ? "tmp.structures" : "structures";
    private final static String FINGERPRINT_TABLE = USE_NEW_FINGERPRINTS ? "tmp.fingerprints" : "fingerprints";
    private final static String FINGERPRINT_ID = USE_NEW_FINGERPRINTS ? "2" : "1";

    private static final int DEFAULT_SQL_CAPACITY = 5;
    protected static final Logger log = LoggerFactory.getLogger(ChemicalDatabase.class);

    static {
        FingerIDProperties.fingeridVersion();//just to load the props
    }

    protected final ConnectionPool<Connection> connection;
    protected String host, username, password;


    /**
     * initialize a chemical database connection using default values for password and username
     * <p>
     * note: We assume that the chemical database can only be accessed read-only from the
     * local network. Otherwise, releasing password and usernames together with the bytecode
     * would be a security problem.
     */
    public ChemicalDatabase(final int numOfConnections) {
        setup();
        connection = new ConnectionPool<>(new SqlConnector(host, username, password), numOfConnections);
    }

    public ChemicalDatabase() {
        this(DEFAULT_SQL_CAPACITY);
    }


    protected ChemicalDatabase(ChemicalDatabase db) {
        this.connection = db.connection.newSharedConnectionPool();
        this.host = db.host;
        this.username = db.username;
        this.password = db.password;

    }

    @Override
    public ChemicalDatabase clone() {
        return new ChemicalDatabase(this);
    }


    private void setup() {
        // check for system variables -> we do not want defaults because it is secret
        if (host == null)
            this.host = PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.fingerid.chemical_db.host");
        if (username == null)
            this.username = PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.fingerid.chemical_db.username");
        if (password == null)
            this.password = PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.fingerid.chemical_db.password");
    }

    /**
     * initialize a chemical database connection using the given authentification values
     *
     * @param host     JDBC URI
     * @param username
     * @param password
     */
    public ChemicalDatabase(String host, String username, String password) {
        this.host = host;
        this.username = username;
        this.password = password;
        setup();
        this.connection = new ConnectionPool<>(new SqlConnector(this.host, this.username, this.password), DEFAULT_SQL_CAPACITY);
    }

//    public BioFilter getBioFilter() {
//        return bioFilter;
//    }

   /* public void setBioFilter(BioFilter bioFilter) {
        this.bioFilter = bioFilter;
    }*/

    /**
     * Search for molecular formulas in the database
     *
     * @param mass      exact mass of the ion
     * @param deviation allowed mass deviation
     * @param ionType   adduct of the ion
     * @return list of formula candidates which theoretical mass (+ adduct mass) is within the given mass window
     */
    public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        return lookupMolecularFormulas(BioFilter.ALL, mass, deviation, ionType);
    }

    public List<FormulaCandidate> lookupMolecularFormulas(BioFilter bioFilter, double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        final ArrayList<FormulaCandidate> xs = new ArrayList<>();
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            try (final PreparedStatement statement = c.connection.prepareStatement(
                    "SELECT formula, flags FROM formulas WHERE exactmass >= ? AND exactmass <= ?"
            )) {
                xs.addAll(lookupFormulaWithIon(bioFilter, statement, mass, deviation, ionType));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } catch (IOException | SQLException e) {
            throw new ChemicalDatabaseException(e);
        }
        return xs;
    }


    public long getFlagsByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            try (final PreparedStatement statement = c.connection.prepareStatement(
                    "SELECT flags FROM formulas WHERE formula = ?"
            )) {
                statement.setString(1, formula.toString());
                try (final ResultSet set = statement.executeQuery()) {
                    if (set.next())
                        return set.getLong(1);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | SQLException e) {
            throw new ChemicalDatabaseException(e);
        }
        return 0;
    }

    /**
     * Search for molecular formulas in the database
     *
     * @param mass      exact mass of the ion
     * @param deviation allowed mass deviation
     * @param ionTypes  allowed adducts of the ion
     * @return list of formula candidates which theoretical mass (+ adduct mass) is within the given mass window
     */
    public List<List<FormulaCandidate>> lookupMolecularFormulas(BioFilter bioFilter, double mass, Deviation deviation, PrecursorIonType[] ionTypes) throws ChemicalDatabaseException {
        final ArrayList<List<FormulaCandidate>> xs = new ArrayList<>();
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            final PreparedStatement statement = c.connection.prepareStatement(
                    "SELECT formula, flags FROM formulas WHERE exactmass >= ? AND exactmass <= ?"
            );
            for (PrecursorIonType ionType : ionTypes) {
                try {
                    xs.add(lookupFormulaWithIon(bioFilter, statement, mass, deviation, ionType));
                } catch (ChemicalDatabaseException e) {
                    throw new ChemicalDatabaseException(e);
                }
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } catch (IOException | SQLException e) {
            log.error(e.getMessage(), e);
            throw new ChemicalDatabaseException(e);
        }
        return xs;
    }

    private List<FormulaCandidate> lookupFormulaWithIon(BioFilter bioFilter, PreparedStatement statement, double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException, SQLException {
        if (ionType.isIntrinsicalCharged()) {
            final List<FormulaCandidate> protonated = lookupFormulaWithIonIntrinsicalChargedAreConsidered(bioFilter, statement, mass, deviation, ionType.getCharge() > 0 ? PrecursorIonType.getPrecursorIonType("[M+H]+") : PrecursorIonType.getPrecursorIonType("[M-H]-"));
            final List<FormulaCandidate> intrinsical = lookupFormulaWithIonIntrinsicalChargedAreConsidered(bioFilter, statement, mass, deviation, ionType);
            // merge both together
            final HashMap<MolecularFormula, FormulaCandidate> map = new HashMap<>();
            for (FormulaCandidate fc : intrinsical) {
                map.put(fc.formula, fc);
            }
            final MolecularFormula hydrogen = MolecularFormula.parse("H");
            for (FormulaCandidate fc : protonated) {
                final MolecularFormula intrinsic = ionType.getCharge() > 0 ? fc.formula.subtract(hydrogen) : fc.formula.add(hydrogen);
                map.put(intrinsic, new FormulaCandidate(intrinsic, ionType, fc.bitset));
            }
            return new ArrayList<>(map.values());
        } else {
            return lookupFormulaWithIonIntrinsicalChargedAreConsidered(bioFilter, statement, mass, deviation, ionType);
        }
    }

    private List<FormulaCandidate> lookupFormulaWithIonIntrinsicalChargedAreConsidered(BioFilter bioFilter, PreparedStatement statement, double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException, SQLException {
        final double delta = deviation.absoluteFor(mass);
        final double neutralMass = ionType.precursorMassToNeutralMass(mass);
        final double minmz = neutralMass - delta;
        final double maxmz = neutralMass + delta;
        final ArrayList<FormulaCandidate> list = new ArrayList<>();
        statement.setDouble(1, minmz);
        statement.setDouble(2, maxmz);
        try (final ResultSet set = statement.executeQuery()) {
            while (set.next()) {
                final long flag = set.getLong(2);
                final boolean isPubchemOnly = !DatasourceService.isBio(flag);
                if (bioFilter == BioFilter.ONLY_BIO && isPubchemOnly) continue;
                if (bioFilter == BioFilter.ONLY_NONBIO && !isPubchemOnly) continue;
                final FormulaCandidate fc = new FormulaCandidate(MolecularFormula.parse(set.getString(1)), ionType, set.getLong(2));
                list.add(fc);
            }
        } catch (SQLException e) {
            throw new ChemicalDatabaseException(e);
        }
        return list;
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        return lookupStructuresByFormula(BioFilter.ALL, formula);
    }

    public List<CompoundCandidate> lookupStructuresByFormula(BioFilter bioFilter, MolecularFormula formula) throws ChemicalDatabaseException {
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            return lookupStructuresByFormula(bioFilter, formula, c);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } catch (IOException | SQLException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    private List<CompoundCandidate> lookupStructuresByFormula(BioFilter bioFilter, MolecularFormula formula, final PooledConnection<Connection> c) throws SQLException {
        final boolean enforceBio = bioFilter == BioFilter.ONLY_BIO;
        final PreparedStatement statement;
        if (enforceBio) {
            final long bioflag = DatasourceService.BIOFLAG;
            statement = c.connection.prepareStatement("SELECT inchi_key_1, inchi, name, smiles, flags, xlogp FROM " + STRUCTURES_TABLE + " WHERE formula = ? AND (flags & " + bioflag + " ) != 0");
        } else {
            statement = c.connection.prepareStatement("SELECT inchi_key_1, inchi, name, smiles, flags, xlogp FROM " + STRUCTURES_TABLE + " WHERE formula = ?");
        }
        statement.setString(1, formula.toString());
        ArrayList<CompoundCandidate> candidates = new ArrayList<>();
        try (final ResultSet set = statement.executeQuery()) {
            while (set.next()) {
                final CompoundCandidate candidate = new CompoundCandidate(new InChI(set.getString(1), set.getString(2)));
                candidate.setName(set.getString(3));
                candidate.setSmiles(set.getString(4));
                candidate.setBitset(set.getLong(5));
                candidate.setXlogp(set.getDouble(6));
                candidates.add(candidate);
            }
        }
        return candidates;
    }


    public List<FingerprintCandidate> lookupStructuresAndFingerprintsByFormula(BioFilter bioFilter, MolecularFormula formula) throws ChemicalDatabaseException {
        return lookupStructuresAndFingerprintsByFormula(bioFilter, formula, new ArrayList<>());
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        return lookupStructuresAndFingerprintsByFormula(BioFilter.ALL, formula, fingerprintCandidates);
    }

    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(BioFilter bioFilter, MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            // first lookup structures
            final List<CompoundCandidate> candidates = lookupStructuresByFormula(bioFilter, formula, c);
            final HashMap<String, CompoundCandidate> hashMap = new HashMap<>(candidates.size());
            for (CompoundCandidate candidate : candidates)
                hashMap.put(candidate.getInchiKey2D(), candidate);

            // optionally lookup citations
            try (final PreparedStatement statement = c.connection.prepareStatement("SELECT inchi_key_1, pmids FROM meta_information WHERE formula = ?")) {
                statement.setString(1, formula.toString());
                try (final ResultSet r = statement.executeQuery()) {
                    while (r.next()) {
                        final String inchikey = r.getString(1);
                        final CompoundCandidate compoundCandidate = hashMap.get(inchikey);
                        if (compoundCandidate != null) {
                            ResultSet rs = r.getArray(2).getResultSet();
                            TIntSet idSet = new TIntHashSet();
                            while (rs.next())
                                idSet.add(rs.getInt(2));
                            compoundCandidate.setPubmedIDs(new PubmedLinks(idSet));
                        }
                    }
                }
            }

            // then lookup fingerprints
            try (final PreparedStatement statement = c.connection.prepareStatement("SELECT inchi_key_1, fingerprint FROM "+FINGERPRINT_TABLE+" WHERE fp_id = "+FINGERPRINT_ID+" AND formula = ?")) {
                statement.setString(1, formula.toString());
                try (final ResultSet r = statement.executeQuery()) {
                    while (r.next()) {
                        final String inchikey = r.getString(1);
                        final CompoundCandidate compoundCandidate = hashMap.get(inchikey);
                        if (compoundCandidate != null) {
                            final Fingerprint fingerprint = parseFingerprint(r, 2);
                            fingerprintCandidates.add(new FingerprintCandidate(compoundCandidate, fingerprint));
                        }
                    }
                }
            }


            return fingerprintCandidates;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fingerprintCandidates;
        } catch (IOException | SQLException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        final ArrayList<FingerprintCandidate> candidates = new ArrayList<>();
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            try (final PreparedStatement statement = c.connection.prepareStatement("SELECT s.inchi_key_1, s.inchi, s.name, s.smiles, s.flags, s.xlogp, f.fingerprint FROM "+STRUCTURES_TABLE+" as s, "+FINGERPRINT_TABLE+" as f WHERE f.fp_id = "+FINGERPRINT_ID+" AND s.inchi_key_1 = ? AND f.inchi_key_1 = s.inchi_key_1")) {
                for (String inchikey : inchi_keys) {
                    statement.setString(1, inchikey);
                    try (final ResultSet set = statement.executeQuery()) {
                        if (set.next()) {
                            final FingerprintCandidate candidate = new FingerprintCandidate(new InChI(set.getString(1), set.getString(2)), parseFingerprint(set, 7));
                            candidate.setName(set.getString(3));
                            candidate.setSmiles(set.getString(4));
                            candidate.setBitset(set.getLong(5));
                            //candidate.setpLayer(set.getInt(6));
                            //candidate.setqLayer(set.getInt(7));
                            candidate.setXlogp(set.getDouble(6));
                            candidates.add(candidate);
                        }
                    }
                }
            }
            return candidates;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return candidates;
        } catch (IOException | SQLException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        final ArrayList<InChI> candidates = new ArrayList<>();
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            try (final PreparedStatement statement = c.connection.prepareStatement("SELECT inchi_key_1, inchi FROM "+STRUCTURES_TABLE+" WHERE inchi_key_1 = ?")) {
                statement.setFetchSize(10000);
                for (String inchikey : inchi_keys) {
                    statement.setString(1, inchikey);
                    try (final ResultSet set = statement.executeQuery()) {
                        if (set.next()) {
                            candidates.add(new InChI(set.getString(1), set.getString(2)));
                        }
                    }
                }
            }
            return candidates;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return candidates;
        } catch (IOException | SQLException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        return lookupFingerprintsByInchis(inchi_keys);
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
        final ArrayList<FingerprintCandidate> candidates = new ArrayList<>();
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            try (final PreparedStatement statement = c.connection.prepareStatement("SELECT fingerprint FROM "+FINGERPRINT_TABLE+" WHERE fp_id = "+FINGERPRINT_ID+" AND inchi_key_1 = ?")) {
                for (CompoundCandidate candidate : compounds) {
                    statement.setString(1, candidate.getInchiKey2D());
                    try (final ResultSet set = statement.executeQuery()) {
                        if (set.next()) {
                            candidates.add(new FingerprintCandidate(candidate, parseFingerprint(set, 1)));
                        }
                    }
                }
            }
            return candidates;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return candidates;
        } catch (IOException | SQLException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws ChemicalDatabaseException {
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            final DatasourceService.Sources[] sources = DatasourceService.Sources.values();
            final PreparedStatement[] statements = new PreparedStatement[sources.length];
            int k = 0;
            for (DatasourceService.Sources source : sources) {
                statements[k++] = source.sqlQuery == null ? null : c.connection.prepareStatement(source.sqlQuery);
            }
            final ArrayList<DBLink> buffer = new ArrayList<>();
            for (CompoundCandidate candidate : sublist) {
                for (int i = 0; i < sources.length; ++i) {
                    final DatasourceService.Sources source = sources[i];
                    if (/* legacy mode */ source == DatasourceService.Sources.PUBCHEM || ((candidate.getBitset() & source.flag)) != 0) {
                        final PreparedStatement statement = statements[i];
                        if (statement != null) {
                            statement.setString(1, candidate.getInchiKey2D());
                            try (final ResultSet set = statement.executeQuery()) {
                                while (set.next()) {
                                    buffer.add(new DBLink(source.name, set.getString(1)));
                                }
                            }
                        }
                    }
                }
                candidate.setLinks(buffer.toArray(new DBLink[buffer.size()]));
                buffer.clear();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | SQLException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            try (final PreparedStatement statement = c.connection.prepareStatement("SELECT distinct r.inchi_key_1, r.inchi FROM pubchem.synonyms as syn, ref.pubchem as r WHERE lower(syn.name) = lower(?) AND r.compound_id = syn.compound_id")) {
                final HashSet<InChI> inchis = new HashSet<>();
                for (String name : names) {
                    statement.setString(1, name);
                    try (final ResultSet set = statement.executeQuery()) {
                        while (set.next()) {
                            inchis.add(new InChI(set.getString(1), set.getString(2)));
                        }
                    }
                }
                return new ArrayList<>(inchis);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } catch (IOException | SQLException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    public <R> R useConnection(PoolFunction<Connection, R> runWithConnection) throws IOException, SQLException, InterruptedException {
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            return runWithConnection.apply(c);
        }
    }

    /**
     * closes all connections and restart them again if necessary
     * Should be called after long times of idling as the database server might shut down the connection
     *
     * @throws ChemicalDatabaseException
     */
    public final void refresh() throws IOException {
        if (connection != null)
            connection.closeAllIdlingConnections();
        else throw new IOException("ConnectionPool of " + getClass().getName() + " is not initialized!");
    }

    @Override
    public boolean hasConnection(int timeout) throws IOException, InterruptedException, SQLException {
        if (timeout < 0) {
            log.warn("Timeout has to be greater than 0. Value=" + timeout + ". Falling back to a default of 30s!");
            timeout = 30;
        }

        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            return c.connection.isValid(timeout);
        } catch (IOException e) {
            if (e.getCause() instanceof SQLException) throw (SQLException) e.getCause();
            throw e;
        }
    }

    @Override
    public int getMaxConnections() {
        return connection.getCapacity();
    }

    @Override
    public int getNumberOfIdlingConnections() {
        return connection.getNumberOfIdlingConnections();
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }

    protected static class SqlConnector implements ConnectionPool.Connection<Connection> {
        static {
            //it seems that tomcat need that to ensure that the driver is loaded before usage
            try {
                log.info("Manually loading psql Driver");
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                log.error("Manual PSQl driver load failed!", e);
            }
        }

        private String host, username, password;

        public SqlConnector(String host, String username, String password) {
            this.host = host;
            this.username = username;
            this.password = password;
        }

        @Override
        public Connection open() throws IOException {
            try {
                final Connection c = DriverManager.getConnection("jdbc:postgresql://" + host + "/pubchem", username, password);
                c.setNetworkTimeout(Runnable::run, 30000);
                return c;
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }

        @Override
        public void close(Connection connection) throws IOException {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
    }


    private Fingerprint parseFingerprint(ResultSet result, int index) throws SQLException {
        try (final ResultSet fp = result.getArray(index).getResultSet()) {
            return parseFingerprint(fp);
        }
    }

    private Fingerprint parseFingerprint(ResultSet fp) throws SQLException {
        TShortArrayList shorts = new TShortArrayList();
        while (fp.next()) {
            final short s = fp.getShort(2);
            shorts.add(s);
        }
        return new ArrayFingerprint(USE_NEW_FINGERPRINTS ?  CdkFingerprintVersion.getExtended() : CdkFingerprintVersion.getComplete(), shorts.toArray());
    }
}
