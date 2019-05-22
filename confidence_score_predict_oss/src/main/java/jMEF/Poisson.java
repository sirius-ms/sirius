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
 * The Poisson distribution is an exponential family and, as a consequence, the probability density function is given by
 * \f[ f(x; \mathbf{\Theta}) = \exp \left( \langle t(x), \mathbf{\Theta} \rangle - F(\mathbf{\Theta}) + k(x) \right) \f]
 * where \f$ \mathbf{\Theta} \f$ are the natural parameters.
 * This class implements the different functions allowing to express a Poisson distribution as a member of an exponential family.
 * 
 * @section Parameters
 * 
 * The parameters of a given distribution are:
 *   - Source parameters \f$\mathbf{\Lambda} = \lambda \in R^+\f$
 *   - Natural parameters \f$\mathbf{\Theta} = \theta \in R\f$
 *   - Expectation parameters \f$ \mathbf{H} = \eta \in R^+\f$
 *
 */
public final class Poisson extends ExponentialFamily<PVector, PVector>{	

	
	/**
	 * Constant for serialization.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Computes the log normalizer \f$ F( \mathbf{\Theta} ) \f$.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     \f$ F(\mathbf{\Theta})	= \exp \theta \f$
	 */
	public double F(PVector T){
		return Math.exp(T.array[0]);
	}


	/**
	 * Computes \f$ \nabla F ( \mathbf{\Theta} )\f$.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     \f$ \nabla F( \mathbf{\Theta} ) = \exp \theta \f$
	 */
	public PVector gradF(PVector T){
		PVector g  = new PVector(1);
		g.array[0] = Math.exp(T.array[0]);
		g.type     = TYPE.EXPECTATION_PARAMETER;
		return g;
	}


	/**
	 * Computes \f$ G(\mathbf{H})\f$
	 * @param   H  expectation parameters \f$ \mathbf{H} = \eta \f$
	 * @return     \f$ G(\mathbf{H}) = \eta \log \eta - \eta \f$
	 */
	public double G(PVector H){
		return H.array[0] * Math.log(H.array[0]) - H.array[0];
	}

	
	/**
	 * Computes \f$ \nabla G (\mathbf{H})\f$
	 * @param   H  expectation parameters \f$ \mathbf{H} = \eta \f$
	 * @return     \f$ \nabla G( \mathbf{H} ) = \log \eta \f$
	 */
	public PVector gradG(PVector H){
		PVector g  = new PVector(1);
		g.array[0] = Math.log(H.array[0]);
		g.type     = TYPE.NATURAL_PARAMETER;
		return g;
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
	 * @return     \f$ k(x) = - \log (x!) \f$
	 */
	public double k(PVector x){
		return -Math.log( (double)fact((int)x.array[0]) );	
	}


	/**
	 * Converts source parameters to natural parameters.
	 * @param   L  source parameters  \f$ \mathbf{\Lambda} = \lambda \f$
	 * @return     natural parameters \f$ \mathbf{\Theta}  = \log \lambda \f$
	 */
	public PVector Lambda2Theta(PVector L){
		PVector T  = new PVector(1);
		T.array[0] = Math.log(L.array[0]);
		T.type     = TYPE.NATURAL_PARAMETER;
		return T;
	}


	/**
	 * Converts natural parameters to source parameters.
	 * @param   T  natural parameters \f$ \mathbf{\Theta}  = \theta \f$
	 * @return     source parameters  \f$ \mathbf{\Lambda} = \exp \theta \f$
	 */
	public PVector Theta2Lambda(PVector T){
		PVector L  = new PVector(1);
		L.array[0] = Math.exp(T.array[0]);
		L.type     = TYPE.SOURCE_PARAMETER;
		return L;	
	}


	/**
	 * Converts source parameters to expectation parameters.
	 * @param   L  source parameters      \f$ \mathbf{\Lambda} = \lambda \f$
	 * @return     expectation parameters \f$ \mathbf{H}       = \lambda \f$
	 */
	public PVector Lambda2Eta(PVector L){
		PVector H  = new PVector(1);
		H.array[0] = L.array[0];
		H.type     = TYPE.EXPECTATION_PARAMETER;
		return H;
	}


	/**
	 * Converts expectation parameters to source parameters.
	 * @param   H  expectation parameters \f$ \mathbf{H}       = \eta \f$
	 * @return     source parameters      \f$ \mathbf{\Lambda} = \eta \f$
	 */
	public PVector Eta2Lambda(PVector H){
		PVector L  = new PVector(1);
		L.array[0] = H.array[0];
		L.type     = TYPE.SOURCE_PARAMETER;
		return L;	
	}
	

	/**
	 * Computes the density value \f$ f(x;\lambda) \f$.
	 * @param  x      a point
	 * @param  param  parameters (source, natural, or expectation)
	 * @return        \f$ f(x;\lambda) = \frac{\lambda^x \exp(-\lambda)}{x!} \f$
	 */
	public double density(PVector x, PVector param){
		if (param.type==TYPE.SOURCE_PARAMETER)
			return (Math.pow(param.array[0], x.array[0])*Math.exp(-param.array[0])) / ((double)fact((int)x.array[0]));
		else if(param.type==TYPE.NATURAL_PARAMETER)
			return super.density(x, param);
		else
			return super.density(x, Eta2Theta(param));
	}
	
	
	/**
	 * Computes the factorial of a number.
	 * @param  n  number
	 * @return n!
	 */
	private double fact(double n){
		double f = 1;
		for (int i=1; i<=n; i++)
			f *= i;
		return f;
	}


	/**
	 * Draws a point from the considered Poisson distribution.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = \lambda \f$
	 * @return     a point.
	 */
	public PVector drawRandomPoint(PVector L) {
		
		// Initialization
		double l = Math.exp(-L.array[0]);
		double p = 1.0;
		int    k = 0;
		
		// Loop
		do{
			k++;
			p *= Math.random();
		} while(p>l);
		
		// Point
		PVector point  = new PVector(1);
		point.array[0] = k-1;
		return point;
	}


	/**
	 * Computes the Kullback-Leibler divergence between two Poisson distributions.
	 * @param   LP  source parameters \f$ \mathbf{\Lambda}_P \f$
	 * @param   LQ  source parameters \f$ \mathbf{\Lambda}_Q \f$
	 * @return      \f$ D_{\mathrm{KL}}(f_P\|f_Q) = \lambda_Q - \lambda_P \left( 1 + \log \left( \frac{\lambda_Q}{\lambda_P} \right) \right) \f$
	 */
	public double KLD(PVector LP, PVector LQ) {
		double lp = LP.array[0];
		double lq = LQ.array[0];
		return lq - lp * ( 1 + Math.log(lq/lp) );
	}



}
