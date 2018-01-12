package de.unijena.bioinf.ChemistryBase.ms;

import java.util.List;

/**
 * Created by ge28quv on 01/07/17.
 */
public interface Ms2Dataset extends Iterable<Ms2Experiment>{

    public IsolationWindow getIsolationWindow();

    public <E extends Ms2Experiment> List<E> getExperiments();

    /**
     * todo currently uses absolute median noise intensity!!!
     * @return
     */
    public MeasurementProfile getMeasurementProfile();

    public String getProfile();

    public double getIsolationWindowWidth();

    public DatasetStatistics getDatasetStatistics();
}
