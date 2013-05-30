import de.unijena.bioinf.babelms.GenericParser
import de.unijena.bioinf.babelms.ms.JenaMsParser
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter
import de.unijena.bioinf.ChemistryBase.chem.{ChemicalAlphabet, FormulaConstraints, PeriodicTable, Ionization}
import de.unijena.bioinf.ChemistryBase.ms.{Deviation, Normalization, Ms2Experiment}
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NormalizeToSumPreprocessor
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis
import de.unijena.bioinf.FragmentationTreeConstruction.model.{ProfileImpl, Ms2ExperimentImpl}
import de.unijena.bioinf.MassDecomposer.{ValenceValidator, MassDecomposerFast}
import scala.collection.JavaConversions._
import java.io.File

class NoiseLearner(val root:String) {

  val path = new File(root)

  val analysis = new FragmentationPatternAnalysis
  analysis.setInitial()
  analysis.getPreprocessors.add(new NormalizeToSumPreprocessor)

  def allPeaks = path.listFiles().filter(f=>f.isFile && f.getName.endsWith(".ms")).flatMap(f => peaks(f))

  def peaks(file:File) = {
    val exp:Ms2Experiment = new GenericParser(new JenaMsParser()).parseFile(file)
    val parent = exp.getMolecularFormula
    val ionMass: Double = exp.getIonMass - exp.getMolecularFormula.getMass
    val ion: Ionization = PeriodicTable.getInstance.ionByMass(ionMass, 1e-2, 1)
    val expi = new Ms2ExperimentImpl(exp)
    expi.setIonization(ion)
    expi.setMeasurementProfile(new ProfileImpl(new Deviation(10, 2e-3), null, new Deviation(20),
      new FormulaConstraints(new ChemicalAlphabet(parent.elementArray():_*), List(new ValenceFilter(-0.5d)))))
    val preprocessed = analysis.preprocessing(expi)
    preprocessed.getMergedPeaks.filter(p=>(p.getDecompositions.isEmpty || p.getDecompositions.forall(f=> !parent.isSubtractable(f.getFormula)))).map(f=>{
      if (f.getIntensity > 50)
        println(file + " => " + f.getMz + " (" + f.getIntensity + " %)")
      f.getIntensity
    })

  }



}
