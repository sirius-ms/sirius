import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer

object EvalApp extends App{
  //new Evaluation(args(0)).fragmentProperties()
  //val n = new Normalization(args(0), ChemicalCompoundScorer.createMassDependendFormulaScorer(100, ChemicalCompoundScorer.createDefaultCompoundScorer(true)))
  //n.getMassDevsSd()
  val s = ChemicalCompoundScorer.createDefaultCompoundScorer(true)
  println(new Normalization("D:/daten/arbeit/analysis/results/merged", s).getNormalizationConstantForRoot)
}
