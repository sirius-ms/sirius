import de.unijena.bioinf.babelms.dot.Parser
import de.unijena.bioinf.babelms.GenericParser
import de.unijena.bioinf.babelms.ms.JenaMsParser
import de.unijena.bioinf.ChemistryBase.chem.utils.{ScoredMolecularFormula, MolecularFormulaScorer}
import de.unijena.bioinf.ChemistryBase.chem.{Ionization, Element, PeriodicTable, MolecularFormula}
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer
import de.unijena.bioinf.ChemistryBase.math.{MathUtils, LogNormalDistribution}
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring._
import de.unijena.bioinf.FragmentationTreeConstruction.model.{GraphFragment, Loss}
import java.io.{IOException, FileReader, File, PrintStream}
import collection._
import scala.Predef._
import scala.Some
import scala.Some
import scala.Some
import scalax.io.Resource
import scala.collection.JavaConversions._
import util.Random

object EvalApp extends App{

  val USE_INTENSITY = true

  def commonLosses(specialElements:Boolean)={
    val element:Option[Element] = None;//Some("F").map(x=>PeriodicTable.getInstance().getByName(x))

    val ls = new LossSize(args(0), element)
    println(ls.commonLosses.foldLeft(0d)((s,x)=>s+x.count))
    val ln = new LogNormDistributionLossSize()
    var cm:Traversable[LossObservation] = ls.commonLosses
    ln.learn(cm)
    ln.printAdjustedLosses("pre_", cm)
    println(ln)
    var cl = ln.learnCommonLosses(ls.commonLosses)
    for (i <- 1 to 30) {
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

    val specialElemPenaltyMap = mutable.Map[Element, Double]()

    // special elements
    val specialLossMap = mutable.Map[MolecularFormula, Double]()
    specialLossMap ++= cl
    val elements = List("Br", "Cl", "P", "F", "I", "S")
    for (element <- elements) {
      val elem = PeriodicTable.getInstance().getByName(element)
      var losses = new LossSize(args(0), Some(elem)).commonLosses
      var n=0
      /*
      for (i <- (1 to 5)) {
        n = losses.foldLeft(0d)(_+_.count).round.toInt
        losses = losses.map(l=>{
          val c=cl.get(l.loss) match {
            case Some(_) => Math.min((ln.density(l.loss)*n), l.count)
            case None => l.count
          }
          LossObservation(l.loss, c, 0)
        })
      }
      */
      n = losses.foldLeft(0d)(_+_.count).round.toInt
      val specialLosses = losses.filter(p=>p.loss.numberOf(elem) > 0)
      val lossN = specialLosses.foldLeft(0d)(_+_.count).round.toInt
      specialElemPenaltyMap += (elem -> Math.log(lossN.toFloat/n))
      for (specialLoss <- specialLosses) {
        val expectedFrequency = ln.density(specialLoss.loss)
        val expectedCount = Math.max(1e-6,(expectedFrequency*n))
        val specialCountExpectation = Math.max(1e-6, (expectedFrequency*lossN))
        println(specialLoss.loss+": " + specialLoss.count + " / " + expectedCount +  " / " + specialCountExpectation + " => " + Math.log(specialLoss.count/specialCountExpectation))
        val diff = if (!specialElements) specialLoss.count - expectedCount else specialLoss.count - specialCountExpectation
        val quota = if (!specialElements) specialLoss.count / expectedCount else specialLoss.count / specialCountExpectation

        if (diff > 3 && quota >= 1.66) {
          val d = specialLossMap.get(specialLoss.loss) match {
            case Some(x) => x
            case None => 0
          }
          specialLossMap += (specialLoss.loss -> Math.max(d, quota))
          println((specialLoss.loss -> Math.max(d, Math.log(specialLoss.count.toFloat / specialCountExpectation))))
        }
      }
    }
    ln.printCommonLossesWithOriginalFrequency(prefix, specialLossMap.filter(pair=>element.forall(x=>pair._1.numberOf(x)>0)), ls.commonLosses)
    println(specialElemPenaltyMap)
    println(specialElemPenaltyMap.values.sum / specialElemPenaltyMap.size.toFloat)
    val penalty = specialElemPenaltyMap.values.min
    println(penalty)
    val boni = mutable.Map[MolecularFormula, Double]()
    specialLossMap.foreach(f=>{
      val es = specialElemPenaltyMap.filter(e=>f._1.numberOf(e._1)>0)
      if (!es.isEmpty) {
        val pen = es.minBy(e=>e._2)._2
        val bonus = -penalty + pen
        boni.put(f._1, f._2 * Math.exp(bonus))
      }
    })
    println(boni)
    specialLossMap ++= boni
    println(specialLossMap)
    println(ln.mean + "d, " + ln.sd + "d")
    println("----------")
    val clfs = specialLossMap.toList.sortBy(y => -(y._2))
    println("public final static String[] optimizedList = new String[]{\n\t")
    println(clfs.map(x=>"\""+x._1+"\"").mkString(", "))
    println("\n};")
    println("public final static double[] optimizedListScores = new double[]{\n\t")
    println(clfs.map(x=>Math.log(specialLossMap.get(x._1).get)+"d").mkString(", "))
    println("\n};")

  }

  def normalization = {
    val scorers = List(
      new LossSizeScorer(LossSizeScorer.LEARNED_DISTRIBUTION, 0),
      CommonLossEdgeScorer.getOptimizedCommonLossScorerWithoutNormalization(),
    new StrangeElementScorer(StrangeElementScorer.LEARNED_PENALTY, 0d)
    )
    //val scorer = new LossSizeScorer(LogNormalDistribution.withMeanAndSd(3.8917028997544745, 0.5912946817526695), 0)

    scorers.foreach(s => {
      val n = new Normalization(args(0),s)
      println(s.getClass.getSimpleName + ": " + n.getNormalizationConstantForLosses())
    })
  }

  def lossNorm = {
    val scorer = CommonLossEdgeScorer.getOptimizedCommonLossScorerWithoutNormalization
    val n = new Normalization(args(0),scorer)
    println("all: " + n.getNormalizationConstantForLosses())
    List("Br", "Cl", "P", "F", "I", "S").foreach(e=>{
      println(e + ": " + n.getNormalizationConstantForLosses(PeriodicTable.getInstance().getByName(e)))
    })
  }

  def ppms = {
    val n = new Normalization(args(0),null)
    prints("ppms_metlin2.csv", p=>{
      p.println("mz,abs,ppm, int")
      n.correctTrees.filter(f=>f.getName.startsWith("m")).flatMap(t=>n.getFragmentsFromTree(t)).foreach(x=>p.println(x.mz + "," + (x.ppm*1e-6*x.mz) + "," + x.ppm + ","+ x.intensity))
    })
    prints("ppms_metlin2_wrong.csv", p=>{
      p.println("mz,abs,ppm, int")
      n.wrongTrees.filter(f=>f.getName.startsWith("m")).flatMap(t=>n.getFragmentsFromTree(t)).foreach(x=>p.println(x.mz + "," + (x.ppm*1e-6*x.mz) + "," + x.ppm + ","+ x.intensity))
    })
    /*
    prints("ppms_agilent2.csv", p=>{
      p.println("mz,abs,ppm, int")
      n.correctTrees.filter(f=>f.getName.startsWith("a")).flatMap(t=>n.getFragmentsFromTree(t)).foreach(x=>p.println(x.mz + "," + (x.ppm*1e-6*x.mz) + "," + x.ppm + ","+ x.intensity))
    })
    */
  }

  def foobar = {
    val n = new Normalization("D:\\daten\\arbeit\\eval\\lowts", null)
    val losslist = CommonLossEdgeScorer.optimizedList++ List("Br", "F", "Cl", "H", "H2")
    val nearlyCommonFragments = CommonFragmentsScore.COMMON_FRAGMENTS.sliding(2,2).flatMap(f => {
      val frag = MolecularFormula.parse(f(0).asInstanceOf[String])
      losslist.map(l=>MolecularFormula.parse(l).add(frag))
    }).toSet -- CommonFragmentsScore.COMMON_FRAGMENTS.sliding(2,2).map(x=>MolecularFormula.parse(x(0).asInstanceOf[String]))
    val counter = mutable.Map[MolecularFormula,Int]() ++= nearlyCommonFragments.map(x=>(x,0))
    val numbers = n.correctTrees.map(f => {
      val tree = n.getFragmentsFromTree(f)
      if (tree.size==0) {
        -1
      } else {
        val xs = tree.map(_.formula).filter(nearlyCommonFragments)
        xs.foreach(x=>counter.update(x, counter.getOrElse(x,0)+1))
        xs.size.toFloat/tree.size
      }
    }).filter(_>=0)
    val otherNumbers = n.wrongTrees.map(f => {
      val tree = n.getFragmentsFromTree(f)
      if (tree.size==0) {
        -1
      } else {
        tree.map(_.formula).count(nearlyCommonFragments).toFloat/tree.size
      }
    }).filter(_>=0)
    println(numbers.sum/numbers.size.toFloat)
    println(otherNumbers.sum/otherNumbers.size.toFloat)
    println(counter.filter(_._2>=100))
  }

  def strangeLosses = {
    val n = new Normalization(args(0), null)
    val regexp = """[S|P|B|l|I|F]""".r
    def notChnops(s:String) = !regexp.findFirstIn(s).isEmpty
    val map = mutable.Map[MolecularFormula,Int]()
    val ys = n.correctTrees.filter(f=>notChnops(n.getRootsFromTree(f).formula.toString))
    val xs = Random.shuffle(n.wrongTrees.filter(f=>notChnops(n.getRootsFromTree(f).formula.toString)).toList).take(ys.size)

    xs.foreach(file=>n.getLossesFromTree(file).filter(notChnops).foreach(s=>{
      val form = MolecularFormula.parse(s)
      map put(form, map.getOrElse(form, 0)+1)
    }))
    // lösche alle Losses die in richtigen Bäumen vorkommen
    val alsoInCorrect = mutable.Map[MolecularFormula,Int]()
    ys.foreach(file=>n.getLossesFromTree(file).filter(notChnops).foreach(s=>{
      val form = MolecularFormula.parse(s)
      alsoInCorrect put(form, alsoInCorrect.getOrElse(form, 0)+1)
    }))
    println("-----WRONG-----")
    println(map -- alsoInCorrect.keys)
    println("-----CORRECT-----")
    println(alsoInCorrect)
    prints("strangeLosses.csv", (p)=>{
      p.println("formula,mass,count")
      alsoInCorrect.foreach(x=>p.println(List(x._1, x._1.getMass, x._2).mkString(", ")))
    })

  }

  def prints(s:String, p:(PrintStream)=>Unit) {
    print(new File(s), p)
  }

  def print(f:File, p:(PrintStream)=>Unit) {
    val printer = new PrintStream(f)
    try {p(printer)} finally {printer.close()}
  }

  def fragmentChemicalProperties = {
    val prior = ChemicalCompoundScorer.createDefaultCompoundScorer
    /* new MolecularFormulaScorer {
      def score(formula: MolecularFormula): Double = {
        return Math.log(MathUtils.pdf(formula.hetero2CarbonRatio, 0.5886335, 0.5550574))
      }
    } */

    val scorer = new ChemicalPriorEdgeScorer(prior, 0d)
    var scoreAvg = 0d
    var c=0
    val n = new Normalization(args(0),null)
    val rdbes = mutable.Buffer[Double]()
    def calc(fs:Traversable[File]) = {
      fs.foreach(f=>{
        n.getFragmentsFromTree(f).foreach(g=>rdbes+=g.formula.rdbe())
        /*
        graph.getEdges.foreach(e => {
          /*
          val u = MolecularFormula.parse(graph.getVertex(e.getHead).getProperties.get("label"))
          val v = MolecularFormula.parse(graph.getVertex(e.getTail).getProperties.get("label"))
          val us = Math.max(Math.log(0.0001), prior.score(u))
          val vs = Math.max(Math.log(0.0001), prior.score(v))
          scoreAvg += Math.min(0, vs-us)
          c += 1
          */
        })
        */
      })
    }
    calc(n.correctTrees)
    println(n.mean(rdbes))
    println(n.sd(rdbes))
    rdbes.clear()
    calc(n.wrongTrees)
    println(n.mean(rdbes))
    println(n.sd(rdbes))
  }

  def chemprobs()={
    new ChemProp(args(0))
  }

  def findCommonFragments = {
    val counter = mutable.Map[MolecularFormula,Int]()
    val n = new Normalization("D:\\daten\\arbeit\\eval\\lowts", null)
    val numbers = n.correctTrees.foreach(f => {
      val tree = n.getFragmentsFromTree(f)
      tree.foreach(g=>counter.update(g.formula, counter.getOrElse(g.formula, 0)+1))
    })
    val cf = counter.filter(x=>x._2>=100)
    println(cf)
    println(cf.size)
    val bigcf = cf.filter(x=>x._1.getMass>100)
    println(bigcf)
    println(bigcf.size)
    val average = (counter -- bigcf.keySet).foldLeft(0)(_+_._2).toFloat/counter.size
    // BIG
    println(average)
    println(bigcf.flatMap(x=>List("\"" + x._1 + "\"", Math.log(x._2/average))).mkString(", "))
    val printer = new PrintStream(new File("fragmentliste.csv"))
    printer.println("formula,mass,count,score")
    counter.filter(x=>x._2>=10).foreach(x=>printer.println(List("\"" + x._1 + "\"", x._1.getMass, x._2, Math.log(x._2/average)).mkString(", ")))
    printer.close()
  }

  def rootNorm = {
    val n = new Normalization(args(0), ChemicalCompoundScorer.createDefaultCompoundScorer(true))
    println(n.getNormalizationConstantForRoot())
  }

  //strangeLosses
  //fragmentChemicalProperties
  //val n = new Normalization(args(0), CommonFragmentsScore.map( CommonFragmentsScore.COMMON_FRAGMENTS_NEW :_*))
  //println(n.getNormalizationConstant())

  //findCommonFragments
  //commonLosses(true)
  //strangeLosses
  normalization
  //new NoiseLearner("D:\\daten\\arbeit\\analysis\\agilent\\train").analyze()
  //new NoiseLearner("D:\\daten\\arbeit\\analysis\\metlin\\train").analyze()

  //chemprobs
  //rootNorm

  //ppms
}
