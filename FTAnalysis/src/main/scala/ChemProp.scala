import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter
import de.unijena.bioinf.ChemistryBase.chem.{FormulaConstraints, PeriodicTable, ChemicalAlphabet}
import de.unijena.bioinf.ChemistryBase.ms.Deviation
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer
import java.io.PrintStream
import scala.collection.JavaConversions._

class ChemProp(root:String) {
  println(root)
  val printer = new PrintStream("chemicalProperties.csv")
  val right = new PrintStream("correctChemicalProperties.csv")
  printer.println("h2c")
  right.println("h2c")
  val alphabet = new ChemicalAlphabet(PeriodicTable.getInstance().getAllByName("C", "H", "N", "O", "P", "S", "F", "I"):_*)
  val decomposer = new MassToFormulaDecomposer(alphabet)
  val constraints = new FormulaConstraints(alphabet, List(new ValenceFilter(-0.5d)))
  val n = new Normalization(root, null)
  n.correctTrees.foreach(t=>{
    val r = n.getRootsFromTree(t)
    val mz = r.mz
    val decomps = decomposer.decomposeToFormulas(mz, new Deviation(20, 2e-3)).take(5000)
    decomps.foreach(d => {
      printer.println(d.heteroWithoutOxygenToCarbonRatio())
    })
    right.println(r.formula.heteroWithoutOxygenToCarbonRatio())
  })
}
