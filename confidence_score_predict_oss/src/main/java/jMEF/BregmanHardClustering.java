package jMEF;

import jMEF.Clustering.CLUSTERING_TYPE;

import java.util.Arrays;
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
 * The Bregman hard clustering is the generalization of the hard clustering (also know as k-means) towards the exponential family.
 * Given a set of weighted distributions (mixture model), the Bregman hard clustering partition the set into a determined number of classes.
 * The centroid of each class is a weighted distribution. As a consequence, this algorithm simplifies a mixture model.
 */
public class BregmanHardClustering {
	

	/**
	 * Maximum number of iterations permitted.
	 */
	private static int MAX_ITERATIONS = 30;
	
	
	/**
	 * Simplifies a mixture model f into a mixture model g of m components using Bregman hard clustering algorithm.
	 * @param   f           initial mixture model
	 * @param   m           number of components in g
	 * @param   type        type of the Bregman divergence used (right-sided, left-sided, or symmetric)
	 * @param   iterations  maximum number of iterations allowed
	 * @return              simplified mixture model g of m components
	 */
	public static MixtureModel simplify(MixtureModel f, int m, Clustering.CLUSTERING_TYPE type, int iterations){
		MAX_ITERATIONS = iterations;
		return simplify(f, m, type);
	}
	
	
	/**
	 * Simplifies a mixture model f into a mixture model g of m components using Bregman hard clustering algorithm.
	 * @param   f     initial mixture model
	 * @param   m     number of components in g
	 * @param   type  type of the Bregman divergence used (right-sided, left-sided, or symmetric)
	 * @return        simplified mixture model g of m components
	 */
	public static MixtureModel simplify(MixtureModel f, int m, Clustering.CLUSTERING_TYPE type){
		
		// Initialization
		MixtureModel fT = mixtureL2T(f);
		MixtureModel gT = initialize(fT, m, type);
		//MixtureModel gT = initialize2(fT, m);
		
		// Batch k-means
		int[] repartition = new int[fT.size];
		batchKMeans(fT, gT, type, repartition);
				
		// Return
		return mixtureT2L(gT);
	}
	

	/**
	 * Simplifies a mixture model f into a mixture model g of m components using Bregman hard clustering algorithm.
	 * @param   f     initial mixture model
	 * @param   g     initialization of the mixture model g
	 * @param   type  type of the Bregman divergence used (right-sided, left-sided, or symmetric)
	 * @return        simplified mixture model g of m components
	 */
	public static MixtureModel simplify(MixtureModel f, MixtureModel g, Clustering.CLUSTERING_TYPE type){
		
		// Initialization
		MixtureModel fT = mixtureL2T(f);
		MixtureModel gT = mixtureL2T(g);
		
		// Batch k-means
		int[] repartition = new int[fT.size];
		batchKMeans(fT, gT, type, repartition);
				
		// Return
		return mixtureT2L(gT);
	}
	

	/**
	 * Converts a mixture model from source parameters to natural parameters.
	 * @param   fL  mixture model given in source parameters
	 * @return      mixture model given in natural parameters
	 */
	private static MixtureModel mixtureL2T(MixtureModel fL){
		int size = fL.size;
		MixtureModel fTheta = new MixtureModel(size);
		fTheta.EF = fL.EF;
		for (int i=0; i<size; i++){
			fTheta.weight[i]     = fL.weight[i];
			fTheta.param[i]      = fL.EF.Lambda2Theta(fL.param[i]);
			fTheta.param[i].type = Parameter.TYPE.NATURAL_PARAMETER;
		}
		return fTheta;
	}
	
	
	/**
	 * Converts a mixture model from source parameters to natural parameters.
	 * @param   fT  mixture model given in natural parameters
	 * @return      mixture model given in source parameters
	 */
	private static MixtureModel mixtureT2L(MixtureModel fT){
		int size = fT.size;
		MixtureModel fLambda = new MixtureModel(size);
		fLambda.EF = fT.EF;
		for (int i=0; i<size; i++){
			fLambda.weight[i]     = fT.weight[i];
			fLambda.param[i]      = fT.EF.Theta2Lambda(fT.param[i]);
			fLambda.param[i].type = Parameter.TYPE.SOURCE_PARAMETER;
		}
		return fLambda;
	}
	
	
	/**
	 * Initializes a mixture model g by selecting the first m components of the mixture model f. 
	 * @param   f     initial mixture model
	 * @param   m     number of components in the simplified mixture model
	 * @param   type  type of the Bregman divergence used (right-sided, left-sided, or symmetric)
	 * @return        initialization of the simplified mixture model
	 */
	private static MixtureModel initialize2(MixtureModel f, int m){
		MixtureModel g = new MixtureModel(m);
		g.EF = f.EF;
		for (int i=0; i<m; i++){
			g.weight[i]     = f.weight[i];
			g.param[i]      = f.param[i];
			g.param[i].type = f.param[i].type;
		}
		g.normalizeWeights();
		return g;
	}
	
	
	/**
	 * Initializes a simplified mixture model g from f using k-means++ algorithm. 
	 * @param   f     initial mixture model
	 * @param   m     number of components in the simplified mixture model
	 * @param   type  type of the Bregman divergence used (right-sided, left-sided, or symmetric)
	 * @return        initialization of the simplified mixture model
	 */
	private static MixtureModel initialize(MixtureModel f, int m, Clustering.CLUSTERING_TYPE type){

		// Variables
		int n = f.size;
		int i,j;
		MixtureModel g = new MixtureModel(m);
		g.EF = f.EF;
		Random     rand    = new Random();
		double[][] kld_mat = new double[m][n];

		// "Gaussian already used" (gau) table
		int[] gau = new int[n]; // 
		for (i=0; i<n; i++)
			gau[i]=0;

		// Get the first centroid
		int index   = rand.nextInt(n);
		g.weight[0] = f.weight[index];
		g.param[0]  = f.param[index];
		gau[index]  = 1;

		// Compute the distance between the first centroid to all other Gaussians
		for (i=0; i<n; i++){
			if (type==CLUSTERING_TYPE.RIGHT_SIDED)
				kld_mat[0][i] = f.EF.BD(f.param[i], g.param[0]);
			else if (type==CLUSTERING_TYPE.LEFT_SIDED)
				kld_mat[0][i] = f.EF.BD(g.param[0], f.param[i]);
			else if (type==CLUSTERING_TYPE.SYMMETRIC){
				kld_mat[0][i] = 0.5 * ( f.EF.BD(f.param[i], g.param[0]) + f.EF.BD(g.param[0], f.param[i]) );
			}
		}

		// k-means++ loop
		for (int row=1; row<m; row++){

			double[] kld_min = new double[n];
			int[]   idx      = new int[n];
			for (i=0; i<n; i++){
				double min = Double.MAX_VALUE;
				for (j=0; j<row; j++)
					min = Math.min(min, kld_mat[j][i]);
				kld_min[i] = min;
				idx[i]     = i;
			}
			Quicksort.quicksort(kld_min, idx);

			// Select smallest KLD in the matrix, initialize index for future sort, and sort smallest KLD
			// Compute cumulative function
			double[] kld_cdf = new double[n];
			double   cdf     = 0;
			for (i=0; i<n; i++){
				cdf       += kld_min[i];
				kld_cdf[i] = cdf;
			}

			// Draw a random value in [0,cdf[end-1]]
			double cdf_min = 0;
			for (i=0; i<n; i++)
				if (kld_cdf[i]>0){
					cdf_min = kld_cdf[i];
					break;
				}
			double value = rand.nextDouble() * (kld_cdf[n-1]-cdf_min) + cdf_min;

			// Select the corresponding Gaussian
			index = -1;
			for (i=0; i<n; i++){
				if ( gau[idx[i]]==0 && ( kld_cdf[i]<value || index==-1 ) )
					index = idx[i];
				else if(kld_cdf[i]>=value)
					break;
			}

			// Add the new centroid
			g.weight[row] = f.weight[index];
			g.param[row]  = f.param[index];
			gau[index]    = 1;
	

			// Compute the KLD matrix
			for (i=0; i<n; i++){
				if (type==CLUSTERING_TYPE.RIGHT_SIDED)
					kld_mat[row][i] = f.EF.BD(f.param[i], g.param[row]);
				else if (type==CLUSTERING_TYPE.LEFT_SIDED)
					kld_mat[row][i] = f.EF.BD(g.param[row], f.param[i]);
				else if (type==CLUSTERING_TYPE.SYMMETRIC){
					kld_mat[row][i] = 0.5 * ( f.EF.BD(f.param[i], g.param[row]) + f.EF.BD(g.param[row], f.param[i]) );
				}
			}
		}

		// Normalize the weights
		g.normalizeWeights();

		return g;
	}
	
	
	/**
	 * Performs the batch Bregman k-means algorithm.
	 * @param f            initial mixture model
	 * @param g            simplified mixture model
	 * @param type         type of the Bregman divergence used (right-sided, left-sided, or symmetric)
	 * @param repartition  array of repartition
	 */
	private static void batchKMeans(MixtureModel f, MixtureModel g, Clustering.CLUSTERING_TYPE type, int[] repartition){
		int[] repartition_old = new int[f.size];
		int   iterations      = 0;
		do{
			repartition_old = repartition.clone();
			computeRepartition( f, g, type, repartition );
			computeCentroids(   f, g, type, repartition );
			iterations++;
		} while( !Arrays.equals(repartition, repartition_old) && iterations<MAX_ITERATIONS );
	}

	
	/**
	 * Performs the incremental Bregman k-means algorithm.
	 * @param f            initial mixture model
	 * @param g            simplified mixture model containing the centroids
	 * @param type         type of the Bregman divergence used (right-sided, left-sided, or symmetric)
	 * @param repartition  array of repartition
	 * @return             true if the incremental k-means decraeses the loss function, false otherwise
	 */
	private static boolean incrementalKMeans(MixtureModel f, MixtureModel g, Clustering.CLUSTERING_TYPE type, int[] repartition){
		boolean cond = false;
		int iterations = 0;
		double lf_init = getLossFunction(f, g, type); 
		double lf_min  = lf_init;
		int n = f.size;
		int m = g.size;
		int i;
		int[] rep_opt = new int[n];
		
		MixtureModel g_tmp = new MixtureModel(m);
		
		int[] count = new int[m];
		for (i=0; i<n; i++)
			count[repartition[i]]++;
		

		for (i=0; i<n; i++){
			int[] rep_tmp = Arrays.copyOf(repartition,n);
			int cl = repartition[i];
			if (count[cl]>1){
				for (int j=0; j<m; j++){
					if (j!=cl){
						rep_tmp[i]=j;
						computeCentroids(f, g_tmp, type, rep_tmp );
						double lf = getLossFunction(f, g_tmp, type);
						if (lf<lf_min){
							lf_min = lf;
							rep_opt = Arrays.copyOf(rep_tmp,n);
							iterations++;
							System.out.println(lf_min);
						}
					}
				}
			}
		}

		if ((lf_min/lf_init)<0.98){
			cond=true;
			for (i=0; i<n; i++)
				repartition[i] = rep_opt[i];
			computeCentroids(f, g, type, repartition);
			System.out.println("--> " + getLossFunction(f, g, type));
		}	
		
		return cond;
	}
	
	
	/**
	 * Computes the repartition of components of f in g
	 * @param f            initial mixture model
	 * @param g            simplified mixture model
	 * @param type         type of the Bregman divergence used (right-sided, left-sided, or symmetric)
	 * @param repartition  array of repartition
	 */
	private static void computeRepartition(MixtureModel f, MixtureModel g, Clustering.CLUSTERING_TYPE type, int[] repartition){
		int n = f.size;
		int m = g.size;
		    
		for (int i=0; i<n; i++){

			// Initial values
			int    index = -1;
			double d_min = Double.MAX_VALUE;
						
			// Compute distance between f[i] and g[j]
			for (int j=0; j<m ; j++){
				double d = 0;
				if (type==CLUSTERING_TYPE.RIGHT_SIDED)
					d = f.EF.BD(f.param[i], g.param[j]);
				else if (type==CLUSTERING_TYPE.LEFT_SIDED)
					d = f.EF.BD(g.param[j], f.param[i]);
				else if (type==CLUSTERING_TYPE.SYMMETRIC){
					d = 0.5 * ( f.EF.BD(f.param[i], g.param[j]) + f.EF.BD(g.param[j], f.param[i]) );
				}
				if (d<d_min){
					d_min = d;
					index = j;
				}
			}
			
			// Set the repartition array
			repartition[i] = index;
		}
	}
	
	
	/**
	 * Computes the centroids of the clusters.
	 * @param f            initial mixture model
	 * @param g            simplified mixture model containing the centroids
	 * @param type         type of the Bregman divergence used (right-sided, left-sided, or symmetric)
	 * @param repartition  array of repartition
	 */
	private static void computeCentroids(MixtureModel f, MixtureModel g, Clustering.CLUSTERING_TYPE type, int[] repartition){
		int n = f.size;
		int m = g.size;
		int i,j;
		
		// Count how many param compose each class
		int[] count = new int[m];
		for (i=0; i<n; i++)
			count[repartition[i]]++;

		for (i=0; i<m; i++){
			
			if (count[i]==0){
				g.param[i]  = null;
				g.weight[i] = 0;
				System.err.printf("The class %d is empty. Impossible to compute the centroid.", i);
			}
			else if (count[i]==1){
				for (j=0; j<n; j++){
					if (repartition[j]==i){
						g.param[i]  = f.param[j];
						g.weight[i] = f.weight[j];
						break;
					}
				}
			}
			else{
				MixtureModel mix = new MixtureModel(count[i]);
				mix.EF = f.EF;
				
				// Build the mixture model and normalize weights
				int ind = 0;
				double sum = 0;
				for (j=0; j<n; j++){
					if (repartition[j]==i){
						mix.weight[ind] = f.weight[j];
						mix.param[ind]  = f.param[j];
						sum            += f.weight[j];
						ind++;
					}
				}
				mix.normalizeWeights();
				
				// Compute the centroid
				g.param[i] = Clustering.getCentroid(mix, type);
				
				// Set the weight of the centroid
				g.weight[i] = sum;
			}
		}
	}
	
	
	/**
	 * Computes the loss function.
	 * @param   f     initial mixture model
	 * @param   g     simplified mixture model containing the centroids
	 * @param   type  type of the Bregman divergence used (right-sided, left-sided, or symmetric)
	 * @return        value of the loss function
	 */
	private static double getLossFunction(MixtureModel f, MixtureModel g, Clustering.CLUSTERING_TYPE type){
		int    n     = f.size;
		int    m     = g.size;
		double value = 0;
		for (int i=0; i<n; i++){
			double min = Double.MAX_VALUE;
			for (int j=0; j<m; j++){
				if (type==CLUSTERING_TYPE.RIGHT_SIDED)
					min = f.EF.BD(f.param[i], g.param[j]);
				else if (type==CLUSTERING_TYPE.LEFT_SIDED)
					min = f.EF.BD(g.param[j], f.param[i]);
				else if (type==CLUSTERING_TYPE.SYMMETRIC){
					min = 0.5 * ( f.EF.BD(f.param[i], g.param[j]) + f.EF.BD(g.param[j], f.param[i]) );
				}
			}
			value += min;
		}
		return value;
	}

}
