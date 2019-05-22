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
 * The Bernoulli distribution is an exponential family and, as a consequence, the probability density function is given by
 * \f[ f(x; \mathbf{\Theta}) = \exp \left( \langle t(x), \mathbf{\Theta} \rangle - F(\mathbf{\Theta}) + k(x) \right) \f]
 * where \f$ \mathbf{\Theta} \f$ are the natural parameters.
 * This class implements the different functions allowing to express a Bernoulli distribution as a member of an exponential family.
 * 
 * @section Parameters
 * 
 * The parameters of a given distribution are:
 *   - Source parameters \f$\mathbf{\Lambda} = p \in [0,1]\f$
 *   - Natural parameters \f$\mathbf{\Theta} = \theta \in R^+\f$
 *   - Expectation parameters \f$ \mathbf{H} = \eta \in [0,1] \f$
 */
public final class Bernoulli extends ExponentialFamily<PVector, PVector>{	

	
	/**
	 * Constant for serialization.
	 */
	private static final long serialVersionUID = 1L;

	
	/**
	 * Computes the log normalizer \f$ F( \mathbf{\Theta} ) \f$.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     \f$ F(\mathbf{\Theta}) = \log \left( 1 + \exp \theta \right) \f$
	 */
	public double F(PVector T){
		return Math.log(1+Math.exp(T.array[0]));
	}


	/**
	 * Computes \f$ \nabla F ( \mathbf{\Theta} )\f$.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     \f$ \nabla F( \mathbf{\Theta} ) = \frac{\exp \theta}{1 + \exp \theta} \f$
	 */
	public PVector gradF(PVector T){
		PVector gradient  = new PVector(1);
		gradient.array[0] = Math.exp(T.array[0]) / (1+Math.exp(T.array[0]));
		gradient.type     = TYPE.EXPECTATION_PARAMETER;
		return gradient;
	}


	/**
	 * Computes \f$ G(\mathbf{H})\f$.
	 * @param   H  expectation parameters \f$ \mathbf{H} = \eta \f$
	 * @return     \f$ G(\mathbf{H}) = \log \left( \frac{\eta}{1-\eta} \right) \eta - \log \left( \frac{1}{1-\eta} \right) \f$
	 */
	public double G(PVector H){
		return H.array[0] * Math.log(H.array[0]/(1-H.array[0])) - Math.log(1.0/(1-H.array[0]));
	}

	
	/**
	 * Computes \f$ \nabla G (\mathbf{H})\f$.
	 * @param   H  expectation parameters \f$ \mathbf{H} = \eta \f$
	 * @return     \f$ \nabla G( \mathbf{H} ) = \log \left( \frac{\eta}{1-\eta} \right) \f$
	 */
	public PVector gradG(PVector H){
		PVector gradient  = new PVector(1);
		gradient.array[0] = Math.log(H.array[0]/(1-H.array[0]));
		gradient.type     = TYPE.NATURAL_PARAMETER;
		return gradient;
	}
	
	
	/**
	 * Computes the sufficient statistic \f$ t(x)\f$.
	 * @param   x  a point
	 * @return     \f$ t(x) = x \f$
	 */
	public PVector t(PVector x){
		PVector t  = new PVector(1);
		t.array[0] = x.array[0];
		t.type     = TYPE.EXPECTATION_PARAMETER;
		return t;
	}


	/**
	 * Computes the carrier measure \f$ k(x) \f$.
	 * @param   x  a point
	 * @return     \f$ k(x) = 0 \f$
	 */
	public double k(PVector x){
		return 0.0d;	
	}


	/**
	 * Converts source parameters to natural parameters.
	 * @param   L  source parameters  \f$ \mathbf{\Lambda} = p \f$
	 * @return     natural parameters \f$ \mathbf{\Theta}  = \log \left( \frac{p}{1-p} \right) \f$
	 */
	public PVector Lambda2Theta(PVector L){
		PVector T  = new PVector(1);
		T.array[0] = Math.log(L.array[0]/(1-L.array[0]));
		T.type     = TYPE.NATURAL_PARAMETER;
		return T;
	}


	/**
	 * Converts natural parameters to source parameters.
	 * @param   T  natural parameters \f$ \mathbf{\Theta}  = \theta \f$
	 * @return     source parameters  \f$ \mathbf{\Lambda} = \frac{\exp\theta}{1+\exp\theta} \f$
	 */
	public PVector Theta2Lambda(PVector T){
		PVector L  = new PVector(1);
		L.array[0] = Math.exp(T.array[0]) / ( 1 + Math.exp(T.array[0]) );
		L.type     = TYPE.SOURCE_PARAMETER;
		return L;	
	}


	/**
	 * Converts source parameters to expectation parameters.
	 * @param   L  source parameters      \f$ \mathbf{\Lambda} = p \f$
	 * @return     expectation parameters \f$ \mathbf{H}       = p \f$
	 */
	public PVector Lambda2Eta(PVector L){
		PVector H  = new PVector(1);
		H.array[0] = L.array[0];
		H.type     = TYPE.EXPECTATION_PARAMETER;
		return H;
	}


	/**
	 * Converts expectation parameters to source parameters.
	 * @param   H  expectation parameters \f$ \mathbf{H}       = \eta\f$
	 * @return     source parameters      \f$ \mathbf{\Lambda} = \eta \f$
	 */
	public PVector Eta2Lambda(PVector H){
		PVector L  = new PVector(1);
		L.array[0] = H.array[0];
		L.type     = TYPE.SOURCE_PARAMETER;
		return L;	
	}
	

	/**
	 * Computes the density value \f$ f(x;p) \f$.
	 * @param  x      a point
	 * @param  param  parameters (source, natural, or expectation)
	 * @return        \f$ f(x;p) = p^x (1-p)^{1-x} \mbox{ for } x \in \{0,1\} \f$
	 */
	public double density(PVector x, PVector param){
		if (param.type==TYPE.SOURCE_PARAMETER)
			return Math.pow(param.array[0], x.array[0]) * Math.pow(1-param.array[0], 1-x.array[0]);
		else if(param.type==TYPE.NATURAL_PARAMETER)
			return super.density(x, param);
		else
			return super.density(x, Eta2Theta(param));
	}


	/**
	 * Draws a point from the considered distribution.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = p \f$
	 * @return     a point
	 */
	public PVector drawRandomPoint(PVector L) {
		PVector x = new PVector(1);
		if (Math.random()<L.array[0])
			x.array[0] = 1;
		else
			x.array[0] = 0;
		return x;
	}


	/**
	 * Computes the Kullback-Leibler divergence between two Bernoulli distributions.
	 * @param   L1  source parameters \f$ \mathbf{\Lambda}_1 \f$
	 * @param   L2  source parameters \f$ \mathbf{\Lambda}_2 \f$
	 * @return      \f$ D_{\mathrm{KL}}(f_1\|f_2) = \log \left( \frac{1-p_1}{1-p_2} \right) - p_1 \log \left( \frac{p_2(1-p_1)}{p_1(1-p_2)} \right) \f$
	 */
	public double KLD(PVector L1, PVector L2) {
		double p1 = L1.array[0];
		double p2 = L2.array[0];
		double q1 = 1-p1;
		double q2 = 1-p2;
		return Math.log(q1/q2) - p1 * Math.log( (p2*q1) / (p1*q2) );
	}
}
