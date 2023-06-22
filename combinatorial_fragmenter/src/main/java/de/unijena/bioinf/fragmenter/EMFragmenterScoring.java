package de.unijena.bioinf.fragmenter;

import org.openscience.cdk.interfaces.IBond;

public class EMFragmenterScoring implements CombinatorialFragmenterScoring {

    protected static double rearrangementScore = -0.15d;

    public EMFragmenterScoring() {
        super();
    }

    @Override
    public double scoreFragment(CombinatorialNode fragment){
        if(fragment.fragment.isInnerNode()){
            return 0.0;
        }else{
            return 1000.0;
        }
    }

    public double scoreBond(IBond bond, boolean direction){
        return switch (bond.getOrder()) {
            case SINGLE -> -1d;
            case DOUBLE -> -2d;
            case TRIPLE -> -3d;
            case QUADRUPLE -> -4d;
            case QUINTUPLE -> -5d;
            case SEXTUPLE -> -6d;
            default -> -1.5;
        };
    }

    @Override
    public double scoreEdge(CombinatorialEdge edge){
        CombinatorialFragment sourceFragment = edge.source.fragment;
        CombinatorialFragment targetFragment = edge.target.fragment;

        if(targetFragment.isInnerNode()){
            return scoreBond(edge.cut1, edge.getDirectionOfFirstCut()) + ((edge.cut2 != null) ? scoreBond(edge.cut2, edge.getDirectionOfSecondCut()) : 0d);
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
