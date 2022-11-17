package de.unijena.bioinf.ms.frontend.subtools.harvester;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeStatistics;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.babelms.MsIO;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.elgordo.AnnotatedLipidSpectrum;
import de.unijena.bioinf.elgordo.MassToLipid;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.fragmenter.*;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.ms.frontend.subtools.CLIRootOptions;
import de.unijena.bioinf.ms.frontend.subtools.PostprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.summaries.SummaryOptions;
import de.unijena.bioinf.ms.frontend.subtools.summaries.SummarySubToolJob;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.sirius.Ms2Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.elementdetection.FluorineDetector;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.set.hash.TShortHashSet;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HarvesterWorkflow extends PostprocessingJob<Boolean> implements Workflow {

    private static final Logger LOG = LoggerFactory.getLogger(HarvesterWorkflow.class);
    private final HarvesterOptions options;
    private final ParameterConfig config;
    private final RootOptions<?,?,?, ?> rootOptions;

    public HarvesterWorkflow(RootOptions<?,?,?,?> rootOptions, ParameterConfig config, HarvesterOptions options) {
        this.rootOptions = rootOptions;
        this.config = config;
        this.options = options;
    }

    @Override
    public void run() {
        SiriusJobs.getGlobalJobManager().submitJob(this).takeResult();
    }

    @Override
    protected Boolean compute() throws Exception {
        if (!options.location.toFile().exists()) {
            options.location.toFile().mkdirs();
        }
        if (options.locationForm!=null && !options.locationForm.toFile().exists()) {
            options.locationForm.toFile().mkdirs();
        }
        if (options.lipidPath!=null && !options.lipidPath.toFile().exists()) {
            options.lipidPath.toFile().mkdirs();
        }
        if (options.fluorinePath!=null && !options.fluorinePath.toFile().exists()) {
            options.fluorinePath.toFile().mkdirs();
        }
        try {
            ProjectSpaceManager project = rootOptions.getProjectSpace();
            Iterable<? extends Instance> compounds = rootOptions instanceof CLIRootOptions ? project
                    : SiriusJobs.getGlobalJobManager().submitJob(rootOptions.makeDefaultPreprocessingJob()).awaitResult();

            final String prefix;
            if (options.prefix.isEmpty())
                prefix =project.projectSpace().getLocation().toFile().getName();
            else
                prefix = options.prefix;
            final boolean alsoExportTopFormula = options.locationForm!=null;
            List<CompoundContainerId> ids = null;
            if (compounds != null) {
                List<CompoundContainerId> idsTMP = new ArrayList<>(project.size());
                compounds.forEach(i -> idsTMP.add(i.getID()));
                ids = idsTMP;
            }
            int INDEX = 1, FORM_INDEX = 1, FL_INDEX=1;
            try (final PrintStream outstream = new PrintStream(new File(options.location.toFile(), "summary.tsv" ))) {
                outstream.println(CSV_HEADER);
                for (CompoundContainerId cid : ids) {
                    if (cid.getConfidenceScore().orElse(0d) < options.minConfidenceThreshold) continue;
                    else System.out.println(cid.getDirectoryName() + "\t confidence = " + cid.getConfidenceScore().orElse(0d));
                    final Instance instance = project.getInstanceFromCompound(cid);

                    // check fluorine and el gordo
                    {
                        final Ms2Experiment experiment = instance.getExperiment();
                        if (checkFluorineAndLipid(instance, options, experiment, prefix, FL_INDEX))
                            ++FL_INDEX;
                    }


                    final List<? extends SScored<FormulaResult, ? extends FormulaScore>> list = instance.loadFormulaResults(FormulaScoring.class, FTree.class, FingerprintResult.class, FBCandidates.class, FBCandidateFingerprints.class, CanopusResult.class);
                    final ArrayList<Scored<CompoundCandidate>> allStructures = new ArrayList<>();
                    final HashMap<String, ArrayFingerprint> allFingerprints = new HashMap<>();
                    final HashMap<String, ArrayFingerprint> classyFps = new HashMap<>();
                    final HashMap<String, FormulaResult> struct2formula = new HashMap<>();
                    final HashMap<String, EpimetheusResult> epimetheus = new HashMap<>();

                    TDoubleArrayList formulaScores = new TDoubleArrayList();

                    for (var entry : list) {
                        // add all structures
                        final List<Fingerprint> fingerprints = entry.getCandidate().getAnnotation(FBCandidateFingerprints.class).map(FBCandidateFingerprints::getFingerprints).orElseGet(Collections::emptyList);
                        final Optional<CanopusResult> canopus = entry.getCandidate().getAnnotation(CanopusResult.class);
                        final List<Scored<CompoundCandidate>> candidates = entry.getCandidate().getAnnotation(FBCandidates.class).map(FBCandidates::getResults).orElseGet(Collections::emptyList);
                        formulaScores.add(candidates.stream().mapToDouble(SScored::getScore).max().orElse(Double.NEGATIVE_INFINITY));
                        for (int k = 0; k < fingerprints.size(); ++k) {
                            allStructures.add(candidates.get(k));
                            struct2formula.put(candidates.get(k).getCandidate().getInchiKey2D(), entry.getCandidate());
                            if (canopus.isPresent()) classyFps.put(candidates.get(k).getCandidate().getInchiKey2D(), canopus.get().getCanopusFingerprint().asDeterministic().asArray());
                            allFingerprints.put(candidates.get(k).getCandidate().getInchiKey2D(), fingerprints.get(k).asArray());
                        }
                    }
                    // sort structures by score
                    allStructures.sort(Comparator.comparingDouble(x -> -x.getScore()));
                    formulaScores.sort();
                    formulaScores.reverse();

                    // the list of candidates that are used for internal statistics
                    final ArrayList<Scored<CompoundCandidate>> sublist = new ArrayList<>();
                    // the list of candidates that are soo similar we cannot distinguish them
                    final ArrayList<Scored<CompoundCandidate>> consideredCandidates = new ArrayList<>();
                    boolean exportAsConfident = false;
                    filterStructures(instance, allStructures, struct2formula, epimetheus, sublist, consideredCandidates);
                    if (consideredCandidates.size() > 0) {

                        final boolean high = isHighlyConfidentlyAnnotated(instance, consideredCandidates, epimetheus);
                        if (high) {
                            final Scored<CompoundCandidate> top = consideredCandidates.get(0);
                            consideredCandidates.clear();
                            consideredCandidates.add(top);
                        } else {
                            // if we have more than one formula in top scoring hits, remove
                        }
                        if (!high && consideredCandidates.size() <= 1) continue;
                        ConfidentAnnotation confi = new ConfidentAnnotation(instance, sublist, consideredCandidates, struct2formula, epimetheus, allFingerprints, classyFps);
                        if (confi.weight >= 0.2) {
                            outstream.println(confi.toCsvRow(INDEX));
                            confi.exportFile(options.location.toFile(), prefix, INDEX);
                            exportAsConfident = true;
                            ++INDEX;
                        }
                    }

                    if (alsoExportTopFormula && !exportAsConfident && formulaIsConfidentlyAnnotated(formulaScores,struct2formula,allStructures )) {
                        exportFormulaFile(options.locationForm.toFile(), prefix, FORM_INDEX, instance, struct2formula, allStructures);
                        ++FORM_INDEX;
                    }

                }
            }

            return true;
        } finally {

        }
    }

    private boolean checkFluorineAndLipid(Instance instance, HarvesterOptions options, Ms2Experiment experiment, String prefix, int index) throws IOException {
        boolean oneofboth=false;
        final ProcessedInput input = new Ms2Preprocessor().preprocess(experiment);

        if (options.lipidPath!=null) {
            final List<MassToLipid.LipidCandidate> lipidCandidates = new MassToLipid(input.getAnnotation(MS1MassDeviation.class).map(x -> x.allowedMassDeviation).orElseGet(() -> new Deviation(20)), input.getExperimentInformation().getPrecursorIonType().getCharge()).analyzePrecursor(input.getExperimentInformation().getIonMass());
            final Deviation ms2dev = input.getAnnotation(MS2MassDeviation.class).map(x -> x.allowedMassDeviation).orElseGet(() -> new Deviation(20));
            final MassToLipid m2l = new MassToLipid(ms2dev, input.getExperimentInformation().getPrecursorIonType().getCharge());
            final Spectrum<ProcessedPeak> peaklist = Spectrums.wrap(input.getMergedPeaks());
            final SimpleSpectrum ms2 = new SimpleSpectrum(peaklist);
            final Optional<AnnotatedLipidSpectrum<SimpleSpectrum>> annotated = lipidCandidates.stream().map(x -> m2l.annotateSpectrum(x, ms2)).filter(Objects::nonNull).max(Comparator.naturalOrder());
            if (annotated.isPresent() && annotated.get().getDetectionLevel().isFormulaSpecified()) {
                // copy File to lipid path
                final AdditionalFields add = experiment.getAnnotation(AdditionalFields.class, AdditionalFields::new);
                add.put("Harvester$elgordo-class", annotated.get().getAnnotatedSpecies().toString());
                add.put("Harvester$elgordo-class-score", String.valueOf(annotated.get().getClassCorrectScore()));
                add.put("Harvester$elgordo-formula", annotated.get().getFormula().toString());
                add.put("Harvester$elgordo-formula-score", String.valueOf(annotated.get().getFormulaCorrectScore()));
                add.put("Harvester$source", instance.getID().getDirectoryName());
                experiment.setAnnotation(AdditionalFields.class, add);
                try (final BufferedWriter bw = FileUtils.getWriter(new File(options.lipidPath.toFile(), prefix + "_" + index + ".ms"))) {
                    JenaMsWriter w = new JenaMsWriter();
                    w.write(bw, experiment);
                }
                oneofboth=true;
            }
        }
        if (options.fluorinePath!=null) {
            final double fl = new FluorineDetector().decisionScore(input);
            if (fl >= 0) {
                final AdditionalFields add = experiment.getAnnotation(AdditionalFields.class, AdditionalFields::new);
                add.put("Harvester$fluorine-score", String.valueOf(fl));
                add.put("Harvester$source", instance.getID().getDirectoryName());
                experiment.setAnnotation(AdditionalFields.class, add);
                try (final BufferedWriter bw = FileUtils.getWriter(new File(options.fluorinePath.toFile(), prefix + "_" + index + ".ms"))) {
                    JenaMsWriter w = new JenaMsWriter();
                    w.write(bw, experiment);
                }
                oneofboth=true;
            }
        }
        return oneofboth;
    }

    private void exportFormulaFile(File toFile, String prefix, int form_index, Instance instance, HashMap<String, FormulaResult> struct2formula, ArrayList<Scored<CompoundCandidate>> allStructures) throws IOException {
        final CompoundCandidate topStructure = allStructures.stream().max(Comparator.naturalOrder()).map(SScored::getCandidate).orElse(null);
        final FormulaResultId topFormula = struct2formula.get(topStructure.getInchiKey2D()).getId();
        final MutableMs2Experiment exp = instance.getExperiment().mutate();
        exp.setMolecularFormula(topFormula.getMolecularFormula());
        exp.setPrecursorIonType(topFormula.getIonType());

        final AdditionalFields additionalFields = exp.getAnnotation(AdditionalFields.class).orElseGet(AdditionalFields::new);
        additionalFields.put("Harvester$source", instance.getID().getDirectoryName());
        exp.setAnnotation(AdditionalFields.class, additionalFields);
        try (final BufferedWriter bw = FileUtils.getWriter(new File(toFile, prefix + "_" + form_index + ".ms" ))) {
            JenaMsWriter w = new JenaMsWriter();
            w.write(bw, exp);
        }
    }

    private boolean formulaIsConfidentlyAnnotated(TDoubleArrayList formulaScores, HashMap<String, FormulaResult> struct2formula, ArrayList<Scored<CompoundCandidate>> allStructures) {
        final CompoundCandidate topStructure = allStructures.stream().max(Comparator.naturalOrder()).map(SScored::getCandidate).orElse(null);
        if (topStructure==null) return false;
        final FormulaResultId topFormula = struct2formula.get(topStructure.getInchiKey2D()).getId();
        // if there are less than 3 structures and their score is bad, ignore
        long numberOfStructuresWithThisFormula = struct2formula.values().stream().filter(x->x.getId().equals(topFormula)).count();
        double zodiac = struct2formula.get(topStructure.getInchiKey2D()).getAnnotation(FormulaScoring.class).flatMap(x->x.getAnnotation(ZodiacScore.class)).map(Score.AbstDoubleScore::score).orElse(0d);
        double explainedIntensityByTree = struct2formula.get(topStructure.getInchiKey2D()).getAnnotation(FTree.class).get().getAnnotation(TreeStatistics.class).get().getExplainedIntensity();
        if (numberOfStructuresWithThisFormula <= 3) {
            // could be a wrong hit. We check if its tree has high quality
            if (formulaScores.size()>1 && formulaScores.get(0)-formulaScores.get(1) < 200) return false;
            return (zodiac >= 0.95 && explainedIntensityByTree >= 0.7);
        } else {
            // there are enough formulas such that we can trust this annotation as long as score difference is large
            if (formulaScores.size()<=1) return true;
            if (zodiac >= 0.95 && explainedIntensityByTree >= 0.7 && formulaScores.get(0)-formulaScores.get(1) > 100) return true;
            return (formulaScores.get(0)-formulaScores.get(1) > 300);
        }
    }

    private void filterStructures(Instance instance, ArrayList<Scored<CompoundCandidate>> allStructures, HashMap<String, FormulaResult> struct2formula, HashMap<String, EpimetheusResult> epimetheus, ArrayList<Scored<CompoundCandidate>> sublist, ArrayList<Scored<CompoundCandidate>> consideredCandidates) {
        int sublistMarker=5, sublistConsideredMarker=1;
        final double confidence = instance.getID().getConfidenceScore().orElse(0d);
        final double maxTanimoto = allStructures.stream().mapToDouble(x->x.getCandidate().getTanimoto()).max().orElse(0d);
        final double maxScore = allStructures.stream().mapToDouble(SScored::getScore).max().orElse(0d);
        final double tanimotoThreshold = Math.max(maxTanimoto, options.minTanimotoThreshold)-0.0666;
        final double scoreThreshold = maxScore - Math.max(100,maxScore)*0.33;
        int r=0;
        for (; r < allStructures.size(); ++r) {
            final Scored<CompoundCandidate> c = allStructures.get(r);
            if (c.getScore() < scoreThreshold || c.getCandidate().getTanimoto() < tanimotoThreshold) {
                break;
            }
        }
        sublistMarker = Math.max(r, sublistMarker);
        sublistConsideredMarker = Math.max(sublistConsideredMarker, r);
        final ArrayList<Scored<CompoundCandidate>> list = new ArrayList<>(allStructures.subList(0, r));
        final JobManager mg = SiriusJobs.getGlobalJobManager();
        // compute epimetheus for the remaining
        HashMap<String, BasicJJob<EpimetheusResult>> jobs = new HashMap<>();
        for (final Scored<CompoundCandidate> c : allStructures.subList(0,Math.min(allStructures.size(),sublistMarker))) {
            if (!epimetheus.containsKey(c.getCandidate().getInchiKey2D())) {
                jobs.put(c.getCandidate().getInchiKey2D(),mg.submitJob(new BasicJJob<EpimetheusResult>() {
                    @Override
                    protected EpimetheusResult compute() throws Exception {
                        final FTree tree = struct2formula.get(c.getCandidate().getInchiKey2D()).getAnnotation(FTree.class).get();
                        final String smiles = c.getCandidate().getSmiles();
                        return getEpimetheusFor(smiles, tree);
                    }
                }));
            }
        }
        jobs.forEach((key,value)->epimetheus.put(key,value.takeResult()));
        for (r=0; r < list.size(); ++r) {
            final Scored<CompoundCandidate> c = allStructures.get(r);
            final EpimetheusResult res = epimetheus.get(c.getCandidate().getInchiKey2D());
            if (res.explainedIntensity < options.minEpimetheusIntensityThreshold || !res.acceptForExplainedPeakThreshold(options.minEpimetheusPeakThreshold)) {
                break;
            }
        }
        sublistConsideredMarker = Math.min(sublistConsideredMarker, r);


        sublist.clear();
        sublist.addAll(allStructures.subList(0, Math.min(allStructures.size(),sublistMarker)));
        consideredCandidates.clear();
        consideredCandidates.addAll(allStructures.subList(0, sublistConsideredMarker));
    }

    private EpimetheusResult getEpimetheusFor(String smiles, FTree tree) throws InterruptedException {
        final MolecularGraph graph;
        try {
            graph = new MolecularGraph(
                    new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles)
            );
        } catch (InvalidSmilesException e) {
            throw new RuntimeException(e); // should not happen
        }
        final EMFragmenterScoring2 scoring = new EMFragmenterScoring2(graph, tree);
        final CriticalPathSubtreeCalculator calc = new CriticalPathSubtreeCalculator(tree, graph, scoring, true);
        calc.setMaxNumberOfNodes(50000);
        final HashSet<MolecularFormula> fset = new HashSet<>();
        for (Fragment ft : tree.getFragmentsWithoutRoot()) {
            fset.add(ft.getFormula());
            fset.add(ft.getFormula().add(MolecularFormula.getHydrogen()));
            fset.add(ft.getFormula().add(MolecularFormula.getHydrogen().multiply(2)));
            if (ft.getFormula().numberOfHydrogens()>0) fset.add(ft.getFormula().subtract(MolecularFormula.getHydrogen()));
            if (ft.getFormula().numberOfHydrogens()>1) fset.add(ft.getFormula().subtract(MolecularFormula.getHydrogen().multiply(2)));
        }
        try {
            calc.initialize((node, nnodes, nedges) -> {
                try {
                    checkForInterruption();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (fset.contains(node.getFragment().getFormula())) return true;
                return (node.getTotalScore() > -5f);
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof InterruptedException) {
                throw (InterruptedException)e.getCause();
            } else throw e;
        }
        final CombinatorialSubtree subtree = calc.computeSubtree();

        final HashMap<Fragment, CombinatorialNode> mapping = new HashMap<>();
        final HashMap<MolecularFormula, Fragment> formMap = new HashMap<>();
        final FragmentAnnotation<AnnotatedPeak> peakAno = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        for (Fragment f : tree.getFragmentsWithoutRoot()) {
            if (peakAno.get(f).isMeasured()) {
                mapping.put(f, null);
                formMap.compute(f.getFormula(), (key, before) -> before == null ? f : (peakAno.get(before).getRelativeIntensity() > peakAno.get(f).getRelativeIntensity() ? before : f));
            }
        }
        for (CombinatorialNode node :subtree) {
            if (!node.getFragment().isInnerNode()) {
                final MolecularFormula formula = node.getFragment().getFormula();
                if (formMap.containsKey(formula)) {
                    mapping.put(formMap.get(formula), node);
                }
            }
        }
        double explainedIntensity = 0d, totalIntensity = 0d;
        int totalPeaks = 0, explainedPeaks = 0;
        for (Map.Entry<Fragment, CombinatorialNode> entry : mapping.entrySet()) {
            ++totalPeaks;
            final double intens = peakAno.get(entry.getKey()).getRelativeIntensity();
            totalIntensity += intens;
            if (entry.getValue()!=null) {
                explainedPeaks++;
                explainedIntensity += intens;
            }
        }

        return new EpimetheusResult(
                explainedIntensity / totalIntensity,
                subtree.getScore(),
                subtree.getScore() / totalPeaks,
                explainedPeaks,
                totalPeaks
        );

    }

    // 4 von 6 müssen zutreffen:
    // confidence >= 0.9
    // confidence >= 0.7
    // Tanimoto >= 0.7
    // Epi1, Epi2
    // Prio-DB
    protected boolean isHighlyConfidentlyAnnotated(Instance instance, ArrayList<Scored<CompoundCandidate>> filteredStructures, HashMap<String, EpimetheusResult> epimetheus) {
        int x = 0;
        final double confidence = instance.getID().getConfidenceScore().orElse(0d);
        if (confidence >= 0.7) ++x;
        if (confidence >= 0.8) ++x;
        if (confidence >= 0.9) ++x;
        final double tanimoto = filteredStructures.get(0).getCandidate().getTanimoto();
        if (tanimoto >= 0.7) ++x;
        if (filteredStructures.size()>1 && confidence < 0.6) return false;
        EpimetheusResult epi = epimetheus.get(filteredStructures.get(0).getCandidate().getInchiKey2D());
        if (epi.acceptForExplainedPeakThreshold(options.highEpimetheusPeakThreshold))
            ++x;
        if (!epi.acceptForExplainedPeakThreshold(options.minEpimetheusPeakThreshold)) return false;
        // none of the top candidates should be in a prio database if top hit is not in a prio database
        if (!hasPrio(filteredStructures.get(0).getCandidate())) {
            boolean found=false;
            for (int k = 1; k < filteredStructures.size(); ++k) {
                if (hasPrio(filteredStructures.get(k).getCandidate())) found=true;
            }
            if (!found) ++x;
        } else ++x;
        // none of the top candidates should have a much higher epimetheus score than the top one
        double epimetheusThreshold = epi.fragmenterScore*1.20;
        boolean epithreshold = false;
        for (int k=1; k< filteredStructures.size(); ++k) {
            if (epimetheus.get(filteredStructures.get(k).getCandidate().getInchiKey2D()).fragmenterScore > epimetheusThreshold) {
                epithreshold = true;
                break;
            };
        }
        if (!epithreshold) ++x;
        return x >= 4;
    }

    boolean hasPrio(CompoundCandidate c) {
        if ((c.getBitset() & options.priorityDbFlag) != 0)
            return true;
        return c.getLinks().stream().anyMatch(x->options.priorityDbPattern.matcher(x.name).find());
    }

    @Override
    public void cancel() {
        cancel(true);
    }

    protected static class EpimetheusResult {
        double explainedIntensity, fragmenterScore, averageScore;
        int explainedPeaks, totalPeaks;

        public EpimetheusResult(double explainedIntensity, double fragmenterScore, double averageScore, int explainedPeaks, int totalPeaks) {
            this.explainedIntensity = explainedIntensity;
            this.fragmenterScore = fragmenterScore;
            this.averageScore = averageScore;
            this.explainedPeaks = explainedPeaks;
            this.totalPeaks = totalPeaks;
        }

        public boolean acceptForExplainedPeakThreshold(double threshold) {
            return explainedPeaks >= Math.floor(totalPeaks*threshold);
        }
    }

    protected class ConfidentAnnotation {
        private double zodiac, explainedIntensityByTree, explainedIntensityByEpimetheus, epimetheusScoreRelative, epimetheusExplainedPeaks, epimetheusScorePerPeak, csiScore, confidence;
        private double topTanimoto;
        private double weight;
        private double hightestAlternativeTanimoto;
        private int numberOfAlternativeCandidates;
        private int numberOfDatabases, numberOfPriorityDatabases;

        private ClassyfireProperty canopus1, canopus2;

        private Ms2Experiment experiment;
        private MolecularFormula formula;
        private PrecursorIonType ionType;
        private short[] maskedPositions;
        private ArrayFingerprint fingerprint;
        private List<String> consideredSMILES, consideredKeys, consideredInchis;
        private List<String> dbNames;
        private boolean novel;
        private CompoundContainerId id;



        public ConfidentAnnotation(Instance instance, ArrayList<Scored<CompoundCandidate>> filteredList, ArrayList<Scored<CompoundCandidate>> consideredCandidates, HashMap<String, FormulaResult> struct2formula, HashMap<String, EpimetheusResult> epimetheus, HashMap<String, ArrayFingerprint> allFingerprints, HashMap<String, ArrayFingerprint> classif) {
            final String topkey = filteredList.get(0).getCandidate().getInchiKey2D();
            this.id = instance.getID();
            this.formula = struct2formula.get(topkey).getId().getMolecularFormula();
            this.ionType = struct2formula.get(topkey).getId().getIonType();
            this.zodiac = struct2formula.get(topkey).getAnnotation(FormulaScoring.class).flatMap(x->x.getAnnotation(ZodiacScore.class)).map(Score.AbstDoubleScore::score).orElse(0d);
            this.confidence = instance.getID().getConfidenceScore().orElse(0d);
            this.explainedIntensityByTree = struct2formula.get(topkey).getAnnotation(FTree.class).get().getAnnotation(TreeStatistics.class).get().getExplainedIntensity();
            final EpimetheusResult epimetheusResult = epimetheus.get(topkey);
            this.explainedIntensityByEpimetheus = epimetheusResult.explainedIntensity;
            this.topTanimoto = filteredList.get(0).getCandidate().getTanimoto();
            this.hightestAlternativeTanimoto = filteredList.subList(1,filteredList.size()).stream().mapToDouble(x->x.getCandidate().getTanimoto()).max().orElse(0d);
            this.numberOfAlternativeCandidates = filteredList.size()-1;
            this.numberOfDatabases = filteredList.get(0).getCandidate().getLinkedDatabases().keySet().size();
            this.dbNames = new ArrayList<>(filteredList.get(0).getCandidate().getLinkedDatabases().keySet());
            this.numberOfPriorityDatabases = (int)filteredList.get(0).getCandidate().getLinkedDatabases().keySet().stream().filter(x->options.priorityDbPattern.matcher(x).find()).count();
            this.epimetheusScoreRelative = epimetheusResult.fragmenterScore / epimetheus.values().stream().mapToDouble(x->x.fragmenterScore).max().orElse(1d);
            this.epimetheusScorePerPeak = epimetheusResult.averageScore;
            this.epimetheusExplainedPeaks = epimetheusResult.explainedPeaks/((double) epimetheusResult.totalPeaks);
            this.csiScore = filteredList.get(0).getScore();
            this.experiment = instance.getExperiment();
            final boolean confident = consideredCandidates.size()==1;
            this.consideredSMILES = consideredCandidates.stream().map(x->x.getCandidate().getSmiles()).collect(Collectors.toList());
            this.consideredKeys = consideredCandidates.stream().map(x->x.getCandidate().getInchiKey2D()).collect(Collectors.toList());
            this.consideredInchis = consideredCandidates.stream().map(x->x.getCandidate().getInchi().in2D).collect(Collectors.toList());
            this.novel = !consideredCandidates.get(0).getCandidate().getLinkedDatabases().containsKey(DataSource.TRAIN.realName);

            if (classif.get(topkey)!=null) {
                for (FPIter x : classif.get(topkey).presentFingerprints()) {
                    if (canopus1==null || canopus1.getFixedPriority() < ((ClassyfireProperty)x.getMolecularProperty()).getFixedPriority()) {
                        canopus1 = (ClassyfireProperty) x.getMolecularProperty();
                    }
                    if (canopus2==null || canopus2.getAltPriority() < ((ClassyfireProperty)x.getMolecularProperty()).getAltPriority()) {
                        canopus2 = (ClassyfireProperty) x.getMolecularProperty();
                    }
                }
            }

            this.fingerprint = allFingerprints.get(topkey);
            if (!confident) {
                // find a subset of bits that is identical in all fingerprints
                TShortHashSet forbiddenIndizes = new TShortHashSet();
                for (int k = 1; k < consideredCandidates.size(); ++k) {
                    final ArrayFingerprint fp = allFingerprints.get(consideredCandidates.get(k).getCandidate().getInchiKey2D());
                    for (FPIter x : fp.presentFingerprints()) {
                        if (!fingerprint.isSet(x.getIndex())) {
                            forbiddenIndizes.add((short) x.getIndex());
                        }
                    }
                    for (FPIter x : this.fingerprint.presentFingerprints()) {
                        if (!fp.isSet(x.getIndex())) {
                            forbiddenIndizes.add((short) x.getIndex());
                        }
                    }
                }
                this.maskedPositions = forbiddenIndizes.toArray();
                Arrays.sort(maskedPositions);
            } else {
                this.maskedPositions = new short[0];
            }


                double weight = (instance.getID().getConfidenceScore().orElse(1d));
                weight += (numberOfDatabases*0.01);
                weight += (numberOfPriorityDatabases*0.1);
                if (topTanimoto>hightestAlternativeTanimoto) weight += 0.1;
                if (epimetheusScoreRelative>=1) weight += 0.1;
                if (instance.loadTopFormulaResult().get().getId().equals(struct2formula.get(topkey).getId())) {
                    weight += 0.2;
                }
                weight *= 1d/(1d-Math.log10(struct2formula.get(topkey).getAnnotation(ZodiacScore.class).map(Score.AbstDoubleScore::score).orElse(1d)));
                weight *= topTanimoto;
                weight *= Math.pow(topTanimoto/Math.max(topTanimoto,hightestAlternativeTanimoto), 3);
                weight *= Math.pow(epimetheusScoreRelative,3);
                weight *= explainedIntensityByEpimetheus;
                weight *= explainedIntensityByTree;
                this.weight = Math.min(1d,weight);

        }

        public void exportFile(File targetDirectory, String prefix, int index) throws IOException {
            final MutableMs2Experiment exp = experiment.mutate();
            exp.setMolecularFormula(formula);
            exp.setPrecursorIonType(ionType);
            if (consideredKeys.size()==1) {
                exp.setAnnotation(Smiles.class, new Smiles(consideredSMILES.get(0)));
                exp.setAnnotation(InChI.class, new InChI(consideredKeys.get(0), consideredInchis.get(0)));
            }
            final AdditionalFields additionalFields = exp.getAnnotation(AdditionalFields.class).orElseGet(AdditionalFields::new);
            additionalFields.put("Harvester$weight", String.valueOf(weight));
            additionalFields.put("Harvester$novel", String.valueOf(novel ? 1 : 0));
            additionalFields.put("Harvester$fingerprint", Arrays.toString(fingerprint.toIndizesArray()));
            additionalFields.put("Harvester$maskedPositions", Arrays.toString(maskedPositions));
            additionalFields.put("Harvester$keys", String.join(";", consideredKeys));
            additionalFields.put("Harvester$smiles", String.join(";", consideredSMILES));
            additionalFields.put("Harvester$source", this.id.getDirectoryName());
            exp.setAnnotation(AdditionalFields.class, additionalFields);
            try (final BufferedWriter bw = FileUtils.getWriter(new File(targetDirectory, prefix + "_" + index + ".ms" ))) {
                JenaMsWriter w = new JenaMsWriter();
                w.write(bw, exp);
            }
        }

        public String toCsvRow(int index) {
            return index + "\t" + id.getCompoundName() + "\t" + id.getDirectoryName() + "\t" + formula + "\t" + ionType + "\t"
                    + confidence + "\t" + weight + "\t" + (novel ? '1' : '0') + "\t"  + csiScore + "\t" + consideredKeys.size() + "\t" + numberOfPriorityDatabases + "\t" + numberOfDatabases + "\t" + dbNames + "\t" +
                    topTanimoto + "\t" + hightestAlternativeTanimoto + "\t" + zodiac + "\t" + explainedIntensityByTree + "\t"
                    + explainedIntensityByEpimetheus + "\t" + epimetheusExplainedPeaks + "\t" + epimetheusScoreRelative + "\t"
                    + epimetheusScorePerPeak + "\t" + (1d - (maskedPositions.length/((double)fingerprint.getFingerprintVersion().size()))) + "\t" + String.join(";", consideredKeys) + "\t" + String.join(";", consideredSMILES + "\t" + (canopus1==null ? "" : canopus1.getName()) + "\t" + (canopus2==null ? "" : canopus2.getName()));
        }
    }
    protected static String CSV_HEADER = "index\tid\tdirectory\tformula\tion\tconfidence\tweight\tnovel\tcsiScore\tconsideredCandidates\tprioDbs\tdbs\tdbNames\ttanimotoScore\talternativeTanimoto\tzodiacScore\ttreeIntensityExplained\tstructureIntensityExplained\tstructurePeaksExplained\tepimetheusScoreRelative\tepimetheusscoreAverage\tfingerprintUnmasked\tkeys\tsmiles\tcanopus1\tcanopus2";

}