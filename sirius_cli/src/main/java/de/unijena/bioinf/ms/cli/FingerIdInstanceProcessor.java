package de.unijena.bioinf.ms.cli;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.CSIPredictor;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.TrainingStructuresPerPredictor;
import de.unijena.bioinf.fingerid.db.*;
import de.unijena.bioinf.fingerid.jjobs.FingerIDJJob;
import de.unijena.bioinf.fingerid.net.WebAPI;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.projectspace.ExperimentResultJJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static de.unijena.bioinf.ms.cli.FingerIdOptions.CONSIDER_ALL_FORMULAS;


public class FingerIdInstanceProcessor implements InstanceProcessor<Map<IdentificationResult, ProbabilityFingerprint>> {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    FingerIdOptions options;

    protected CSIPredictor positivePredictor, negativePredictor;

    public FingerIdInstanceProcessor(FingerIdOptions options) {
        this.options = options;
    }

    @Override
    public boolean validate() {
        //todo shared logger?
        if (options.getFingerIdDb() != null && !options.isFingerid()) {
            LoggerFactory.getLogger(this.getClass()).error("--fingerid_db defines the database CSI:FingerID should search in. This option makes only sense when used together with --fingerid. Use --db for setting up the database in which SIRIUS should search for molecular formulas.");
            return false;
        }
        if (options.getExperimentalCanopus() != null && !options.isFingerid()) {
            LoggerFactory.getLogger(this.getClass()).error("Cannot predict compound categories with CSI:FingerID is disabled. Please enable CSI:FingerID with --fingerid option.");
            return false;
        }
        return true;
    }

//    protected FingerIDJJob makeFingerIdJob(final Instance i, BasicJJob<List<IdentificationResult>> siriusJob) {
//        //todo make compatible with every job which provides List<IdentificationResult>
//        if (siriusJob == null) return null;
//
//        //predict fingerprint
//        final HashMap<String, Long> dbMap = getDatabaseAliasMap(); //todo this is the same for all instances or?????
//        Long flagW = dbMap.get(options.getDatabase());
//        if (flagW == null) flagW = 0L;
//        final long flag = flagW;
//        CSIPredictor csi = i.experiment.getPrecursorIonType().getCharge() > 0 ? positivePredictor : negativePredictor;
//        FingerIDJJob fingerIdJob = new FingerIDJJob(csi.getBlaster(), csi.getFingerprintVersion(), csi.getDatabase(), getFingerIdDatabase(), options.getPredictors());
//        fingerIdJob.addRequiredJob(siriusJob);
//        fingerIdJob.setDbFlag(flag);
//        fingerIdJob.setBioFilter(getBioFilter());
//        fingerIdJob.setCanopus(canopus);
//
//        return fingerIdJob;
//    }

    protected FingerIDJJob makeFingerIdJob(final Instance i, ExperimentResultJJob experimentResultJJob) {
        //todo make compatible with every job which provides List<IdentificationResult>
        if (experimentResultJJob == null) return null;

        //predict fingerprint
        final HashMap<String, Long> dbMap = getDatabaseAliasMap(); //todo this is the same for all instances or?????
        Long flagW = dbMap.get(options.getDatabase());
        if (flagW == null) flagW = 0L;
        final long flag = flagW;
        CSIPredictor csi = i.experiment.getPrecursorIonType().getCharge() > 0 ? positivePredictor : negativePredictor;
        FingerIDJJob fingerIdJob = new FingerIDJJob(csi.getBlaster(), csi.getFingerprintVersion(), csi.getDatabase(), getFingerIdDatabase(), options.getPredictors());
        fingerIdJob.addRequiredJob(experimentResultJJob);
        fingerIdJob.setDbFlag(flag);
        fingerIdJob.setBioFilter(getBioFilter());
        fingerIdJob.setCanopus(canopus);

        return fingerIdJob;
    }

    //todo here or somewhere else?


    @Override
    public void output(Map<IdentificationResult, ProbabilityFingerprint> propPrints) {
        //collect candidates
        final List<FingerIdWorkflow.CandidateElement> allCandidates = new ArrayList<>();
        for (IdentificationResult identificationResult : propPrints.keySet()) {
            FingerIdResult fingerIdResult = identificationResult.getAnnotationOrNull(FingerIdResult.class);
            if (fingerIdResult != null) {
                for (Scored<FingerprintCandidate> fpc : fingerIdResult.getCandidates())
                    allCandidates.add(new FingerIdWorkflow.CandidateElement(fingerIdResult, fpc));
            }
        }

        //sort by score
        allCandidates.sort(Scored.<FingerprintCandidate>desc());
        if (allCandidates.size() == 0) {
            logger.info("No candidate structures found for given mass and computed trees.");
            return;
        }

        // find top identificationResult
        FingerIdResult topResult = allCandidates.get(0).origin;

        // compute confidence for top hit from bio database
        final ArrayList<Scored<CompoundWithAbstractFP<Fingerprint>>> confidenceList = new ArrayList<>();
        final ArrayList<Scored<CompoundWithAbstractFP<Fingerprint>>> bioConfidenceList = new ArrayList<>();
        FingerIdWorkflow.CandidateElement topBio = null;
        for (FingerIdWorkflow.CandidateElement fc : allCandidates) {
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
            logger.info(String.format(Locale.US, "Top biocompound is %s %s (%s)\n", name, topBio.origin.getPrecursorIonType().toString(), fc.getInchi().in2D));
            //progress.info(String.format(Locale.US, "Top biocompound is %s %s (%s) with confidence %.2f\n", name, topBio.origin.getPrecursorIonType().toString(), fc.getInchi().in2D, confidenceScore));
        }

        for (int k = 0; k < Math.min(20, allCandidates.size()); ++k) {
            FingerIdWorkflow.CandidateElement e = allCandidates.get(k);
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

    //////////////////////////////////////////

    @Override
    public boolean setup() {
        initializeDatabaseCache();
        SearchableDatabases.getCustomDatabases();
        initDatabasesAndVersionInfoIfNecessary();

        if (options.getGeneratingCompoundDatabase() != null) {
            try {
                generateCustomDatabase(options);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
            return true;
        }

        if (options.isFingerid()) {
            initFingerBlast();
            initCanopus();
        }

        return true;
    }

    private void generateCustomDatabase(FingerIdOptions options) throws IOException {
        DatabaseImporter.importDatabase(options.getGeneratingCompoundDatabase(), options.getInput());
    }

    private void initDatabasesAndVersionInfoIfNecessary() {
        if (isOffline()) return;
        try {
            positivePredictor = new CSIPredictor(PredictorType.CSI_FINGERID_POSITIVE);
            negativePredictor = new CSIPredictor(PredictorType.CSI_FINGERID_NEGATIVE);
            positivePredictor.initialize();
            negativePredictor.initialize();
            //download training structures
            TrainingStructuresPerPredictor.getInstance().addAvailablePredictorTypes(PredictorType.CSI_FINGERID_POSITIVE, PredictorType.CSI_FINGERID_NEGATIVE);
        } catch (IOException e) {
            System.err.println("Cannot connect to CSI:FingerID webserver and online chemical database. You can still use SIRIUS in offline mode: just do not use any chemical database and omit the --fingerid option.");
            logger.error(e.getMessage(), e);
            System.exit(1);
        }

    }

    protected SearchableDatabase getDatabase() {
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

    private BioFilter getBioFilter(String option) {
        final BioFilter bioFilter;
        if (option.equalsIgnoreCase("pubchem") || option.equalsIgnoreCase("all")) {
            bioFilter = BioFilter.ALL;
        } else bioFilter = BioFilter.ONLY_BIO;
        return bioFilter;
    }

    protected BioFilter getBioFilter() {
        final BioFilter bioFilter;
        if (options.getDatabase().equalsIgnoreCase("pubchem") || options.getDatabase().equalsIgnoreCase("all")) {
            bioFilter = BioFilter.ALL;
        } else bioFilter = BioFilter.ONLY_BIO;
        return bioFilter;
    }


    SearchableDatabase pubchemDatabase, bioDatabase;
    HashMap<File, FilebasedDatabase> customDatabaseCache;
    private HashMap<String, SearchableDatabase> customDatabases;
    protected File db_cache_dir;

    protected void initializeDatabaseCache() {
        final File d = Paths.get(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.fingerID.cache")).toFile();
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

    public boolean isOffline() {
        return !options.isFingerid() && options.getDatabase().equals(CONSIDER_ALL_FORMULAS);
    }


    protected HashMap<String, Long> getDatabaseAliasMap() {
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


    private void initFingerBlast() {
        //todo does nothing
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


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void print(String s) {
        if (!CombinedCLI.shellOutputSurpressed) System.out.print(s);
    }

    public void println(String s) {
        if (!CombinedCLI.shellOutputSurpressed) System.out.println(s);
    }

    protected void printf(String msg, Object... args) {
        if (!CombinedCLI.shellOutputSurpressed)
            System.out.printf(Locale.US, msg, args);
    }
}
