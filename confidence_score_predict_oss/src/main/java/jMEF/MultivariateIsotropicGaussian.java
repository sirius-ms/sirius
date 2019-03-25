package jMEF;

import jMEF.Parameter.TYPE;

import java.util.Random;


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
 * The multivariate isotropic Gaussian distribution (\f$\Sigma=\f$Id) is an exponential family and, as a consequence, the probability density function is given by
 * \f[ f(x; \mathbf{\Theta}) = \exp \left( \langle t(x), \mathbf{\Theta} \rangle - F(\mathbf{\Theta}) + k(x) \right) \f]
 * where \f$ \mathbf{\Theta} \f$ are the natural parameters.
 * This class implements the different functions allowing to express a multivariate Gaussian distribution as a member of an exponential family.
 * 
 * @section Parameters
 * 
 * The parameters of a given distribution are:
 *   - Source parameters \f$\mathbf{\Lambda} = \mu \mbox{ with } \mu \in \mathds{R}^d \f$
 *   - Natural parameters \f$\mathbf{\Theta} = \theta \f$
 *   - Expectation parameters \f$\mathbf{H} = \eta \f$
 */
public final class MultivariateIsotropicGaussian extends ExponentialFamily<PVector, PVector>{	


	/**
	 * Constant for serialization
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Computes the log normalizer \f$ F( \mathbf{\Theta} ) \f$.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     \f$ F(\mathbf{\theta}) = \frac{1}{2} \theta^\top\theta + \frac{d}{2}\log 2\pi \f$
	 */
	public double F(PVector T){
		return 0.5d*( T.InnerProduct(T) + T.dim * Math.log(2*Math.PI) );
	}


	/**
	 * Computes \f$ \nabla F ( \mathbf{\Theta} )\f$.
	 * @param   T  natural  \f$ \mathbf{\Theta} = \theta \f$
	 * @return     \f$ \nabla F( \mathbf{\Theta} ) = \theta \f$
	 */
	public PVector gradF(PVector T){
		PVector gradient = (PVector)T.clone();
		gradient.type    = TYPE.EXPECTATION_PARAMETER;
		return gradient;
	}


	/**
	 * Computes \f$ G(\mathbf{H})\f$
	 * @param   H  expectation parameters \f$ \mathbf{H} = \eta \f$
	 * @return     \f$ F(\mathbf{\theta})= \frac{1}{2} \eta^\top\eta + \frac{d}{2}\log 2\pi \f$
	 */
	public double G(PVector H){
		return	0.5d*( H.InnerProduct(H) + H.dim * Math.log(2*Math.PI) );
	}


	/**
	 * Computes \f$ \nabla G (\mathbf{H})\f$
	 * @param   H  expectation parameters \f$ \mathbf{H} = \eta \f$
	 * @return     \f$ \nabla G(\mathbf{H}) = \eta \f$
	 */
	public PVector gradG(PVector H){
		PVector gradient = (PVector)H.clone();
		gradient.type    = TYPE.NATURAL_PARAMETER;
		return gradient;	
	}


	/**
	 * Computes the sufficient statistic \f$ t(x)\f$.
	 * @param   x  a point
	 * @return     \f$ t(x) = x \f$
	 */
	public PVector t(PVector x){
		PVector t = (PVector)x.clone();
		t.type    = TYPE.EXPECTATION_PARAMETER;
		return t;
	}


	/**
	 * Computes the carrier measure \f$ k(x) \f$.
	 * @param   x  a point
	 * @return     \f$ k(x) = -\frac{1}{2}x^\top x \f$
	 */
	public double k(PVector x){
		return -0.5d * x.InnerProduct(x);
	}


	/**
	 * Converts source parameters to natural parameters.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = \mu \f$
	 * @return     natural parameters \f$ \mathbf{\Theta} = \mu \f$
	 */
	public PVector Lambda2Theta(PVector L){
		PVector T = (PVector)L.clone();
		T.type    = TYPE.NATURAL_PARAMETER;
		return T;
	}


	/**
	 * Converts natural parameters to source parameters.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     source parameters \f$ \mathbf{\Lambda} = \theta \f$
	 */
	public PVector Theta2Lambda(PVector T){
		PVector L = (PVector)T.clone();
		L.type    = TYPE.SOURCE_PARAMETER;
		return L;
	}


	/**
	 * Converts source parameters to expectation parameters.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = \mu \f$
	 * @return     expectation parameters \f$ \mathbf{H}  = \mu \f$
	 */
	public PVector Lambda2Eta(PVector L){
		PVector H = (PVector)L.clone();
		H.type    = TYPE.EXPECTATION_PARAMETER;
		return H;
	}


	/**
	 * Converts expectation parameters to source parameters.
	 * @param   H  expectation parameters \f$ \mathbf{H}  = \eta \f$
	 * @return     source parameters \f$ \mathbf{\Lambda} = \eta \f$
	 */
	public PVector Eta2Lambda(PVector H){
		PVector L = (PVector)H.clone();
		L.type    = TYPE.SOURCE_PARAMETER;
		return L;
	}


	/**
	 * Computes the density value \f$ f(x;\mu) \f$.
	 * @param  x      point
	 * @param  param  parameters (source, natural, or expectation)
	 * @return         \f$ f(x;\mu) = \frac{1}{ (2\pi)^{d/2} } \exp \left( - \frac{(x-\mu)^T (x-\mu)}{2} \right) \mbox{ for } x \in \mathds{R}^d \f$
	 */
	public double density(PVector x, PVector param){
		if (param.type==TYPE.SOURCE_PARAMETER){
			double v1 = (x.Minus(param)).InnerProduct(x.Minus(param));
			double v2 = Math.exp(-0.5d*v1);
			return v2 / Math.pow( 2.0d*Math.PI , (double)x.dim/2.0d );
		}
		else if(param.type==TYPE.NATURAL_PARAMETER)
			return super.density(x, param);
		else
			return super.density(x, Eta2Theta(param));
	}


	/**
	 * Draws a point from the considered distribution.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = \mu \f$
	 * @return     a point.
	 */
	public PVector drawRandomPoint(PVector L) {
		Random  rand = new Random();
		PVector x    = new PVector(L.getDimension());
		for (int i=0; i<L.getDimension(); i++)
			x.array[i] = L.array[i] + rand.nextGaussian();
		return x;
	}


	/**
	 * Computes the Kullback-Leibler divergence between two multivariate isotropic Gaussian distributions.
	 * @param   LP  source parameters \f$ \mathbf{\Lambda}_P \f$
	 * @param   LQ  source parameters \f$ \mathbf{\Lambda}_Q \f$
	 * @return      \f$ D_{\mathrm{KL}}(f_P \| f_Q) = \frac{1}{2} ( \mu_Q - \mu_P )^\top( \mu_Q - \mu_P ) \f$
	 */
	public double KLD(PVector LP, PVector LQ) {
		PVector diff = LQ.Minus(LP);
		return 0.5d * diff.InnerProduct(diff);
	}


}
