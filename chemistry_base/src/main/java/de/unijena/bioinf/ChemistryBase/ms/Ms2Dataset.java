package de.unijena.bioinf.ChemistryBase.ms;

import java.util.List;

/**
 * Created by ge28quv on 01/07/17.
 */
public interface Ms2Dataset {

    public IsolationWindow getIsolationWindow();

    public List<Ms2Experiment> getExperiments();

    /**
     * todo currently uses absolute median noise intensity!!!
     * @return
     */
    public MeasurementProfile getMeasurementProfile();

    public String getProfile();

    public double getIsolationWindowWidth();

}
