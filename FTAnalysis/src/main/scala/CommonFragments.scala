import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula
import de.unijena.bioinf.ChemistryBase.chem.utils.scoring.ChemicalCompoundScorer
import java.io.File

class CommonFragments(root:String) {

  def path = new File(root)

  lazy val fragments = Array("C5H6N4", "C5H5N5", "C3H4ClN5", "C6H6N4", "C6H8N4", "C4H4N2S", "C5H4N4", "C4H5N3O", "C8H7ClN4")

  def fragmentChemicalPrior = {
    val prior = ChemicalCompoundScorer.createMassDependendFormulaScorer(100, ChemicalCompoundScorer.createDefaultCompoundScorer(true))
    println(fragments.zip(fragments.map(f=>prior.score(MolecularFormula.parse(f)))).map(x=>"\"" + x._1.toString + "\","+ x._2.toString).mkString(","))
  }




}
