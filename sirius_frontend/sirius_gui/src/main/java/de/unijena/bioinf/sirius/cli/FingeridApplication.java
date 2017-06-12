package de.unijena.bioinf.sirius.cli;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ConfidenceScore.PredictionException;
import de.unijena.bioinf.ConfidenceScore.QueryPredictor;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.Fingerprinter;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.dbgen.DatabaseImporter;
import de.unijena.bioinf.sirius.fingerid.FingerIdResult;
import de.unijena.bioinf.sirius.fingerid.FingerIdResultReader;
import de.unijena.bioinf.sirius.fingerid.FingerIdResultWriter;
import de.unijena.bioinf.sirius.gui.fingerid.VersionsInfo;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
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
            generateCustomDatabase(options);
            return;
        }
        fingeridEnabled = options.isFingerid();
        if (fingeridEnabled) initFingerBlast();
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

    private void generateCustomDatabase(FingerIdOptions options) {
        DatabaseImporter.importDatabase((CdkFingerprintVersion) WebAPI.getFingerprintVersion(), options.getGeneratingCompoundDatabase(), options.getInput());
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
                    filteredResults.add(e);
                }
                final List<Future<ProbabilityFingerprint>> futures = new ArrayList<>();
                for (IdentificationResult result : filteredResults) {

                    // workaround... we should think about that carefully
                    final FTree tree = result.getResolvedTree();
                    // ???
                    //tree.setAnnotation(PrecursorIonType.class, tree.getFragmentAnnotationOrNull(PrecursorIonType.class).get(tree.getRoot()));
                    futures.add(webAPI.predictFingerprint(executorService, i.experiment, tree, fingerprintVersion));
                }
                final List<Scored<FingerprintCandidate>> allCandidates = new ArrayList<>();
                final HashMap<String, Integer> dbMap = getDatabaseAliasMap();
                Integer flagW = dbMap.get(options.getDatabase());
                if (flagW == null) flagW = 0;
                final int flag = flagW;
                final HashMap<MolecularFormula, ProbabilityFingerprint> predictedFingerprints = new HashMap<>();
                for (int k = 0; k < filteredResults.size(); ++k) {
                    final ProbabilityFingerprint fp = futures.get(k).get();
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
                    final double confidenceScore = confidence.estimateProbability(new CompoundWithAbstractFP<ProbabilityFingerprint>(c.getCandidate().getInchi(), predictedFingerprints.get(c.getCandidate().getInchi().extractFormula())), list);

                    if (topResult!=null) topResult.setConfidence(confidenceScore);

                    String name = fc.getName();
                    if (name == null || name.isEmpty()) name = fc.getSmiles();
                    if (name == null || name.isEmpty()) name = "";
                    progress.info(String.format(Locale.US, "Top compound is %s (%s) with confidence %.2f\n", name, fc.getInchi().in2D, confidenceScore));
                }
                if (bioFilter != BioFilter.ONLY_BIO && topBio!=null && topBio != allCandidates.get(0).getCandidate()) {
                    final Scored<CompoundWithAbstractFP<Fingerprint>> c = bioConfidenceList.get(0);
                    final CompoundCandidate fc = topBio;
                    final CompoundWithAbstractFP[] list = new CompoundWithAbstractFP[bioConfidenceList.size()];
                    for (int k = 0; k < bioConfidenceList.size(); ++k) list[k] = bioConfidenceList.get(k).getCandidate();
                    final double confidenceScore = bioConfidence.estimateProbability(new CompoundWithAbstractFP<ProbabilityFingerprint>(c.getCandidate().getInchi(), predictedFingerprints.get(c.getCandidate().getInchi().extractFormula())), list);

                    String name = fc.getName();
                    if (name == null || name.isEmpty()) name = fc.getSmiles();
                    if (name == null || name.isEmpty()) name = "";
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
    }

    private AbstractChemicalDatabase getDatabase() {
        return getDatabase(options.getDatabase());
    }

    private AbstractChemicalDatabase getDatabase(String name) {
        final HashMap<String, Integer> aliasMap = getDatabaseAliasMap();
        if (!aliasMap.containsKey(name.toLowerCase()) && new File(name).exists()) {
            try {
                return new FileDatabase(new File(name));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new RESTDatabase(getBioFilter());
        }
    }

    private AbstractChemicalDatabase getFingerIdDatabase() {
        if (options.getFingerIdDb() != null) {
            return getDatabase(options.getFingerIdDb());
        } else {
            return getDatabase(options.getDatabase());
        }
    }

    private void initFingerBlast() {
        progress.info("Initialize CSI:FingerId...");
        try (WebAPI webAPI = WebAPI.newInstance()) {
            final VersionsInfo needsUpdate = webAPI.needsUpdate();
            if (needsUpdate != null && needsUpdate.outdated()) {
                progress.info("Your current SIRIUS+CSI:FingerID version is outdated. Please download the latest software version if you want to use CSI:FingerId search. Current version: " + WebAPI.VERSION + ". New version is " + needsUpdate.siriusGuiVersion + ". You can download the last version here: " + WebAPI.SIRIUS_DOWNLOAD);
                System.exit(1);
            }
            final TIntArrayList indizes = new TIntArrayList();
            final BioFilter bioFilter = getBioFilter();
            final PredictionPerformance[] performances = webAPI.getStatistics(indizes);
            this.fingerblast = new Fingerblast(getFingerIdDatabase());
//            this.fingerblast.setScoring(new CSIFingerIdScoring(performances));
            this.confidence = webAPI.getConfidenceScore(bioFilter != BioFilter.ALL);
            this.bioConfidence = bioFilter != BioFilter.ALL ? confidence : webAPI.getConfidenceScore(true);
            MaskedFingerprintVersion.Builder b = MaskedFingerprintVersion.buildMaskFor(webAPI.getFingerprintVersion());
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

    @Override
    protected Set<MolecularFormula> getFormulaWhiteset(Instance i, List<String> whitelist) {
        if (options.getDatabase().equalsIgnoreCase("all")) return super.getFormulaWhiteset(i, whitelist);
        else {
            final HashMap<String, Integer> aliasMap = getDatabaseAliasMap();

            final AbstractChemicalDatabase database = getDatabase();
            final int flag;

            if (aliasMap.containsKey(options.getDatabase().toLowerCase())) {
                flag = aliasMap.get(options.getDatabase().toLowerCase());
            } else {
                flag = 0;
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
                List<List<FormulaCandidate>> candidates = database.lookupMolecularFormulas(i.experiment.getIonMass(), dev, allowedIonTypes.toArray(new PrecursorIonType[allowedIonTypes.size()]));
                final HashSet<MolecularFormula> allowedSet = new HashSet<>();
                for (List<FormulaCandidate> fc : candidates) {
                    for (FormulaCandidate f : fc) {
                        final int bitset = f.getBitset();
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

    private HashMap<String, Integer> getDatabaseAliasMap() {
        final HashMap<String, Integer> aliasMap = new HashMap<>();
        for (DatasourceService.Sources source : DatasourceService.Sources.values()) {
            aliasMap.put(source.name.toLowerCase(), source.flag);
        }
        aliasMap.put("biocyc", DatasourceService.Sources.METACYC.flag);
        aliasMap.put("bio", DatasourceService.Sources.BIO.flag);
        aliasMap.put("all", 0);
        if (!aliasMap.containsKey(options.getDatabase().toLowerCase()) && !new File(options.getDatabase()).exists()) {
            LoggerFactory.getLogger(this.getClass()).error("Unknown database '" + options.getDatabase().toLowerCase() + "'. Available are: " + Joiner.on(", ").join(aliasMap.keySet()));
        }
        return aliasMap;
    }
}
