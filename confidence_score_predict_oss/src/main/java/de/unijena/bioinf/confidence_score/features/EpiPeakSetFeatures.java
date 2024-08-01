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
import de.unijena.bioinf.fragmenter.CombinatorialNode;
import de.unijena.bioinf.fragmenter.CombinatorialSubtree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EpiPeakSetFeatures implements FeatureCreator {

    CombinatorialSubtree[] epiTrees;
    Map<Fragment,ArrayList<CombinatorialFragment>>[] originalMappings;

    Map<CombinatorialFragment,List<Double>>[] combFragMassMappings;

    FTree[] fTrees;


    public EpiPeakSetFeatures(CombinatorialSubtree[] epiTrees, Map<Fragment, ArrayList<CombinatorialFragment>>[] originalMappings, FTree[] ftrees){

        this.epiTrees=epiTrees;
        this.originalMappings=originalMappings;
        this.fTrees=ftrees;



    }

    private Map<CombinatorialFragment,List<Double>>[] prepareMappings(){

        HashMap<CombinatorialFragment, List<Double>>[] maps = new HashMap[originalMappings.length];


        for(int i=0;i<originalMappings.length;i++){

            FragmentAnnotation<AnnotatedPeak> ano = fTrees[i].getFragmentAnnotationOrThrow(AnnotatedPeak.class);
            HashMap<CombinatorialFragment, List<Double>> tmpMap = new HashMap<>();
            for (Fragment frag : originalMappings[i].keySet()){
                if(!tmpMap.containsKey(originalMappings[i].get(frag).get(0))){
                    ArrayList<Double> d  = new ArrayList<>();
                    d.add(ano.get(frag).getMass());
                    tmpMap.put(originalMappings[i].get(frag).get(0),d);
                }else {

                    tmpMap.get(originalMappings[i].get(frag).get(0)).add(ano.get(frag).getMass());
                }
            }
            maps[i]=tmpMap;



        }
        return maps;
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
    public double[] computeFeatures(ParameterStore parameters) {
        this.combFragMassMappings=prepareMappings();


        double[] scores= new double[1];


        HashMap<Double, Boolean> peakMassExistMap = new HashMap<Double, Boolean>();

        HashMap<Double, Float> peakMassIntMap = new HashMap<>();

        HashMap<Double, Boolean> peakMassesinTopHit = new HashMap<>();



        for(int i=0;i< epiTrees.length;i++){



            CombinatorialSubtree currTree = epiTrees[i];

            for(CombinatorialNode node :currTree.getTerminalNodes()) {
                for(Double peakMass : combFragMassMappings[i].get(node.getIncomingEdges().get(0).getSource().getFragment())) {
                    peakMassExistMap.put(peakMass, true);
                    peakMassIntMap.put(peakMass, node.getFragment().getPeakIntensity());

                    if (i == 0) peakMassesinTopHit.put(peakMass, true);
                }

            }

        }



        double percentageOfAllExplainedPeaksInTopHit = (double) peakMassesinTopHit.size()/ (double)peakMassExistMap.keySet().size();

    scores[0]=percentageOfAllExplainedPeaksInTopHit;



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
        names[0]="EpiPeakSet";
        return names;
    }
}
