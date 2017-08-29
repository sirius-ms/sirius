package de.unijena.bioinf.sirius.cli;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ConfidenceScore.PredictionException;
import de.unijena.bioinf.ConfidenceScore.QueryPredictor;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.dbgen.DatabaseImporter;
import de.unijena.bioinf.sirius.fingerid.FingerIdResult;
import de.unijena.bioinf.sirius.fingerid.FingerIdResultReader;
import de.unijena.bioinf.sirius.fingerid.FingerIdResultWriter;
import de.unijena.bioinf.sirius.fingerid.SearchableDbOnDisc;
import de.unijena.bioinf.sirius.gui.db.CustomDatabase;
import de.unijena.bioinf.sirius.gui.db.SearchableDatabase;
import de.unijena.bioinf.sirius.gui.fingerid.CanopusResult;
import de.unijena.bioinf.sirius.gui.fingerid.VersionsInfo;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.projectspace.DirectoryReader;
import de.unijena.bioinf.sirius.projectspace.DirectoryWriter;
import de.unijena.bioinf.sirius.projectspace.ProjectReader;
import de.unijena.bioinf.sirius.projectspace.ProjectWriter;
import gnu.trove.list.array.TIntArrayList;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FingeridApplication extends CLI<FingerIdOptions> {
    private static final String FINGERID_RESULT_HEADER = "file\tinstance\tprecursor m/z\tinchi\tinchikey2D\tname\tsmiles\tscore\tconfidence\n";
    protected Fingerblast fingerblast;
    protected QueryPredictor confidence, bioConfidence;
    protected ExecutorService executorService;
    protected MaskedFingerprintVersion fingerprintVersion;
    protected List<Scored<String>> orderedByConfidence;

    protected boolean fingeridEnabled = false;

    @Override
    protected ProjectWriter getSiriusOutputWriter(String sirius, DirectoryWriter.WritingEnvironment env) {
        return new FingerIdResultWriter(env);
    }

    @Override
    protected ProjectWriter getDirectoryOutputWriter(String sirius, DirectoryWriter.WritingEnvironment env) {
        return new FingerIdResultWriter(env);
    }

    @Override
    protected ProjectReader getSiriusOutputReader(String sirius, DirectoryReader.ReadingEnvironment env) {
        return new FingerIdResultReader(env);
    }

    @Override
    protected ProjectReader getDirectoryOutputReader(String sirius, DirectoryReader.ReadingEnvironment env) {
        return new FingerIdResultReader(env);
    }

    @Override
    public void compute() {
        if (options.getGeneratingCompoundDatabase() != null) {
            try {
                generateCustomDatabase(options);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
            return;
        }
        fingeridEnabled = options.isFingerid();
        if (fingeridEnabled) {
            initFingerBlast();
            initCanopus();
        }

        try {
            super.compute();
        } finally {
            try {
                if (fingerblast != null) fingerblast.getSearchEngine().close();
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            }
        }
    }

    private void generateCustomDatabase(FingerIdOptions options) throws IOException {
        DatabaseImporter.importDatabase(options.getGeneratingCompoundDatabase(), options.getInput());
    }

    @Override
    protected void handleResults(Instance i, List<IdentificationResult> results) {
        super.handleResults(i, results);
        if (results.isEmpty()) return;
        final BioFilter bioFilter = getBioFilter();
        // this service is just use to submit several fingerprint jobs at the same time
        executorService = Executors.newFixedThreadPool(16);
        try (WebAPI webAPI = WebAPI.newInstance()) {
            // search CSI:FingerId identifications
            if (options.isFingerid()) {
                // first filter result list by top scoring formulas
                final ArrayList<IdentificationResult> filteredResults = new ArrayList<>();
                final IdentificationResult top = results.get(0);
                if (top == null || top.getResolvedTree() == null) return;
                progress.info("Search with CSI:FingerId");
                filteredResults.add(top);
                final double threshold = Math.max(top.getScore(), 0) - Math.max(5, top.getScore() * 0.25);
                for (int k = 1; k < results.size(); ++k) {
                    IdentificationResult e = results.get(k);
                    if (e.getScore() < threshold) break;
                    if (e.getResolvedTree().numberOfVertices()<=1){
                        progress.info("Cannot estimate structure for "+e.getMolecularFormula()+". Fragmentation Tree is empty.");
                        continue;
                    }
                    filteredResults.add(e);
                }
                final List<Future<ProbabilityFingerprint>> futures = new ArrayList<>();
                for (IdentificationResult result : filteredResults) {

                    // workaround... we should think about that carefully
                    final FTree tree = result.getResolvedTree();
                    futures.add(webAPI.predictFingerprint(executorService, i.experiment, tree, fingerprintVersion));
                }
                final List<Scored<FingerprintCandidate>> allCandidates = new ArrayList<>();
                final HashMap<String, Long> dbMap = getDatabaseAliasMap();
                Long flagW = dbMap.get(options.getDatabase());
                if (flagW == null) flagW = 0L;
                final long flag = flagW;
                final HashMap<MolecularFormula, ProbabilityFingerprint> predictedFingerprints = new HashMap<>();
                for (int k = 0; k < filteredResults.size(); ++k) {
                    final ProbabilityFingerprint fp = futures.get(k).get();
                    if (canopus != null) handleCanopus(filteredResults.get(k), fp);
                    final List<Scored<FingerprintCandidate>> cds = fingerblast.search(filteredResults.get(k).getMolecularFormula(), fp);
                    if (bioFilter != BioFilter.ALL) {
                        final Iterator<Scored<FingerprintCandidate>> iter = cds.iterator();
                        while (iter.hasNext()) {
                            final Scored<FingerprintCandidate> c = iter.next();
                            if (flag != 0 && (c.getCandidate().getBitset() & flag) == 0) {
                                iter.remove();
                            }
                        }
                    }
                    Collections.sort(cds);
                    allCandidates.addAll(cds);
                    predictedFingerprints.put(filteredResults.get(k).getMolecularFormula(), fp);
                    filteredResults.get(k).setAnnotation(FingerIdResult.class, new FingerIdResult(cds, 0d, fp));
                }
                Collections.sort(allCandidates, Scored.<FingerprintCandidate>desc());
                if (allCandidates.size() == 0) {
                    progress.info("No candidate structures found for given mass and computed trees.");
                    return;
                }
                // compute confidence for top hit from bio database
                final ArrayList<Scored<CompoundWithAbstractFP<Fingerprint>>> confidenceList = new ArrayList<>();
                final ArrayList<Scored<CompoundWithAbstractFP<Fingerprint>>> bioConfidenceList = new ArrayList<>();
                CompoundCandidate topBio = null;
                for (Scored<FingerprintCandidate> fc : allCandidates) {
                    Scored<CompoundWithAbstractFP<Fingerprint>> c = new Scored<>(new CompoundWithAbstractFP<Fingerprint>(fc.getCandidate().getInchi(), fc.getCandidate().getFingerprint()), fc.getScore());

                    confidenceList.add(c);
                    if (DatasourceService.isBio(fc.getCandidate().getBitset())) {
                        bioConfidenceList.add(c);
                        if (topBio==null) topBio = fc.getCandidate();
                    }
                }

                // find top result
                FingerIdResult topResult = null;
                for (IdentificationResult ir : results) {
                    FingerIdResult r = ir.getAnnotationOrNull(FingerIdResult.class);
                    if (r != null && r.getCandidates().size()>0 && r.getCandidates().get(0).equals(allCandidates.get(0))) {
                        topResult = r;
                        break;
                    }
                }

                if (!confidenceList.isEmpty()) {
                    final Scored<CompoundWithAbstractFP<Fingerprint>> c = confidenceList.get(0);
                    final CompoundCandidate fc = allCandidates.get(0).getCandidate();
                    final CompoundWithAbstractFP[] list = new CompoundWithAbstractFP[confidenceList.size()];
                    for (int k = 0; k < confidenceList.size(); ++k) list[k] = confidenceList.get(k).getCandidate();
                    final double confidenceScore = confidence==null ? 0d : confidence.estimateProbability(new CompoundWithAbstractFP<ProbabilityFingerprint>(c.getCandidate().getInchi(), predictedFingerprints.get(c.getCandidate().getInchi().extractFormula())), list);

                    if (topResult!=null) topResult.setConfidence(confidenceScore);

                    String name = fc.getName();
                    if (name == null || name.isEmpty()) name = fc.getSmiles();
                    if (name == null || name.isEmpty()) name = "";
                    if (confidence==null)
                        progress.info(String.format(Locale.US, "Top compound is %s (%s)\n", name, fc.getInchi().in2D));
                    else
                        progress.info(String.format(Locale.US, "Top compound is %s (%s) with confidence %.2f\n", name, fc.getInchi().in2D, confidenceScore));
                }
                if (bioFilter != BioFilter.ONLY_BIO && topBio!=null && topBio != allCandidates.get(0).getCandidate()) {
                    final Scored<CompoundWithAbstractFP<Fingerprint>> c = bioConfidenceList.get(0);
                    final CompoundCandidate fc = topBio;
                    final CompoundWithAbstractFP[] list = new CompoundWithAbstractFP[bioConfidenceList.size()];
                    for (int k = 0; k < bioConfidenceList.size(); ++k) list[k] = bioConfidenceList.get(k).getCandidate();
                    final double confidenceScore = bioConfidence==null ? 0 : bioConfidence.estimateProbability(new CompoundWithAbstractFP<ProbabilityFingerprint>(c.getCandidate().getInchi(), predictedFingerprints.get(c.getCandidate().getInchi().extractFormula())), list);

                    String name = fc.getName();
                    if (name == null || name.isEmpty()) name = fc.getSmiles();
                    if (name == null || name.isEmpty()) name = "";
                    if (bioConfidence==null)
                        progress.info(String.format(Locale.US, "Top biocompound is %s (%s)\n", name, fc.getInchi().in2D));
                    else
                        progress.info(String.format(Locale.US, "Top biocompound is %s (%s) with confidence %.2f\n", name, fc.getInchi().in2D, confidenceScore));
                }

                for (int k = 0; k < Math.min(20, allCandidates.size()); ++k) {
                    FingerprintCandidate f = allCandidates.get(k).getCandidate();
                    String n = f.getName();
                    if (n == null || n.isEmpty()) n = f.getSmiles();
                    if (n == null) n = "";
                    println(String.format(Locale.US, "%2d.) %s\t%s\tscore: %.2f", (k + 1), n, f.getInchi().in2D, allCandidates.get(k).getScore()));
                }
                if (allCandidates.size() > 20) {
                    println("... " + (allCandidates.size() - 20) + " further candidates.");
                }
                println("");
            }
        } catch (InterruptedException | ExecutionException | DatabaseException | PredictionException | IOException e) {
            LoggerFactory.getLogger(this.getClass()).error("Error while searching structure for " + i.experiment.getName() + " (" + i.file + "): " + e.getMessage(), e);
        } finally {
            executorService.shutdown();
        }
    }

    private void handleCanopus(IdentificationResult identificationResult, ProbabilityFingerprint fp) {
        if (canopus==null) return;
        println("Predict compound categories: \nid\tname\tprobability");
        final ProbabilityFingerprint fingerprint = canopus.predictClassificationFingerprint(identificationResult.getMolecularFormula(), fp);
        for (FPIter category : fingerprint.iterator()) {
            if (category.getProbability()>=0.333) {
                ClassyfireProperty prop = ((ClassyfireProperty)category.getMolecularProperty());
                println(prop.getChemontIdentifier() + "\t"  + prop.getName() + "\t" + category.getProbability());
            }
        }
        println("");
        identificationResult.setAnnotation(CanopusResult.class, new CanopusResult(fingerprint));
    }

    private String escape(String name) {
        if (name == null) return "\"\"";
        return name.replace('\t', ' ').replace('"', '\'');
    }

    private BioFilter getBioFilter() {
        final BioFilter bioFilter;
        if (options.getDatabase().equalsIgnoreCase("pubchem") || options.getDatabase().equalsIgnoreCase("all")) {
            bioFilter = BioFilter.ALL;
        } else bioFilter = BioFilter.ONLY_BIO;
        return bioFilter;
    }

    @Override
    public void validate() {
        super.validate();
        if (options.getFingerIdDb() != null && !options.isFingerid()) {
            LoggerFactory.getLogger(this.getClass()).error("--fingerid_db defines the database CSI:FingerId should search in. This option makes only sense when used together with --fingerid. Use --db for setting up the database in which SIRIUS should search for molecular formulas.");
            System.exit(1);
        }
        if (options.getExperimentalCanopus() != null && !options.isFingerid()) {
            LoggerFactory.getLogger(this.getClass()).error("Cannot predict compound categories with CSI:FingerID is disabled. Please enable CSI:FingerID with --fingerid option.");
            System.exit(1);
        }
    }

    private SearchableDatabase getDatabase() {
        return getDatabase(options.getDatabase());
    }

    private SearchableDatabase getDatabase(String name) {
        final HashMap<String, Long> aliasMap = getDatabaseAliasMap();
        if (!aliasMap.containsKey(name.toLowerCase()) && new File(name).exists()) {
            try {
                final CustomDatabase db = new CustomDatabase(new File(name).getName(), new File(name));
                db.readSettings();
                if (db.needsUpgrade()) {
                    System.err.println("Database '" + name + "' is outdated and have to be upgraded or reimported.");
                    System.exit(1);
                }
                return db;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            if (customDatabases.containsKey(name)) return customDatabases.get(name);

            final BioFilter filter = getBioFilter();
            if (filter == BioFilter.ALL) return pubchemDatabase;
            else return bioDatabase;
        }
    }


    SearchableDatabase pubchemDatabase, bioDatabase;
    HashMap<File, FilebasedDatabase> customDatabaseCache;
    private HashMap<String, SearchableDatabase> customDatabases;
    protected File db_cache_dir;
    protected void initializeDatabaseCache() {
        final File d = Workspace.CONFIG_STORAGE.getDatabaseDirectory();
        db_cache_dir = d;
        pubchemDatabase = new SearchableDbOnDisc("PubChem", d, true,true,false);
        bioDatabase = new SearchableDbOnDisc("biological database", d, true,true,false);
        this.customDatabaseCache = new HashMap<>();
        customDatabases = new HashMap<>();
        for (SearchableDatabase db : CustomDatabase.customDatabases(true)) {
            customDatabases.put(db.name(), db);
        }
    }

    protected FilebasedDatabase getFileBasedDb(SearchableDatabase db) throws IOException {
        if (!customDatabaseCache.containsKey(db.getDatabasePath())) {
            customDatabaseCache.put(db.getDatabasePath(), new FilebasedDatabase(WebAPI.getFingerprintVersion(), db.getDatabasePath()));
        }
        return customDatabaseCache.get(db.getDatabasePath());
    }

    private SearchableDatabase getFingerIdDatabase() {
        if (options.getFingerIdDb() != null) {
            return getDatabase(options.getFingerIdDb());
        } else {
            return getDatabase(options.getDatabase());
        }
    }

    private void initFingerBlast() {
        progress.info("Initialize CSI:FingerId...");
        try (WebAPI webAPI = WebAPI.newInstance()) {
            final VersionsInfo needsUpdate = webAPI.getVersionInfo();
            if (needsUpdate != null && needsUpdate.outdated()) {
                progress.info("Your current SIRIUS+CSI:FingerID version is outdated. Please download the latest software version if you want to use CSI:FingerId search. Current version: " + WebAPI.VERSION + ". New version is " + needsUpdate.siriusGuiVersion + ". You can download the last version here: " + WebAPI.SIRIUS_DOWNLOAD);
                System.exit(1);
            }
            final TIntArrayList indizes = new TIntArrayList();
            final BioFilter bioFilter = getBioFilter();
            final PredictionPerformance[] performances = webAPI.getStatistics(indizes);
            this.fingerblast = new Fingerblast(getFingerIdDatabaseWrapper());
//            this.fingerblast.setScoring(new CSIFingerIdScoring(performances));
            this.confidence = webAPI.getConfidenceScore(bioFilter != BioFilter.ALL);
            this.bioConfidence = bioFilter != BioFilter.ALL ? confidence : webAPI.getConfidenceScore(true);
            MaskedFingerprintVersion.Builder b = MaskedFingerprintVersion.buildMaskFor(WebAPI.getFingerprintVersion());
            b.disableAll();
            for (int index : indizes.toArray()) {
                b.enable(index);
            }
            this.fingerprintVersion = b.toMask();
            this.fingerblast.setScoring(webAPI.getCovarianceScoring(this.fingerprintVersion, 1d/performances[0].withPseudoCount(0.25d).numberOfSamplesWithPseudocounts()).getScoring());
        } catch (IOException e) {
            LoggerFactory.getLogger(this.getClass()).error("Our webservice is currently not available. You can still use SIRIUS without the --fingerid option. Please feel free to mail us at sirius-devel@listserv.uni-jena.de", e);
            System.exit(1);
        }
        progress.info("CSI:FingerId initialization done.");
    }

    protected Canopus canopus = null;
    public void initCanopus() {
        if (options.getExperimentalCanopus()!=null) {
            try {
                this.canopus = Canopus.loadFromFile(options.getExperimentalCanopus());
            } catch (IOException e) {
                System.err.println("Cannot load given canopus model: " + e.getMessage());
            }
        }
    }

    // bad hack
    private AbstractChemicalDatabase getFingerIdDatabaseWrapper() {
        final SearchableDatabase db = getFingerIdDatabase();
        if (db.isCustomDb()) {
            return new AbstractChemicalDatabase() {
                @Override
                public List<FormulaCandidate> lookupMolecularFormulas(double v, Deviation deviation, PrecursorIonType precursorIonType) throws DatabaseException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula molecularFormula) throws DatabaseException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula molecularFormula, T fingerprintCandidates) throws DatabaseException {
                    try (final WebAPI webAPI = WebAPI.newInstance()) {
                        if (db.searchInBio()) {
                            try (final RESTDatabase db = webAPI.getRESTDb( BioFilter.ONLY_BIO,db_cache_dir)) {
                                fingerprintCandidates = db.lookupStructuresAndFingerprintsByFormula(molecularFormula,fingerprintCandidates);
                            }
                        }
                        if (db.searchInPubchem()) {
                            try (final RESTDatabase db = webAPI.getRESTDb( BioFilter.ONLY_NONBIO,db_cache_dir)) {
                                fingerprintCandidates = db.lookupStructuresAndFingerprintsByFormula(molecularFormula,fingerprintCandidates);
                            }
                        }
                        if (db.isCustomDb()) {
                            fingerprintCandidates = getFileBasedDb(db).lookupStructuresAndFingerprintsByFormula(molecularFormula, fingerprintCandidates);
                        }
                        return fingerprintCandidates;
                    } catch (IOException e) {
                        logger.error(e.getMessage(),e);
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> iterable) throws DatabaseException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> iterable) throws DatabaseException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> iterable) throws DatabaseException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> iterable) throws DatabaseException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void annotateCompounds(List<? extends CompoundCandidate> list) throws DatabaseException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public List<InChI> findInchiByNames(List<String> list) throws DatabaseException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void close() throws IOException {

                }
            };
        } else {
            final BioFilter bf = getBioFilter();
            return new RESTDatabase(bf);
        }

    }

    @Override
    protected Set<MolecularFormula> getFormulaWhiteset(Instance i, List<String> whitelist) {
        if (options.getDatabase().equalsIgnoreCase("all")) return super.getFormulaWhiteset(i, whitelist);
        else {
            final HashMap<String, Long> aliasMap = getDatabaseAliasMap();
            final SearchableDatabase searchableDatabase = getDatabase();
            final long flag;

            if (aliasMap.containsKey(options.getDatabase().toLowerCase())) {
                flag = aliasMap.get(options.getDatabase().toLowerCase());
            } else {
                flag = 0L;
            }
            final Deviation dev;
            if (options.getPPMMax() != null) dev = new Deviation(options.getPPMMax());
            else dev = sirius.getMs2Analyzer().getDefaultProfile().getAllowedMassDeviation();
            final List<PrecursorIonType> allowedIonTypes = new ArrayList<>();
            if (i.experiment.getPrecursorIonType() == null || i.experiment.getPrecursorIonType().isIonizationUnknown()) {
                if (i.experiment.getPrecursorIonType() == null || i.experiment.getPrecursorIonType().getCharge() > 0) {
                    allowedIonTypes.addAll(Arrays.asList(WebAPI.positiveIons));
                } else {
                    allowedIonTypes.addAll(Arrays.asList(WebAPI.negativeIons));
                }
            } else {
                allowedIonTypes.add(i.experiment.getPrecursorIonType());
            }
            try {
                final FormulaConstraints allowedAlphabet;
                if (options.getElements() != null) allowedAlphabet = options.getElements();
                else allowedAlphabet = new FormulaConstraints("CHNOPSBBrClIF");

                List<List<FormulaCandidate>> candidates = new ArrayList<>();
                try (final WebAPI api = WebAPI.newInstance()) {
                    if (searchableDatabase.searchInBio()) {
                        try (final RESTDatabase db = api.getRESTDb(BioFilter.ONLY_BIO, bioDatabase.getDatabasePath())) {
                            candidates.addAll(db.lookupMolecularFormulas(i.experiment.getIonMass(), dev, allowedIonTypes.toArray(new PrecursorIonType[allowedIonTypes.size()])));
                        }
                    }
                    if (searchableDatabase.searchInPubchem()){
                        try (final RESTDatabase db = api.getRESTDb(BioFilter.ONLY_NONBIO, pubchemDatabase.getDatabasePath())) {
                            candidates.addAll(db.lookupMolecularFormulas(i.experiment.getIonMass(), dev, allowedIonTypes.toArray(new PrecursorIonType[allowedIonTypes.size()])));
                        }
                    }
                    if (searchableDatabase.isCustomDb()) {
                        candidates.addAll(getFileBasedDb(searchableDatabase).lookupMolecularFormulas(i.experiment.getIonMass(), dev, allowedIonTypes.toArray(new PrecursorIonType[allowedIonTypes.size()])));
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(),e);
                    System.exit(1);
                }

                final HashSet<MolecularFormula> allowedSet = new HashSet<>();
                for (List<FormulaCandidate> fc : candidates) {
                    for (FormulaCandidate f : fc) {
                        final long bitset = f.getBitset();
                        if (flag == 0 || (bitset & flag) != 0)
                            if (allowedAlphabet.isSatisfied(f.getFormula()))
                                allowedSet.add(f.getFormula());
                    }
                }
                return allowedSet;
            } catch (DatabaseException e) {
                LoggerFactory.getLogger(this.getClass()).error("Connection to database fails. Probably our webservice is currently offline. You can still use SIRIUS in offline mode - you just have to remove the database flags -d or --database because database search is not available in offline mode.", e);
                System.exit(1);
                return null;
            }
        }


    }

    private HashMap<String, Long> getDatabaseAliasMap() {
        final HashMap<String, Long> aliasMap = new HashMap<>();
        for (DatasourceService.Sources source : DatasourceService.Sources.values()) {
            aliasMap.put(source.name.toLowerCase(), source.flag);
        }
        aliasMap.put("biocyc", DatasourceService.Sources.METACYC.flag);
        aliasMap.put("bio", DatasourceService.Sources.BIO.flag);
        aliasMap.put("all", 0L);
        if (!aliasMap.containsKey(options.getDatabase().toLowerCase()) && !new File(options.getDatabase()).exists() && !customDatabases.containsKey(options.getDatabase())) {
            final List<String> knownDatabases = new ArrayList<>();
            knownDatabases.addAll(aliasMap.keySet());
            knownDatabases.addAll(customDatabases.keySet());
            LoggerFactory.getLogger(this.getClass()).error("Unknown database '" + options.getDatabase().toLowerCase() + "'. Available are: " + Joiner.on(", ").join(knownDatabases));
            System.exit(1);
        }
        return aliasMap;
    }

    @Override
    public void setup() {
        initializeDatabaseCache();
        super.setup();
    }
}
