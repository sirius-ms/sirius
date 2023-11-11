package de.unijena.bioinf.evaluation;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.babelms.MsIO;
import de.unijena.bioinf.cmlFragmentation.*;
import de.unijena.bioinf.cmlSpectrumPrediction.BarcodeSpectrumPredictor;
import de.unijena.bioinf.cmlSpectrumPrediction.ICEBERGSpectrumPredictor;
import de.unijena.bioinf.fragmenter.CombinatorialFragmenterScoring;
import de.unijena.bioinf.fragmenter.DirectedBondTypeScoring;
import de.unijena.bioinf.fragmenter.EMFragmenterScoring2;
import de.unijena.bioinf.fragmenter.MolecularGraph;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bionf.spectral_alignment.RecallSpectralAlignment;
import de.unijena.bionf.spectral_alignment.WeightedRecallSpectralAlignment;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public class SpecPredictionEvaluation {

    private final File msrdSpectraDir;
    private final File outputFile;
    private final Deviation deviation;
    private final int NUM_FRAGMENTS;
    private final int NUM_H_SHIFTS;
    private final FragmentationRules fragRule;

    public SpecPredictionEvaluation(File msrdSpectraDir, File outputFile, File myScoringFile, Deviation deviation, FragmentationRules fragRule, int numFragments, int numHydrogenShifts) throws IOException {
        this.msrdSpectraDir = msrdSpectraDir;
        this.outputFile = outputFile;
        this.deviation = deviation;
        this.fragRule = fragRule;
        this.NUM_FRAGMENTS = numFragments;
        this.NUM_H_SHIFTS = numHydrogenShifts;
        DirectedBondTypeScoring.loadScoringFromFile(myScoringFile);
    }

    private SimpleSpectrum parseMsrdSpectrum(ProcessedInput processedInput){
        List<ProcessedPeak> mergedPeaks = processedInput.getMergedPeaks();
        SimpleMutableSpectrum spec = new SimpleMutableSpectrum(mergedPeaks.size());
        for(Peak peak : mergedPeaks) spec.addPeak(peak);
        Spectrums.sortSpectrumByMass(spec);
        spec.removePeakAt(spec.size()-1);
        return new SimpleSpectrum(spec);
    }

    private CombinatorialFragmenterScoring getScoring(String scoringMethod, MolecularGraph molecule){
        return switch (scoringMethod) {
            case "KaiScoring" -> new EMFragmenterScoring2(molecule, null);
            case "MyScoring" -> new DirectedBondTypeScoring(molecule);
            case "FragStepKaiScoring" -> new FragStepDependentScoring(new EMFragmenterScoring2(molecule, null));
            case "FragStepMyScoring" -> new FragStepDependentScoring(new DirectedBondTypeScoring(molecule));
            default -> null;
        };
    }

    private FragmentationPredictor getFragPredictor(String fragMethod, MolecularGraph molecule, CombinatorialFragmenterScoring scoring, PrecursorIonType ionization){
        return switch(fragMethod) {
            case "Iterative" -> new PrioritizedIterativeFragmentationPredictor(molecule, scoring, this.NUM_FRAGMENTS);
            case "RuleBased" -> new RuleBasedFragmentationPredictor(molecule, scoring, this.fragRule, (node, nnodes, nedges) -> true);
            default -> null;
        };
    }

    public void evaluate() {
        System.out.println("Initialise...");
        String[] fragMethods = new String[]{"Iterative", "RuleBased"};
        String[] scoringMethods = new String[]{"KaiScoring", "MyScoring", "FragStepKaiScoring", "FragStepMyScoring"};

        String[] fileNames = Objects.requireNonNull(this.msrdSpectraDir.list());
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());

        System.out.println("Collect all tasks...");
        ArrayList<Callable<String>> tasks = new ArrayList<>(fileNames.length);
        for(String fileName : fileNames){
            tasks.add(() -> {
                // Initialisation:
                RecallSpectralAlignment recallScorer = new RecallSpectralAlignment(this.deviation);
                WeightedRecallSpectralAlignment weightedRecallScorer = new WeightedRecallSpectralAlignment(this.deviation);

                final File msFile = new File(this.msrdSpectraDir, fileName);
                Ms2Experiment ms2Experiment = MsIO.readExperimentFromFile(msFile).next();
                ProcessedInput processedMs2Experiment = new Sirius().preprocessForMs2Analysis(ms2Experiment);
                SimpleSpectrum msrdSpectrum = parseMsrdSpectrum(processedMs2Experiment);

                final String smiles = processedMs2Experiment.getAnnotation(Smiles.class).orElseThrow().toString();
                final MolecularGraph molecule = new MolecularGraph(smiParser.parseSmiles(smiles));
                final PrecursorIonType ionization = ms2Experiment.getPrecursorIonType();

                // 1. ICEBERG: predict a spectrum using ICEBERG
                final ICEBERGSpectrumPredictor icebergPredictor = new ICEBERGSpectrumPredictor(smiles, ionization, this.NUM_FRAGMENTS, fileName.replaceAll("\\.ms", ".json"));
                final SimpleSpectrum icebergSpectrum = new SimpleSpectrum(icebergPredictor.predictSpectrum());
                final double icebergRecall = recallScorer.score(msrdSpectrum, icebergSpectrum).similarity;
                final double icebergWeightedRecall = weightedRecallScorer.score(msrdSpectrum, icebergSpectrum).similarity;

                final StringBuilder strBuilder = new StringBuilder();
                strBuilder.append(fileName.replaceAll("\\.ms", "")).append(',').append(icebergRecall).append(',').append(icebergWeightedRecall);

                // For each scoring and for each prediction method (except ICEBERG),
                // predict a spectrum and compute the (weighted) recall:
                for(String fragMethod : fragMethods){
                    for(String scoringMethod : scoringMethods){
                        CombinatorialFragmenterScoring scoring = this.getScoring(scoringMethod, molecule);
                        FragmentationPredictor fragPredictor = this.getFragPredictor(fragMethod, molecule, scoring, ionization);
                        fragPredictor.predictFragmentation();

                        BarcodeSpectrumPredictor spectrumPredictor = new BarcodeSpectrumPredictor(fragPredictor, ionization, this.NUM_H_SHIFTS);
                        SimpleSpectrum predSpectrum = new SimpleSpectrum(spectrumPredictor.predictSpectrum());

                        double recall = recallScorer.score(msrdSpectrum, predSpectrum).similarity;
                        double weightedRecall = weightedRecallScorer.score(msrdSpectrum, predSpectrum).similarity;

                        strBuilder.append(',').append(recall).append(',').append(weightedRecall);
                    }
                }

                return strBuilder.toString();
            });
        }

        try {
            System.out.println("Execute all collected tasks...");
            List<Future<String>> futures = executor.invokeAll(tasks);
            executor.shutdown();

            System.out.println("Tasks completed. Write results to the output file...");
            StringBuilder strBuilder = new StringBuilder("instanceName,ICEBERG:Recall,ICEBERG:WeightedRecall");
            for(String fragMethod : fragMethods){
                for(String scoringMethod : scoringMethods){
                    strBuilder.append(',');
                    strBuilder.append(fragMethod).append(':').append(scoringMethod).append(':').append("Recall");
                    strBuilder.append(',');
                    strBuilder.append(fragMethod).append(':').append(scoringMethod).append(':').append("WeightedRecall");
                }
            }
            String header = strBuilder.toString();

            try(BufferedWriter fileWriter = Files.newBufferedWriter(this.outputFile.toPath(), StandardCharsets.UTF_8)){
                    fileWriter.write(header);
                    for(Future<String> future : futures){
                        fileWriter.newLine();
                        fileWriter.write(future.get());
                    }
            }
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        try {
            final File msrdSpectraDir = new File(args[0]);
            final File outputFile = new File(args[1]);
            final File scoringFile = new File(args[2]);
            final Deviation deviation = new Deviation(Double.parseDouble(args[3]));
            final SimpleFragmentationRule fragRule = new SimpleFragmentationRule(new String[]{"N", "O", "P", "S"});
            final int numFragments = Integer.parseInt(args[4]);
            final int numHydrogenShifts = Integer.parseInt(args[5]);

            final File pythonPath = new File(args[6]);
            final File icebergScriptPath = new File(args[7]);
            final File icebergModelsDir = new File(args[8]);
            final File icebergOutputDir = new File(args[9]);

            ICEBERGSpectrumPredictor.initializeClass(pythonPath, icebergScriptPath, icebergModelsDir, icebergOutputDir);
            SpecPredictionEvaluation eval = new SpecPredictionEvaluation(msrdSpectraDir, outputFile, scoringFile, deviation, fragRule, numFragments, numHydrogenShifts);
            eval.evaluate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
