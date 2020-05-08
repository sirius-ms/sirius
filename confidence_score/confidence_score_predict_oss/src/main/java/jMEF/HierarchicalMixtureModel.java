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
 * A hierarchical mixture model is a hierarchical structure (tree) containing the elements of a mixture model.
 * A HierarchicalMixtureModel object is created by the BregmanHierarchicalClustering class.
 */
public class HierarchicalMixtureModel {

	/**
	 * Exponential family member.
	 */
	public ExponentialFamily EF;
	
	/**
	 * Weight of the tree.
	 */
	public double weight;
	
	/**
	 * Node containing a mixture model.
	 */
	public MixtureModel node;
	
	
	/**
	 * Parent of the node.
	 */
	public HierarchicalMixtureModel parent;
	
	
	/**
	 * Left child of the node.
	 */
	public HierarchicalMixtureModel leftChild;
	
	
	/**
	 * Right child of the node.
	 */
	public HierarchicalMixtureModel rightChild;
	
	
	/**
	 * Type of the Bregman divergence.
	 */
	public CLUSTERING_TYPE type;
	
	
	/**
	 * Maximum resolution of the hierarchical mixture model.
	 */
	public int resolutionMax;
	
	
	/**
	 * Class constructor.
	 */
	public HierarchicalMixtureModel(){
		this.EF         = null;
		this.weight     = 0.0d;
		this.node       = null;
		this.parent     = null;
		this.leftChild  = null;
		this.rightChild = null;
		this.type       = CLUSTERING_TYPE.SYMMETRIC;
	}
	
	
	/**
	 * Extracts a mixture model for a given resolution from the hierarchical mixture model.
	 * The resolution 1 corresponds to a mixture model with only one model.
	 * @param   resolution  resolution of the mixture model
	 * @return              mixture model for a given resolution
	 */
	public MixtureModel getResolution(int resolution){
		
		if (resolution==1 || (this.leftChild==null && this.rightChild==null)){
			
			// Mixture model
			MixtureModel mix = new MixtureModel(1);
			mix.EF           = this.EF;
			mix.weight[0]    = this.weight;
			mix.param[0]     = Clustering.getCentroid(this.node, this.type);
			
			// Return the mixture model converted in source (lambda) parameters
			return mixtureT2L(mix);
		}
		else{
			
			// Variables
			int          n1  = 0   , n2  = 0;
			double       w1  = 0   , w2  = 0;
			MixtureModel mm1 = null, mm2 = null;
			
			// Get the mixture model of the right and left child
			if (this.leftChild!=null){
				mm1 = this.leftChild.getResolution(resolution-1);
				n1  = mm1.size;
				w1  = this.leftChild.weight;
			}
			if (this.rightChild!=null){
				mm2 = this.rightChild.getResolution(resolution-1);
				n2  = mm2.size;
				w2  = this.rightChild.weight;
			}
			
			// Fusion the two mixture models
			MixtureModel mix = new MixtureModel(n1+n2);
			mix.EF = this.EF;
			int i;
			for (i=0; i<n1; i++){
				mix.param[i]  = mm1.param[i];
				mix.weight[i] = w1 * mm1.weight[i];
			}
			for (i=0; i<n2; i++){
				mix.param[n1+i]  = mm2.param[i];
				mix.weight[n1+i] = w2 * mm2.weight[i];
			}
			mix.normalizeWeights();
			
			// Return the mixture model
			return mix;
		}
	}
	

	/**
	 * Computes the "optimal" mixture model given a threshold on the KLD (estimated using a Monte-Carlo method).
	 * @param   t  maximum value of the KLD between the initial and the optimal models
	 * @return     optimal mixture model
	 */
	public MixtureModel getOptimalMixtureModel(double t){
		return getOptimalMixtureModel(t, 5000);
	}
	
	
	/**
	 * Computes the "optimal" mixture model given a threshold on the KLD (estimated using a Monte-Carlo method).
	 * @param   t  maximum value of the KLD between the initial and the optimal models
	 * @param   n  number of points drawn from the initial mixture model
	 * @return     optimal mixture model
	 */
	public MixtureModel getOptimalMixtureModel(double t, int n){
		
		// Variables
		MixtureModel mm_init = getResolution(resolutionMax);
		MixtureModel mm_opt  = mm_init.clone();
		int          idx_1   = 1;
		int          idx_2   = this.resolutionMax;
		PVector[]    points  = mm_init.drawRandomPoints(n);
		
		// Dichotomy
		while(idx_1<idx_2-1){
			
			int          idx = (idx_1+idx_2)/2;
			MixtureModel mm  = getResolution(idx);
			double       kld = MixtureModel.KLDMC(mm_init, mm, points);
			//double       kld = MixtureModel.KLDMC(mm_init, mm, n);
			
			if (kld<t){
				idx_2  = idx;
				mm_opt = mm;
			}
			else
				idx_1 = idx;
		}
		
		// Return
		return mm_opt;
	}
	
	
	/**
	 * Converts a mixture from natural parameters to source parameters
	 * @param   fT  mixture model given in natural parameters
	 * @return          mixture model given in source parameters
	 */
	private static MixtureModel mixtureT2L(MixtureModel fT){
		int size = fT.size;
		MixtureModel fL = new MixtureModel(size);
		fL.EF = fT.EF;
		for (int i=0; i<size; i++){
			fL.weight[i] = fT.weight[i];
			fL.param[i]  = fT.EF.Theta2Lambda(fT.param[i]);
			fL.param[i].type = Parameter.TYPE.SOURCE_PARAMETER;
		}
		return fL;
	}
	
}
