package de.unijena.bioinf.sirius.cli;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.GibbsSampling.GibbsSamplerMain;
import de.unijena.bioinf.GibbsSampling.LibraryHitQuality;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.GibbsSampling.model.distributions.*;
import de.unijena.bioinf.GibbsSampling.model.scorer.CommonFragmentAndLossScorer;
import de.unijena.bioinf.GibbsSampling.model.scorer.EdgeScorings;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.projectspace.DirectoryReader;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import de.unijena.bioinf.sirius.projectspace.SiriusFileReader;
import de.unijena.bioinf.sirius.projectspace.SiriusWorkspaceReader;
import oshi.SystemInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/**
 * Created by ge28quv on 18/05/17.
 */
public class Zodiac {

    private Path outputPath;
    private Path libraryHitsFile;
    private Path workSpacePath;
    private ZodiacOptions options;
    final int maxCandidates = 10;

//
//    public Zodiac(String workSpacePath, String libraryHitsFile, String outputPath, ){
//        this.workSpacePath = Paths.get(workSpacePath);
//        this.libraryHitsFile = Paths.get(libraryHitsFile);
//        this.outputPath = Paths.get(outputPath);
//    }


    public Zodiac(ZodiacOptions options){
        this.workSpacePath = Paths.get(options.getInput());
        this.libraryHitsFile = Paths.get(options.getLibraryHitsFile());
        this.outputPath = Paths.get(options.getOutputPath());
        this.options = options;
    }

    public void run(){
        //todo test path..


        try {


            //todo or rather use option
            int workerCount = options.processors()>0 ? options.processors() : (new SystemInfo()).getHardware().getProcessor().getPhysicalProcessorCount()-1;




//        //reactions
//        int stepsize = 2; //low for testing purposes
//        Reaction[] reactions = parseReactions(stepsize);
            //all possible netto changes of MolecularFormulas using on of the reactions.

            Reaction[] reactions = GibbsSamplerMain.parseReactions(1);
            Set<MolecularFormula> netSingleReactionDiffs = new HashSet<>();
            for (Reaction reaction : reactions) {
                netSingleReactionDiffs.add(reaction.netChange());
            }


            System.out.println("Read sirius input");
            List<ExperimentResult> input = newLoad(workSpacePath.toFile());

//            //// TODO: 24/05/17
//            Map<String, List<FragmentsCandidate>> candidatesMap = GibbsSamplerMain.parseMFCandidates(workSpacePath, Paths.get(options.getSpectraFile()), maxCandidates, workerCount);

            Map<String, List<FragmentsCandidate>> candidatesMap = parseMFCandidates(input, maxCandidates);
            if (libraryHitsFile!=null) GibbsSamplerMain.parseLibraryHits(libraryHitsFile, Paths.get(options.getSpectraFile()),  candidatesMap);
            setKnownCompounds(candidatesMap, netSingleReactionDiffs);



            String[] ids = getIdsOfKnownEmptyCompounds(candidatesMap);
            FragmentsCandidate[][] candidatesArray = new FragmentsCandidate[ids.length][];

            for (int i = 0; i < ids.length; i++) {
                String id = ids[i];
                candidatesArray[i] = candidatesMap.get(id).toArray(new FragmentsCandidate[0]);
            }


            NodeScorer[] nodeScorers;
            //todo useful score
            boolean useLibraryHits = (libraryHitsFile!=null);
            double libraryScore = 1d;
            if (useLibraryHits){

                //todo fix LibraryHitScorer!!!!!!!!!!!
                nodeScorers = new NodeScorer[]{new RankNodeScorer(), new LibraryHitScorer(libraryScore, 0.5, netSingleReactionDiffs)};
                System.out.println("use LibraryHitScorer");
            } else {
                nodeScorers = new NodeScorer[]{new RankNodeScorer()};
                System.out.println("ignore Library Hits");
            }



            EdgeFilter edgeFilter = null;

            if(options.getThresholdFilter() > 0.0D && options.getLocalFilter() > 0.0D) {
                edgeFilter = new EdgeThresholdMinConnectionsFilter(options.getThresholdFilter(), options.getLocalFilter(), options.getMinLocalConnections());
            } else if(options.getThresholdFilter() > 0.0D) {
                edgeFilter = new EdgeThresholdFilter(options.getThresholdFilter());
            } else if(options.getLocalFilter() > 0.0D) {
                edgeFilter = new LocalEdgeFilter(options.getLocalFilter());
            }
            if(edgeFilter == null) {
                edgeFilter = new EdgeThresholdFilter(0);
            }
            



            boolean estimateByMedian = true;
            ScoreProbabilityDistribution probabilityDistribution = null;
            if(options.getProbabilityDistribution().equals(EdgeScorings.exponential)) {
                probabilityDistribution = new ExponentialDistribution(0.0D, options.getThresholdFilter(), estimateByMedian);
            } else if (options.getProbabilityDistribution().equals(EdgeScorings.lognormal)) {
                probabilityDistribution = new LogNormalDistribution(options.getThresholdFilter(), estimateByMedian);
            }


            //todo changed !!!?!?!??!?!?!
            double minimumOverlap = 0.1D;
            ScoreProbabilityDistributionEstimator commonFragmentAndLossScorer = new ScoreProbabilityDistributionEstimator(new CommonFragmentAndLossScorer(minimumOverlap), probabilityDistribution);
            EdgeScorer[] edgeScorers = new EdgeScorer[]{commonFragmentAndLossScorer};


            GibbsParallel<FragmentsCandidate> gibbsParallel = new GibbsParallel(ids, candidatesArray, nodeScorers, edgeScorers, edgeFilter, workerCount, options.getSeparateRuns());


            System.out.println("start gibbs sampling");
            gibbsParallel.iteration(options.getIterationSteps(), options.getBurnInSteps());


            Scored<FragmentsCandidate>[][] bestInitial = getBestInitialAssignments(ids, candidatesMap);

            Scored<FragmentsCandidate>[][] result = gibbsParallel.getChosenFormulasBySampling();
            //todo this or rather Scored<FragmentsCandidate>[][] result = gibbsParallel.getChosenFormulasBySampling();

            writeOutput(ids, bestInitial, result, outputPath);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    protected static List<ExperimentResult> newLoad(File file) throws IOException {
        final List<ExperimentResult> results = new ArrayList<>();
        final DirectoryReader.ReadingEnvironment env;
        if (file.isDirectory()) {
            env = new SiriusFileReader(file);
        } else {
            env = new SiriusWorkspaceReader(file);
        }
        final DirectoryReader reader = new DirectoryReader(env);

        while (reader.hasNext()) {
            final ExperimentResult result = reader.next();
            results.add(result);
        }
        return results;
    }


    private final static String SEP = "\t";
    private final static int NUMBER_OF_HITS = 5;
    private static void writeOutput(String[] ids, Scored<FragmentsCandidate>[][] initial, Scored<FragmentsCandidate>[][] result, Path outputPath) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(outputPath, Charset.defaultCharset());
        writer.write("id"+SEP+"SiriusMF"+SEP+"SiriusScore"+SEP+"ZodiacMF"+SEP+"ZodiacScore");
        for (int i = 0; i < ids.length; i++) {
            final StringBuffer buffer = new StringBuffer();

            final String id = ids[i];
            final String siriusMF = initial[i][0].getCandidate().getFormula().formatByHill();
            final double siriusScore = initial[i][0].getCandidate().getScore();

            buffer.append(id); buffer.append(SEP);
            buffer.append(siriusMF); buffer.append(SEP);
            buffer.append(Double.toString(siriusScore));


            final Scored<FragmentsCandidate>[] currentResults = result[i];
            for (int j = 0; j < Math.min(currentResults.length, NUMBER_OF_HITS); j++) {
                Scored<FragmentsCandidate> currentResult = currentResults[j];
                final String mf = currentResult.getCandidate().getFormula().formatByHill();
                final double score = currentResult.getScore();

                buffer.append(SEP);
                buffer.append(mf);
                buffer.append(SEP);
                buffer.append(Double.toString(score));
            }

            writer.write("\n");
            writer.write(buffer.toString());

        }

        writer.close();

    }

//    private <C extends Candidate & HasLibraryHit> void parseLibraryHits(Path libraryHitsPath, Path mgfFile, Map<String, List<C>> candidatesMap) throws IOException {
//        final MsExperimentParser parser = new MsExperimentParser();
//        List<Ms2Experiment> allExperiments = parser.getParser(mgfFile.toFile()).parseFromFile(mgfFile.toFile());
//
//        //assumption "#Scan#" is the number of the ms2 scan starting with 1
//        String[] featureIDs = new String[allExperiments.size()];
//        int j = 0;
//        for (Ms2Experiment experiment : allExperiments) {
//            featureIDs[j++] = experiment.getName();
//        }
//
//
//        List<String> lines = Files.readAllLines(libraryHitsPath, Charset.defaultCharset());
//        String[] header = lines.remove(0).split("\t");
////        String[] ofInterest = new String[]{"Feature_id", "Formula", "Structure", "Adduct", "Cosine", "SharedPeaks", "Quality"};
//        String[] ofInterest = new String[]{"#Scan#", "INCHI", "Smiles", "Adduct", "MQScore", "SharedPeaks", "Quality"};
//        int[] indices = new int[ofInterest.length];
//        for (int i = 0; i < ofInterest.length; i++) {
//            int idx = arrayFind(header, ofInterest[i]);
//            if (idx<0) throw new RuntimeException("Cannot parse spectral library hits file. Column "+ofInterest[i]+" not found.");
//            indices[i] = idx;
//        }
//        for (String line : lines) {
//            String[] cols = line.split("\t");
//            final int scanNumber = Integer.parseInt(cols[indices[0]]);
//            final String featureId = featureIDs[scanNumber-1]; //starting with 1!
//
//            List<C> candidatesList = candidatesMap.get(featureId);
//            if (candidatesList == null) {
//                System.err.println("corresponding query (FEATURE_ID: " +featureId+ ", #SCAN# "+scanNumber+") to library hit not found");
//                continue;
//            }
//
//            final Ms2Experiment experiment = candidatesList.get(0).getExperiment();
//            final MolecularFormula formula = getFormulaFromStructure(cols[indices[1]], cols[indices[2]]);
//
//            if (formula==null){
//                System.err.println("cannot compute molecular formula of library hit #SCAN# "+scanNumber);
//                continue;
//            }
//
//            final String structure = cols[indices[2]]; ...test
//            final PrecursorIonType ionType = PeriodicTable.getInstance().ionByName(cols[indices[3]]);
//            final double cosine = Double.parseDouble(cols[indices[4]]);
//            final int sharedPeaks = Integer.parseInt(cols[indices[5]]);
//            final LibraryHitQuality quality = LibraryHitQuality.valueOf(cols[indices[6]]);
//            LibraryHit libraryHit = new LibraryHit(experiment, formula, structure, ionType, cosine, sharedPeaks, quality);
//            for (C candidate : candidatesList) {
//                candidate.setLibraryHit(libraryHit);
//            }
//        }
//    }
//
//
//    private MolecularFormula getFormulaFromStructure(String inchi, String smiles){
//
//        MolecularFormula formula = null;
//        if (inchi!=null && isInchi(inchi)){
//            formula = new InChI(null, inchi).extractFormula();
//        }
//
//        if (formula==null){
//            try {
//                final SmilesParser parser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
//                final IAtomContainer c = parser.parseSmiles(smiles);
//                experiment.setAnnotation(InChI.class, mol2inchi(c));
//                return;
//            } catch (CDKException e) {
//                e.printStackTrace();
//            }
//        }
//
//    }
//
//    private boolean isInchi(String inchi) {
//        if (!inchi.toLowerCase().startsWith("inchi=")) return false;
//        int idx1 = inchi.indexOf("/");
//        int idx2 = inchi.indexOf("/", idx1);
//        if (idx1>0 && idx2>0 && (idx2-idx1)>1) return true;
//        return false;
//    }

//    private <C extends Candidate & HasLibraryHit> void parseLibraryHits(Path libraryHitsPath, Map<String, List<C>> candidatesMap) throws IOException {
//        List<String> lines = Files.readAllLines(libraryHitsPath, Charset.defaultCharset());
//        String[] header = lines.remove(0).split("\t");
//        String[] ofInterest = new String[]{"Feature_id", "Formula", "Structure", "Adduct", "Cosine", "SharedPeaks", "Quality"};
//        int[] indices = new int[ofInterest.length];
//        for (int i = 0; i < ofInterest.length; i++) {
//            int idx = arrayFind(header, ofInterest[i]);
//            if (idx<0) throw new RuntimeException("Column "+ofInterest[i]+" not found");
//            indices[i] = idx;
//        }
//        for (String line : lines) {
//            String[] cols = line.split("\t");
//            final String featureId = cols[indices[0]];
//
//            List<C> candidatesList = candidatesMap.get(featureId);
//            if (candidatesList == null) {
//                System.out.println("corresponding query (" +featureId+ ") to library hit not found");
//                System.err.println("corresponding query (" +featureId+ ") to library hit not found");
////                System.err.println("all candidates have been removed: "+id);
//                continue;
//            }
//
//            final Ms2Experiment experiment = candidatesList.get(0).getExperiment();
//            final MolecularFormula formula = MolecularFormula.parse(cols[indices[1]]);
//            final String structure = cols[indices[2]];
//            final PrecursorIonType ionType = PeriodicTable.getInstance().ionByName(cols[indices[3]]);
//            final double cosine = Double.parseDouble(cols[indices[4]]);
//            final int sharedPeaks = Integer.parseInt(cols[indices[5]]);
//            final LibraryHitQuality quality = LibraryHitQuality.valueOf(cols[indices[6]]);
//            LibraryHit libraryHit = new LibraryHit(experiment, formula, structure, ionType, cosine, sharedPeaks, quality);
//            for (C candidate : candidatesList) {
//                candidate.setLibraryHit(libraryHit);
//            }
//        }
//    }


    public static Map<String, List<FragmentsCandidate>> parseMFCandidates(List<ExperimentResult> experimentResults, int maxCandidates) throws IOException {
        final Map<String, List<FragmentsCandidate>> listMap = new HashMap<>();
        for (ExperimentResult experimentResult : experimentResults) {
            Ms2Experiment experiment = experimentResult.getExperiment();
            List<IdentificationResult> identificationResults = experimentResult.getResults();
            //todo beautify trees!!!!
            List<FTree> trees = new ArrayList<>();
            for (IdentificationResult identificationResult : identificationResults) {
                trees.add(identificationResult.getResolvedTree());
            }
            List<FragmentsCandidate> candidates = FragmentsCandidate.createAllCandidateInstances(trees, experiment);
            Collections.sort(candidates);
            if (candidates.size()>maxCandidates) candidates = candidates.subList(0, maxCandidates);

            if (candidates.size()>0) listMap.put(experiment.getName(), candidates);


        }


        return listMap;
    }

//    private Map<String, LibraryHit> identifyCorrectLibraryHits(Map<String, List<FragmentsCandidate>> candidatesMap, Set<MolecularFormula> allowedDifferences) {
//        Map<String, LibraryHit> correctHits = new HashMap<>();
//        Deviation deviation = new Deviation(20);
//        for (String id : candidatesMap.keySet()) {
//            final List<FragmentsCandidate> candidateList = candidatesMap.get(id);
//            if (!candidateList.get(0).hasLibraryHit()) continue;
//
//            final LibraryHit libraryHit = candidateList.get(0).getLibraryHit();
//
//            System.out.println("using lower threshold for references: Bronze, cos 0.66, shared 5 peaks");
//            if (libraryHit.getCosine()<0.66 || libraryHit.getSharedPeaks()<5) continue;
//
//            final double theoreticalMass = libraryHit.getIonType().neutralMassToPrecursorMass(libraryHit.getMolecularFormula().getMass());
//            final double measuredMass = libraryHit.getQueryExperiment().getIonMass();
//
//            //todo compare without aduct!?
//            PrecursorIonType hitIonization = libraryHit.getIonType().withoutAdduct().withoutInsource();
//
//            boolean sameIonization = false;
//            for (FragmentsCandidate candidate : candidateList) {
//                if (candidate.getIonType().equals(hitIonization)){
//                    sameIonization = true;
//                    break;
//                }
//            }
//
//            //same mass?
//            boolean matches = deviation.inErrorWindow(theoreticalMass, measuredMass);
//            //changed --> disadvantage: just 'correct' if Sirius has found it as well
//            if (!matches){
//                //any know MF diff?
//                for (FragmentsCandidate candidate : candidateList) {
//                    if (!candidate.getIonType().equals(libraryHit.getIonType())) continue;
//                    MolecularFormula mFDiff = libraryHit.getMolecularFormula().subtract(candidate.getFormula());
//                    if (mFDiff.getMass()<0) mFDiff = mFDiff.negate();
//                    if (allowedDifferences.contains(mFDiff)){
//                        matches = true;
//                        break;
//                    }
//                }
//            }
//
//
//            if (matches){
//                if (sameIonization){
//                    correctHits.put(id, libraryHit);
//                } else {
//                    System.out.println("warning: different ionizations for library hit "+id);
//                }
//            }
//        }
//
//        return correctHits;
//    }


    private void setKnownCompounds(Map<String, List<FragmentsCandidate>> candidatesMap, Set<MolecularFormula> allowedDifferences) {
        Set<String> ids = candidatesMap.keySet();
        for (String id : ids) {
            final List<FragmentsCandidate> candidateList = candidatesMap.get(id);
            if (!candidateList.get(0).hasLibraryHit()) continue;

            final LibraryHit libraryHit = candidateList.get(0).getLibraryHit();

            //todo at least 5 peaks match, no cosine threshold?
            if (libraryHit.getSharedPeaks()<5) continue;

            MolecularFormula correctMF = libraryHit.getMolecularFormula();
            List<FragmentsCandidate> candidates = candidatesMap.get(id);

            //todo does the ionization of library hit and compound have to match!?
            for (FragmentsCandidate candidate : candidates) {
                boolean matches = candidate.getFormula().equals(correctMF);
                if (!matches){
                    MolecularFormula diff = candidate.getFormula().subtract(correctMF);
                    if (diff.getMass()<0) diff = diff.negate();
                    matches = allowedDifferences.contains(diff);
                }
                if (matches){
                    candidate.setCorrect(true);
                }
                candidate.setInTrainingSet(true);


            }
        }
    }

    private <C> String[] getIdsOfKnownEmptyCompounds(Map<String, List<C>> candidatesMap){
        List<String> ids = new ArrayList<>();
        for (String key : candidatesMap.keySet()) {
            if (candidatesMap.get(key).size()>0) ids.add(key);
        }
        return ids.toArray(new String[0]);
    }



    private <T> boolean arrayContains(T[] array, T object) {
        return this.arrayFind(array, object) >= 0;
    }

    private <T> int arrayFind(T[] array, T object) {
        for(int i = 0; i < array.length; ++i) {
            Object t = array[i];
            if(t.equals(object)) {
                return i;
            }
        }

        return -1;
    }


    /**
     *
     * @param ids
     * @param explanationsMap
     * @return best initial formulas with RELATIVE SCORE!!
     */
    private static Scored<FragmentsCandidate>[][] getBestInitialAssignments(String[] ids, Map<String, List<FragmentsCandidate>> explanationsMap){
        Scored<FragmentsCandidate>[][] array = new Scored[ids.length][];
        for (int i = 0; i < ids.length; i++) {
            List<FragmentsCandidate> smfList = explanationsMap.get(ids[i]);
            Scored<FragmentsCandidate>[] smfarray = new Scored[smfList.size()];
            double sum = 0d;
            for (FragmentsCandidate candidateScored : smfList) {
                sum += candidateScored.getScore();
            }
            for (int j = 0; j < smfarray.length; j++) {
                smfarray[j] = new Scored<FragmentsCandidate>(smfList.get(j), smfList.get(j).getScore()/sum);
            }
            Arrays.sort(smfarray);
            array[i] = smfarray;
        }
        return array;
    }

}
