import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer

object EvalApp extends App{
  //new Evaluation(args(0)).fragmentProperties()
  //val n = new Normalization(args(0), ChemicalCompoundScorer.createMassDependendFormulaScorer(100, ChemicalCompoundScorer.createDefaultCompoundScorer(true)))
  //n.getMassDevsSd()
  new CommonFragments("D:/daten/arbeit/analysis/results/merged").fragmentChemicalPrior
}
