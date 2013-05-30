import de.unijena.bioinf.ChemistryBase.math.{DensityFunction, ParetoDistribution}

case class ParetoLossSizeDistribution(k:Double,xmin:Double, a:Double, b:Double) {

  def density(x:Double) {
    if (x <= xmin) linearDistribution.getDensity(x)*scalingFactorLinear
    else paretoDistribution.getDensity(x)*scalingFactorPareto
  }

  val paretoDistribution = new ParetoDistribution(k, xmin)
  val linearDistribution = new DensityFunction {
    def getDensity(x: Double) = a * x + b
  }

  def linearProbability(x:Double) = a/2 *  x*x + b * x

  val scalingFactorLinear = linearProbability(xmin)/(1 + linearProbability(xmin))
  val scalingFactorPareto = 1/(1 + linearProbability(xmin))

  override def toString = "{\n\tpareto(x0="+xmin+", k="+k+"\t| for x > " + xmin + "\n" +
                          "\tlinear(" + a + "x + " + b + "\t| for x < " + xmin + "\n}";
}
