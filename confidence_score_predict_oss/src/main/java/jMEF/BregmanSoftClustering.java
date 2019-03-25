package jMEF;

import java.util.Vector;

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
 * The Bregman soft clustering is the generalization of the soft clustering (also know as expectation-maximization algorithm) towards the exponential family.
 * Given a set of points, the Bregman soft clustering algorithm estimates the parameters of the mixture model for a chosen exponential family.
 */
public class BregmanSoftClustering {

	
	/**
	 * Maximum number of iterations permitted.
	 */
	private static int MAX_ITERATIONS = 30;

	
	/**
	 * Initializes a mixture model from clusters of points, each cluster containing a set of points.
	 * @param   clusters  clusters of points
	 * @param   EF        exponential family member
	 * @return            initialized mixture model
	 */
	public static MixtureModel initialize(Vector<PVector>[] clusters, ExponentialFamily EF){
		
		// Mixture initialization
		MixtureModel mm = new MixtureModel(clusters.length);
		mm.EF = EF;
		
		// Amount of points
		int nb = 0;
		for (int i=0; i<clusters.length; i++)
			nb += clusters[i].size();
			
		// Loop on clusters
		for (int i=0; i<clusters.length; i++){
			
			// Weight
			mm.weight[i] = ((double)clusters[i].size())/nb;
			
			// Expectation parameters
			Parameter param = EF.t(clusters[i].get(0));
			for (int j=1; j<clusters[i].size(); j++)
				param = param.Plus(mm.EF.t((PVector)clusters[i].get(j)));
			param = param.Times(1.0d/clusters[i].size());
			mm.param[i] = mm.EF.Eta2Lambda(param);
		}
		
		// Return
		return mm;
	}
	
	/**
	 * Processes the Bregman soft clustering (EM) algorithm.
	 * @param   pointSet    set of points
	 * @param   fL          initial mixture model given in source parameters
	 * @param   iterations  maximum number of iterations allowed
	 * @return              estimated mixture model
	 */
	public static MixtureModel run(PVector[] pointSet, MixtureModel fL, int iterations){
		MAX_ITERATIONS = iterations;
		return run(pointSet, fL);
	}


	/**
	 * Processes the Bregman soft clustering (EM) algorithm.
	 * @param   pointSet  set of points
	 * @param   fL        initial mixture model given in source parameters
	 * @return            estimated mixture model
	 */
	public static MixtureModel run(PVector[] pointSet, MixtureModel fL){

		// Variables
		int col, row;
		int n          = fL.size;
		int m          = pointSet.length;
		int iterations = 0;
		
		// Conversion of the mixture in expectation parameters
		MixtureModel fH = mixtureL2H(fL);
		
		// Initial log likelihood
		double logLikelihoodNew       = logLikelihood(pointSet, fH);
		double logLikelihoodThreshold = Math.abs(logLikelihoodNew) * 0.01;
		double logLikelihoodOld;
		
		// Display
		//System.out.printf("%2d : %12.6f\n", iterations, logLikelihoodNew);

		// Start of the soft clustering		
		do{
			
			// Log likelihood update
			logLikelihoodOld = logLikelihoodNew; 

			// Step E: computation of P (matrix of weights)
			double[][] p = new double[m][n];
			for (row=0; row<m; row++){
				double sum = 0;
				for (col=0; col<n; col++){
					double tmp  = fH.weight[col] * Math.exp( fL.EF.G(fH.param[col]) +  fL.EF.t(pointSet[row]).Minus(fH.param[col]).InnerProduct(fL.EF.gradG(fH.param[col])) );
					p[row][col] = tmp;
					sum        += tmp;
				}
				for (col=0; col<n; col++)
					p[row][col] /= sum;
			}

			// Step M: computation of parameters
			for (col=0; col<n; col++){
				double sum    = p[0][col];
				fH.param[col] = fL.EF.t(pointSet[0]).Times(p[0][col]);
				for (row=1; row<m; row++){
					sum          += p[row][col];
					fH.param[col] = fH.param[col].Plus( fL.EF.t(pointSet[row]).Times(p[row][col]) );
				}
				fH.weight[col]     = sum / m;
				fH.param[col]      = fH.param[col].Times(1./sum);
				fH.param[col].type = TYPE.EXPECTATION_PARAMETER;
			}
			
			// Iterations and log likelihood update
			iterations++;
			logLikelihoodNew = logLikelihood(pointSet, fH);
			
			// Display
			//System.out.printf("%2d : %12.6f\n", iterations, logLikelihoodNew);

		}while ( iterations<MAX_ITERATIONS && Math.abs(logLikelihoodOld-logLikelihoodNew)>logLikelihoodThreshold );
		
		// Conversion of mixture in source parameters
		return mixtureH2L(fH);
		
	}

	
	/**
	 * Converts a mixture model from source parameters to expectation parameters.
	 * @param   fL  mixture model in source parameters
	 * @return      mixture model in expected parameters
	 */
	private static MixtureModel mixtureL2H(MixtureModel fL){
		int size        = fL.size;
		MixtureModel fH = new MixtureModel(size);
		fH.EF = fL.EF;
		for (int i=0; i<size; i++){
			fH.weight[i] = fL.weight[i];
			fH.param[i]  = fL.EF.Theta2Eta(fL.EF.Lambda2Theta(fL.param[i]));
		}
		return fH;
	}
	
	
	/**
	 * Converts a mixture model from expectation parameters to sources parameters
	 * @param   fH  mixture model in expected parameters
	 * @return      mixture model in source parameters
	 */
	private static MixtureModel mixtureH2L(MixtureModel fH){
		int size = fH.size;
		MixtureModel fL = new MixtureModel(size);
		fL.EF = fH.EF;
		for (int i=0; i<size; i++){
			fL.weight[i] = fH.weight[i];
			fL.param[i]  = fH.EF.Theta2Lambda( fH.EF.Eta2Theta(fH.param[i]) );
		}
		return fL;
	}

	
	/**
	 * Computes the log likelihood.
	 * @param   points  set of points
	 * @param   f       mixture model
	 * @return          log likelihood
	 */
	private static double logLikelihood(PVector[] points, MixtureModel f){
		double value = 0;
		for (int i=0; i<points.length; i++)
			value += Math.log( f.density(points[i]) );
		return value;
	}
}
