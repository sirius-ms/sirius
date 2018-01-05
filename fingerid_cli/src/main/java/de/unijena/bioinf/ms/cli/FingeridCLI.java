package de.unijena.bioinf.ms.cli;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ConfidenceScore.PredictionException;
import de.unijena.bioinf.ConfidenceScore.QueryPredictor;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.FingerIdResultReader;
import de.unijena.bioinf.fingerid.FingerIdResultWriter;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.db.CustomDatabase;
import de.unijena.bioinf.fingerid.db.DatabaseImporter;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.fingerid.db.SearchableDbOnDisc;
import de.unijena.bioinf.fingerid.jjobs.FingerIDJJob;
import de.unijena.bioinf.fingerid.net.CachedRESTDB;
import de.unijena.bioinf.fingerid.net.VersionsInfo;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingeriddb.job.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BufferedJJobSubmitter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.projectspace.DirectoryReader;
import de.unijena.bioinf.sirius.projectspace.DirectoryWriter;
import de.unijena.bioinf.sirius.projectspace.ProjectReader;
import de.unijena.bioinf.sirius.projectspace.ProjectWriter;
import gnu.trove.list.array.TIntArrayList;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static de.unijena.bioinf.fingerid.storage.ConfigStorage.CONFIG_STORAGE;

public class FingeridCLI<Options extends FingerIdOptions> extends ZodiacCLI<Options> {
    static {
        CustomDatabase.customDatabases(true);
        DEFAULT_LOGGER.info("Custom DBs initialized!");
    }

    protected Fingerblast fingerblast;
    protected QueryPredictor confidence, bioConfidence;
    protected MaskedFingerprintVersion fingerprintVersion;


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
    protected void parseArgsAndInit(String[] args, Class<Options> optionsClass) {
        super.parseArgsAndInit(args, optionsClass);

        if (options.getGeneratingCompoundDatabase() != null) {
            try {
                generateCustomDatabase(options);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
            return;
        }

        if (options.isFingerid()) {
            initFingerBlast();
            initCanopus();
        }
    }


    @Override
    protected void handleJobs(final BufferedJJobSubmitter<Instance>.JobContainer jc) throws IOException {
        super.handleJobs(jc);
        if (options.isFingerid()) {
            progress.info("CSI:FingerID results for: '" + jc.sourceInstance.file.getName() + "'");
            FingerIDJJob fij = jc.getJob(FingerIDJJob.class);
            if (fij != null)
                handleFingerIdResults(jc.sourceInstance, fij); //handle results
        }
    }

    @Override
    public void compute() {
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
    
    protected FingerIDJJob makeFingerIdJob(final Instance i, BasicJJob<List<IdentificationResult>> siriusJob) {
        if (siriusJob == null) return null;

        //predict fingerprint
        final HashMap<String, Long> dbMap = getDatabaseAliasMap(); //todo this is the same for all instances or?????
        Long flagW = dbMap.get(options.getDatabase());
        if (flagW == null) flagW = 0L;
        final long flag = flagW;

        FingerIDJJob fingerIdJob = new FingerIDJJob(fingerblast, fingerprintVersion, options.getPredictors().toArray(new PredictorType[options.getPredictors().size()]));
        fingerIdJob.addRequiredJob(siriusJob);
        fingerIdJob.setDbFlag(flag);
        fingerIdJob.setBioFilter(getBioFilter());
        fingerIdJob.setCanopus(canopus);

        return fingerIdJob;
    }

    protected static class CandidateElement extends Scored<FingerprintCandidate> {
        protected final FingerIdResult origin;
        public CandidateElement(FingerIdResult ir, Scored<FingerprintCandidate> c) {
            super(c.getCandidate(), c.getScore());
            this.origin = ir;
        }
    }

    protected void handleFingerIdResults(Instance i, BasicJJob<Map<IdentificationResult, ProbabilityFingerprint>> fingerprintJob) {
        try {
            Map<IdentificationResult, ProbabilityFingerprint> propPrints = fingerprintJob.awaitResult();
            if (propPrints != null) {
                //collect candidates
                final List<CandidateElement> allCandidates = new ArrayList<>();
                final HashMap<MolecularFormula, ProbabilityFingerprint> predictedFingerprints = new HashMap<>();
                for (IdentificationResult identificationResult : propPrints.keySet()) {
                    predictedFingerprints.put(identificationResult.getMolecularFormula(), propPrints.get(identificationResult));
                    FingerIdResult fingerIdResult = identificationResult.getAnnotationOrNull(FingerIdResult.class);
                    if (fingerIdResult != null) {
                        for (Scored<FingerprintCandidate> fpc : fingerIdResult.getCandidates())
                            allCandidates.add(new CandidateElement(fingerIdResult,fpc));

                    }
                }

                //sort by score
                allCandidates.sort(Scored.<FingerprintCandidate>desc());
                if (allCandidates.size() == 0) {
                    progress.info("No candidate structures found for given mass and computed trees.");
                    return;
                }

                // find top identificationResult
                FingerIdResult topResult = allCandidates.get(0).origin;

                // compute confidence for top hit from bio database
                final ArrayList<Scored<CompoundWithAbstractFP<Fingerprint>>> confidenceList = new ArrayList<>();
                final ArrayList<Scored<CompoundWithAbstractFP<Fingerprint>>> bioConfidenceList = new ArrayList<>();
                CandidateElement topBio = null;
                for (CandidateElement fc : allCandidates) {
                    Scored<CompoundWithAbstractFP<Fingerprint>> c = new Scored<>(new CompoundWithAbstractFP<Fingerprint>(fc.getCandidate().getInchi(), fc.getCandidate().getFingerprint()), fc.getScore());

                    confidenceList.add(c);
                    if (DatasourceService.isBio(fc.getCandidate().getBitset())) {
                        bioConfidenceList.add(c);
                        if (topBio == null) topBio = fc;
                    }
                }

                if (!confidenceList.isEmpty()) {
                    final Scored<CompoundWithAbstractFP<Fingerprint>> c = confidenceList.get(0);
                    final CompoundCandidate fc = allCandidates.get(0).getCandidate();
                    final CompoundWithAbstractFP[] list = new CompoundWithAbstractFP[confidenceList.size()];
                    for (int k = 0; k < confidenceList.size(); ++k) list[k] = confidenceList.get(k).getCandidate();
                    final double confidenceScore = confidence == null ? 0d : confidence.estimateProbability(new CompoundWithAbstractFP<ProbabilityFingerprint>(c.getCandidate().getInchi(), predictedFingerprints.get(c.getCandidate().getInchi().extractFormula())), list);

                    if (topResult != null) topResult.setConfidence(confidenceScore);

                    String name = fc.getName();
                    if (name == null || name.isEmpty()) name = fc.getSmiles();
                    if (name == null || name.isEmpty()) name = "";
                    if (confidence == null)
                        progress.info(String.format(Locale.US, "Top compound is %s %s (%s)\n", name, topResult.getPrecursorIonType().toString(), fc.getInchi().in2D));
                    else
                        progress.info(String.format(Locale.US, "Top compound is %s %s (%s) with confidence %.2f\n", name, topResult.getPrecursorIonType().toString(), fc.getInchi().in2D, confidenceScore));
                }
                //todo the biofilter does also stay constant for all i
                if (getBioFilter() != BioFilter.ONLY_BIO && topBio != null && topBio.getCandidate() != allCandidates.get(0).getCandidate()) {
                    final Scored<CompoundWithAbstractFP<Fingerprint>> c = bioConfidenceList.get(0);
                    final CompoundCandidate fc = topBio.getCandidate();
                    final CompoundWithAbstractFP[] list = new CompoundWithAbstractFP[bioConfidenceList.size()];
                    for (int k = 0; k < bioConfidenceList.size(); ++k)
                        list[k] = bioConfidenceList.get(k).getCandidate();
                    final double confidenceScore = bioConfidence == null ? 0 : bioConfidence.estimateProbability(new CompoundWithAbstractFP<ProbabilityFingerprint>(c.getCandidate().getInchi(), predictedFingerprints.get(c.getCandidate().getInchi().extractFormula())), list);

                    String name = fc.getName();
                    if (name == null || name.isEmpty()) name = fc.getSmiles();
                    if (name == null || name.isEmpty()) name = "";
                    if (bioConfidence == null)
                        progress.info(String.format(Locale.US, "Top biocompound is %s %s (%s)\n", name, topBio.origin.getPrecursorIonType().toString(), fc.getInchi().in2D));
                    else
                        progress.info(String.format(Locale.US, "Top biocompound is %s %s (%s) with confidence %.2f\n", name, topBio.origin.getPrecursorIonType().toString(), fc.getInchi().in2D, confidenceScore));
                }

                for (int k = 0; k < Math.min(20, allCandidates.size()); ++k) {
                    CandidateElement e = allCandidates.get(k);
                    FingerprintCandidate f = e.getCandidate();
                    String n = f.getName();
                    if (n == null || n.isEmpty()) n = f.getSmiles();
                    if (n == null) n = "";
                    println(String.format(Locale.US, "%2d.) %s\t%s\t%s\t%s\tscore: %.2f", (k + 1), n, e.origin.getResolvedTree().getRoot().getFormula().toString(), e.origin.getPrecursorIonType().toString(),  f.getInchi().in2D, allCandidates.get(k).getScore()));
                }
                if (allCandidates.size() > 20) {
                    println("... " + (allCandidates.size() - 20) + " further candidates.");
                }
                println("");
            }
        } catch (PredictionException | ExecutionException e) {
            LoggerFactory.getLogger(this.getClass()).error("Error while searching structure for " + i.experiment.getName() + " (" + i.file + "): " + e.getMessage(), e);
        }
    }

    private BioFilter getBioFilter(String option) {
        final BioFilter bioFilter;
        if (option.equalsIgnoreCase("pubchem") || option.equalsIgnoreCase("all")) {
            bioFilter = BioFilter.ALL;
        } else bioFilter = BioFilter.ONLY_BIO;
        return bioFilter;
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
        if (!aliasMap.containsKey(name.toLowerCase())) {
            if (new File(name).exists()) {
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
            } else if (customDatabases.containsKey(name)) {
                return customDatabases.get(name);
            } else {
                unknownDatabaseError(name, aliasMap);
                return null;
            }
        } else {
            if (customDatabases.containsKey(name)) return customDatabases.get(name);

            final BioFilter filter = getBioFilter(name);
            if (filter == BioFilter.ALL) return pubchemDatabase;
            else return bioDatabase;
        }
    }


    SearchableDatabase pubchemDatabase, bioDatabase;
    HashMap<File, FilebasedDatabase> customDatabaseCache;
    private HashMap<String, SearchableDatabase> customDatabases;
    protected File db_cache_dir;

    protected void initializeDatabaseCache() {
        final File d = CONFIG_STORAGE.getDatabaseDirectory();
        db_cache_dir = d;
        pubchemDatabase = new SearchableDbOnDisc("PubChem", d, true, true, false);
        bioDatabase = new SearchableDbOnDisc("biological database", d, true, true, false);
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
        return getDatabase(getFingerIdDatabaseOption());
    }

    private String getFingerIdDatabaseOption() {
        if (options.getFingerIdDb() != null) {
            return options.getFingerIdDb();
        } else {
            return options.getDatabase();
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
            final BioFilter bioFilter = getBioFilter(getFingerIdDatabaseOption());
            final PredictionPerformance[] performances = webAPI.getStatistics(indizes);
//            this.fingerblast.setScoring(new CSIFingerIdScoring(performances));
            this.confidence = webAPI.getConfidenceScore(bioFilter != BioFilter.ALL);
            this.bioConfidence = bioFilter != BioFilter.ALL ? confidence : webAPI.getConfidenceScore(true);
            MaskedFingerprintVersion.Builder b = MaskedFingerprintVersion.buildMaskFor(WebAPI.getFingerprintVersion());
            b.disableAll();
            for (int index : indizes.toArray()) {
                b.enable(index);
            }
            this.fingerprintVersion = b.toMask();

            {
                CachedRESTDB db = new CachedRESTDB(needsUpdate, fingerprintVersion, CachedRESTDB.getDefaultDirectory());
                db.checkCache();
            }

            this.fingerblast = new Fingerblast(webAPI.getCovarianceScoring(this.fingerprintVersion, 1d / performances[0].withPseudoCount(0.25d).numberOfSamplesWithPseudocounts()), getFingerIdDatabaseWrapper());
        } catch (IOException e) {
            LoggerFactory.getLogger(this.getClass()).error("Our webservice is currently not available. You can still use SIRIUS without the --fingerid option. Please feel free to mail us at sirius-devel@listserv.uni-jena.de", e);
            System.exit(1);
        }
        progress.info("CSI:FingerId initialization done.");
    }

    protected Canopus canopus = null;

    public void initCanopus() {
        if (options.getExperimentalCanopus() != null) {
            try {
                this.canopus = Canopus.loadFromFile(options.getExperimentalCanopus());
                try (final BufferedWriter bw = FileUtils.getWriter(new File("canopus.csv"))) {
                    bw.write("relativeIndex\tabsoluteIndex\tid\tname\tdescription\n");
                    final ClassyFireFingerprintVersion cv = canopus.getClassyFireFingerprintVersion();
                    final MaskedFingerprintVersion mask = canopus.getCanopusMask();
                    int k = 0;
                    for (int index : mask.allowedIndizes()) {
                        final ClassyfireProperty prop = cv.getMolecularProperty(index);
                        bw.write(String.format(Locale.US, "%d\t%d\t%s\t%s\t%s\n", k++, index, prop.getChemontIdentifier(), prop.getName(), prop.getDescription()));
                    }
                }
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
                            try (final RESTDatabase db = webAPI.getRESTDb(BioFilter.ONLY_BIO, db_cache_dir)) {
                                fingerprintCandidates = db.lookupStructuresAndFingerprintsByFormula(molecularFormula, fingerprintCandidates);
                            }
                        }
                        if (db.searchInPubchem()) {
                            try (final RESTDatabase db = webAPI.getRESTDb(BioFilter.ONLY_NONBIO, db_cache_dir)) {
                                fingerprintCandidates = db.lookupStructuresAndFingerprintsByFormula(molecularFormula, fingerprintCandidates);
                            }
                        }
                        if (db.isCustomDb()) {
                            fingerprintCandidates = getFileBasedDb(db).lookupStructuresAndFingerprintsByFormula(molecularFormula, fingerprintCandidates);
                        }
                        return fingerprintCandidates;
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
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
            return new RESTDatabase(pubchemDatabase.getDatabasePath(), bf) {
                @Override
                public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws DatabaseException {
                    return filterByFlag(super.lookupStructuresAndFingerprintsByFormula(formula, fingerprintCandidates), getFingerIdDatabaseOption());
                }
            };
        }

    }

    private <T extends Collection<FingerprintCandidate>> T filterByFlag(T fingerprintCandidates, String fingerIdDatabaseOption) {
        final HashMap<String, Long> aliasMap = getDatabaseAliasMap();
        final long flag;
        final String dbOptName = fingerIdDatabaseOption.toLowerCase();
        if (aliasMap.containsKey(dbOptName)) {
            flag = aliasMap.get(dbOptName).longValue() == DatasourceService.Sources.BIO.flag ? 0 : aliasMap.get(dbOptName);
        } else {
            flag = 0L;
        }
        if (flag == 0) return fingerprintCandidates;
        final List<FingerprintCandidate> filtered = new ArrayList<>();
        for (FingerprintCandidate fc : fingerprintCandidates) {
            if ((flag & fc.getBitset()) != 0) {
                filtered.add(fc);
            }
        }
        fingerprintCandidates.clear();
        fingerprintCandidates.addAll(filtered);
        return fingerprintCandidates;
    }

    @Override
    protected Set<MolecularFormula> getFormulaWhiteset(Instance i, List<String> whitelist) {
        final String dbOptName = options.getDatabase().toLowerCase();
        if (dbOptName.equals("all")) return super.getFormulaWhiteset(i, whitelist);
        else {
            final HashMap<String, Long> aliasMap = getDatabaseAliasMap();
            final SearchableDatabase searchableDatabase = getDatabase();
            final long flag;

            if (aliasMap.containsKey(dbOptName)) {
                flag = aliasMap.get(dbOptName).longValue() == DatasourceService.Sources.BIO.flag ? 0 : aliasMap.get(dbOptName);
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
                if (searchableDatabase.searchInPubchem()) {
                    try (final RESTDatabase db = api.getRESTDb(BioFilter.ONLY_NONBIO, pubchemDatabase.getDatabasePath())) {
                        candidates.addAll(db.lookupMolecularFormulas(i.experiment.getIonMass(), dev, allowedIonTypes.toArray(new PrecursorIonType[allowedIonTypes.size()])));
                    }
                }
                if (searchableDatabase.isCustomDb()) {
                    candidates.addAll(getFileBasedDb(searchableDatabase).lookupMolecularFormulas(i.experiment.getIonMass(), dev, allowedIonTypes.toArray(new PrecursorIonType[allowedIonTypes.size()])));
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error("Connection to database fails. Probably our webservice is currently offline. You can still use SIRIUS in offline mode - you just have to remove the database flags -d or --database because database search is not available in offline mode.", e);
                System.exit(1);
                return null;
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
        }


    }

    private HashMap<String, Long> getDatabaseAliasMap() {
        final HashMap<String, Long> aliasMap = new HashMap<>();
        for (DatasourceService.Sources source : DatasourceService.Sources.values()) {
            aliasMap.put(source.name.toLowerCase(), source.searchFlag);
        }
        aliasMap.put("biocyc", DatasourceService.Sources.METACYC.flag);
        aliasMap.put("bio", DatasourceService.Sources.BIO.searchFlag);
        aliasMap.put("undp", DatasourceService.Sources.UNDP.flag);
        aliasMap.put("all", 0L);
        if (!aliasMap.containsKey(options.getDatabase().toLowerCase()) && !new File(options.getDatabase()).exists() && !customDatabases.containsKey(options.getDatabase())) {
            unknownDatabaseError(options.getDatabase().toLowerCase(), aliasMap);
        }
        return aliasMap;
    }

    private void unknownDatabaseError(String name, HashMap<String, Long> aliasMap) {
        final List<String> knownDatabases = new ArrayList<>();
        knownDatabases.addAll(aliasMap.keySet());
        knownDatabases.addAll(customDatabases.keySet());
        LoggerFactory.getLogger(this.getClass()).error("Unknown database '" + name + "'. Available are: " + Joiner.on(", ").join(knownDatabases));
        System.exit(1);
    }

    @Override
    public void setup() {
        initializeDatabaseCache();
        super.setup();
    }

    protected class FingerIdSubmitter extends CLIJobSubmitter {

        public FingerIdSubmitter(Iterator<Instance> instances) {
            super(instances);
        }

        @Override
        protected void submitJobs(JobContainer watcher) {
            super.submitJobs(watcher);
            if (options.isFingerid()) {
                FingerIDJJob fij = makeFingerIdJob(watcher.sourceInstance, watcher.getJob(Sirius.SiriusIdentificationJob.class));
                if (fij != null)
                    submitJob(fij, watcher);
            }
        }
    }

    @Override
    protected CLIJobSubmitter newSubmitter(Iterator<Instance> instanceIterator) {
        return new FingerIdSubmitter(instanceIterator);
    }
}
