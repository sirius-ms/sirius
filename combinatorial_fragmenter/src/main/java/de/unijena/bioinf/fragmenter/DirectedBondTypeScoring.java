package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.openscience.cdk.interfaces.IBond;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;

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
        if(fragment.fragment.isInnerNode()){
            return 0;
        }else{
            return pseudoFragmentScore;
        }
    }

    @Override
    public double scoreEdge(CombinatorialEdge edge) {
        CombinatorialFragment sourceFragment = edge.source.fragment;
        CombinatorialFragment targetFragment = edge.target.fragment;

        if(targetFragment.isInnerNode()){
            return scoreBond(edge.getCut1(), edge.getDirectionOfFirstCut()) + (edge.getCut2() != null ? scoreBond(edge.getCut2(), edge.getDirectionOfSecondCut()) : 0d);
        }else{
            int hydrogenDiff = Math.abs(targetFragment.hydrogenRearrangements(sourceFragment.getFormula()));
            if(hydrogenDiff == 0){
                return 0d;
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
                if(currentLine.startsWith(">bondScores")){
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

    private static void writeFieldName(String fieldName, BufferedWriter writer) throws IOException {
        writer.write(fieldName);
        writer.newLine();
    }

    public static void writeScoringToFile(File scoringFile, String[] bondNames, double[] bondScores, double wildcardScore, double hydrogenRearrangementProb, double pseudoFragmentScore) throws IOException {
        try(BufferedWriter fileWriter = Files.newBufferedWriter(scoringFile.toPath(), Charset.defaultCharset())){
            // Write the bond scores into the file:
            writeFieldName(">bondScores", fileWriter);
            for(int i = 0; i < bondNames.length; i++){
                fileWriter.write(bondNames[i]+" "+bondScores[i]);
                fileWriter.newLine();
            }

            // Write the wildcard score into the file:
            writeFieldName(">wildcard", fileWriter);
            fileWriter.write(Double.toString(wildcardScore));
            fileWriter.newLine();

            // Write the hydrogen rearrangement probability into the given file:
            writeFieldName(">hydrogenRearrangementProbability", fileWriter);
            fileWriter.write(Double.toString(hydrogenRearrangementProb));
            fileWriter.newLine();

            // Write the pseudo fragment score into the file:
            writeFieldName(">pseudoFragmentScore", fileWriter);
            fileWriter.write(Double.toString(pseudoFragmentScore));
        }
    }

    public static String bondNameGeneric(IBond b, boolean fromLeftToRight) {
        return explainBondBy(b, b.getAtom(0).getSymbol()  ,b.getAtom(1).getSymbol(), fromLeftToRight);
    }

    public static String bondNameSpecific(IBond b, boolean fromLeftToRight) {
        return explainBondBy(b, b.getAtom(0).getAtomTypeName()  ,b.getAtom(1).getAtomTypeName(), fromLeftToRight);
    }

    public static String bondNameEcfp(IBond b, boolean fromLeftToRight) {
        int ecfp = b.getAtom(0).getProperty("ECFP");
        int ecfp2 = b.getAtom(1).getProperty("ECFP");
        return explainBondBy(b, b.getAtom(0).getAtomTypeName() + "#" + ecfp  ,b.getAtom(1).getAtomTypeName() + "#" + ecfp2, fromLeftToRight);
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
