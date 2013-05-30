import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer
import de.unijena.bioinf.ChemistryBase.chem.{Element, PeriodicTable, MolecularFormula}
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer
import de.unijena.bioinf.ChemistryBase.math.LogNormalDistribution
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.{LossSizeScorer, FreeRadicalEdgeScorer, CommonFragmentsScore}
import scalax.io.Resource

object EvalApp extends App{

  def commonLosses={
    val element:Option[Element] = None;//Some("F").map(x=>PeriodicTable.getInstance().getByName(x))

    val ls = new LossSize(args(0), element)
    println(ls.commonLosses.foldLeft(0)((s,x)=>s+x.count))
    val ln = new LogNormDistributionLossSize()
    var cm:Traversable[LossObservation] = ls.commonLosses
    ln.learn(cm)
    println(ln)
    var cl = ln.learnCommonLosses(ls.commonLosses)
    for (i <- 1 to 20) {
      println("-----------------")
      cm = ln.adjustCounts(cm)
      ln.learn(cm)
      println(ln)
      println(ln.count(cm))
      cl = ln.learnCommonLosses(cm, cl)
    }
    val prefix =  element match {
      case Some(name) => name.toString + "_"
      case None => ""
    }

    ln.printAdjustedLosses(prefix, cm)
    ln.printCommonLossesWithOriginalFrequency(prefix, cl.filter(pair=>element.forall(x=>pair._1.numberOf(x)>0)), ls.commonLosses)
    println(ln)
  }

  def normalization = {
    val scorer = new LossSizeScorer(new LogNormalDistribution(3.4484318558075935, 1.070374352318858), 0)
    val n = new Normalization(args(0),scorer)
    println(n.getNormalizationConstant())
  }

  normalization
}
