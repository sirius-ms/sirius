import de.unijena.bioinf.babelms.dot.Parser
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula
import de.unijena.bioinf.ChemistryBase.chem.utils.MolecularFormulaScorer
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer
import java.lang.RuntimeException
import java.util
import scala.collection.JavaConversions._
import java.io.{FileReader, File}
import scala.RuntimeException
import scalax.file.Path
import scalax.io.Resource

class Normalization(val root:String, val scorer:MolecularFormulaScorer) {

  def getNormalizationConstantForSmallCompounds():(Double, Double) = {
    val xs = (new File(root, "correctOptimalTrees").listFiles() ++ new File(root, "correctSuboptimalTrees").listFiles()).flatMap(f => {
      getFragmentsFromTree(f).filter(_.formula.getMass>100).map(m=>scorer.score(m.formula))
    })
    val ys = (new File(root, "wrongOptimalTrees").listFiles() ++ new File(root, "wrongSuboptimalTrees").listFiles()).flatMap(f => {
      getFragmentsFromTree(f).filter(_.formula.getMass>100).map(m=>scorer.score(m.formula))
    })
    (xs.reduce(_+_)/xs.size.toDouble, ys.reduce(_+_)/ys.size.toDouble)
  }

  def getNormalizationConstant():(Double, Double) = {
    val xs = (new File(root, "correctOptimalTrees").listFiles() ++ new File(root, "correctSuboptimalTrees").listFiles()).flatMap(f => {
      getFragmentsFromTree(f).map(m=>scorer.score(m.formula))
    })
    val ys = (new File(root, "wrongOptimalTrees").listFiles() ++ new File(root, "wrongSuboptimalTrees").listFiles()).flatMap(f => {
      getFragmentsFromTree(f).map(m=>scorer.score(m.formula))
    })
    (xs.reduce(_+_)/xs.size.toDouble, ys.reduce(_+_)/ys.size.toDouble)
  }

  def getNormalizationConstantForLosses():(Double, Double) = {
    val xs = (new File(root, "correctOptimalTrees").listFiles() ++ new File(root, "correctSuboptimalTrees").listFiles()).flatMap(f => {
      getLossesFromTree(f).map(m=>scorer.score(MolecularFormula.parse(m)))
    })
    val ys = (new File(root, "wrongOptimalTrees").listFiles() ++ new File(root, "wrongSuboptimalTrees").listFiles()).flatMap(f => {
      getLossesFromTree(f).map(m=>scorer.score(MolecularFormula.parse(m)))
    })
    (xs.reduce(_+_)/xs.size.toDouble, ys.reduce(_+_)/ys.size.toDouble)
  }

  def getNormalizationConstantForRoot():(Double, Double) = {
    val xs = (new File(root, "correctOptimalTrees").listFiles() ++ new File(root, "correctSuboptimalTrees").listFiles()).map(f => {
      scorer.score(getRootsFromTree(f).formula)
    })
    val ys = (new File(root, "wrongOptimalTrees").listFiles() ++ new File(root, "wrongSuboptimalTrees").listFiles()).map(f => {
      scorer.score(getRootsFromTree(f).formula)
    })
    (xs.reduce(_+_)/xs.size.toDouble, ys.reduce(_+_)/ys.size.toDouble)
  }

  def getMassDevsSd() {
    val xs = (new File(root, "correctOptimalTrees").listFiles() ++ new File(root, "correctSuboptimalTrees").listFiles()).flatMap(f => {
      getFragmentsFromTree(f).filter(_.mz>100).map(m=>m.ppm)
    })
    val ys = (new File(root, "wrongOptimalTrees").listFiles() ++ new File(root, "wrongSuboptimalTrees").listFiles()).flatMap(f => {
      getFragmentsFromTree(f).filter(_.mz>100).map(m=>m.ppm)
    })
    Resource.fromFile("correctTreesPPM.csv").writer.write(xs.mkString("\n"))
    Resource.fromFile("wrongTreesPPM.csv").writer.write(ys.mkString("\n"))
    println("++++++++++++++")
    println(sd(xs))
    println(sd(ys))

  }

  def sd(xs:Traversable[Double]) = {
    val e = xs.reduce(_+_)/xs.size.toDouble
    Math.pow(xs.map(x => Math.pow(x-e, 2)).reduce(_+_)/xs.size.toDouble , 0.5)
  }

  def getFragmentsFromTree(treeFile:File) = {
    val reader = new FileReader(treeFile)
    val graph = Parser.parse(reader)
    reader.close()
    val regexp = """(.+?)\\n(\d+\.\d+) Da, (\d+(?:\.\d+)) %\\nMassDev: (-?\d+(?:\.\d+)) ppm""".r
    graph.getEdges.view.map(e => graph.getVertex(e.getTail)).map(_.getProperties.get("label")).map(l=>
      regexp.findFirstIn(l) match {
        case Some(regexp(form, mz, inte, ppm)) => Fragment(MolecularFormula.parse(form), mz.toDouble, inte.toDouble, ppm.toDouble)
        case bla => throw new RuntimeException(l + " does not match =(\n" + bla)
      }
    )
  }

  def getLossesFromTree(treeFile:File) = {
    val reader = new FileReader(treeFile)
    val graph = Parser.parse(reader)
    reader.close()
    graph.getEdges.map(e=>e.getProperties.get("label"))
  }

  def getRootsFromTree(treeFile: File):Fragment = {
    val reader = new FileReader(treeFile)
    val graph = Parser.parse(reader)
    reader.close()
    val regexp = """(.+?)\\n(\d+\.\d+) Da, (\d+(?:\.\d+)) %\\nMassDev: (-?\d+(?:\.\d+)) ppm""".r
    val root = graph.getVertices.maxBy(_.getName.substring(1).toInt)
    regexp.findFirstIn(root.getProperties.get("label")) match {
      case Some(regexp(form, mz, inte, ppm)) => Fragment(MolecularFormula.parse(form), mz.toDouble, inte.toDouble, ppm.toDouble)
      case bla => throw new RuntimeException(root.getProperties.get("label") + " does not match =(\n" + bla)
    }
  }

  case class Fragment(val formula:MolecularFormula, val mz:Double, val intensity:Double, val ppm:Double)

}
