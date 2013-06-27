package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Marcus
 * Date: 10.06.13
 * Time: 23:59
 * To change this template use File | Settings | File Templates.
 */
@Called("EI common loss")
public class EICommonLossEdgeScorer extends CommonLossEdgeScorer {
    public final static String[] neutralLossList;

    static {
        neutralLossList = new String[4];
        neutralLossList[3] = "NH3 HCN CH3 H2O C2H2 C2H4 NO CO C2H5 CH3O H2S C3H5 C3H7 C2H3O CO2 C2H5O C2H6O C2H3O2 C2H4O2 C6H5 C7H7"; // very common
        neutralLossList[2] = "CHO OH NH2 CH5O C3H6 C2H4O C2H7N CHO2 NO2 C4H8 C3H5O2"; //common
        neutralLossList[1]="H H3 CH4O C2H3N C2H2O C4H7 C2O2 C2O3"; // ~
        neutralLossList[0] ="N2 HS S CN H3O O H2 C2H3 CH2O SO"; //uncommon

    }


    public EICommonLossEdgeScorer(HashMap<MolecularFormula, Double> map){
        super(map);
    }

    public static EICommonLossEdgeScorer getDefaultGCMSCommonLossScorer(){
        return new EICommonLossEdgeScorer(initializeMap(true));
    }

    public static EICommonLossEdgeScorer getHalogenGCMSCommonLossScorer(){
        EICommonLossEdgeScorer.neutralLossList[3] += " HF HCl Br";
        EICommonLossEdgeScorer.neutralLossList[2] += " F Cl I";
        return new EICommonLossEdgeScorer(initializeMap(true));
    }

    public static EICommonLossEdgeScorer getChlorineGCMSCommonLossScorer(){
        EICommonLossEdgeScorer.neutralLossList[3] += " HCl";
        EICommonLossEdgeScorer.neutralLossList[2] += " Cl";
        return new EICommonLossEdgeScorer(initializeMap(true));
    }

    public static EICommonLossEdgeScorer getSingleLossesGCMSCommonLossScorer(){
        return new EICommonLossEdgeScorer(initializeMap(false));
    }

    private static HashMap<MolecularFormula, Double> initializeMap(boolean combine){
        List<List<MolecularFormula>> allLosses = new ArrayList<List<MolecularFormula>>(4);
        HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        Set<MolecularFormula> singleLosses = new HashSet<MolecularFormula>();
        for (int i = 0; i < neutralLossList.length; i++) {
            String[] array = neutralLossList[i].split(" ");
            allLosses.add(new ArrayList<MolecularFormula>());
            for (String lossString : array){
                MolecularFormula mf = MolecularFormula.parse(lossString);
                allLosses.get(i).add(mf);
                singleLosses.add(mf);
            }
        }

        if (combine){
            for (int i = 0; i < neutralLossList.length; i++) {
                List<MolecularFormula> newList = new ArrayList<MolecularFormula>();
                for (int j = i; j < neutralLossList.length; j++) {
                    for (MolecularFormula mf1 : allLosses.get(i)) {
                        for (MolecularFormula mf2 : allLosses.get(j)) {
                            final MolecularFormula newMF = mf1.add(mf2);
                            if (!singleLosses.contains(newMF)) {
                                newList.add(newMF);
                            }
                        }
                    }
                }
                allLosses.get(i).addAll(newList);
            }
        }


        final double score0 = Math.log(5.0);
        final double score1 = Math.log(10.0);
        final double score2 = Math.log(50.0);
        final double score3 = Math.log(100.0);
        for (int i = 0; i < neutralLossList.length; i++) {
            for (MolecularFormula molecularFormula : allLosses.get(i)) {
                switch (i) {
                    case 0: map.put(molecularFormula, score0); break;
                    case 1: map.put(molecularFormula, score1); break;
                    case 2: map.put(molecularFormula, score2); break;
                    case 3: map.put(molecularFormula, score3); break;
                }
            }

        }
        return map;
    }


    @Override
    public Object prepare(ProcessedInput inputh) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        return score(loss.getFormula());
    }

    @Override
    public double score(MolecularFormula formula) {
        if (formula.toString().equals("C3HO3")) System.out.println("C3HO3 found: "+getMap().get(formula));
        final Double score = getMap().get(formula);
        if (score==null) {
            //unknown loss
            double score2 = (Math.log(0.1)+(formula.rdbe()>=0 ? 0 : Math.log(0.25))); //The loss should obey the seniorRule: DBE >= 0
            //todo compare to DBELossScorer -> uses doubledRDBE
            if (formula.toString().equals("C3HO3")) System.out.println("C3HO3 found: "+score2);

            final List<Element> elements = formula.elements();
            if (elements.size() == 1) {
                //C and N may not be a single loss
                final String mayNotBeSingle = "CN";
                if (mayNotBeSingle.contains(elements.get(0).getSymbol())) score2 += Math.log(0.0001);
            }
            return score2;

        }
        return score;
    }
}
