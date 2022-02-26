package de.unijena.bioinf.fragmenter;

public class EMFragmenterScoring extends DirectedBondTypeScoring.Impl {

    public static double rearrangementProb;

    public EMFragmenterScoring(MolecularGraph graph) {
        super(graph, null);
    }

    @Override
    public double scoreFragment(CombinatorialNode fragment){
        if(fragment.fragment.isRealFragment()){
            return 0.0;
        }else{
            return 1000.0;
        }
    }

    @Override
    public double scoreEdge(CombinatorialEdge edge){
        CombinatorialFragment sourceFragment = edge.source.fragment;
        CombinatorialFragment targetFragment = edge.target.fragment;

        if(targetFragment.isRealFragment()){
            return super.scoreEdge(edge);
        }else{
            int hydrogenDiff = Math.abs(sourceFragment.hydrogenRearrangements(targetFragment.getFormula()));
            if(hydrogenDiff == 0){
                return 0;
            }else{
                return (rearrangementProb > 0.0) ? hydrogenDiff * Math.log(rearrangementProb) : -Double.MAX_VALUE;
            }
        }
    }
}
