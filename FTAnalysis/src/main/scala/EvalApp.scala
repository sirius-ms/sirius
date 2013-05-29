import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer

object EvalApp extends App{
  val ls = new LossSize(args(0))
  val ln = new LogNormDistribution()
  var cm:Traversable[LossObservation] = ls.commonLosses
  ln.learn(cm)
  var cl = ln.learnCommonLosses(ls.commonLosses)
  for (i <- 1 to 20) {
    println("-----------------")
    cm = ln.adjustCounts(cm)
    ln.learn(cm)
    cl = ln.learnCommonLosses(cm, cl)
  }
  ln.printAdjustedLosses(cm)
  ln.printCommonLosses(cl)
  println(ln)

}
