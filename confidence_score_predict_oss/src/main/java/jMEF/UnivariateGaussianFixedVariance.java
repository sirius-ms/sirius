package jMEF;

import java.util.Random;

import jMEF.Parameter.TYPE;


/**
 * @author  Vincent Garcia
 * @author  Frank Nielsen
 * @version 1.0
 *
 * @section License
 * 
 * See file LICENSE.txt
 *
 * @section Description
 * 
 * The univariate Gaussian distribution, with fixed variance \f$ \sigma^2 \f$, is an exponential family and, as a consequence, the probability density function is given by
 * \f[ f(x; \mathbf{\Theta}) = \exp \left( \langle t(x), \mathbf{\Theta} \rangle - F(\mathbf{\Theta}) + k(x) \right) \f]
 * where \f$ \mathbf{\Theta} \f$ are the natural parameters.
 * This class implements the different functions allowing to express a univariate Gaussian distribution as a member of an exponential family.
 * 
 * @section Parameters
 * 
 * The parameters of a given distribution are:
 *   - Source parameters \f$\mathbf{\Lambda} = \mu \in R\f$
 *   - Natural parameters \f$\mathbf{\Theta} = \theta \in R\f$
 *   - Expectation parameters \f$ \mathbf{H} = \eta \in R\f$
 */
public final class UnivariateGaussianFixedVariance extends ExponentialFamily<PVector, PVector>{	

	
	/**
	 * Constant for serialization.
	 */
	private static final long serialVersionUID = 1L;
	
	
	/**
	 * Variance \f$ \sigma^2\f$
	 */
	private double variance;
	
	
	/**
	 * Class constructor with \f$\sigma^2=1\f$
	 */
	public UnivariateGaussianFixedVariance(){
		this.variance = 1.0;
	}

	
	/**
	 * Class constructor.
	 * @param variance variance \f$ \sigma^2\f$
	 */
	public UnivariateGaussianFixedVariance(double variance){
		this.variance = variance;
	}

	
	/**
	 * Computes the log normalizer \f$ F( \mathbf{\Theta} ) \f$.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     \f$ F(\mathbf{\Theta}) = \frac{\sigma^2 \theta^2 +  \log(2 \pi \sigma^2)}{2} \f$
	 */
	public double F(PVector T){
		return (T.array[0] * T.array[0] * variance + Math.log(2 * Math.PI * variance)) / 2.0;
	}


	/**
	 * Computes \f$ \nabla F ( \mathbf{\Theta} )\f$.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     \f$ \nabla F( \mathbf{\Theta} ) = \sigma^2 \theta \f$
	 */
	public PVector gradF(PVector T){
		PVector gradient  = new PVector(1);
		gradient.array[0] = variance * T.array[0];
		gradient.type     = TYPE.EXPECTATION_PARAMETER;
		return gradient;
	}


	/**
	 * Computes \f$ G(\mathbf{H})\f$.
	 * @param   H  expectation parameters \f$ \mathbf{H} = \eta \f$
	 * @return     \f$ G(\mathbf{H}) = \frac{\eta^2}{2 \sigma^2} \f$
	 */
	public double G(PVector H){
		return (H.array[0] * H.array[0]) / (2 * variance);
	}

	
	/**
	 * Computes \f$ \nabla G (\mathbf{H})\f$
	 * @param   H  expectation parameters \f$ \mathbf{H} = \eta \f$
	 * @return     \f$ \nabla G( \mathbf{H} ) = \frac{\eta}{\sigma^2} \f$
	 */
	public PVector gradG(PVector H){
		PVector gradient  = new PVector(1);
		gradient.array[0] = H.array[0] / variance;
		gradient.type     = TYPE.NATURAL_PARAMETER;
		return gradient;
	}
	
	
	/**
	 * Computes the sufficient statistic \f$ t(x)\f$.
	 * @param   x  a point
	 * @return     \f$ t(x) = x \f$
	 */
	public PVector  t(PVector x){
		PVector t  = new PVector(1);
		t.array[0] = x.array[0];
		t.type     = TYPE.EXPECTATION_PARAMETER;
		return t;
	}


	/**
	 * Computes the carrier measure \f$ k(x) \f$.
	 * @param   x  a point
	 * @return     \f$ k(x) = -\frac{x^2}{2 \sigma^2} \f$
	 */
	public double k(PVector x){
		return -(x.array[0]*x.array[0])/(2*variance);	
	}


	/**
	 * Converts source parameters to natural parameters.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = \mu \f$
	 * @return     natural parameters \f$ \mathbf{\Theta} =	\frac{\mu}{\sigma^2} \f$
	 */
	public PVector Lambda2Theta(PVector L){
		PVector T  = new PVector(1);
		T.array[0] = L.array[0] / variance;
		T.type     = TYPE.NATURAL_PARAMETER;
		return T;
	}


	/**
	 * Converts natural parameters to source parameters.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     source parameters \f$ \mathbf{\Lambda} = \theta \sigma^2 \f$
	 */
	public PVector Theta2Lambda(PVector T){
		PVector L  = new PVector(1);
		L.array[0] = T.array[0] * variance;
		L.type     = TYPE.SOURCE_PARAMETER;
		return L;	
	}


	/**
	 * Converts source parameters to expectation parameters.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = \lambda \f$
	 * @return     expectation parameters \f$ \mathbf{H} = \lambda \f$
	 */
	public PVector Lambda2Eta(PVector L){
		PVector H  = new PVector(1);
		H.array[0] = L.array[0];
		H.type     = TYPE.EXPECTATION_PARAMETER;
		return H;
	}


	/**
	 * Converts expectation parameters to source parameters.
	 * @param   H  natural parameters \f$ \mathbf{H} = \eta \f$
	 * @return     source parameters  \f$ \mathbf{\Lambda} = \eta \f$
	 */
	public PVector Eta2Lambda(PVector H){
		PVector L  = new PVector(1);
		L.array[0] = H.array[0];
		L.type     = TYPE.SOURCE_PARAMETER;
		return L;	
	}
	

	/**
	 * Computes the density value \f$ f(x;\mu,\sigma^2) \f$.
	 * @param  x      a point
	 * @param  param  parameters (source, natural, or expectation)
	 * @return        \f$ f(x;\mu,\sigma^2) = \frac{1}{ \sqrt{2\pi \sigma^2} } \exp \left( - \frac{(x-\mu)^2}{ 2 \sigma^2} \right) \f$
	 */
	public double density(PVector x, PVector param){
		if (param.type==TYPE.SOURCE_PARAMETER)
			return Math.exp(-((x.array[0]-param.array[0])*(x.array[0]-param.array[0]))/(2*variance)) / (Math.sqrt(2 * Math.PI * variance));
		else if(param.type==TYPE.NATURAL_PARAMETER)
			return super.density(x, param);
		else
			return super.density(x, Eta2Theta(param));
	}


	/**
	 * Draws a point from the considered distribution.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = \lambda \f$
	 * @return     a point
	 */
	public PVector drawRandomPoint(PVector L) {
		Random  rand = new Random();
		PVector v    = new PVector(1);
		v.array[0]   = rand.nextGaussian() * Math.sqrt(this.variance);
		return v.Plus(L);
	}


	/**
	 * Computes the Kullback-Leibler divergence between two univariate Gaussian distributions.
	 * @param   LP  source parameters \f$ \mathbf{\Lambda}_P \f$
	 * @param   LQ  source parameters \f$ \mathbf{\Lambda}_Q \f$
	 * @return      \f$ D_{\mathrm{KL}}(f_P\|f_Q) = \frac{(\mu_Q-\mu_P)^2}{2\sigma^2} \f$
	 */
	public double KLD(PVector LP, PVector LQ) {
		double mP = LP.array[0];
		double mQ = LQ.array[0];
		return ((mQ-mP)*(mQ-mP)) / (2*variance);
	}

}
