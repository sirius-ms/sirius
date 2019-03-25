package jMEF;

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
 * The Rayleigh is an exponential family and, as a consequence, the probability density function is given by
 * \f[ f(x; \mathbf{\Theta}) = \exp \left( \langle t(x), \mathbf{\Theta} \rangle - F(\mathbf{\Theta}) + k(x) \right) \f]
 * where \f$ \mathbf{\Theta} \f$ are the natural parameters.
 * This class implements the different functions allowing to express a Rayleigh distribution as a member of an exponential family.
 * 
 * @section Parameters
 * 
 * The parameters of a given distribution are:
 *   - Source parameters \f$ \mathbf{\Lambda} = \sigma^2 \in \mathds{R}^+ \f$
 *   - Natural parameters \f$ \mathbf{\Theta} = \theta \in \mathds{R}^- \f$
 *   - Expectation parameters \f$ \mathbf{H} = \eta \in \mathds{R}^+ \f$
 *
 */
public final class Rayleigh extends ExponentialFamily<PVector, PVector>{	

	
	/**
	 * Constant for serialization.
	 */
	private static final long serialVersionUID = 1L;

	
	/**
	 * Computes the log normalizer \f$ F( \mathbf{\Theta} ) \f$.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     \f$ F(\mathbf{\theta}) = - \log (-2 \theta) \f$
	 */
	public double F(PVector T){
		return - Math.log( -2 * T.array[0] );
	}


	/**
	 * Computes \f$ \nabla F ( \mathbf{\Theta} )\f$.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     \f$ \nabla F(\mathbf{\theta}) = -\frac{1}{\theta} \f$
	 */
	public PVector gradF(PVector T){
		PVector gradient  = new PVector(1);
		gradient.array[0] = -1.0d / T.array[0];
		gradient.type     = TYPE.EXPECTATION_PARAMETER;
		return gradient;
	}


	/**
	 * Computes \f$ G(\mathbf{H})\f$.
	 * @param   H  expectation parameters \f$ \mathbf{H} = \eta \f$
	 * @return     \f$ G(\mathbf{\eta}) = - \log \eta \f$
	 */
	public double G(PVector H){
		return - Math.log(H.array[0]);
	}

	
	/**
	 * Computes \f$ \nabla G (\mathbf{H})\f$
	 * @param   H  expectation parameters \f$ \mathbf{H} = \eta \f$
	 * @return     \f$ \nabla G(\mathbf{\eta}) = -\frac{1}{\eta} \f$
	 */
	public PVector gradG(PVector H){
		PVector gradient  = new PVector(1);
		gradient.array[0] = -1.0d / H.array[0];
		gradient.type     = TYPE.NATURAL_PARAMETER;
		return gradient;
	}
	
	
	/**
	 * Computes the sufficient statistic \f$ t(x)\f$.
	 * @param   x  a point
	 * @return     \f$ t(x) = x^2 \f$
	 */
	public PVector  t(PVector x){
		PVector t  = new PVector(1);
		t.array[0] = x.array[0] * x.array[0];
		t.type     = TYPE.EXPECTATION_PARAMETER;
		return t;
	}


	/**
	 * Computes the carrier measure \f$ k(x) \f$.
	 * @param   x  a point
	 * @return     \f$ k(x) = \log x \f$
	 */
	public double k(PVector x){
		return Math.log(x.array[0]);	
	}


	/**
	 * Converts source parameters to natural parameters.
	 * @param   L  source parameters  \f$ \mathbf{\Lambda} = \sigma^2 \f$
	 * @return     natural parameters \f$ \mathbf{\Theta}  = -\frac{1}{2 \sigma^2} \f$
	 */
	public PVector Lambda2Theta(PVector L){
		PVector T  = new PVector(1);
		T.array[0] = -1.0d / (2.0d * L.array[0]);
		T.type     = TYPE.NATURAL_PARAMETER;
		return T;
	}


	/**
	 * Converts natural parameters to source parameters.
	 * @param   T  natural parameters \f$ \mathbf{\Theta}  = \theta \f$
	 * @return     source parameters  \f$ \mathbf{\Lambda} = -\frac{1}{2\theta} \f$
	 */
	public PVector Theta2Lambda(PVector T){
		PVector L  = new PVector(1);
		L.array[0] = -1.0d / (2.0d * T.array[0]);
		L.type     = TYPE.SOURCE_PARAMETER;
		return L;	
	}


	/**
	 * Converts source parameters to expectation parameters.
	 * @param   L  source parameters      \f$ \mathbf{\Lambda} = \sigma^2 \f$
	 * @return     expectation parameters \f$ \mathbf{H}       = 2 \sigma^2 \f$
	 */
	public PVector Lambda2Eta(PVector L){
		PVector H  = new PVector(1);
		H.array[0] = 2 * L.array[0];
		H.type     = TYPE.EXPECTATION_PARAMETER;
		return H;
	}


	/**
	 * Converts expectation parameters to source parameters.
	 * @param   H  expectation parameters \f$ \mathbf{H}       = \eta\f$
	 * @return     source parameters      \f$ \mathbf{\Lambda} = \frac{\eta}{2} \f$
	 */
	public PVector Eta2Lambda(PVector H){
		PVector L  = new PVector(1);
		L.array[0] = H.array[0] / 2.0d;
		L.type     = TYPE.SOURCE_PARAMETER;
		return L;	
	}
	

	/**
	 * Computes the density value \f$ f(x;p) \f$.
	 * @param  x      a point
	 * @param  param  parameters (source, natural, or expectation)
	 * @return        \f$ f(x;\sigma^2) = \frac{x}{\sigma^2} \exp \left( -\frac{x^2}{2\sigma^2} \right) \f$
	 */
	public double density(PVector x, PVector param){
		if (param.type==TYPE.SOURCE_PARAMETER)
			return ( Math.exp( - (x.array[0]*x.array[0]) / (2*param.array[0])) * x.array[0] ) / param.array[0];
		else if(param.type==TYPE.NATURAL_PARAMETER)
			return super.density(x, param);
		else
			return super.density(x, Eta2Theta(param));
	}


	/**
	 * Draws a point from the considered distribution.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = \sigma^2 \f$
	 * @return     a point
	 */
	public PVector drawRandomPoint(PVector L) {
		PVector x  = new PVector(1);
		x.array[0] =  Math.sqrt( - 2 * Math.log( Math.random() ) * L.array[0] );
		return x;
	}


	/**
	 * Computes the Kullback-Leibler divergence between two Binomial distributions.
	 * @param   LP  source parameters \f$ \mathbf{\Lambda}_P \f$
	 * @param   LQ  source parameters \f$ \mathbf{\Lambda}_Q \f$
	 * @return      \f$ D_{\mathrm{KL}}(f_P \| f_Q) = \log \left( \frac{\sigma_Q^2}{\sigma_P^2} \right) + \frac{ \sigma_P^2 - \sigma_Q^2 }{\sigma_Q^2} \f$
	 */
	public double KLD(PVector LP, PVector LQ) {
		double vP = LP.array[0];
		double vQ = LQ.array[0];
		return Math.log(vQ/vP) + ( (vP-vQ) / vQ);
	}
}
