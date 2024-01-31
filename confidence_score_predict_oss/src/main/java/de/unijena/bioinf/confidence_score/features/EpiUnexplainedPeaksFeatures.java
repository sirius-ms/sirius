package de.unijena.bioinf.confidence_score.features;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.confidence_score.FeatureCreator;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.CombinatorialSubtree;

import java.util.ArrayList;
import java.util.Map;

public class EpiUnexplainedPeaksFeatures implements FeatureCreator {

    CombinatorialSubtree epiTree;
    Map<Fragment, ArrayList<CombinatorialFragment>> originalMappings;

    public EpiUnexplainedPeaksFeatures(CombinatorialSubtree epiTree, Map<Fragment, ArrayList<CombinatorialFragment>> originalMappings){
        this.epiTree=epiTree;
        this.originalMappings=originalMappings;


    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public int weight_direction() {
        return -1;
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
        int sum=0;
        FTree fTree =treePara.get(FTree.class).orElseThrow();
        FragmentAnnotation<AnnotatedPeak> ano = fTree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);


        for (Fragment f :fTree.getFragments()){
            if(!originalMappings.containsKey(f)){
                if(ano.get(f).getRelativeIntensity()>0.01){
                    sum++;
                }
            }
        }

        scores[0]=sum;

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
        names[0]="EpiUnexplPeaks";
        return names;
    }
}
