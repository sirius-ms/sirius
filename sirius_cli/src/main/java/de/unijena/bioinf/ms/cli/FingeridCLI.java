package de.unijena.bioinf.ms.cli;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.CSIPredictor;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.FingerIdResultReader;
import de.unijena.bioinf.fingerid.FingerIdResultWriter;
import de.unijena.bioinf.fingerid.db.*;
import de.unijena.bioinf.fingerid.jjobs.FingerIDJJob;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingeriddb.job.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BufferedJJobSubmitter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.projectspace.*;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class FingeridCLI<Options extends FingerIdOptions> extends CLI<Options> {

    protected CSIPredictor positivePredictor, negativePredictor;


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
        SearchableDatabases.getCustomDatabases(); //todo why?
        super.parseArgsAndInit(args, optionsClass);
        initDatabasesAndVersionInfoIfNecessary();
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

    private void initDatabasesAndVersionInfoIfNecessary() {
        if (isOffline()) return;
        try {
            positivePredictor = new CSIPredictor(PredictorType.CSI_FINGERID_POSITIVE);
            negativePredictor = new CSIPredictor(PredictorType.CSI_FINGERID_NEGATIVE);
            positivePredictor.initialize();
            negativePredictor.initialize();
        } catch (IOException e) {
            System.err.println("Cannot connect to CSI:FingerID webserver and online chemical database. You can still use SIRIUS in offline mode: just do not use any chemical database and omit the --fingerid option.");
            LoggerFactory.getLogger(FingeridCLI.class).error(e.getMessage(), e);
            System.exit(1);
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
        CSIPredictor csi = i.experiment.getPrecursorIonType().getCharge() > 0 ? positivePredictor : negativePredictor;
        FingerIDJJob fingerIdJob = new FingerIDJJob(csi.getBlaster(), csi.getFingerprintVersion(), csi.getDatabase(), getFingerIdDatabase(), options.getPredictors());
        fingerIdJob.addRequiredJob(siriusJob);
        fingerIdJob.setDbFlag(flag);
        fingerIdJob.setBioFilter(getBioFilter());
        fingerIdJob.setCanopus(canopus);

        return fingerIdJob;
    }

    public boolean isOffline() {
        return !options.isFingerid() && options.getDatabase().equals(CONSIDER_ALL_FORMULAS);
    }

    protected static final class CandidateElement extends Scored<FingerprintCandidate> {
        protected final FingerIdResult origin;

        public CandidateElement(FingerIdResult ir, Scored<FingerprintCandidate> c) {
            super(c.getCandidate(), c.getScore());
            this.origin = ir;
        }
    }

    protected ExperimentResult createExperimentResult(BufferedJJobSubmitter<Instance>.JobContainer jc, Sirius.SiriusIdentificationJob siriusJob, List<IdentificationResult> results) {
        FingerIDJJob fid = jc.getJob(FingerIDJJob.class);
        final List<IdentificationResult> total = new ArrayList<>(results);
        if (fid != null) {
            fid.takeResult();
            total.addAll(fid.getAddedIdentificationResults());
        }
        return new ExperimentResult(siriusJob.getExperiment(), total);
    }


    protected void handleFingerIdResults(Instance i, FingerIDJJob fingerprintJob) {
        try {
            Map<IdentificationResult, ProbabilityFingerprint> propPrints = fingerprintJob.awaitResult();
            if (propPrints != null) {
                //collect candidates
                final List<CandidateElement> allCandidates = new ArrayList<>();
                for (IdentificationResult identificationResult : propPrints.keySet()) {
                    FingerIdResult fingerIdResult = identificationResult.getAnnotationOrNull(FingerIdResult.class);
                    if (fingerIdResult != null) {
                        for (Scored<FingerprintCandidate> fpc : fingerIdResult.getCandidates())
                            allCandidates.add(new CandidateElement(fingerIdResult, fpc));
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
                /*
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
                */
                //todo the biofilter does also stay constant for all i
                if (getBioFilter() != BioFilter.ONLY_BIO && topBio != null && topBio.getCandidate() != allCandidates.get(0).getCandidate()) {
                    final Scored<CompoundWithAbstractFP<Fingerprint>> c = bioConfidenceList.get(0);
                    final CompoundCandidate fc = topBio.getCandidate();
                    final CompoundWithAbstractFP[] list = new CompoundWithAbstractFP[bioConfidenceList.size()];
                    for (int k = 0; k < bioConfidenceList.size(); ++k)
                        list[k] = bioConfidenceList.get(k).getCandidate();
                    //final double confidenceScore = bioConfidence == null ? 0 : bioConfidence.estimateProbability(new CompoundWithAbstractFP<ProbabilityFingerprint>(c.getCandidate().getInchi(), predictedFingerprints.get(c.getCandidate().getInchi().extractFormula())), list);

                    String name = fc.getName();
                    if (name == null || name.isEmpty()) name = fc.getSmiles();
                    if (name == null || name.isEmpty()) name = "";
                    progress.info(String.format(Locale.US, "Top biocompound is %s %s (%s)\n", name, topBio.origin.getPrecursorIonType().toString(), fc.getInchi().in2D));
                    //progress.info(String.format(Locale.US, "Top biocompound is %s %s (%s) with confidence %.2f\n", name, topBio.origin.getPrecursorIonType().toString(), fc.getInchi().in2D, confidenceScore));
                }

                for (int k = 0; k < Math.min(20, allCandidates.size()); ++k) {
                    CandidateElement e = allCandidates.get(k);
                    FingerprintCandidate f = e.getCandidate();
                    String n = f.getName();
                    if (n == null || n.isEmpty()) n = f.getSmiles();
                    if (n == null) n = "";
                    println(String.format(Locale.US, "%2d.) %s\t%s\t%s\t%s\tscore: %.2f", (k + 1), n, e.origin.getResolvedTree().getRoot().getFormula().toString(), e.origin.getPrecursorIonType().toString(), f.getInchi().in2D, allCandidates.get(k).getScore()));
                }
                if (allCandidates.size() > 20) {
                    println("... " + (allCandidates.size() - 20) + " further candidates.");
                }
                println("");
            }
        } catch (ExecutionException e) {
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
            LoggerFactory.getLogger(this.getClass()).error("--fingerid_db defines the database CSI:FingerID should search in. This option makes only sense when used together with --fingerid. Use --db for setting up the database in which SIRIUS should search for molecular formulas.");
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
        final File d = SearchableDatabases.getDatabaseDirectory();
        db_cache_dir = d;
        pubchemDatabase = new SearchableDbOnDisc("PubChem", d, true, true, false);
        bioDatabase = new SearchableDbOnDisc("biological database", d, true, true, false);
        this.customDatabaseCache = new HashMap<>();
        customDatabases = new HashMap<>();
        for (SearchableDatabase db : SearchableDatabases.getCustomDatabases()) {
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
            final Set<PrecursorIonType> allowedIonTypes = new HashSet<>();
            if (i.experiment.getPrecursorIonType() == null || i.experiment.getPrecursorIonType().isIonizationUnknown()) {
                allowedIonTypes.addAll(i.experiment.getAnnotation(PossibleAdducts.class).getAdducts());
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

    protected static final String CONSIDER_ALL_FORMULAS = "all";

    private HashMap<String, Long> getDatabaseAliasMap() {
        final HashMap<String, Long> aliasMap = new HashMap<>();
        for (DatasourceService.Sources source : DatasourceService.Sources.values()) {
            aliasMap.put(source.name.toLowerCase(), source.searchFlag);
        }
        aliasMap.put("biocyc", DatasourceService.Sources.METACYC.flag);
        aliasMap.put("bio", DatasourceService.Sources.BIO.searchFlag);
        aliasMap.put("unpd", DatasourceService.Sources.UNDP.flag);
        aliasMap.put(CONSIDER_ALL_FORMULAS, 0L);
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
