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
 * The multivariate Gaussian distribution is an exponential family and, as a consequence, the probability density function is given by
 * \f[ f(x; \mathbf{\Theta}) = \exp \left( \langle t(x), \mathbf{\Theta} \rangle - F(\mathbf{\Theta}) + k(x) \right) \f]
 * where \f$ \mathbf{\Theta} \f$ are the natural parameters.
 * This class implements the different functions allowing to express a multivariate Gaussian distribution as a member of an exponential family.
 * 
 * @section Parameters
 * 
 * The parameters of a given distribution are:
 *   - Source parameters \f$\mathbf{\Lambda} = ( \mu , \Sigma ) \mbox{ with } \mu \in \mathds{R}^d \mbox{ and } \Sigma \succ 0\f$
 *   - Natural parameters \f$\mathbf{\Theta} = ( \theta , \Theta )\f$
 *   - Expectation parameters \f$\mathbf{H} = ( \eta , H )\f$
 */
public final class MultivariateGaussian extends ExponentialFamily<PVector, PVectorMatrix>{	


	/**
	 * Constant for serialization
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Computes the log normalizer \f$ F( \mathbf{\Theta} ) \f$.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = ( \theta , \Theta ) \f$
	 * @return     \f$ F(\mathbf{\Theta})=\frac{1}{4} \mathrm{tr}(\Theta^{-1}\theta\theta^T) - \frac{1}{2} \log \det\Theta + \frac{d}{2} log \pi  \f$
	 */
	public double F(PVectorMatrix T){
		return 0.25d*( (T.M.Inverse()).Multiply(T.v.OuterProduct()) ).Trace()
		       - 0.5d*Math.log( T.M.Determinant() )
		       + (0.5d*T.v.dim)*Math.log(Math.PI);
	}


	/**
	 * Computes \f$ \nabla F ( \mathbf{\Theta} )\f$.
	 * @param   T  natural  \f$ \mathbf{\Theta} = ( \theta , \Theta ) \f$
	 * @return     \f$ \nabla F( \mathbf{\Theta} ) = \left( \frac{1}{2} \Theta^{-1} \theta , -\frac{1}{2} \Theta^{-1} -\frac{1}{4} (\Theta^{-1} \theta)(\Theta^{-1} \theta)^T \right) \f$
	 */
	public PVectorMatrix gradF(PVectorMatrix T){
		PVectorMatrix gradient = new PVectorMatrix(T.v.dim);
		gradient.v    = T.M.Inverse().MultiplyVectorRight(T.v).Times(0.5d);
		gradient.M    = T.M.Inverse().Times(-0.5d).Minus( (T.M.Inverse().MultiplyVectorRight(T.v)).OuterProduct().Times(0.25d) );
		gradient.type = TYPE.EXPECTATION_PARAMETER;
		return gradient;
	}


	/**
	 * Computes \f$ G(\mathbf{H})\f$
	 * @param   H  expectation parameters \f$ \mathbf{H} = ( \eta , H ) \f$
	 * @return     \f$ G(\mathbf{H}) = - \frac{1}{2} \log \left( 1 + \eta^T H^{-1} \eta \right) - \frac{1}{2} \log \det (-H) - \frac{d}{2} \log (2 \pi e) \f$
	 */
	public double G(PVectorMatrix H){
		return	-0.5d * Math.log( 1.0d + H.v.InnerProduct(H.M.Inverse().MultiplyVectorRight(H.v)) ) - 0.5d * Math.log( H.M.Times(-1.0d).Determinant() ) - H.v.dim*0.5d*Math.log(2*Math.PI*Math.E);
	}


	/**
	 * Computes \f$ \nabla G (\mathbf{H})\f$
	 * @param   H  expectation parameters \f$ \mathbf{H} = ( \eta , H ) \f$
	 * @return     \f$ \nabla G(\mathbf{H}) = \left( -( H + \eta \eta^T )^{-1} \eta , -\frac{1}{2} ( H + \eta \eta^T )^{-1}  \right) \f$
	 */
	public PVectorMatrix gradG(PVectorMatrix H){
		PVectorMatrix gradient = new PVectorMatrix(H.v.dim);
		PMatrix tmp   = H.M.Plus(H.v.OuterProduct()).Inverse();
		gradient.v    = tmp.MultiplyVectorRight(H.v).Times(-1.0d);
		gradient.M    = tmp.Times(-0.5d);
		gradient.type = TYPE.NATURAL_PARAMETER;
		return gradient;	
	}


	/**
	 * Computes the sufficient statistic \f$ t(x)\f$.
	 * @param   x  a point
	 * @return     \f$ t(x) = (x , -x x^\top) \f$
	 */
	public PVectorMatrix t(PVector x){
		PVectorMatrix t = new PVectorMatrix(x.dim);
		t.v    = x;
		t.M    = x.OuterProduct().Times(-1);
		t.type = TYPE.EXPECTATION_PARAMETER;
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
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = ( \mu , \Sigma ) \f$
	 * @return     natural parameters \f$ \mathbf{\Theta} = \left( \Sigma^{-1} \mu , \frac{1}{2} \Sigma^{-1} \right)\f$
	 */
	public PVectorMatrix Lambda2Theta(PVectorMatrix L){
		PVectorMatrix T   = new PVectorMatrix(L.v.dim);
		PMatrix       tmp = L.M.Inverse();
		T.v               = tmp.MultiplyVectorRight(L.v);	
		T.M               = tmp.Times(0.5d);
		T.type            = TYPE.NATURAL_PARAMETER;
		return T;
	}


	/**
	 * Converts natural parameters to source parameters.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = ( \theta , \Theta )\f$
	 * @return     source parameters \f$ \mathbf{\Lambda} = \left( \frac{1}{2} \Theta^{-1} \theta ,  \frac{1}{2} \Theta^{-1} \right) \f$
	 */
	public PVectorMatrix Theta2Lambda(PVectorMatrix T){
		PVectorMatrix L = new PVectorMatrix(T.v.dim);
		L.M             = T.M.Inverse().Times(0.5d);
		L.v             = L.M.MultiplyVectorRight(T.v);
		L.type          = TYPE.SOURCE_PARAMETER;
		return L;
	}


	/**
	 * Converts source parameters to expectation parameters.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = ( \mu , \Sigma ) \f$
	 * @return     expectation parameters \f$ \mathbf{H} = \left( \mu , - (\Sigma + \mu \mu^T) \right) \f$
	 */
	public PVectorMatrix Lambda2Eta(PVectorMatrix L){
		PVectorMatrix H = new PVectorMatrix(L.v.dim);
		H.v             = (PVector)L.v.clone();
		H.M             = L.M.Plus(L.v.OuterProduct()).Times(-1);
		H.type          = TYPE.EXPECTATION_PARAMETER;
		return H;
	}


	/**
	 * Converts expectation parameters to source parameters.
	 * @param   H  expectation parameters \f$ \mathbf{H} = ( \eta , H )\f$
	 * @return     source parameters \f$ \mathbf{\Lambda} = \left( \eta , - (H + \eta \eta^T) \right) \f$
	 */
	public PVectorMatrix Eta2Lambda(PVectorMatrix H){
		PVectorMatrix L = new PVectorMatrix(H.v.dim);
		L.v             = (PVector)H.v.clone();
		L.M             = H.M.Plus(H.v.OuterProduct()).Times(-1);
		L.type          = TYPE.SOURCE_PARAMETER;
		return L;
	}


	/**
	 * Computes the density value \f$ f(x;\mu,\Sigma) \f$.
	 * @param  x      point
	 * @param  param  parameters (source, natural, or expectation)
	 * @return         \f$ f(x;\mu,\Sigma) = \frac{1}{ (2\pi)^{d/2} |\Sigma|^{1/2} } \exp \left( - \frac{(x-\mu)^T \Sigma^{-1}(x-\mu)}{2} \right) \mbox{ for } x \in \mathds{R}^d \f$
	 */
	public double density(PVector x, PVectorMatrix param){
		if (param.type==TYPE.SOURCE_PARAMETER){
			double v1 = (x.Minus(param.v)).InnerProduct(param.M.Inverse().MultiplyVectorRight(x.Minus(param.v)));
			double v2 = Math.exp(-0.5d*v1);
			double v3 = Math.pow(2.0d*Math.PI, (double)x.dim/2.0d)*Math.sqrt(param.M.Determinant());
			return v2 / v3;
		}
		else if(param.type==TYPE.NATURAL_PARAMETER)
			return super.density(x, param);
		else
			return super.density(x, Eta2Theta(param));
	}


	/**
	 * Draws a point from the considered distribution.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = ( \mu , \Sigma ) \f$
	 * @return     a point.
	 */
	public PVector drawRandomPoint(PVectorMatrix L) {
		
		// Compute Z vector containing dim values i.i.d. drawn from N(0,1)
		Random  rand = new Random();
		PVector z    = new PVector(L.getDimension());
		for (int i=0; i<L.getDimension(); i++)
			z.array[i] = rand.nextGaussian();
		
		// Generates point
		return L.M.Cholesky().MultiplyVectorRight(z).Plus(L.v);
	}


	/**
	 * Computes the Kullback-Leibler divergence between two multivariate Gaussian distributions.
	 * @param   LP  source parameters \f$ \mathbf{\Lambda}_P \f$
	 * @param   LQ  source parameters \f$ \mathbf{\Lambda}_Q \f$
	 * @return      \f$ D_{\mathrm{KL}}(f_P \| f_Q) = \frac{1}{2} \left( \log \left( \frac{\det \Sigma_Q}{\det \Sigma_P} \right)
	                                                 + \mathrm{tr} \left( \Sigma_Q^{-1} \Sigma_P \right) 
                                                     + ( \mu_Q - \mu_P )^\top \Sigma_Q^{-1} ( \mu_Q - \mu_P ) - d \right) \f$
	 */
	public double KLD(PVectorMatrix LP, PVectorMatrix LQ) {
		PVector mP  = LP.v;
		PMatrix vP  = LP.M;
		PVector mQ  = LQ.v;
		PMatrix vQ  = LQ.M;
		PVector tmp = mQ.Minus(mP);
		return 0.5d * ( Math.log(vQ.Determinant()/vP.Determinant()) 
				+ vQ.Inverse().Multiply(vP).Trace() 
				+ tmp.InnerProduct(vQ.Inverse().MultiplyVectorRight(tmp))
				- LP.dim);
	}


}
