package jMEF;


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
 * A statistical distribution is parameterized by a set of values (parameters).
 * The PVectorMatrix class implements a parameter object.
 * Parameters are represented as a mixed type vector-matrix.
 */
public final class PVectorMatrix extends Parameter{


	/**
	 * Constant for serialization.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Vector parameter.
	 */
	public PVector v;
	
	
	/**
	 * Matrix parameter.
	 */
	public PMatrix M;


	/**
	 * Dimension of the matrix-vector.
	 */
	public int dim;


	/**
	 * Class constructor.
	 * @param dim dimension of the matrix
	 */
	public PVectorMatrix(int dim){
		this.dim = dim;
		this.v   = new PVector(dim);
		this.M   = new PMatrix(dim);	
	}


	/**
	 * Adds (not in place) the current vector-matrix \f$ (v_1, m_1) \f$ to the vector-matrix \f$ (v_2, m_2) \f$.
	 * @param   v2m2  vector-matrix \f$ (v_2, m_2) \f$
	 * @return        \f$ ( v_1 + v_2, m_1 + m_2 ) \f$
	 */
	public PVectorMatrix Plus(Parameter v2m2){
		PVectorMatrix Q      = (PVectorMatrix)v2m2;
		PVectorMatrix result = new PVectorMatrix(Q.v.dim);
		result.v = this.v.Plus(Q.v);
		result.M = this.M.Plus(Q.M);
		return result;	
	}


	/**
	 * Subtracts (not in place) the vector-matrix \f$ (v_2, m_2) \f$ to the current vector-matrix \f$ (v_1, m_1) \f$.
	 * @param   v2m2  vector-matrix \f$ (v_2, m_2) \f$
	 * @return        \f$ ( v_1 - v_2, m_1 - m_2 ) \f$
	 */
	public PVectorMatrix Minus(Parameter v2m2){
		PVectorMatrix Q      = (PVectorMatrix)v2m2;
		PVectorMatrix result = new PVectorMatrix(Q.v.dim);
		result.v = this.v.Minus(Q.v);
		result.M = this.M.Minus(Q.M);
		return result;	
	}


	/**
	 * Multiplies (not in place) the current vector-matrix \f$ (v,m) \f$ by a real number \f$ \lambda \f$.
	 * @param  lambda  value \f$ \lambda \f$
	 * @return         \f$ ( \lambda v , \lambda m ) \f$
	 */
	public PVectorMatrix Times(double lambda){
		PVectorMatrix result = new PVectorMatrix(v.dim);
		result.v = this.v.Times(lambda);
		result.M = (this.M).Times(lambda);
		return result;	
	}


	/**
	 * Computes the inner product (real number) between the current vector-matrix \f$ ( v_1 , m_1 ) \f$ and the vector-matrix \f$ ( v_2 , m_2 ) \f$.
	 * @param   v2m2  vector-matrix \f$ (v_2, m_2) \f$
	 * @return        \f$ \langle v_1,v_2 \rangle + \langle m_1,m_2 \rangle \f$ 
	 */
	public double InnerProduct(Parameter v2m2){
		PVectorMatrix Q = (PVectorMatrix)v2m2;	
		return this.v.InnerProduct(Q.v) + this.M.InnerProduct(Q.M);
	}


	/**
	 * Generates a random vector-matrix \f$ (v,m) \f$ such as \f$ m \f$ is a positive definite matrix.
	 * @param  dim  dimension of the matrix
	 * @return      random vector-matrix \f$ ( v , m ) \f$
	 */
	public static PVectorMatrix RandomDistribution(int dim){
		PVectorMatrix vM = new PVectorMatrix(dim);
		vM.v = PVector.Random(dim);
		vM.M = PMatrix.RandomPositiveDefinite(dim);
		return vM;
	}


	/**
	 * Verifies if two vector-matrices \f$ (v_1 , m_1) \f$ and \f$ (v_2 , m_2) \f$ are similar. 
	 * @param   v1m1  vector-matrix \f$ (v_1, m_1) \f$
	 * @param   v2m2  vector-matrix \f$ (v_2, m_2) \f$
	 * @return        true if \f$ v_1 = v_2 \f$ and \f$ m_1 = m_2 \f$, false otherwise
	 */
	public static boolean equals(PVectorMatrix v1m1, PVectorMatrix v2m2){
		return (PVector.equals(v1m1.v, v2m2.v) && PMatrix.equals(v1m1.M, v2m2.M));
	}
	

	/**
	 * Method toString.
	 * @return value of the vector-matrix as a string
	 */
	public String toString(){
		return "" + v + "\n" + M + "\n";	
	}


	/**
	 * Creates and returns a copy of the instance.
	 * @return a clone of the instance.
	 */
	public Parameter clone(){
		PVectorMatrix param = new PVectorMatrix(this.dim);
		param.type          = this.type;
		param.v             = (PVector)this.v.clone();
		param.M             = (PMatrix)this.M.clone();
		return param;
	}
	
	
	/**
	 * Returns parameters' dimension.
	 * @return parameters' dimension.
	 */
	public int getDimension(){
		return this.v.dim;
	}
	
}
