package de.unijena.bioinf.fragmenter;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.openscience.cdk.interfaces.IBond;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DirectedBondTypeScoring implements CombinatorialFragmenterScoring{

    private static TObjectDoubleHashMap<String> name2score;
    private static double hydrogenRearrangementProb;
    private static double pseudoFragmentScore;
    private static double wildcard;

    private double[] bondScoresLeft, bondScoresRight;

    public DirectedBondTypeScoring(MolecularGraph molecule){
        this.bondScoresLeft = new double[molecule.bonds.length];
        this.bondScoresRight = new double[molecule.bonds.length];

        for(int i = 0; i < molecule.bonds.length; i++){
            IBond bond = molecule.bonds[i];
            {
                String bondName = bondNameSpecific(bond, true);
                if(name2score.containsKey(bondName)){
                    this.bondScoresLeft[i] = name2score.get(bondName);
                }else{
                    bondName = bondNameGeneric(bond, true);
                    if(name2score.containsKey(bondName)){
                        this.bondScoresLeft[i] = name2score.get(bondName);
                    }else{
                        this.bondScoresLeft[i] = wildcard;
                    }
                }
            }
            {
                String bondName = bondNameSpecific(bond, false);
                if(name2score.containsKey(bondName)){
                    this.bondScoresRight[i] = name2score.get(bondName);
                }else{
                    bondName = bondNameGeneric(bond, false);
                    if(name2score.containsKey(bondName)){
                        this.bondScoresRight[i] = name2score.get(bondName);
                    }else{
                        this.bondScoresRight[i] = wildcard;
                    }
                }
            }
        }
    }

    @Override
    public double scoreBond(IBond bond, boolean direction) {
        return direction ? this.bondScoresLeft[bond.getIndex()] : this.bondScoresRight[bond.getIndex()];
    }

    @Override
    public double scoreFragment(CombinatorialNode fragment) {
        if(fragment.fragment.isRealFragment()){
            return 0;
        }else{
            return pseudoFragmentScore;
        }
    }

    @Override
    public double scoreEdge(CombinatorialEdge edge) {
        CombinatorialFragment sourceFragment = edge.source.fragment;
        CombinatorialFragment targetFragment = edge.target.fragment;

        if(targetFragment.isRealFragment()){
            return scoreBond(edge.getCut1(), edge.getDirectionOfFirstCut()) + (edge.getCut2() != null ? scoreBond(edge.getCut2(), edge.getDirectionOfSecondCut()) : 0);
        }else{
            int hydrogenDiff = Math.abs(targetFragment.hydrogenRearrangements(sourceFragment.getFormula()));
            if(hydrogenDiff == 0){
                return 0.0;
            }else{
                double score = hydrogenDiff * Math.log(hydrogenRearrangementProb);
                return Double.isNaN(score) ? -(1.0E6) : score;
            }
        }
    }

    public static void loadScoringFromFile(File scoringFile) throws IOException {
        try(BufferedReader fileReader = new BufferedReader(new FileReader(scoringFile))){
            name2score = new TObjectDoubleHashMap<>();
            String currentLine = fileReader.readLine();

            while(currentLine != null){
                if(currentLine.startsWith(">bondSores")){
                    currentLine = fileReader.readLine();
                    while(currentLine != null && currentLine.charAt(0) != '>'){
                        String[] bondInformation = currentLine.split(" ");
                        String bondName = bondInformation[0];
                        double bondScore = Double.parseDouble(bondInformation[1]);
                        name2score.put(bondName, bondScore);
                        currentLine = fileReader.readLine();
                    }
                }else{
                    if(currentLine.startsWith(">wildcard")){
                        currentLine = fileReader.readLine();
                        wildcard = Double.parseDouble(currentLine);
                    }else if(currentLine.startsWith(">hydrogenRearrangementProbability")){
                        currentLine = fileReader.readLine();
                        hydrogenRearrangementProb = Double.parseDouble(currentLine);
                    }else if(currentLine.startsWith(">pseudoFragmentScore")){
                        currentLine = fileReader.readLine();
                        pseudoFragmentScore = Double.parseDouble(currentLine);
                    }
                    currentLine = fileReader.readLine();
                }
            }
        }
    }

    public static String bondNameGeneric(IBond b, boolean fromLeftToRight) {
        return explainBondBy(b, b.getAtom(0).getSymbol()  ,b.getAtom(1).getSymbol(), fromLeftToRight);
    }

    public static String bondNameSpecific(IBond b, boolean fromLeftToRight) {
        return explainBondBy(b, b.getAtom(0).getAtomTypeName()  ,b.getAtom(1).getAtomTypeName(), fromLeftToRight);
    }

    public static String explainBondBy(IBond b, String labelA, String labelB, boolean fromLeftToRight) {
        if (!fromLeftToRight) {
            String c = labelA;
            labelA = labelB;
            labelB = c;
        }
        String s = labelA;
        if (b.isAromatic()) {
            s += ":";
        } else {
            switch (b.getOrder()) {
                case SINGLE:
                    s += "-";
                    break;
                case DOUBLE:
                    s += "=";
                    break;
                case TRIPLE:
                    s += "#";
                    break;
                default:
                    s += "?";
            }
        }
        s += labelB;
        return s;
    }
}
