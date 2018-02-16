//package de.unijena.bioinf.GibbsSampling.model.scorer;
//
//import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
//import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
//import de.unijena.bioinf.GibbsSampling.model.Candidate;
//import de.unijena.bioinf.GibbsSampling.model.EdgeScorer;
//import de.unijena.bioinf.ftalign.StandardScoring;
//import de.unijena.bioinf.treealign.multijoin.DPMultiJoin;
//import de.unijena.bioinf.treealign.scoring.Scoring;
//import de.unijena.bioinf.treealign.sparse.DPSparseTreeAlign;
//import gnu.trove.map.hash.TObjectDoubleHashMap;
//
//public class FTAlignScorer implements EdgeScorer {
//    protected Scoring<Fragment> scoring;
//    protected String name;
//    private boolean useMultijoins;
//    private final boolean normalize;
//    private TObjectDoubleHashMap<Candidate> norm;
//
//    public FTAlignScorer(boolean normalize) {
//        this(new StandardScoring(true), normalize);
//    }
//
//    public FTAlignScorer(Scoring<Fragment> s, boolean normalize) {
//        this.scoring = s;
//        this.name = this.name;
//        this.useMultijoins = true;
//        this.normalize = normalize;
//    }
//
//    public static FTAlignScorer withoutFragments(boolean normalize) {
//        StandardScoring scoring = new StandardScoring(true);
//        scoring.matchScore = scoring.scoreForEachNonHydrogen = scoring.missmatchPenalty = scoring.penaltyForEachNonHydrogen = 0.0F;
//        scoring.matchScore = 0.25F;
//        return new FTAlignScorer(scoring, normalize);
//    }
//
//    public static FTAlignScorer realWithoutFragments(boolean normalize) {
//        return new FTAlignScorer(new StandardScoring(false), normalize);
//    }
//
//    public static FTAlignScorer withFragments(boolean normalize) {
//        return new FTAlignScorer(new StandardScoring(true), normalize);
//    }
//
//    public static FTAlignScorer withDeletionPenalty(boolean normalize) {
//        StandardScoring scoring = new StandardScoring(true);
//        scoring.gapScore = -2.0F;
//        scoring.penaltyForEachJoin = -0.25F;
//        scoring.missmatchPenalty = -1.0F;
//        scoring.lossMissmatchPenalty = -1.0F;
//        return new FTAlignScorer(scoring, normalize);
//    }
//
//    private double align(FTree left, FTree right) {
//        if(this.useMultijoins) {
//            DPMultiJoin aligner1 = new DPMultiJoin(this.scoring, 3, left.getRoot(), right.getRoot(), FTree.treeAdapter());
//            return (double)aligner1.compute();
//        } else {
//            DPSparseTreeAlign aligner = new DPSparseTreeAlign(this.scoring, true, left.getRoot(), right.getRoot(), FTree.treeAdapter());
//            return (double)aligner.compute();
//        }
//    }
//
//    private double selfAlign(FTree tree) {
//        return this.useMultijoins?this.align(tree, tree):(double)this.scoring.selfAlignScore(tree.getRoot());
//    }
//
//    public void prepare(Candidate[][] candidates) {
//        System.out.println("prepare align");
//        if(this.normalize) {
//            this.norm = new TObjectDoubleHashMap(candidates.length, 0.75F);
//            Candidate[][] var2 = candidates;
//            int var3 = candidates.length;
//
//            for(int var4 = 0; var4 < var3; ++var4) {
//                Candidate[] candidateArray = var2[var4];
//                Candidate[] var6 = candidateArray;
//                int var7 = candidateArray.length;
//
//                for(int var8 = 0; var8 < var7; ++var8) {
//                    Candidate candidate = var6[var8];
//                    this.norm.put(candidate, this.selfAlign(candidate.getTree()));
//                }
//            }
//        }
//
//        System.out.println("prepare align finished");
//    }
//
//    public double score(Candidate candidate1, Candidate candidate2) {
//        double score;
//        try {
//            score = this.align(candidate1.getTree(), candidate2.getTree());
//            double e = this.align(candidate2.getTree(), candidate1.getTree());
//            if(score != e) {
//                System.out.println("FTalign : " + candidate1.getExperiment().getName() + "(" + candidate1.getFormula() + ") vs " + candidate2.getExperiment().getName() + "(" + candidate2.getFormula() + ")");
//                System.out.println("size " + candidate1.getTree().numberOfVertices() + " " + candidate2.getTree().numberOfVertices());
//                System.out.println(score + " vs " + e);
//            }
//        } catch (Exception var7) {
//            var7.printStackTrace();
//            System.out.println("FTreeAlignment failed for : " + candidate1.getExperiment().getName() + "(" + candidate1.getFormula() + ") vs " + candidate2.getExperiment().getName() + "(" + candidate2.getFormula() + ")");
//            System.out.println("ionizations: " + candidate1.getIonMode().toString() + " " + candidate2.getIonMode().toString());
//            throw var7;
//        }
//
//        return this.normalize?score / Math.max(this.norm.get(candidate1), this.norm.get(candidate2)):score;
//    }
//
//    public void clean() {
//        this.norm.clear();
//        this.norm = null;
//    }
//
//    public double[] normalization(Candidate[][] candidates) {
//        return null;
//    }
//}
