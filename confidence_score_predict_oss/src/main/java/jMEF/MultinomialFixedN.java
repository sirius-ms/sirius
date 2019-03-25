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
 * The Multinomial distribution, with \f$ n\f$ fixed, is an exponential family and, as a consequence, the probability density function is given by
 * \f[ f(x; \mathbf{\Theta}) = \exp \left( \langle t(x), \mathbf{\Theta} \rangle - F(\mathbf{\Theta}) + k(x) \right) \f]
 * where \f$ \mathbf{\Theta} \f$ are the natural parameters.
 * This class implements the different functions allowing to express a Multinomial distribution as a member of an exponential family.
 * 
 * @section Parameters
 * 
 * The parameters of a given distribution are:
 *   - Source parameters \f$\mathbf{\Lambda} = (p_1,\cdots,p_k) \in [0,1]^k\f$
 *   - Natural parameters \f$\mathbf{\Theta} = (\theta_1,\cdots,\theta_{k-1}) \in R^{k-1}\f$
 *   - Expectation parameters \f$ \mathbf{H} = (\eta_1,\cdots,\eta_{k-1}) \in [0,n]^{k-1} \f$
 */
public final class MultinomialFixedN extends ExponentialFamily<PVector, PVector>{	

	
	/**
	 * Constant for serialization.
	 */
	private static final long serialVersionUID = 1L;
	
	
	/**
	 * Parameter n.
	 */
	private int n;
	
	
	/**
	 * Class constructor.
	 */
	public MultinomialFixedN(){
		this.n = 100;
	}
	
	
	/**
	 * Class constructor.
	 * @param n parameter n
	 */
	public MultinomialFixedN(int n){
		this.n = n;
	}

	
	/**
	 * Computes \f$ F( \mathbf{\Theta} ) \f$.
	 * @param   T  parameters \f$ \mathbf{\Theta} = (\theta_1, \cdots, \theta_{k-1}) \f$
	 * @return     \f$ F(\mathbf{\Theta}) = n \log \left( 1 + \sum_{i=1}^{k-1} \exp \theta_i \right) - \log n! \f$
	 */
	public double F(PVector T){
		double sum = 0;
		for (int i=0; i<T.getDimension(); i++)
			sum += Math.exp(T.array[i]);
		return n * Math.log( 1 + sum ) - Math.log(fact(n));
	}


	/**
	 * Computes \f$ \nabla F ( \mathbf{\Theta} )\f$.
	 * @param   T  naturel parameters \f$ \mathbf{\Theta} = (\theta_1, \cdots, \theta_{k-1}) \f$
	 * @return     \f$ \nabla F( \mathbf{\Theta} ) = \left( \frac{n \exp \theta_i}{1 + \sum_{j=1}^{k-1} \exp \theta_j} \right)_i \f$
	 */
	public PVector gradF(PVector T){
		
		// Sum
		double sum = 0;
		for (int i=0; i<T.getDimension(); i++)
			sum += Math.exp(T.array[i]);
		
		// Gradient
		PVector gradient  = new PVector(T.getDimension());
		gradient.type     = TYPE.EXPECTATION_PARAMETER;
		for (int i=0; i<T.getDimension(); i++)
			gradient.array[i] = (n * Math.exp(T.array[i])) / (1 + sum);
		
		// Return
		return gradient;
	}


	/**
	 * Computes \f$ G(\mathbf{H})\f$.
	 * @param   H  expectation parameters \f$ \mathbf{H} = (\eta_1, \cdots, \eta_{k-1}) \f$
	 * @return     \f$ G(\mathbf{H}) = \left( \sum_{i=1}^{k-1} \eta_i \log \eta_i \right) + \left( n - \sum_{i=1}^{k-1} \eta_i \right) \log \left( n - \sum_{i=1}^{k-1} \eta_i \right) \f$
	 */
	public double G(PVector H){
		double sum1 = 0;
		double sum2 = 0;
		for (int i=0; i<H.getDimension(); i++){
			sum1 += H.array[i] * Math.log(H.array[i]);
			sum2 += H.array[i];
		}
		return sum1 + (n-sum2) * Math.log(n-sum2);
	}

	
	/**
	 * Computes \f$ \nabla G (\mathbf{H})\f$
	 * @param   H  expectation parameters \f$ \mathbf{H} = (\eta_1, \cdots, \eta_{k-1}) \f$
	 * @return     \f$ \nabla G( \mathbf{H} ) = \left( \log \left( \frac{\eta_i}{n - \sum_{j=1}^{k-1} \eta_j} \right) \right)_i \f$
	 */
	public PVector gradG(PVector H){

		// Sum
		double sum = 0;
		for (int i=0; i<H.getDimension(); i++)
			sum += H.array[i];
		
		// Gradient
		PVector gradient  = new PVector(H.getDimension());
		gradient.type     = TYPE.NATURAL_PARAMETER;
		for (int i=0; i<H.getDimension(); i++)
			gradient.array[i] = Math.log( H.array[i] / (n - sum ) );
		
		// Return
		return gradient;
	}
	
	
	/**
	 * Computes the sufficient statistic \f$ t(x)\f$.
	 * @param   x  a point
	 * @return     \f$ t(x) = (x_1, \cdots, x_{k-1}) \f$
	 */
	public PVector t(PVector x){
		PVector t = new PVector(x.getDimension()-1);
		t.type    = TYPE.EXPECTATION_PARAMETER;
		for (int i=0; i<x.getDimension()-1; i++)
			t.array[i] = x.array[i];
		return t;
	}


	/**
	 * Computes the carrier measure \f$ k(x) \f$.
	 * @param   x  a point
	 * @return     \f$ k(x) = - \sum_{i=1}^{k} \log x_i ! \f$
	 */
	public double k(PVector x){
		double sum = 0;
		for (int i=0; i<x.getDimension(); i++)
			sum -= Math.log(fact(x.array[i]));
		return sum;
	}


	/**
	 * Converts source parameters to natural parameters.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = (p_1, \cdots, p_k)\f$
	 * @return     natural parameters \f$ \mathbf{\Theta} = \left( \log \left( \frac{p_i}{p_k} \right) \right)_i \f$
	 */
	public PVector Lambda2Theta(PVector L){
		PVector theta  = new PVector(L.getDimension()-1);
		theta.type     = TYPE.NATURAL_PARAMETER;
		for (int i=0; i<L.getDimension()-1; i++)
			theta.array[i] = Math.log( L.array[i] / L.array[L.getDimension()-1] );
		return theta;
	}


	/**
	 * Converts natural parameters to source parameters.
	 * @param   T  natural parameters \f$ \mathbf{\Theta} = ( \theta_1, \cdots, \theta_{k-1} )\f$
	 * @return     source parameters \f$ \mathbf{\Lambda} =	\begin{cases}
	                                                          p_i = \frac{\exp \theta_i}{1 + \sum_{j=1}^{k-1}(\exp \theta_j)} & \mbox{if $i<k$}\\
	                                                          p_k = \frac{1}{1 + \sum_{j=1}^{k-1}(\exp \theta_j)}
	                                                        \end{cases} \f$
	 */
	public PVector Theta2Lambda(PVector T){
		
		// Sums
		double sum = 0;
		for (int i=0; i<T.getDimension(); i++)
			sum += Math.exp(T.array[i]);
		
		// Conversion
		PVector lambda  = new PVector(T.getDimension()+1);
		lambda.type     = TYPE.SOURCE_PARAMETER;
		for (int i=0; i<T.getDimension(); i++)
			lambda.array[i] = Math.exp(T.array[i]) / (1.0 + sum);
		lambda.array[T.getDimension()] = 1.0 / (1.0 + sum);

		// Return
		return lambda;	
	}


	/**
	 * Converts source parameters to expectation parameters.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = ( p_1, \cdots, p_k )\f$
	 * @return     expectation parameters \f$ \mathbf{H} = \left( n p_i \right)_i\f$
	 */
	public PVector Lambda2Eta(PVector L){
		PVector H  = new PVector(L.getDimension()-1);
		H.type     = TYPE.EXPECTATION_PARAMETER;
		for (int i=0; i<L.getDimension()-1; i++)
			H.array[i] = n * L.array[i];
		return H;
	}


	/**
	 * Converts expectation parameters to source parameters.
	 * @param   H  natural parameters \f$ \mathbf{H} = (\eta_1, \cdots, \eta_{k-1})\f$
	 * @return     source parameters \f$ \mathbf{\Lambda} = \begin{cases}
	                                                          p_i = \frac{\eta_i}{n} & \mbox{if $i<k$}\\
	                                                          p_k = \frac{n - \sum_{j=1}^{k-1} \eta_j}{n}
	                                                        \end{cases}\f$
	 */

	public PVector Eta2Lambda(PVector H){
		PVector L  = new PVector(H.getDimension()+1);
		L.type     = TYPE.SOURCE_PARAMETER;
		double sum = 0;
		for (int i=0; i<H.getDimension(); i++){
			L.array[i] = H.array[i] / n;
			sum += H.array[i];
		}
		L.array[H.getDimension()] = (n - sum) / n;
		return L;
	}
	

	/**
	 * Computes the density value \f$ f(x) \f$.
	 * @param  x      point
	 * @param  param  parameters (source, natural, or expectation)
	 * @return        \f$ f(x_1,\cdots,x_k;p_1,\cdots,p_k,n) = \frac{n!}{x_1! \cdots x_k!} p_1^{x_1} \cdots p_k^{x_k} \f$
	 */
	public double density(PVector x, PVector param){
		if (param.type==TYPE.SOURCE_PARAMETER){
			double prod1 = 1;
			double prod2 = 1;
			for (int i=0; i<param.getDimension(); i++){
				prod1 *= fact(x.array[i]);
				prod2 *= Math.pow(param.array[i], x.array[i]);
			}
			return (fact(n) * prod2) / prod1;
		}
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
	 * Draws a point from the considered distribution.
	 * @param   L  source parameters \f$ \mathbf{\Lambda} = ( p_1, \cdots, p_k )\f$
	 * @return     a point
	 */
	public PVector drawRandomPoint(PVector L) {
		
		// Variables
		int      k = L.dim;
		double[] p = new double[k];
		
		// Build array of sum of probabilities
		p[0] = L.array[0];
		for (int i=1; i<k-1; i++)
			p[i] = p[i-1] + L.array[i];
		p[k-1] = 1;
		
		// Draw a point
		PVector x = new PVector(k);
		for (int i=0; i<n; i++){
			double u   = Math.random();
			int    idx = 0;
			while(u>p[idx])
				idx++;
			x.array[idx] += 1;
		}
		
		// Return
		return x;
	}


	/**
	 * Computes the Kullback-Leibler divergence between two Binomial distributions.
	 * @param   LA  source parameters \f$ \mathbf{\Lambda}_\alpha \f$
	 * @param   LB  source parameters \f$ \mathbf{\Lambda}_\beta \f$
	 * @return      \f$ D_{\mathrm{KL}}(f_1\|f_2) = n p_{\alpha,k} \log \frac{p_{\alpha,k}}{p_{\beta,k}} - n \sum_{i=1}^{k-1} p_{\alpha,i} \log \frac{p_{\beta,i}}{p_{\alpha,i}} \f$
	 */
	public double KLD(PVector LA, PVector LB) {
		int k = LA.getDimension()-1;
		double sum = 0;
		for (int i=0; i<k; i++)
			sum += LA.array[i] * Math.log( LB.array[i] / LA.array[i] );
		return n * LA.array[k] * Math.log(LA.array[k]/LB.array[k]) - n * sum;
	}



}
