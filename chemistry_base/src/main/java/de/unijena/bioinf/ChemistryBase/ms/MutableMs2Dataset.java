package de.unijena.bioinf.ChemistryBase.ms;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ge28quv on 05/07/17.
 */
public class MutableMs2Dataset implements Ms2Dataset {

    private List<Ms2Experiment> experiments;
    String profile; //todo enum
    double isolationWindowWidth;
    private IsolationWindow isolationWindow;
    private MeasurementProfile measurementProfile;


    public MutableMs2Dataset() {
        experiments = new ArrayList<>();
        profile = "default";
    }

    /**
     *
     * @param experiments
     * @param profile profile which is used for Sirius
     * @param isolationWindowWidth
     * @param measurementProfile
     */
    public MutableMs2Dataset(List<Ms2Experiment> experiments, String profile, double isolationWindowWidth, MeasurementProfile measurementProfile) {
        this.experiments = experiments;
        this.profile = profile;
        this.isolationWindowWidth = isolationWindowWidth;
        this.measurementProfile = measurementProfile;
    }

    public MutableMs2Dataset(Ms2Dataset dataset) {
        this.experiments = new ArrayList<>();
        for (Ms2Experiment experiment : dataset.getExperiments()) {
            this.experiments.add(new MutableMs2Experiment(experiment));
        }
        this.profile = dataset.getProfile();
        this.isolationWindowWidth = dataset.getIsolationWindowWidth();
        this.measurementProfile = new MutableMeasurementProfile(dataset.getMeasurementProfile());
        this.isolationWindow = dataset.getIsolationWindow();
    }


    public IsolationWindow getIsolationWindow() {
        return isolationWindow;
    }

    public void setIsolationWindow(IsolationWindow isolationWindow) {
        this.isolationWindow = isolationWindow;
    }


    public List<Ms2Experiment> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<Ms2Experiment> experiments) {
        this.experiments = experiments;
    }


    public MeasurementProfile getMeasurementProfile() {
        return measurementProfile;
    }

    public void setMeasurementProfile(MeasurementProfile measurementProfile) {
        this.measurementProfile = measurementProfile;
    }


    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }


    public double getIsolationWindowWidth() {
        return isolationWindowWidth;
    }

    public void setIsolationWindowWidth(double isolationWindowWidth) {
        this.isolationWindowWidth = isolationWindowWidth;
    }





}
