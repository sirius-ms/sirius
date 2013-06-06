import au.com.bytecode.opencsv.CSVReader
import collection._
import de.unijena.bioinf.ChemistryBase.chem.{Element, MolecularFormula}
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution
import java.io.{FileReader, File}
import scala.collection.JavaConversions._
import Math._

class LossSize(val root:String, val specialElement:Option[Element]) {

  def lossPath = new File(root, "correctLosses.csv")

  val SMALL = 40

  val compounds = {
    val map = mutable.Map[String, MolecularFormula]()
    val csvr = new CSVReader(new FileReader(new File(root, "ranking.csv")))
    csvr.readNext()
    var line = csvr.readNext()
    while (line != null) {
      val name = line(0)
      val formula = line(1)
      map += name->MolecularFormula.parse(formula)
      line = csvr.readNext()
    }
    map
  }

  val commonLosses = {
    val map = mutable.Map[MolecularFormula, Int]()
    val csvr = new CSVReader(new FileReader(lossPath))
    csvr.readNext()
    var line = csvr.readNext()
    while (line != null) {
      val name = line(0)
      val pass = specialElement match {
        case Some(element) => compounds.get(name).get.numberOf(element)>0
        case None => true
      }
      if (pass) {
        val formula = MolecularFormula.parse(line(1))
        map.get(formula) match {
          case Some(counting) => map += formula->(counting+1)
          case None => map += formula->1
        }
      }
      line = csvr.readNext()
    }
    csvr.close()
    val sum = map.values.sum
    map.map(x =>LossObservation(x._1, x._2, x._2/sum.toDouble))
  }

  val n = commonLosses.foldLeft(0d)(_+_.count).round.toInt

  def learnCommonLosses() = {
    val pareto = learnPareto(commonLosses)
    // search losses which are more frequently as they should be
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

  def meanSquare(xs:Seq[Double], ys:Seq[Double]) = {
    val x_ = xs.foldLeft(0d)(_+_)/xs.size
    val y_ = ys.foldLeft(0d)(_+_)/ys.size
    val b = (0 to xs.size).foldLeft(0d)((s,i)=>s+(xs(i) - x_)*(ys(i) - y_)) / xs.foldLeft(0d)((s,x)=>s+(x-x_)*(x-x_))
    (y_, b)
  }

  def learnLossSizeDistribution(xs:Traversable[LossObservation]):ParetoLossSizeDistribution = {
    val pareto = learnPareto(xs.filter(x=>x.loss.getMass>=SMALL))
    // binne Massen in 25 Dalton Paare für Least-Square
    val bin0 = xs.filter(x=>x.loss.getMass<20)
    val bin1 = xs.filter(x=>x.loss.getMass<SMALL && x.loss.getMass >= 20)
    val x0 = 12.5
    val x1 = 23.5
    val y0 = bin0.foldLeft(0d)((s,x)=> s + x.count)/n.toDouble
    val y1 = bin1.foldLeft(0d)((s,x)=> s + x.count)/n.toDouble
    val a=(y1-y0)/(x1-x0)
    val b = y1-a*x1
    ParetoLossSizeDistribution(pareto.getK, SMALL, a, b)
  }


}
