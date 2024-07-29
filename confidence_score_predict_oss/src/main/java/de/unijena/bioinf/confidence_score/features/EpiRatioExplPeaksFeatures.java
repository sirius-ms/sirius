package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.confidence_score.FeatureCreator;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.fragmenter.CombinatorialSubtree;

public class EpiRatioExplPeaksFeatures implements FeatureCreator {

    CombinatorialSubtree epiTree;

    public EpiRatioExplPeaksFeatures(CombinatorialSubtree epiTree){
        this.epiTree=epiTree;


    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public int weight_direction() {
        return 1;
    }

    @Override
    public int min_quartil() {
        return 1;
    }

    @Override
    public int max_quartil() {
        return 99;
    }

    @Override
    public double[] computeFeatures(ParameterStore treePara) {

        double[] scores = new double[1];
        FTree fTree =treePara.get(FTree.class).orElseThrow();





        scores[0]=(double) epiTree.getTerminalNodes().size()/(double) fTree.getFragments().size();

        return scores;
    }

    @Override
    public int getFeatureSize() {
        return 1;
    }

    @Override
    public void setMinQuartil(int quartil) {

    }

    @Override
    public void setMaxQuartil(int quartil) {

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
        String[] names = new String[1];
        names[0]="EpiRatioExplPeaks";
        return names;
    }
}
