import au.com.bytecode.opencsv.{CSVWriter, CSVReader}
import collection.mutable
import de.unijena.bioinf.babelms.dot._
import de.unijena.bioinf.babelms.GenericParser
import de.unijena.bioinf.babelms.ms.JenaMsParser
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment
import java.io.{FileWriter, BufferedReader, FileReader, File}
import scala.None
import scalax.file.{FileSystem, Path}
import scalax.io.Resource
import scala.collection.JavaConversions._
;
class Evaluation(val root:String) {

  def path:Path =  Path.fromString(root)

  def getPPMs() {
    new File(path.fileOption.get, "")
  }

  def fragmentProperties() {
    val header:Array[String] = Array("formula","mass","h2c","hy2c","xh2c","h2o","rdbe")
    Array("correctFragments.csv", "wrongFragments.csv").foreach(e => {
      val fileName:File = new File(path.fileOption.get, e)
      val xs = (mutable.Buffer[Array[String]]() += header) ++= fragmentPropertiesFromCsv(Path.fromString(fileName.toString))
      val writer = new CSVWriter(new FileWriter(fileName.getName.split("\\.")(0) + "Properties.csv"))
      xs.foreach(x => writer.writeNext(x))
      writer.close()
    })
  }

  def fragmentPropertiesFromCsv(csv:Path):Traversable[Array[String]] = {
    val t = readCsv(csv)
    t.drop(1).map(row => {
      val f = MolecularFormula.parse(row(1))
      Array(f.toString, f.getMass, f.hetero2CarbonRatio(), f.hydrogen2CarbonRatio(), f.heteroWithoutOxygenToCarbonRatio(),
      f.hetero2OxygenRatio(), f.rdbe()).map(_.toString)
    })
  }

  def readCsv(name:Path) = {
    val reader = new FileReader(name.fileOption.get)
    val list = new CSVReader(reader).readAll()
    reader.close()
    list
  }

  def toCsv(name:Path, xs:Traversable[Seq[String]]) {
    name.deleteIfExists()
    name.writeStrings(xs.map(x=>x.mkString(",")), "\n")
  }

  def readSpectrum(file:File) = new GenericParser[Ms2Experiment](new JenaMsParser()).parseFile(file)

  def readTree(file:File) = {
    val reader = new BufferedReader(new FileReader(file))
    val graph = Parser.parse(reader)
    reader.close()
    graph
  }

  def readProperties(label:String) = {
    val map = mutable.Map[String, String]()
    val lines = label.split("""\\n""")
    map("formula") = lines(0)
    val mzIntR = """(\d+(?:\.\d+)?) Da, (\d+(?:\.\d+)?) %""".r
    mzIntR findFirstIn (lines(1)) match {
      case Some(mzIntR(mz, int)) => map ++= Map("mz" -> mz, "int" -> int)
      case _ =>
    }
    val devR = """MassDev: (-?\d+(?:\.\d+)?) ppm, (-?\d+(?:\.\d+)?) Da""".r
    devR findFirstIn(lines(2)) match {
      case Some(devR(ppm, abs)) => map ++= Map("ppm" -> ppm, "abs" -> abs)
      case _ =>
    }
    map
  }


}
