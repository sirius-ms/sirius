package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.CommonLossEdgeScorer;
import de.unijena.bioinf.confidence_score.FeatureCreator;
import de.unijena.bioinf.sirius.IdentificationResult;
import org.openscience.cdk.config.Elements;

import java.util.List;

/**
 * Created by martin on 05.06.19.
 */
public class SIRIUSTreeScoreFeatures implements FeatureCreator {

    List<IdentificationResult> idlist;
    Ms2Experiment exp;

    public SIRIUSTreeScoreFeatures(List<IdentificationResult> idlist, Ms2Experiment exp){
        this.idlist=idlist;
        this.exp=exp;
    }
    @Override
    public void prepare(PredictionPerformance[] statistics) {

    }

    @Override
    public double[] computeFeatures(ProbabilityFingerprint query, IdentificationResult idresult) {
        double[] scores = new double[]{
                idresult.getExplainedIntensityRatio(),


        idresult.getIsotopeScore(),
                Math.abs(idlist.get(0).getIsotopeScore()-idlist.get(1).getIsotopeScore()),
        idresult.getExplainedPeaksRatio(),
        idresult.getNumberOfExplainablePeaks(),
        idresult.getNumOfExplainedPeaks(),
        idresult.getTreeScore(),
                Math.abs(idlist.get(0).getTreeScore()-idlist.get(1).getTreeScore()),
                Math.abs(idlist.get(0).getTreeScore()-idlist.get(2).getTreeScore()),
                Math.log(Math.abs(idlist.get(0).getTreeScore()-idlist.get(1).getTreeScore())),
                Math.log(Math.abs(idlist.get(0).getTreeScore()-idlist.get(2).getTreeScore())),
                getRareElementCounter(),
                idresult.getMolecularFormula().getMass(),
                idresult.getTree().numberOfVertices(),
                idresult.getTree().numberOfEdges(),
                idresult.getMolecularFormula().union(idlist.get(1).getMolecularFormula()).atomCount(),
                commonLossCounter()



        };

      //  System.out.println(scores.length+" - "+scores[0]+" - "+idresult.getMolecularFormula());

        return scores;
    }

    private double commonLossCounter(){

        double counter=0;
        for (int i=0;i<idlist.get(0).getTree().losses().size();i++){
            for(int j=0;j<CommonLossEdgeScorer.ales_list.length;j++){
                if (CommonLossEdgeScorer.ales_list[j].equals(idlist.get(0).getTree().losses().get(i).getFormula().toString())){
                    counter += 1;
                    break;
                }
            }
        }
        return counter;
    }

    private double getRareElementCounter(){
        double return_value=0;

        if (idlist.get(0).getMolecularFormula().isCHNOPS()) {
            return_value = 0;
        }
        return_value+=idlist.get(0).getMolecularFormula().numberOf(Element.fromString("Cl"));
        return_value+=idlist.get(0).getMolecularFormula().numberOf(Element.fromString("Br"));
        return_value+=idlist.get(0).getMolecularFormula().numberOf(Element.fromString("F"));


        return return_value;
    }

    @Override
    public int getFeatureSize() {
        return 19;
    }

    @Override
    public boolean isCompatible(ProbabilityFingerprint query, CompoundWithAbstractFP<Fingerprint>[] rankedCandidates) {
        return false;
    }

    @Override
    public int getRequiredCandidateSize() {
        return 0;
    }

    @Override
    public String[] getFeatureNames() {
        return new String[0];
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
