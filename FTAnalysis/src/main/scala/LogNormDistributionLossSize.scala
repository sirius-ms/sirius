import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution
import Math._

class LogNormDistributionLossSize extends LossSizeDistribution {

  var mean:Double = 0
  var sd:Double = 0
  var n:NormalDistribution = new NormalDistribution(0,0)

  override def learn(xs: Traversable[LossObservation]) {
    val N = count(xs)
    mean = xs.foldLeft(0d)((X,x)=> X + x.count*Math.log(x.loss.getMass))/N
    val variance = xs.foldLeft(0d)((X,x)=> X + x.count*(mean-log(x.loss.getMass))*(mean-log(x.loss.getMass)))/N
    sd = Math.sqrt(variance)
    n = new NormalDistribution(mean, variance)
  }

  override def probability(observation: MolecularFormula) =
    new NormalDistribution(mean, sd*sd).getCumulativeProbability((log(observation.getMass) - mean)/sd)

  override def density(observation: MolecularFormula) = {
    val x = observation.getMass;
    1/(sqrt(2*PI)*sd*x) * exp(-pow(log(x)-mean, 2)/(2*sd*sd))
  }

  override def toString = "NormalDistribution: μ=" + mean + ", σ=" + sd;
}
