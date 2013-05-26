import au.com.bytecode.opencsv.CSVReader
import collection._
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution
import java.io.{FileReader, File}
import scala.collection.JavaConversions._
import Math._

class LossSize(val root:String) {

  def lossPath = new File(root, "correctLosses.csv")

  val commonLosses = {
    val map = mutable.Map[MolecularFormula, Int]()
    val csvr = new CSVReader(new FileReader(lossPath))
    csvr.readNext()
    var line = csvr.readNext()
    while (line != null) {
      line = csvr.readNext()
      val formula = MolecularFormula.parse(line(1))
      map.get(formula) match {
        case Some(counting) => map += formula->(counting+1)
        case None => map += formula->1
      }
      line = csvr.readNext()
    }
    csvr.close()
    val sum = map.values.sum
    map.map(x =>LossObservation(x._1, x._2, x._2/sum.toDouble))
  }

  val n = commonLosses.foldLeft(0)(_+_.count)

  def learnCommonLosses() = {
    val pareto = learnPareto(commonLosses)
    // search losses which are more frequently as they should be
    val tolerance = n*0.001
    commonLosses.map(o=>(o, pareto.getDensity(o.loss.getMass)*n)).filter(p=>(p._1.count-p._2)>=10)
  }

  def learnUncommonLosses() = {
    val pareto = learnPareto(commonLosses)
    // search losses which are more frequently as they should be
    val tolerance = n*0.001
    commonLosses.map(o=>(o, pareto.getDensity(o.loss.getMass)*n)).filter(p=>(p._1.count-p._2) <= -10)
  }

  def adjustCommonLosses() = {
    val map = mutable.Map[MolecularFormula, LossObservation]()
    commonLosses.foreach(u=>map += u.loss->u)
    learnCommonLosses.foreach(l => map(l._1.loss) = LossObservation(l._1.loss, l._2.round.toInt, l._2/n ))
    map.values
  }

  def learnPareto(xs:Traversable[LossObservation]):ParetoDistribution = {
    val minimum:Double = xs.minBy(f=>f.loss.getMass).loss.getMass
    val a:Double = n.toDouble / (xs.foldLeft(0d)((v, o) => v + o.count * log(o.loss.getMass/minimum)))
    new ParetoDistribution(a, minimum)
  }


}
