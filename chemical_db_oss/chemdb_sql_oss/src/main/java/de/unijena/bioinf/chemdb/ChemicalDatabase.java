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

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.fingerid.connection_pooling.ConnectionPool;
import de.unijena.bioinf.fingerid.connection_pooling.PoolFunction;
import de.unijena.bioinf.fingerid.connection_pooling.PooledConnection;
import de.unijena.bioinf.fingerid.connection_pooling.PooledDB;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.properties.PropertyManager;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

import static de.unijena.bioinf.ChemistryBase.chem.InChIs.newInChI;

public class ChemicalDatabase implements FilterableChemicalDatabase, PooledDB<Connection> {
    public final static String REF_SCHEME = PropertyManager.getProperty("de.unijena.bioinf.chemdb.scheme.references", null, "ref");
    //REF_MAPPING_TABLE_SUFFIX is included for compatibility reasons all recent database version should contain a VIEW ref.xxx_mapping_id_inchi_key that combines InChIKeys of non-standardized and standardized structures
    public final static String REF_MAPPING_TABLE_SUFFIX = PropertyManager.getProperty("de.unijena.bioinf.chemdb.scheme.references.mapping.suffix", null, "_mapping_id_inchi_key");
    public final static String PUBCHEM_SCHEME = PropertyManager.getProperty("de.unijena.bioinf.chemdb.scheme.pubchem", null, "pubchem");
    public final static String DEFAULT_SCHEME = PropertyManager.getProperty("de.unijena.bioinf.chemdb.scheme.default", null, "public");
    public final static String FINGERPRINT_ID = PropertyManager.getProperty("de.unijena.bioinf.chemdb.fingerprint.id", null, "4");


    public final static String STRUCTURES_TABLE = DEFAULT_SCHEME + ".structures";
    public final static String FINGERPRINT_TABLE = DEFAULT_SCHEME + ".fingerprints";
    public final static String SYNONYMS_TABLE = PUBCHEM_SCHEME + ".synonyms";
    public final static String PUBCHEM_REF_TABLE = REF_SCHEME + ".pubchem";

    public static final int DEFAULT_SQL_CAPACITY = 5;
    protected static final Logger log = LoggerFactory.getLogger(ChemicalDatabase.class);

    static {
        FingerIDProperties.fingeridFullVersion();//just to load the props
    }

    protected final ConnectionPool<Connection> connection;
    protected String host, username, password;
    protected Properties connectionProps;

    //todo we should save the database import dat in the sql db. or is this already somewhere

    /**
     * initialize a chemical database connection using default values for password and username
     * <p>
     * note: We assume that the chemical database can only be accessed read-only from the
     * local network. Otherwise, releasing password and usernames together with the bytecode
     * would be a security problem.
     */
    public ChemicalDatabase(final int numOfConnections) {
        setup();
        connection = new ConnectionPool<>(new SqlConnector(host, username, password, null), numOfConnections);
    }

    public ChemicalDatabase() {
        this(DEFAULT_SQL_CAPACITY);
    }


    protected ChemicalDatabase(ChemicalDatabase db) {
        this.connection = db.connection.newSharedConnectionPool();
        this.host = db.host;
        this.username = db.username;
        this.password = db.password;
        this.connectionProps = null;
    }

    @Override
    public ChemicalDatabase clone() {
        return new ChemicalDatabase(this);
    }



    private void setup() {
        // check for system variables -> we do not want defaults because it is secret
        if (host == null)
            this.host = PropertyManager.getProperty("de.unijena.bioinf.fingerid.chemical_db.host");
        if (username == null)
            this.username = PropertyManager.getProperty("de.unijena.bioinf.fingerid.chemical_db.username");
        if (password == null)
            this.password = PropertyManager.getProperty("de.unijena.bioinf.fingerid.chemical_db.password");

        if (host == null || host.isBlank() || username == null || username.isBlank() || password == null || password.isBlank())
            throw new IllegalArgumentException("No valid credentials available to connect to SQL DB");
    }
    /**
     * initialize a chemical database connection using the given authentification values
     *
     * @param host     JDBC URI
     * @param username
     * @param password
     */
    public ChemicalDatabase(String host, String username, String password, Properties connectionProps) {
        this(host, username, password,connectionProps,DEFAULT_SQL_CAPACITY);
    }
    public ChemicalDatabase(String host, String username, String password, Properties connectionProps, int connections) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.connectionProps = connectionProps;
        setup();
        this.connection = new ConnectionPool<>(new SqlConnector(this.host, this.username, this.password, this.connectionProps), connections);
    }

    public ChemicalDatabase(String host, String username, String password) {
        this(host, username, password, null);
    }

    public ChemicalDatabase(String host, String username, String password, int connections) {
        this(host, username, password, null,connections);
    }


    public List<MolecularFormula> lookupMolecularFormulasByFilter(long filter) throws ChemicalDatabaseException {
            return new ArrayList<>(lookupMolecularFormulasWithFlagsByFilter(filter).keySet());
    }

    public Map<MolecularFormula, Long> lookupMolecularFormulasWithFlagsByFilter(long filter) throws ChemicalDatabaseException {
        final Map<MolecularFormula, Long> xs = new HashMap<>();
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            final PreparedStatement statement = filter == 0
                    ? c.connection.prepareStatement("SELECT formula, flags FROM formulas")
                    : c.connection.prepareStatement("SELECT formula, flags FROM formulas WHERE (flags & " + filter + ") != 0");
            try (final ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    xs.put(MolecularFormula.parseOrThrow(set.getString(1)), set.getLong(2));
                }
            }

        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
            return new HashMap<>();
        } catch (IOException | SQLException e) {
            log.error(e.getMessage(), e);
            throw new ChemicalDatabaseException(e);
        }
        return xs;
    }


    @Override
    public String getName() {
        return PUBCHEM_SCHEME;
    }

    /**
     * Search for molecular formulas in the database
     *
     * @param mass      exact mass of the ion
     * @param deviation allowed mass deviation
     * @param ionType   adduct of the ion
     * @return list of formula candidates which theoretical mass (+ adduct mass) is within the given mass window
     */
    public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        return lookupMolecularFormulas(DataSource.ALL.flag(), mass, deviation, ionType);
    }

    public List<FormulaCandidate> lookupMolecularFormulas(long filter, double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        final ArrayList<FormulaCandidate> xs = new ArrayList<>();
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            try (final PreparedStatement statement = c.connection.prepareStatement(
                    String.format("SELECT formula, flags FROM %s.formulas WHERE exactmass >= ? AND exactmass <= ?", DEFAULT_SCHEME)
            )) {
                xs.addAll(lookupFormulaWithIon(filter, statement, mass, deviation, ionType));
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

    public void checkConnections(int secondsBetweenEachCheck) throws SQLException {
        try {
            this.connection.testConnectionsAfter(secondsBetweenEachCheck);
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }


    private boolean containsFormula(@Nullable MolecularFormula formula, @NotNull final String sql) throws ChemicalDatabaseException {
        if (formula == null)
            return false;

        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            final PreparedStatement statement = c.connection.prepareStatement(sql);
            statement.setString(1,formula.toString());

            final ResultSet r = statement.executeQuery();
            if (r.next())
                return r.getBoolean(1);
            return false;

        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException | SQLException e) {
            log.error(e.getMessage(), e);
            throw new ChemicalDatabaseException(e);
        }
    }

    private static final String CONTAINS_FORMULA = "SELECT EXISTS(SELECT * FROM formulas WHERE formula = ?)";
    public boolean containsFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        return containsFormula(formula,CONTAINS_FORMULA);
    }

    private static final String CONTAINS_FORMULA_FILTERED = "SELECT EXISTS(SELECT * FROM formulas WHERE formula = ? AND (flags & %s) != 0)";
    public boolean containsFormula(long filter, MolecularFormula formula) throws ChemicalDatabaseException {
        return containsFormula(formula,String.format(CONTAINS_FORMULA_FILTERED, filter));
    }


        /**
         * Search for molecular formulas in the database
         *
         * @param mass      exact mass of the ion
         * @param deviation allowed mass deviation
         * @param ionTypes  allowed adducts of the ion
         * @return list of formula candidates which theoretical mass (+ adduct mass) is within the given mass window
         */
    public List<List<FormulaCandidate>> lookupMolecularFormulas(long filter, double mass, Deviation deviation, PrecursorIonType[] ionTypes) throws ChemicalDatabaseException {
        final ArrayList<List<FormulaCandidate>> xs = new ArrayList<>();
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            final PreparedStatement statement = c.connection.prepareStatement(
                    "SELECT formula, flags FROM formulas WHERE exactmass >= ? AND exactmass <= ?"
            );
            for (PrecursorIonType ionType : ionTypes) {
                try {
                    //we do not longer process intrinsically charged compounds differently. Hence, [M]+ and [M+H]+ will result in the exact same list of FormulaCandidates.
                    xs.add(lookupFormulaWithIon(filter, statement, mass, deviation, ionType));
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

    private List<FormulaCandidate> lookupFormulaWithIon(final long filter, PreparedStatement statement, double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException, SQLException {
        final double delta = deviation.absoluteFor(mass);
        final double neutralMass = ionType.precursorMassToNeutralMass(mass);
        final double minmz = neutralMass - delta;
        final double maxmz = neutralMass + delta;
        final ArrayList<FormulaCandidate> list = new ArrayList<>();
        statement.setDouble(1, minmz);
        statement.setDouble(2, maxmz);
        try (final ResultSet set = statement.executeQuery()) {
            //todo why do we not filter on Database level?
            while (set.next()) {
                final long flag = set.getLong(2);
                if (!ChemDBs.inFilter(flag, filter)) continue;
                final FormulaCandidate fc = new FormulaCandidate(MolecularFormula.parseOrThrow(set.getString(1)), ionType, set.getLong(2));
                if (ionType.isApplicableToNeutralFormula(fc.formula)) {
                    list.add(fc);
                }
            }
        } catch (SQLException e) {
            throw new ChemicalDatabaseException(e);
        }
        return list;
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        return lookupStructuresByFormula(DataSource.ALL.flag(), formula);
    }

    public List<CompoundCandidate> lookupStructuresByFormula(long filter, MolecularFormula formula) throws ChemicalDatabaseException {
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            return lookupStructuresByFormula(filter, formula, c);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } catch (IOException | SQLException e) {
            throw new ChemicalDatabaseException(e);
        }
    }


    private final static String SELECT_BY_FORMULA = "SELECT inchi_key_1, inchi, name, smiles, flags, xlogp FROM ";
    private final static String SELECT_BY_FORMULA_FILTERED = SELECT_BY_FORMULA + STRUCTURES_TABLE + " WHERE formula = ? AND (flags & %s) != 0";
    private final static String SELECT_BY_FORMULA_UNFILTERED = SELECT_BY_FORMULA + STRUCTURES_TABLE + " WHERE formula = ?";

    private List<CompoundCandidate> lookupStructuresByFormula(long filter, @NotNull MolecularFormula formula, @NotNull final PooledConnection<Connection> c) throws SQLException {


        final PreparedStatement statement;
        if (filter == 0) {
            statement = c.connection.prepareStatement(SELECT_BY_FORMULA_UNFILTERED);
        } else {
            statement = c.connection.prepareStatement(String.format(SELECT_BY_FORMULA_FILTERED, filter));
        }

        statement.setString(1, formula.toString());
        ArrayList<CompoundCandidate> candidates = new ArrayList<>();
        try (final ResultSet set = statement.executeQuery()) {
            while (set.next()) {
                final CompoundCandidate candidate = new CompoundCandidate(newInChI(set.getString(1), set.getString(2)));
                candidate.setName(set.getString(3));
                candidate.setSmiles(set.getString(4));
                candidate.setBitset(set.getLong(5));
                candidate.setXlogp(set.getObject(6) != null ? set.getDouble(6): Double.NaN);
                candidates.add(candidate);
            }
        }
        return candidates;
    }


    public List<FingerprintCandidate> lookupStructuresAndFingerprintsByFormula(long filter, MolecularFormula formula) throws ChemicalDatabaseException {
        return lookupStructuresAndFingerprintsByFormula(filter, formula, new ArrayList<>());
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        return lookupStructuresAndFingerprintsByFormula(DataSource.ALL.flag(), formula, fingerprintCandidates);
    }

    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(long filter, MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        try (final PooledConnection<Connection> c = connection.orderConnection()) {
            // first lookup structures
            final List<CompoundCandidate> candidates = lookupStructuresByFormula(filter, formula, c);
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
                            final FingerprintCandidate candidate = new FingerprintCandidate(InChIs.newInChI(set.getString(1), set.getString(2)), parseFingerprint(set, 7));
                            candidate.setName(set.getString(3));
                            candidate.setSmiles(set.getString(4));
                            candidate.setBitset(set.getLong(5));
                            //todo if we decide to never store p/q-layer information separately, we can remove these field from CompoundCandidate
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
                            candidates.add(InChIs.newInChI(set.getString(1), set.getString(2)));
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

    public void createDatabaseDump(long flag, File file){
        try {
            BufferedWriter write = new BufferedWriter(new FileWriter(file));
            try (final PooledConnection<Connection> c = connection.orderConnection()) {
                String query = "SELECT s.flags, s.smiles FROM " + STRUCTURES_TABLE;
                if (flag != 0)
                    query = query + " as s WHERE s.flags & " + flag + "!=0";

                try (final PreparedStatement statement = c.connection.prepareStatement(query)) {
                    try (final ResultSet set = statement.executeQuery()) {
                        while (set.next()) {
                            write.write(set.getString(2) + "\t" + set.getString(1) + "\n");
//                            System.out.println(set.getString(2)+"\t"+set.getString(1)+"\n");
                        }
                        write.close();
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
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
            final DataSource[] sources = DataSource.valuesNoALL();
            final PreparedStatement[] statements = new PreparedStatement[sources.length];
            int k = 0;
            for (DataSource source : sources) {
                final String tableName = source.sqlRefTable+REF_MAPPING_TABLE_SUFFIX;
                String sql = String.format("SELECT %s FROM %s.%s WHERE inchi_key_1 = ?", source.sqlIdColumn, REF_SCHEME, tableName);
                statements[k++] = source.sqlRefTable == null ? null : c.connection.prepareStatement(sql);
            }
            for (CompoundCandidate candidate : sublist) {
                final ArrayList<DBLink> links = new ArrayList<>();
                for (int i = 0; i < sources.length; ++i) {
                    final DataSource source = sources[i];
                    if (/* legacy mode */ source == DataSource.PUBCHEM || ((candidate.getBitset() & source.flag)) != 0) {
                        final PreparedStatement statement = statements[i];
                        if (statement != null) {
                            statement.setString(1, candidate.getInchiKey2D());
                            try (final ResultSet set = statement.executeQuery()) {
                                while (set.next()) {
                                    links.add(new DBLink(source.name(), set.getString(1)));
                                }
                            }
                        }
                    }
                }
                candidate.setLinks(links);
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
            try (final PreparedStatement statement = c.connection.prepareStatement("SELECT distinct r.inchi_key_1, r.inchi FROM " + SYNONYMS_TABLE + " as syn, " + PUBCHEM_REF_TABLE + " as r WHERE lower(syn.name) = lower(?) AND r.compound_id = syn.compound_id")) {
                final HashSet<InChI> inchis = new HashSet<>();
                for (String name : names) {
                    statement.setString(1, name);
                    try (final ResultSet set = statement.executeQuery()) {
                        while (set.next()) {
                            inchis.add(InChIs.newInChI(set.getString(1), set.getString(2)));
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

    protected static class SqlConnector implements ConnectionPool.Connector<Connection> {
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
        private Properties connectionProps;

        public SqlConnector(String host, String username, String password, Properties connectionProps) {
            this.host = host;
            this.username = username;
            this.password = password;
            if (connectionProps != null){
                this.connectionProps = new Properties();
                this.connectionProps.putAll(connectionProps);
                this.connectionProps.put("user", username);
                this.connectionProps.put("password", password);
            } else {
                this.connectionProps = null;
            }
        }

        @Override
        public Connection open() throws IOException {
            try {
                final Connection c;
                if (connectionProps!=null) {
                    c = DriverManager.getConnection("jdbc:postgresql://" + host + "/pubchem", connectionProps);
                } else {
                    c = DriverManager.getConnection("jdbc:postgresql://" + host + "/pubchem", username, password);
                }
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

        @Override
        public boolean isValid(Connection connection) {
            try {
                return connection.isValid(10);
            } catch (SQLException e) {
                LoggerFactory.getLogger(getClass()).warn("Error during ChemDB connection validation? Returning inValid state.", e);
                return false;
            }
        }
    }


    public static Fingerprint parseFingerprint(ResultSet result, int index) throws SQLException {
        try (final ResultSet fp = result.getArray(index).getResultSet()) {
            return parseFingerprint(fp);
        }
    }

    public static Fingerprint parseFingerprint(ResultSet fp) throws SQLException {
        TShortArrayList shorts = new TShortArrayList();
        while (fp.next()) {
            final short s = fp.getShort(2);
            shorts.add(s);
        }
        return new ArrayFingerprint(USE_EXTENDED_FINGERPRINTS ? CdkFingerprintVersion.getExtended() : CdkFingerprintVersion.getDefault(), shorts.toArray());
    }
    // todo this should be stored in and retrieved from the relation database, based on the used tables...
    // todo probably the date could be used to choose the table?
    public String getChemDbDate() throws ChemicalDatabaseException{
        return PropertyManager.getProperty("de.unijena.bioinf.fingerid.db.psqlDbDate");
    }

    @Override
    public long countAllFingerprints() throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long countAllFormulas() throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

}
