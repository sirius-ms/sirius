package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.legacy;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossSizeScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.StrangeElementScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Loss;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kai Dührkop
 */
@Called("Common Losses:")
public class CommonLossEdgeScorer implements LossScorer, MolecularFormulaScorer {

    private final HashMap<MolecularFormula, Double> map;
    private final double normalization;

    private final static String[] implausibleLosses = new String[]{"C2O", "C4O", "C3H2", "C5H2", "C7H2", "N", "C"};
    
    public final static String[] ales_list = new String[]{
    	"H2", "H2O", "CH4", "C2H4", "C2H2",
    	"C4H8", "C5H8", "C6H6", "CH2O",
    	"CO", "CH2O2", "CO2", "C2H4O2",
    	"C2H2O", "C3H6O2", "C3H4O4",
    	"C3H2O3", "C5H8O4", "C6H10O5",
    	"C6H8O6", "NH3", "CH5N",
    	"CH3N", "C3H9N", "CHNO", "CH4N2O",
    	"H3PO3", "H3PO4", "HPO3", "C2H5O4P",
    	"H2S", "S", "SO2", "SO3", "H2SO4"
    };

    public final static String[] learnedList = new String[]{
    	"H2O", "CO", "H2", "CH2", "H3N", "C2", "CH2O2", "C6H11NO", "C5H5NO2",
    	"C4H7NO2", "C4H5NO3", "C5H9NO", "C9H9NO", "C3H5NO", "C9H9NO2", "C5H9NOS",
    	"C5H7NO", "C3H5NO2", "C6H13NO2", "C2H3NO", "C4H7NO4", "C9H11NO2", "C5H11NO2",
    	"C5H9NO4", "C4H8N2O3", "H", "C3H7NO2", "C6H12N2O", "C5H7NO3", "C9H11NO3",
    	"C11H12N2O2", "C20H30O"
    };
    private final static double[] learnedListScores = new double[]{
    	2.70d, 2.16d, 4.65d, 2.41d, 2.17d, 1.01d, 0.30d, 2.92d, 2.55d, 2.20d,
    	2.42d, 1.95d, 2.68d, 1.18d, 2.71d, 1.99d, 1.35d, 1.20d, 1.82d, 0.42d,
    	1.69d, 2.30d, 1.29d, 1.88d, 1.13d, 1.46d, 0.29d, 0.74d, 0.69d, 1.56d,
    	1.39d, 2.40d
    };
    /*
    public final static String[] optimizedList = new String[]{

            "C2H2", "Cl", "HCl", "CH2", "O", "HF", "HO3P", "H3N", "CS", "ClN", "C6H6", "C6H5Cl", "CH3", "I", "C2H4", "CH4", "Br", "HN", "HBr", "H2S", "HI", "S", "CO", "H2", "OS", "HO2PS", "O2S", "C3H4", "H2O"

    };
    public final static double[] optimizedListScores = new double[]{

            2.450592466647284d, 2.6322323647885035d, 3.585635090866105d, 5.061869416500993d, 1.98735108678975d, 4.219990206399166d, 4.81556101339377d, 1.712137433054253d, 2.193608226046093d, 0.9675789664272824d, 1.4916520308347205d, 2.0064039882484526d, 2.434693632787536d, 6.1359239584496095d, 0.7300546087164697d, 0.5300547398834722d, 3.477002088748983d, 2.436610178384473d, 4.603462237399737d, 1.8124350160491391d, 5.470340264205764d, 3.4889242233884845d, 2.4718950405077442d, 8.48154326479735d, 0.853549512465558d, 3.1639280772330727d, 1.9797429997262888d, 0.5606299982203099d, 3.379679499949444d

    };
    */
    public final static String[] optimizedList = new String[]{

            "H2", "H", "I2", "I", "HI", "HF", "CH2", "HBr", "Br", "HCl", "HO3P", "C12H8ClNS", "H2O", "C2H7O3PS", "S", "C2H2", "HO2PS", "BrCl", "C2H7O4P", "Cl", "CH3", "CO", "H3N", "C8H6Cl2O", "C12H9NS", "C14H11Cl", "C7H3Cl2NO", "H3O4P", "C11H14ClNO", "C6H5NO2S", "C6H6", "C13H11Cl", "CH4", "CHN", "HN", "H2S", "C6H9NO2S", "O2S", "C6H5Cl", "CH3O3P", "C6H4Cl2", "C2H4", "CS", "C7H4FN", "C2H2O", "C4H2", "C6H3Cl", "C6H2", "O", "HO", "C3H6", "CO2", "C4H8", "C6H4", "CH2O", "CH3N", "C3H4"

    };
    public final static double[] optimizedListScores = new double[]{

            10.554062581508553d, 9.979026958254972d, 8.626435068254311d, 6.10839121182664d, 6.01146973989286d, 4.417520604979559d, 4.247694084225074d, 4.206350753263359d, 3.9555408824312654d, 3.901157086651542d, 3.868892556282309d, 3.6287555170641954d, 3.558876325850449d, 3.3832468165262237d, 3.296853354458054d, 3.0954376096038017d, 3.054338977234424d, 3.0489400108959583d, 3.018544086459603d, 2.781648937190621d, 2.731420465971679d, 2.688107097315641d, 2.6561474312172164d, 2.64571658969221d, 2.5899970694324566d, 2.583829849075102d, 2.373268889349044d, 2.3642671898404863d, 2.3019658122844917d, 2.2343095962820847d, 2.1311094228016922d, 2.1220086080192853d, 1.9622119345549156d, 1.9285921912065125d, 1.8009026611879069d, 1.7443156795421049d, 1.6296884876035904d, 1.61751217380546d, 1.5359459059869094d, 1.4825258177873448d, 1.3349189057029032d, 1.3278627409787906d, 1.3225489604969904d, 1.2245503502065693d, 1.1451207771775491d, 1.1315843538613175d, 1.0187687437997412d, 0.9923761625477816d, 0.9601383921402605d, 0.7743350215923412d, 0.5330521185132685d, 0.5201079279242596d, 0.4669532233585024d, 0.44703171480817444d, 0.3745528724443129d, 0.36057869007933d, 0.3584825231770561d

    };

    public final static double OPTIMIZED_NORMALIZATION = 3.775305520544931d;



    public CommonLossEdgeScorer slightlyFavourAlesList() {
        final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>(this.map);
        for (String s : ales_list) {
            final MolecularFormula f = MolecularFormula.parse(s);
            Double before = map.get(f);
            map.put(f, Math.max(before==null?0:before.doubleValue(), OPTIMIZED_NORMALIZATION-2d));
        }
        return new CommonLossEdgeScorer(map);
    }

    public static CommonLossEdgeScorer getAlesListScorer(LossSizeScorer sc) {
        final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        for (String f : ales_list) {
            final MolecularFormula m = MolecularFormula.parse(f);
            map.put(m, -(sc.score(m)+sc.getNormalization())*0.66 + 0.5);
        }
        return new CommonLossEdgeScorer(map,0d);
    }

    public static CommonLossEdgeScorer getLearnedCommonLossScorerWithFixedScore(double score) {
        final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        for (String loss : ales_list) {
            map.put(MolecularFormula.parse(loss), score); // chemical knowledge
        }
        for (int i=0; i < learnedList.length; ++i) {
            final MolecularFormula formula = MolecularFormula.parse(learnedList[i]);
            Double sc = map.get(formula);
            if (sc == null)
                map.put(formula, score);
        }
        return new CommonLossEdgeScorer(map);
    }

    public static CommonLossEdgeScorer getOptimizedCommonLossScorerWithoutNormalization() {
        final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        for (int i=0; i < optimizedList.length; ++i) {
            final MolecularFormula formula = MolecularFormula.parse(optimizedList[i]);
            map.put(formula, optimizedListScores[i]); // learned losses
        }
        return new CommonLossEdgeScorer(map, 0d);
    }

    public static CommonLossEdgeScorer getOptimizedCommonLossScorer() {
        final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        for (int i=0; i < optimizedList.length; ++i) {
            final MolecularFormula formula = MolecularFormula.parse(optimizedList[i]);
            map.put(formula, optimizedListScores[i]); // learned losses
        }
        return new CommonLossEdgeScorer(map, OPTIMIZED_NORMALIZATION);
    }

    public static CommonLossEdgeScorer getLearnedCommonLossScorer(final double ales, final double multiplicate) {
    	final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        for (String loss : ales_list) {
            map.put(MolecularFormula.parse(loss), ales); // chemical knowledge
        }
        for (int i=0; i < learnedList.length; ++i) {
        	final MolecularFormula formula = MolecularFormula.parse(learnedList[i]);
        	Double sc = map.get(formula);
        	if (sc == null)
        		map.put(formula, learnedListScores[i]*multiplicate); // learned losses
        	else
        		map.put(formula, Math.max(learnedListScores[i]*multiplicate,sc.doubleValue()));
        }
		return new CommonLossEdgeScorer(map);
    }

    public static CommonLossEdgeScorer map(Object... values) {
        final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        for (int i=0; i < values.length; i += 2) {
            final String formula = (String)values[i];
            final double score = ((Number)values[i+1]).doubleValue();
            map.put(MolecularFormula.parse(formula), score);
        }
        return new CommonLossEdgeScorer(map);
    }

    public static CommonLossEdgeScorer getDefaultUnplausibleLossScorer(double score) {
        final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        for (String loss : implausibleLosses) {
            map.put(MolecularFormula.parse(loss), score);
        }
        return new CommonLossEdgeScorer(map);
    }

    public static CommonLossEdgeScorer getDefaultCommonLossScorer(double score, double lossSizeLambda) {
        final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        for (String loss : ales_list) {
            final MolecularFormula formula = MolecularFormula.parse(loss);
            map.put(formula, score + lossSizeLambda*formula.getMass());
        }
        return new CommonLossEdgeScorer(map);
    }
    
    public static CommonLossEdgeScorer getDefaultCommonLossScorer(double score) {
    	final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        for (String loss : ales_list) {
            map.put(MolecularFormula.parse(loss), score);
        }
		return new CommonLossEdgeScorer(map);
    }

    public CommonLossEdgeScorer recombinate(int num) {
        return recombinate(num, 4);
    }

    public CommonLossEdgeScorer recombinateSpec(int num, StrangeElementScorer sc) {
        final HashMap<MolecularFormula, Double> newMap = new HashMap<MolecularFormula, Double>(this.map);
        final ArrayList<ScoredMolecularFormula> decompositions = new ArrayList<ScoredMolecularFormula>();
        map2Decompositions(map, decompositions);
        ArrayList<ScoredMolecularFormula> mixedDecompositions = new ArrayList<ScoredMolecularFormula>(decompositions);
        ArrayList<ScoredMolecularFormula> buffer = new ArrayList<ScoredMolecularFormula>();
        for (int i=0; i < num; ++i) {
            for (ScoredMolecularFormula left : decompositions) {
                final boolean leftNoChno = sc==null?false:sc.containsStrangeElement(left.getFormula());
                for (ScoredMolecularFormula right : mixedDecompositions) {
                    double scoreBonus = 0d;
                    if (sc != null) {
                        final boolean rightNoChno = sc.containsStrangeElement(right.getFormula());
                        if (leftNoChno && rightNoChno) continue;
                        if ((left.getScore() > right.getScore() && leftNoChno) || (right.getScore() > left.getScore() && rightNoChno)) {
                            scoreBonus -= sc.getPenalty()/2;
                        }
                    }
                    final MolecularFormula combination = left.getFormula().add(right.getFormula());
                    final double score =
                            //log((exp(left.getScore())+exp(right.getScore()))/4) ;
                            Math.min(left.getScore(), right.getScore())/6 + scoreBonus;
                    final Double oldScore = map.get(combination);
                    if (oldScore == null || score > oldScore) {
                        newMap.put(combination, score);
                        buffer.add(new ScoredMolecularFormula(combination, score));
                    }
                }
            }
            final ArrayList<ScoredMolecularFormula> swap = mixedDecompositions;
            mixedDecompositions = buffer;
            buffer = swap;
            buffer.clear();
        }
        return new CommonLossEdgeScorer(newMap, normalization);
    }

    public CommonLossEdgeScorer recombinate(int num, LossSizeScorer scorer) {
        final HashMap<MolecularFormula, Double> newMap = new HashMap<MolecularFormula, Double>(this.map);
        final ArrayList<ScoredMolecularFormula> decompositions = new ArrayList<ScoredMolecularFormula>();
        map2Decompositions(map, decompositions);
        ArrayList<ScoredMolecularFormula> mixedDecompositions = new ArrayList<ScoredMolecularFormula>(decompositions);
        ArrayList<ScoredMolecularFormula> buffer = new ArrayList<ScoredMolecularFormula>();
        for (int i=0; i < num; ++i) {
            for (ScoredMolecularFormula left : decompositions) {
                final double leftSizeScore = scorer.score(left.getFormula())+scorer.getNormalization();
                for (ScoredMolecularFormula right : mixedDecompositions) {
                    final double rightSizeScore = scorer.score(right.getFormula())+scorer.getNormalization();
                    final MolecularFormula combination = left.getFormula().add(right.getFormula());
                    final double score = leftSizeScore+rightSizeScore-(scorer.score(combination)+scorer.getNormalization()) + Math.log(0.25) +  left.getScore()+right.getScore();
                    final Double oldScore = map.get(combination);
                    if (oldScore == null || score > oldScore) {
                        newMap.put(combination, score);
                        buffer.add(new ScoredMolecularFormula(combination, score));
                    }
                }
            }
            final ArrayList<ScoredMolecularFormula> swap = mixedDecompositions;
            mixedDecompositions = buffer;
            buffer = swap;
            buffer.clear();
        }
        return new CommonLossEdgeScorer(newMap, normalization);
    }

    public CommonLossEdgeScorer recombinate(int num, double quot) {
        final HashMap<MolecularFormula, Double> newMap = new HashMap<MolecularFormula, Double>(this.map);
        final ArrayList<ScoredMolecularFormula> decompositions = new ArrayList<ScoredMolecularFormula>();
        map2Decompositions(map, decompositions);
        ArrayList<ScoredMolecularFormula> mixedDecompositions = new ArrayList<ScoredMolecularFormula>(decompositions);
        ArrayList<ScoredMolecularFormula> buffer = new ArrayList<ScoredMolecularFormula>();
        for (int i=0; i < num; ++i) {
        	for (ScoredMolecularFormula left : decompositions) {
            	for (ScoredMolecularFormula right : mixedDecompositions) {
            		final MolecularFormula combination = left.getFormula().add(right.getFormula());
                    final double score =
                            //log((exp(left.getScore())+exp(right.getScore()))/4) ;
                            (left.getScore()+right.getScore())/quot;
                    final Double oldScore = map.get(combination);
                    if (oldScore == null || score > oldScore) {
                        newMap.put(combination, score);
                        buffer.add(new ScoredMolecularFormula(combination, score));
                    }
                }
            }
            final ArrayList<ScoredMolecularFormula> swap = mixedDecompositions;
            mixedDecompositions = buffer;
            buffer = swap;
            buffer.clear();
        }
        return new CommonLossEdgeScorer(newMap, normalization);
    }
    
    private void map2Decompositions(Map<MolecularFormula, Double> map, ArrayList<ScoredMolecularFormula> decompositions) {
    	decompositions.clear();
    	for (Map.Entry<MolecularFormula, Double> entry : map.entrySet()) {
    		decompositions.add(new ScoredMolecularFormula(entry.getKey(), entry.getValue()));
    	}
    }

    public CommonLossEdgeScorer(Map<MolecularFormula, Double> map, double normalization) {
        this.map = new HashMap<MolecularFormula, Double>(map);
        this.normalization = normalization;
    }

    public CommonLossEdgeScorer(HashMap<MolecularFormula, Double> map) {
        this(map, 0d);
    }

    @Override
    public Object prepare(ProcessedInput input) {
        return null;
    }

    public HashMap<MolecularFormula, Double> getMap() {
        return map;
    }

    public CommonLossEdgeScorer merge(CommonLossEdgeScorer other) {
        final CommonLossEdgeScorer scorer = new CommonLossEdgeScorer(map, normalization);
        scorer.map.putAll(other.map);
        return scorer;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        return score(loss.getFormula());
    }

    @Override
    public double score(MolecularFormula formula) {
        final Double score = map.get(formula);
        if (score == null) return -normalization;
        else return score.doubleValue()-normalization;
    }
}
