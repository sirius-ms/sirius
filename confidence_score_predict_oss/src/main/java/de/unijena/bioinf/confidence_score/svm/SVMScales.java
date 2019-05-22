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


    public double[] getMedians() {
        return medians;
    }

    public double[] getDeviations() {
        return deviations;
    }

    public double[] getMin_feature_values() {
        return min_feature_values;
    }

    public double[] getMax_feature_values() {
        return max_feature_values;
    }
}
