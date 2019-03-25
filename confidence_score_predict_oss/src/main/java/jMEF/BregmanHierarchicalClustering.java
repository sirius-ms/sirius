package jMEF;

import jMEF.Clustering.CLUSTERING_TYPE;


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
 * The Bregman hierarchical clustering is the generalization of the hierarchical clustering towards the exponential family.
 * Given a set of weighted distributions (mixture model), the Bregman hierarchical clustering builds a hierarchical mixture model (class HierarchicalMixtureModel).
 * It is then possible from this hierarchical mixture model to
 *   - quickly deduce a simpler mixture model,
 *   - automatically learn the "optimal" number of components in the mixture model.
 */
public class BregmanHierarchicalClustering {
	

	/**
	 * Type of the likage criterion used.
	 */
	public enum LINKAGE_CRITERION {MINIMUM_DISTANCE, MAXIMUM_DISTANCE, AVERAGE_DISTANCE}; 
	
	/**
	 * Builds a hierarchical mixture model (class HierarchicalMixtureModel) from an input mixture model and a clustering type.
	 * @param  f        input mixture model given in source parameters
	 * @param  type     type of the Bregman divergence used: right-sided, left-sided, or symmetric
	 * @param  linkage  linkage criterion used: minimum, maximum, or average distance
	 * @return          a hierarchical mixture model
	 */
	public static HierarchicalMixtureModel build(MixtureModel f, Clustering.CLUSTERING_TYPE type, LINKAGE_CRITERION linkage){
		
		// Variables
		int i, j, n = f.size;
		MixtureModel               fT        = mixtureL2T(f);
		HierarchicalMixtureModel[] hmm_array = new HierarchicalMixtureModel[n];
		
		// Build the array, each case containing a hierarchical mixture model with one parameter
		for (i=0; i<n; i++){
			MixtureModel  mm  = new MixtureModel(1);
			mm.EF             = fT.EF;
			mm.param[0]       = fT.param[i];
			mm.weight[0]      = 1.0;
			HierarchicalMixtureModel hmm = new HierarchicalMixtureModel();
			hmm.EF            = fT.EF;
			hmm.weight        = fT.weight[i];
			hmm.node          = mm;
			hmm.type          = type;
			hmm.resolutionMax  = 1;
			hmm_array[i]      = hmm;
		}
		
		// General loop
		for (i=0; i<n-1; i++){
			
			// Temporary array containing new hierarchical mixture models
			HierarchicalMixtureModel[] hmm_array_new = new HierarchicalMixtureModel[n-i-1];
			
			// Find two closest hierarchical mixture models in the array
			int[] index = find2ClosestMixtureModels(hmm_array, type, linkage);
			
			// Fusion the two closest hierarchical mixture models in a new one
			HierarchicalMixtureModel hmm, hmm_1, hmm_2;
			MixtureModel  mm , mm_1 , mm_2 ;
			hmm_1 = hmm_array[index[0]];
			hmm_2 = hmm_array[index[1]];
			mm_1  = hmm_1.node;
			mm_2  = hmm_2.node;
			mm    = new MixtureModel( mm_1.size + mm_2.size );
			mm.EF = mm_1.EF;
			for (j=0; j<mm_1.size; j++){
				mm.param[j]  = mm_1.param[j];
				mm.weight[j] = mm_1.weight[j] * hmm_1.weight;
			}
			for (j=0; j<mm_2.size; j++){
				mm.param[mm_1.size+j]  = mm_2.param[j];
				mm.weight[mm_1.size+j] = mm_2.weight[j] * hmm_2.weight;
			}
			mm.normalizeWeights();
			hmm                = new HierarchicalMixtureModel();
			hmm.EF             = mm_1.EF;
			hmm.weight         = hmm_1.weight + hmm_2.weight;
			hmm.node           = mm;
			hmm.leftChild      = hmm_1;
			hmm.rightChild     = hmm_2;
			hmm.type           = hmm_1.type;
			hmm.resolutionMax  = hmm_1.resolutionMax+1;
			hmm_1.parent       = hmm;
			hmm_2.parent       = hmm;
			hmm_array_new[0]   = hmm;
			
			// Copy other hierarchical mixture models in the temporary array
			int cpt = 1;
			for (j=0; j<n-i; j++){
				if (j!=index[0] && j!=index[1]){
					HierarchicalMixtureModel hmm_current = hmm_array[j];
					HierarchicalMixtureModel hmm_new     = new HierarchicalMixtureModel();
					hmm_new.EF            = hmm_current.EF;
					hmm_new.node          = hmm_current.node;
					hmm_new.weight        = hmm_current.weight;
					hmm_new.leftChild     = hmm_current;
					hmm_new.type          = hmm_current.type;
					hmm_new.resolutionMax = hmm_current.resolutionMax+1;
					hmm_current.parent    = hmm_new;
					hmm_array_new[cpt]    = hmm_new;
					cpt++;
				}
			}
			
			// Remplace the array by the temporary array
			hmm_array = hmm_array_new;
		}
		
		// Return the root of the hierarchical mixture model
		return hmm_array[0];
	}
	
	
	/**
	 * Finds the two closest hierarchical mixture model relatively to a Bregman divergence type.	
	 * @param hmmArray    array of hierarchical mixture models
	 * @param type        type of the Bregman divergence used: right-sided, left-sided, or symmetric
	 * @param linkage     linkage criterion used: minimum, maximum, or average distance
	 * return             indexes of the two closest hierarchical mixture models
	 */
	private static int[] find2ClosestMixtureModels(HierarchicalMixtureModel[] hmmArray, Clustering.CLUSTERING_TYPE type, LINKAGE_CRITERION linkage){
		
		// Variable
		int[]  index = new int[2];
		int    n     = hmmArray.length;
		double dmin  = Double.MAX_VALUE;
		
		// Loops
		for (int i=0; i<n; i++)
			for (int j=0; j<n; j++){
				if (i!=j) {

					// Current mixture models
					MixtureModel mm1 = hmmArray[i].node;
					MixtureModel mm2 = hmmArray[j].node;

					// Distance (min, max, or mean) between the two mixture models
					double d=Double.MAX_VALUE;
					if (linkage==LINKAGE_CRITERION.MINIMUM_DISTANCE)
						d = computeMinDistance(mm1, mm2, type) * hmmArray[i].weight * hmmArray[j].weight;
					else if (linkage==LINKAGE_CRITERION.MAXIMUM_DISTANCE)
						d = computeMaxDistance(mm1, mm2, type) * hmmArray[i].weight * hmmArray[j].weight;
					else if (linkage==LINKAGE_CRITERION.AVERAGE_DISTANCE)
						d = computeAverageDistance(mm1, mm2, type) * hmmArray[i].weight * hmmArray[j].weight;

					// Remember the indexes
					if (d<dmin){
						index[0] = i;
						index[1] = j;
						dmin     = d;
					}
				}
			}
		
		// Return the indexes
		return index;
	}
	

	/**
	 * Computes the min-distance between two mixture models relatively to a Bregman divergence type
	 * @param mm1    mixture model
	 * @param mm2    mixture model
	 * @param type   type of the Bregman divergence used: right-sided, left-sided, or symmetric
	 * @return       min-distance between mm1 and mm2
	 */
	private static double computeMinDistance(MixtureModel mm1, MixtureModel mm2, Clustering.CLUSTERING_TYPE type){
		double d = Double.MAX_VALUE;
		for (int i=0; i<mm1.size; i++)
			for (int j=0; j<mm2.size; j++){
				double tmp = 0;
				if (type==CLUSTERING_TYPE.RIGHT_SIDED)
					tmp = mm1.EF.BD(mm1.param[i], mm2.param[j]) * mm1.weight[i] * mm2.weight[j];
				else if (type==CLUSTERING_TYPE.LEFT_SIDED)
					tmp = mm1.EF.BD(mm2.param[j], mm1.param[i]) * mm1.weight[i] * mm2.weight[j];
				else if (type==CLUSTERING_TYPE.SYMMETRIC){
					tmp = 0.5 * ( mm1.EF.BD(mm1.param[i], mm2.param[j]) + mm1.EF.BD(mm2.param[j], mm1.param[i]) ) * mm1.weight[i] * mm2.weight[j];
				}
				d = Math.min(d, tmp);
			}
		return d;
	}
	

	/**
	 * Computes the max-distance between two mixture models relatively to a Bregman divergence type
	 * @param mm1    mixture model
	 * @param mm2    mixture model
	 * @param type   type of the Bregman divergence used: right-sided, left-sided, or symmetric
	 * @return       max-distance between mm1 and mm2
	 */
	private static double computeMaxDistance(MixtureModel mm1, MixtureModel mm2, Clustering.CLUSTERING_TYPE type){
		double d = 0;
		for (int i=0; i<mm1.size; i++)
			for (int j=0; j<mm2.size; j++){
				double tmp = 0;
				if (type==CLUSTERING_TYPE.RIGHT_SIDED)
					tmp = mm1.EF.BD(mm1.param[i], mm2.param[j]) * mm1.weight[i] * mm2.weight[j];
				else if (type==CLUSTERING_TYPE.LEFT_SIDED)
					tmp = mm1.EF.BD(mm2.param[j], mm1.param[i]) * mm1.weight[i] * mm2.weight[j];
				else if (type==CLUSTERING_TYPE.SYMMETRIC){
					tmp = 0.5 * ( mm1.EF.BD(mm1.param[i], mm2.param[j]) + mm1.EF.BD(mm2.param[j], mm1.param[i]) ) * mm1.weight[i] * mm2.weight[j];
				}
				d = Math.max(d, tmp);
			}
		return d;
	}
	

	/**
	 * Computes the mean-distance between two mixture models relatively to a Bregman divergence type
	 * @param mm1    first mixture model
	 * @param mm2    second mixture model
	 * @param type   type of the Bregman divergence used: right-sided, left-sided, or symmetric
	 * @return       mean-distance between mm1 and mm2
	 */
	private static double computeAverageDistance(MixtureModel mm1, MixtureModel mm2, Clustering.CLUSTERING_TYPE type){
		double d = 0;
		for (int i=0; i<mm1.size; i++)
			for (int j=0; j<mm2.size; j++){
				double tmp = 0;
				if (type==CLUSTERING_TYPE.RIGHT_SIDED)
					tmp = mm1.EF.BD(mm1.param[i], mm2.param[j]) * mm1.weight[i] * mm2.weight[j];
				else if (type==CLUSTERING_TYPE.LEFT_SIDED)
					tmp = mm1.EF.BD(mm2.param[j], mm1.param[i]) * mm1.weight[i] * mm2.weight[j];
				else if (type==CLUSTERING_TYPE.SYMMETRIC){
					tmp = 0.5 * ( mm1.EF.BD(mm1.param[i], mm2.param[j]) + mm1.EF.BD(mm2.param[j], mm1.param[i]) ) * mm1.weight[i] * mm2.weight[j];
				}
				d += tmp;
			}
		return d / (mm1.size * mm2.size);
	}
	

	/**
	 * Converts a mixture from source parameters to natural parameters.
	 * @param   fL  mixture model given in source parameters
	 * @return      mixture model given in natural parameters
	 */
	private static MixtureModel mixtureL2T(MixtureModel fLambda){
		int size = fLambda.size;
		MixtureModel fTheta = new MixtureModel(size);
		fTheta.EF = fLambda.EF;
		for (int i=0; i<size; i++){
			fTheta.weight[i] = fLambda.weight[i];
			fTheta.param[i]  = fLambda.EF.Lambda2Theta(fLambda.param[i]);
			fTheta.param[i].type = Parameter.TYPE.NATURAL_PARAMETER;
		}
		return fTheta;
	}

}
