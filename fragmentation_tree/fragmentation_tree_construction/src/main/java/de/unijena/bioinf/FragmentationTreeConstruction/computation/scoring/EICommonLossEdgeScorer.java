package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Marcus
 * Date: 10.06.13
 * Time: 23:59
 * To change this template use File | Settings | File Templates.
 */
@Called("EI Common Losses")
public class EICommonLossEdgeScorer implements LossScorer {
    public final static List<String> neutralLossList;

    static {
        List<String> lossLists = new ArrayList<String>();

        String[] neutralLosses = new String[4];
        neutralLosses[3] = "NH3 HCN CH3 H2O C2H2 C2H4 NO CO C2H5 CH3O H2S C3H5 C3H7 C2H3O CO2 C2H5O C2H6O C2H3O2 C2H4O2 C6H5 C7H7"; // very common
        neutralLosses[2] = "CHO OH NH2 CH5O C3H6 C2H4O C2H7N CHO2 NO2 C4H8 C3H5O2"; //common
        neutralLosses[1] = "H H3 CH4O C2H3N C2H2O C4H7 C2O2 C2O3"; // ~
        neutralLosses[0] = "N2 HS S CN H3O O H2 C2H3 CH2O SO"; //uncommon

        for (int i = 0; i < neutralLosses.length; i++) {
            lossLists.add(neutralLosses[i]);

        }
        neutralLossList = Collections.unmodifiableList(lossLists);
    }

    private final HashMap<MolecularFormula, Double> commonLosses;


    public EICommonLossEdgeScorer(HashMap<MolecularFormula, Double> map) {
        commonLosses = new HashMap<MolecularFormula, Double>(map);
    }

    public static EICommonLossEdgeScorer getDefaultGCMSCommonLossScorer() {
        String[] neutralLosses = neutralLossList.toArray(new String[0]);
        return new EICommonLossEdgeScorer(initializeMap(neutralLosses, true));
    }

    public static EICommonLossEdgeScorer getGCMSCommonLossScorer(boolean useChlorine, boolean useHalogens, boolean usePFB, boolean useTMS) {
        String[] neutralLosses = neutralLossList.toArray(new String[0]);
        if (useHalogens) {
            neutralLosses[3] += " HF HCl Br";
            neutralLosses[2] += " F Cl I";
        }
        if (useChlorine && !useHalogens) {
            neutralLosses[3] += " HCl";
            neutralLosses[2] += " Cl";
        }
        if (usePFB) {
            neutralLosses[3] += " PfbOH PfbO Pfb";
        }
        if (useTMS) {
            neutralLosses[3] += " OTms";
        }
        System.out.println(neutralLosses[3]);
        return new EICommonLossEdgeScorer(initializeMap(neutralLosses, true));
    }

    public static EICommonLossEdgeScorer getSingleLossesGCMSCommonLossScorer() {
        String[] neutralLosses = neutralLossList.toArray(new String[0]);
        return new EICommonLossEdgeScorer(initializeMap(neutralLosses, false));
    }

    private static HashMap<MolecularFormula, Double> initializeMap(String[] neutralLossList, boolean combine) {
        List<Set<MolecularFormula>> allLosses = new ArrayList<Set<MolecularFormula>>(4);
        HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        Set<MolecularFormula> singleLosses = new HashSet<MolecularFormula>();
        for (int i = 0; i < neutralLossList.length; i++) {
            String[] array = neutralLossList[i].split(" ");
            allLosses.add(new HashSet<MolecularFormula>());
            for (String lossString : array) {
                MolecularFormula mf = MolecularFormula.parse(lossString);
                allLosses.get(i).add(mf);
                singleLosses.add(mf);
            }
        }

        if (combine) {
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

        //for (int i = 0; i < neutralLossList.length; i++) {
        for (int i = neutralLossList.length - 1; i >= 0; i--) { //-> changed to agree with GCMSTool: if for combined losses different scores are possible take worst one
            for (MolecularFormula molecularFormula : allLosses.get(i)) {
                switch (i) {
                    case 0:
                        map.put(molecularFormula, score0);
                        break;
                    case 1:
                        map.put(molecularFormula, score1);
                        break;
                    case 2:
                        map.put(molecularFormula, score2);
                        break;
                    case 3:
                        map.put(molecularFormula, score3);
                        break;
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

    public double score(MolecularFormula formula) {
        final Double score = commonLosses.get(formula);
        if (score == null) {
            //unknown loss
            double score2 = (Math.log(0.1) + (formula.rdbe() >= 0 ? 0 : Math.log(0.25))); //The loss should obey the seniorRule: DBE >= 0

            final List<Element> elements = formula.elements();
            if (elements.size() == 1) {
                //C and N may not be a single loss
                final String mayNotBeSingle = "CN";
                if (mayNotBeSingle.contains(elements.get(0).getSymbol())) {
                    score2 += Math.log(0.0001);
                }
            }
            return score2;
        }
        return score;
    }

    public HashMap<MolecularFormula, Double> getCommonLosses() {
        return new HashMap<MolecularFormula, Double>(commonLosses);
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        final D common = document.newDictionary();
        for (Map.Entry<MolecularFormula, Double> entry : commonLosses.entrySet()) {
            document.addToDictionary(common, entry.getKey().toString(), entry.getValue());
        }
        document.addDictionaryToDictionary(dictionary, "losses", common);
    }
}
