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
    /*
    public final static String[] optimizedList = new String[]{

            "C2H2", "Cl", "HCl", "CH2", "O", "HF", "HO3P", "H3N", "CS", "ClN", "C6H6", "C6H5Cl", "CH3", "I", "C2H4", "CH4", "Br", "HN", "HBr", "H2S", "HI", "S", "CO", "H2", "OS", "HO2PS", "O2S", "C3H4", "H2O"

    };
    public final static double[] optimizedListScores = new double[]{

            2.450592466647284d, 2.6322323647885035d, 3.585635090866105d, 5.061869416500993d, 1.98735108678975d, 4.219990206399166d, 4.81556101339377d, 1.712137433054253d, 2.193608226046093d, 0.9675789664272824d, 1.4916520308347205d, 2.0064039882484526d, 2.434693632787536d, 6.1359239584496095d, 0.7300546087164697d, 0.5300547398834722d, 3.477002088748983d, 2.436610178384473d, 4.603462237399737d, 1.8124350160491391d, 5.470340264205764d, 3.4889242233884845d, 2.4718950405077442d, 8.48154326479735d, 0.853549512465558d, 3.1639280772330727d, 1.9797429997262888d, 0.5606299982203099d, 3.379679499949444d

    };
    */
    public final static String[] optimizedList = new String[]{

            "C2H2", "CH2O", "Cl", "HCl", "CH2", "O", "HF", "HO3P", "H3N", "CS", "CHN", "C6H6", "C6H5Cl", "CH3", "I", "C6H2", "C2H4", "CH4", "Br", "HN", "HBr", "H2S", "HI", "S", "C3H6", "H2", "CO", "OS", "HO2PS", "O2S", "H2O"

    };
    public final static double[] optimizedListScores = new double[]{

            3.1115834530893713d, 0.8547475112700744d, 2.186994541286029d, 4.000404005539266d, 5.152849508793085d, 2.3026794488605953d, 4.394989165008551d, 4.158832761897171d, 2.215461300398177d, 0.9255949091155629d, 2.621493900166479d, 2.4518448047689096d, 1.9449601762777768d, 2.8756059927785835d, 6.808501205457337d, 1.8765183378441672d, 1.7676817930869375d, 1.6963889484398127d, 4.004985663655303d, 1.7622285584877952d, 4.35814181247161d, 2.0913417146783644d, 6.491436180021818d, 3.036081733487893d, 0.3665414365039033d, 2.9979605738824886d, 3.4406465075926307d, 1.4527550116797592d, 3.6353509359353113d, 1.7533763404422238d, 3.245636488867119d

    };

    public final static double OPTIMIZED_NORMALIZATION = 2.8105910127346685d;





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
