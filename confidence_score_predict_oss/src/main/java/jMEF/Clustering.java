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
 * This class provides the functions to compute the centroid of a mixture of exponential families.
 * The centroid, depending on the non-symmetric Bregman divergence, can be:
 *   - right-sided,
 *   - left-sided,
 *   - symmetric.
 */
public class Clustering{
	
	
	/**
	 * Type of the Bregman divergence used.
	 */
	public enum CLUSTERING_TYPE {LEFT_SIDED, RIGHT_SIDED, SYMMETRIC}; 

	
	/**
	 * Computes the center of mass (right-sided centroid) of a mixture model f.
	 * @param   f  mixture model given in natural parameters
	 * @return     center of mass of f
	 */
	public static Parameter getCenterOfMass(MixtureModel f){
		Parameter centroid = f.param[0].Times(f.weight[0]);
		double    sum      = f.weight[0];
		for(int i=1; i<f.size; i++){
			centroid  = centroid.Plus(f.param[i].Times(f.weight[i]));
			sum      += f.weight[i];
		}
		return centroid.Times(1./sum);
	}

	
	/**
	 * Computes the generalized centroid (left-sided centroid) of a mixture model f.
	 * This centroid depends on the exponential family used in the mixture model.
	 * @param   EF  exponential family
	 * @param   f   mixture model given in natural parameters
	 * @return      generalized centroid of f
	 */
	public static Parameter getGeneralizedCentroid(ExponentialFamily EF, MixtureModel f){
		Parameter centroid = EF.gradF(f.param[0]).Times(f.weight[0]);
		double    sum      = f.weight[0];
		for(int i=1; i<f.size; i++){
			centroid  = centroid.Plus( EF.gradF(f.param[i]).Times(f.weight[i]) );
			sum      += f.weight[i];	
		}
		return EF.gradG(centroid.Times(1./sum));
	}

	
	/**
	 * Computes the symmetric centroid of a mixture model f approximated by the geodesic walk algorithm.
	 * This centroid depends on the exponential family used in the mixture model.
	 * @param   EF  exponential family
	 * @param   f   mixture model given in natural parameters
	 * @return      symmetric centroid of f
	 */
	public static Parameter getSymmetricCentroid(ExponentialFamily EF, MixtureModel f){

		Parameter thetageodesic, centroid, thetaR, thetaL;

		// Computation of right-sided and left-sided centroids
		thetaR = getCenterOfMass(f);
		thetaL = getGeneralizedCentroid(EF,f);

		// Computation of symmetric centroids using geodesic walk
		double l;
		double lmin = 0.0;
		double lmax = 1.0; 

		while( (lmax-lmin)>1.0e-6 ){
			l             = (lmin+lmax)/2.0;
			thetageodesic = EF.GeodesicPoint(thetaR, thetaL, l);
			if (EF.BD(thetageodesic,thetaR)>EF.BD(thetaL,thetageodesic)) 
				lmax=l; 
			else 
				lmin=l;
		}
		l        = (lmin+lmax)/2.0;
		centroid = EF.GeodesicPoint(thetaR, thetaL, l);

		return centroid;
	}
	
	
	/**
	 * Computes the centroid of a mixture model. This centroid is sided (right- or left-sided) or is symmetric.
	 * @param  f     mixture model
	 * @param  type  type of Bregman divergence used (right-sided, left-sided, or symmetric)
	 * @return       sided or symmetric centroid of f
	 */
	public static Parameter getCentroid(MixtureModel f, CLUSTERING_TYPE type){
		Parameter centroid;
		if (type==CLUSTERING_TYPE.RIGHT_SIDED)
			centroid = Clustering.getCenterOfMass(f);
		else if (type==CLUSTERING_TYPE.LEFT_SIDED)
			centroid = Clustering.getGeneralizedCentroid(f.EF, f);
		else
			centroid = Clustering.getSymmetricCentroid(f.EF, f);
		return centroid;
	}

}
