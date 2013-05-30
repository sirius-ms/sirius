package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
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

    public final static String[] optimizedList = new String[]{

            "HO3P", "CO", "C6H2", "Br", "C3H6", "H2O2", "HF", "H2O", "HBr", "CHN", "Cl", "H2S", "S", "HI", "O2S", "CH4O", "CH2", "C4H2", "CH5N", "CH3", "C2H4", "HFO", "HO", "C3H4", "C2HN", "I", "H3N", "C2H2", "CH4", "HCl", "C2H2O", "C6H6", "CH3N", "CH2O"

    };
    public final static double[] optimizedListScores = new double[]{

            1.508129002151327d, 2.2512917986064953d, 0.9574089776736818d, 0.8114149359105298d, 2.5851341048677208d, 1.2906136708740301d, 1.7775222832818514d, 2.3899888612834506d, 2.5009906961107125d, 2.891000833728227d, 0.7257054288034636d, 3.3582994439346434d, 0.43767013265039273d, 1.791759469228055d, 0.38416753567274053d, 0.10857803854077959d, 0.8886392272821769d, 0.1139588042526148d, 0.4054651081081644d, 2.5725297528642983d, 1.9401613201499475d, 0.18846151265655106d, 0.7852807375913329d, 1.8299983410562994d, 0.12906389595956586d, 2.916098122484529d, 1.1106606157630348d, 0.48080149651556436d, 1.1422121457072585d, 1.1209957908235615d, 0.49074069992048697d, 2.66258780484698d, 0.505935619187943d, 1.3958626871960569d

    };

    public final static double OPTIMIZED_NORMALIZATION = 1.7332494369066007d;





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
        for (int i=0; i < learnedList.length; ++i) {
            final MolecularFormula formula = MolecularFormula.parse(optimizedList[i]);
            map.put(formula, optimizedListScores[i]); // learned losses
        }
        return new CommonLossEdgeScorer(map, 0d);
    }

    public static CommonLossEdgeScorer getOptimizedCommonLossScorer() {
        final HashMap<MolecularFormula, Double> map = new HashMap<MolecularFormula, Double>();
        for (int i=0; i < learnedList.length; ++i) {
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
                            (left.getScore()+right.getScore())/4;
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
        return new CommonLossEdgeScorer(newMap);
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
        final CommonLossEdgeScorer scorer = new CommonLossEdgeScorer(map);
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
