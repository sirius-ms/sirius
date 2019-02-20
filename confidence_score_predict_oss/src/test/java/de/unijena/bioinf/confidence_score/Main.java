package de.unijena.bioinf.confidence_score;

/**
 * Created by martin on 20.06.18.
 */
public class Main {


    /*public static void main(String[] args){

       //TODO: Read a compound and see if everything works



        try {
        Prediction prediction = Prediction.loadFromFile(new File("/home/martin/IdeaProjects/csi_fingerid/fingerid.data"));

        File trained_svm = new File("/home/martin/Documents/svmexporttest.json");

        File path = new File("/home/martin/Documents/testinput/");


            for(File file : path.listFiles()) {



                Sirius sirius = new Sirius();

                Ms2Experiment exp = sirius.parseExperiment(file).next();


                final SpectralPreprocessor.Preprocessed pre;

                pre = SpectralPreprocessor.preprocess(sirius, sirius.compute(exp, exp.getMolecularFormula()), exp);


                ChemicalDatabase db = new ChemicalDatabase();
                List<FingerprintCandidate> comps = db.lookupStructuresAndFingerprintsByFormula(exp.getMolecularFormula());


                ProbabilityFingerprint fingerprint = prediction.predictProbabilityFingerprint(pre.spectrum, pre.tree, pre.precursorMz);


                Fingerblast blast = new Fingerblast(new ScoringMethodFactory.CSIFingerIdScoringMethod(prediction.getFingerid().getPredictionPerformances()), db);

                List<Scored<FingerprintCandidate>> scored_list = blast.score(comps, fingerprint);

                Scored<FingerprintCandidate>[] scored_array = new Scored[scored_list.size()];

                scored_array = scored_list.toArray(scored_array);

                Utils utils = new Utils();

                Scored<FingerprintCandidate>[] condesnsed = utils.condense_candidates_by_flag(scored_array, 2);

         *//*       CombinedFeatureCreator comb = new CombinedFeatureCreator(new ScoreFeatures(ScoringMethodFactory.getCSIFingerIdScoringMethod(prediction.getFingerid().getPredictionPerformances()).getScoring()),
                        new DistanceFeatures(1, 2), new LogDistanceFeatures(1, 2));

                comb.prepare(prediction.getFingerid().getPredictionPerformances());

                double[] feature = (comb.computeFeatures(new CompoundWithAbstractFP<>(null, fingerprint), condesnsed, new IdentificationResult(pre.tree, 1), 2));
                double[][]featureMatrix= new double[1][feature.length];
                featureMatrix[0]=feature;
                SVMPredict predict = new SVMPredict();

                predict.predict_confidence(featureMatrix,new TrainedSVM(trained_svm));*//*

            }












        }
        catch (Exception e){
            e.printStackTrace();;
        }

    }*/
}
