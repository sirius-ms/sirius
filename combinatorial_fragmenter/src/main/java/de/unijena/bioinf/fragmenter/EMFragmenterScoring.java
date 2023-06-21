package de.unijena.bioinf.fragmenter;

public class EMFragmenterScoring extends DirectedBondTypeScoring.Impl {

    protected static double rearrangementScore = -0.15d;

    public EMFragmenterScoring(MolecularGraph graph) {
        super(graph, null);
    }

    @Override
    public double scoreFragment(CombinatorialNode fragment){
        if(fragment.fragment.isInnerNode()){
            return 0.0;
        }else{
            return 1000.0;
        }
    }

    @Override
    public double scoreEdge(CombinatorialEdge edge){
        CombinatorialFragment sourceFragment = edge.source.fragment;
        CombinatorialFragment targetFragment = edge.target.fragment;

        if(targetFragment.isInnerNode()){
            return super.scoreEdge(edge);
        }else{
            int hydrogenDiff = Math.abs(sourceFragment.hydrogenRearrangements(targetFragment.getFormula()));
            if(hydrogenDiff == 0){
                return 0;
            }else{
                double score = hydrogenDiff * rearrangementScore;
                return (Double.isNaN(score) || Double.isInfinite(score)) ? (-1.0E6) : score;
            }
        }
    }
}
