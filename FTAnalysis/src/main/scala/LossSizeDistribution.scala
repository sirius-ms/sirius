import collection._
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula
import java.io.File
import scalax.io.Resource

abstract class LossSizeDistribution {

  def LIMIT(m:Double) = if (m < 20) 5 else 10

  def QUOTA(m:Double) = if (m < 20) 1.5d else 1.33d

  def learn(xs:Traversable[LossObservation]):Unit

  def count(xs:Traversable[LossObservation]):Int = xs.foldLeft(0d)(_+_.count).round.toInt

  def probability(observation: MolecularFormula):Double

  def density(observation: MolecularFormula):Double

  def learnCommonLosses(xs:Traversable[LossObservation]):Map[MolecularFormula, Double] = learnCommonLosses(xs, Map())

  def learnCommonLosses(xs:Traversable[LossObservation], map:Map[MolecularFormula, Double]):Map[MolecularFormula, Double] = {
    val n = count(xs)
    def getfromMap(f:MolecularFormula) = map.get(f) match {
      case Some(x) => x
      case None => 1d
    }
    xs.view.map(x=>(x, density(x.loss)*n, density(x.loss))).filter(x=> (x._1.count - x._2) >= LIMIT(x._1.loss.getMass) && (x._1.count/x._2 >= QUOTA(x._1.loss.getMass))).foldLeft(mutable.Map()++map)((m,x)=>{
        if (!m.contains(x._1.loss)) println(x._1.loss, x._1.count,  x._2, x._1.count/x._2, Math.log(x._1.count/x._2))
        m += (x._1.loss -> getfromMap(x._1.loss) * (x._1.count.toFloat/x._2))
      }
    )
  }

  def adjustCounts(xs:Traversable[LossObservation]):Traversable[LossObservation] = {
    val n = count(xs)
    xs.map(l=> {
      val expectedFrequency = density(l.loss)
      val expectedCount = (expectedFrequency*n)
      if (l.count >= (expectedCount+LIMIT(l.loss.getMass))) {
        LossObservation(l.loss, expectedCount, expectedFrequency )
      } else {
        l
      }
    })
  }

  def printCommonLosses(map:Map[MolecularFormula, Double]) {
    new File("learnedCommonLosses.csv").delete();
    Resource.fromFile("learnedCommonLosses.csv").writeStrings(List("formula,score") ++ map.map(x=>x._1 + "," + Math.log(x._2)), "\n")
  }
  def printCommonLossesWithOriginalFrequency(prefix:String, map:Map[MolecularFormula, Double], xs:Traversable[LossObservation]) {
    new File(prefix + "learnedCommonLosses.csv").delete();
    val m = map.map(x => List(x._1, Math.log(x._2), xs.find(l=>l.loss==x._1).get.frequency, xs.find(l=>l.loss==x._1).get.count, x._1.getMass))
    Resource.fromFile(prefix + "learnedCommonLosses.csv").writeStrings(List("formula,score,frequency,count,mass") ++ m.map(_.mkString(",")), "\n")
  }
  def printAdjustedLosses(prefix:String, xs:Traversable[LossObservation]) {
    new File(prefix + "learnedLossSizeDistribution.csv").delete();
    Resource.fromFile(prefix + "learnedLossSizeDistribution.csv").writeStrings(List("loss,mass,count,frequency") ++ xs.map(x=>x.loss.toString + "," + x.loss.getMass + "," +
    x.count + "," + x.frequency), "\n")
  }

}
