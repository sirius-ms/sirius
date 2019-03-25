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
 * The Laplacian distribution is an exponential family and, as a consequence, the probability density function is given by
 * \f[ f(x; \mathbf{\Theta}) = \exp \left( \langle t(x), \mathbf{\Theta} \rangle - F(\mathbf{\Theta}) + k(x) \right) \f]
 * where \f$ \mathbf{\Theta} \f$ are the natural parameters.
 * This class implements the different functions allowing to express a Laplacian distribution as a member of an exponential family.
 * 
 * @section Parameters
 * 
 * The parameters of a given distribution are:
 *   - Source parameters \f$\mathbf{\Lambda} = \sigma \in R^+\f$
 *   - Natural parameters \f$\mathbf{\Theta} = \theta \in R^-\f$
 *   - Expectation parameters \f$ \mathbf{H} = \eta \in R^+ \f$
 *
 */
public class Laplacian extends ExponentialFamily<PVector,PVector> {


	/**
	 * Constant for serialization
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Computes the log normalizer \f$ F( \mathbf{\Theta} ) \f$.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     \f$ F(\mathbf{\Theta}) = \log \left( -\frac{2}{\theta} \right)  \f$
	 */
	public double F(PVector T){
		return Math.log( -2.0d / T.array[0] );
	}


	/**
	 * Computes \f$ \nabla F ( \mathbf{\Theta} )\f$.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = \theta \f$
	 * @return     \f$ \nabla F( \mathbf{\Theta} ) = -\frac{1}{\theta} \f$
	 */
	public PVector gradF(PVector T){
		PVector g  = new PVector(T.dim);
		g.array[0] = -1.0d / T.array[0];
		g.type     = TYPE.EXPECTATION_PARAMETER;
		return g;
	}


	/**
	 * Computes \f$ G(\mathbf{H})\f$.
	 * @param   H  expectation parameters \f$ \mathbf{H} = \eta \f$
	 * @return     \f$ G(\mathbf{H}) = - \log \eta \f$
	 */
	public double G(PVector H){
		return -Math.log(H.array[0]);
	}


	/**
	 * Computes \f$ \nabla G (\mathbf{H})\f$.
	 * @param   H  expectation parameters \f$ \mathbf{H} = \eta \f$
	 * @return     \f$ \nabla G(\mathbf{H}) = -\frac{1}{\eta} \f$
	 */
	public PVector gradG(PVector H){
		PVector g  = new PVector(1);
		g.array[0] = -1.0d/H.array[0];
		g.type     = TYPE.NATURAL_PARAMETER;
		return g;
	}


	/**
	 * Computes the sufficient statistic \f$ t(x)\f$.
	 * @param   x  a point
	 * @return     \f$ t(x) = |x| \f$
	 */
	public PVector t(PVector x){
		PVector t  = new PVector(1);
		t.array[0] = Math.abs(x.array[0]);
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
	 * @param   L  source parameters  \f$ \mathbf{\Lambda} = \sigma \f$
	 * @return     natural parameters \f$ \mathbf{\Theta}  = -\frac{1}{\sigma} \f$
	 */
	public PVector Lambda2Theta(PVector L){
		PVector T  = new PVector(L.dim);
		T.array[0] = -1.0d/L.array[0];
		T.type     = TYPE.NATURAL_PARAMETER;
		return T;
	}


	/**
	 * converts natural parameters to source parameters.
	 * @param   T  natural parameters \f$ \mathbf{\Theta}  = \theta \f$
	 * @return     source parameters  \f$ \mathbf{\Lambda} = -\frac{1}{\theta} \f$
	 */
	public PVector Theta2Lambda(PVector T){
		PVector L  = new PVector(T.dim);
		L.array[0] = -1.0d/T.array[0];
		L.type     = TYPE.SOURCE_PARAMETER;
		return L;
	}


	/**
	 * Converts source parameters to expectation parameters.
	 * @param   L  source parameters      \f$ \mathbf{\Lambda} = \sigma \f$
	 * @return     expectation parameters \f$ \mathbf{H}       = \sigma \f$
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
	 * Computes the density value \f$ f(x;\sigma) \f$.
	 * @param  x      a point
	 * @param  param  parameters (source, natural, or expectation)
	 * @return         \f$ f(x;\sigma) = \frac{1}{ 2 \sigma } \exp \left( - \frac{|x|}{\sigma} \right) \f$
	 */
	public double density(PVector x, PVector param){
		if (param.type==TYPE.SOURCE_PARAMETER)
			return (1.0d /(2*param.array[0])) * Math.exp( - Math.abs(x.array[0])/param.array[0] );
		else if(param.type==TYPE.NATURAL_PARAMETER)
			return super.density(x, param);
		else
			return super.density(x, Eta2Theta(param));
	}


	/**
	 * Draws a point from the considered Laplacian distribution.
	 * @param   L  source parameters \f$ \mathbf{\Lambda}\f$.
	 * @return     a point.
	 */
	public PVector drawRandomPoint(PVector L) {
		double  u      = Math.random() - 0.5;
		PVector point  = new PVector(1);
		point.array[0] = -L.array[0] * Math.signum(u) * Math.log(1 - 2 * Math.abs(u));
		return  point;
	}


	/**
	 * Computes the Kullback-Leibler divergence between two Laplacian distributions.
	 * @param   LP  source parameters \f$ \mathbf{\Lambda}_P \f$
	 * @param   LQ  source parameters \f$ \mathbf{\Lambda}_Q \f$
	 * @return      \f$ D_{\mathrm{KL}}(f_P\|f_Q) = \log \left( \frac{\sigma_Q}{\sigma_P} \right) + \frac{\sigma_P - \sigma_Q}{\sigma_Q} \f$
	 */
	public double KLD(PVector LP, PVector LQ) {
		double sP = LP.array[0];
		double sQ = LQ.array[0];
		return Math.log(sQ/sP) + (sP-sQ)/sQ;
	}


}
