import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer
import de.unijena.bioinf.ChemistryBase.chem.{Element, PeriodicTable, MolecularFormula}
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer
import de.unijena.bioinf.ChemistryBase.math.LogNormalDistribution
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.{CommonLossEdgeScorer, LossSizeScorer, FreeRadicalEdgeScorer, CommonFragmentsScore}
import scala.collection._
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

    println(ln)

    // special elements
    val specialLossMap = mutable.Map[MolecularFormula, Double]()
    specialLossMap ++= cl
    val elements = List("Br", "Cl", "P", "F", "I", "S")
    for (element <- elements) {
      val elem = PeriodicTable.getInstance().getByName(element)
      var losses = new LossSize(args(0), Some(elem)).commonLosses
      var n=0
      for (i <- (1 to 5)) {
        n = losses.foldLeft(0)(_+_.count)
        losses = losses.map(l=>{
          val c=cl.get(l.loss) match {
            case Some(_) => Math.min((ln.density(l.loss)*n).round.toInt, l.count)
            case None => l.count
          }
          LossObservation(l.loss, c, 0)
        })
      }
      n = losses.foldLeft(0)(_+_.count)
      val specialLosses = losses.filter(p=>p.loss.numberOf(elem) > 0)
      for (specialLoss <- specialLosses) {
        val expectedFrequency = ln.density(specialLoss.loss)
        val expectedCount = Math.max(1,(expectedFrequency*n).round)
        if (specialLoss.count - expectedCount >= 10) {
          val d = specialLossMap.get(specialLoss.loss) match {
            case Some(x) => x
            case None => 0
          }
          specialLossMap += (specialLoss.loss -> Math.max(d, specialLoss.count.toFloat / expectedCount))
          println((specialLoss.loss -> Math.max(d, Math.log(specialLoss.count.toFloat / expectedCount))))
        }
      }
    }
    ln.printCommonLossesWithOriginalFrequency(prefix, specialLossMap.filter(pair=>element.forall(x=>pair._1.numberOf(x)>0)), ls.commonLosses)
    println("----------")
    val clfs = specialLossMap.keySet
    println("public final static String[] optimizedList = new String[]{\n\t")
    println(clfs.map(x=>"\""+x+"\"").mkString(", "))
    println("\n};")
    println("public final static double[] optimizedListScores = new double[]{\n\t")
    println(clfs.map(x=>Math.log(specialLossMap.get(x).get)+"d").mkString(", "))
    println("\n};")
  }

  def normalization = {
    val scorers = List(
      new LossSizeScorer(LogNormalDistribution.withMeanAndSd(3.8885317749758523d, 0.5729427497241325d), 0),
      CommonLossEdgeScorer.getOptimizedCommonLossScorerWithoutNormalization()
    )
    //val scorer = new LossSizeScorer(LogNormalDistribution.withMeanAndSd(3.8917028997544745, 0.5912946817526695), 0)

    scorers.foreach(s => {
      val n = new Normalization(args(0),s)
      println(s.getClass.getSimpleName + ": " + n.getNormalizationConstantForLosses())
    })

  }

  commonLosses
  //normalization
}
