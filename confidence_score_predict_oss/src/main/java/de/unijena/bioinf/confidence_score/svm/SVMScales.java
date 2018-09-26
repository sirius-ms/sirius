package de.unijena.bioinf.confidence_score.svm;

public class SVMScales{

    double[] medians;
    double[] deviations;
    double[] min_feature_values;
    double[] max_feature_values;

    public SVMScales(double[] medians,double[] deviations,double[] mins, double[] maxs){

        this.medians=medians;
        this.deviations=deviations;
        this.min_feature_values=mins;
        this.max_feature_values=maxs;


    }



}
