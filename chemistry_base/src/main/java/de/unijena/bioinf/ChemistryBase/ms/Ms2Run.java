package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;

import java.util.List;

/**
 * Created by ge28quv on 01/07/17.
 */
public interface Ms2Run extends Annotated<Ms2Run.Annotation>, Iterable<Ms2Experiment> {

    IsolationWindow getIsolationWindow();

    <E extends Ms2Experiment> Iterable<E> getExperiments();
    <E extends Ms2Experiment> List<E> loadExperiments();

    /**
     * todo currently uses absolute median noise intensity!!!
     * @return
     */
    double getIsolationWindowWidth();

    DatasetStatistics getDatasetStatistics();


    interface Annotation extends DataAnnotation {
    }
}
