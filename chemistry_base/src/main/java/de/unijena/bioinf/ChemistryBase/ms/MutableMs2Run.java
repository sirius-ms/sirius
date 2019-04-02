package de.unijena.bioinf.ChemistryBase.ms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ge28quv on 05/07/17.
 */
public class MutableMs2Run implements Ms2Run {

    //todo we should think if this is a good model -> it forces us to keep the whole Run in memory.
    //private List<MutableMs2Experiment> experiments;
    private Iterable<Ms2Experiment> experiments;
    private double isolationWindowWidth;
    private IsolationWindow isolationWindow;
    private DatasetStatistics statistics;

    private final Annotations<Ms2Run.Annotation> annotations;

    @Override
    public Annotations<Ms2Run.Annotation> annotations() {
        return null;
    }

    public MutableMs2Run() {
        annotations = new Annotations<>();
        experiments = new ArrayList<>();
    }

    /**
     *
     * @param experiments
     * @param isolationWindowWidth maximum isolation window width. will be used to bound isolation window estimation.
     */
    public MutableMs2Run(Iterable<Ms2Experiment> experiments, double isolationWindowWidth) {
        this();

        this.experiments = experiments;
        this.isolationWindowWidth = isolationWindowWidth;
    }

    public MutableMs2Run(Ms2Run dataset) {
        annotations = dataset.annotations().clone();
        this.experiments = dataset.getExperiments(); //todo we might want to copy th collection.
        /*for (Ms2Experiment experiment : dataset.getExperiments()) {
            this.experiments.add(new MutableMs2Experiment(experiment));
        }*/
        this.isolationWindowWidth = dataset.getIsolationWindowWidth();
        this.isolationWindow = dataset.getIsolationWindow();
        this.setDatasetStatistics(dataset.getDatasetStatistics());
    }


    public IsolationWindow getIsolationWindow() {
        return isolationWindow;
    }

    public void setIsolationWindow(IsolationWindow isolationWindow) {
        this.isolationWindow = isolationWindow;
    }


    public Iterable<Ms2Experiment> getExperiments() {
        return experiments;
    }


    public List<Ms2Experiment> loadExperiments() {
        List<Ms2Experiment> ex = new ArrayList<>();
        getExperiments().forEach(ex::add);
        return ex;
    }

    public void setExperiments(List<Ms2Experiment> experiments) {
        this.experiments = experiments;
    }

    public double getIsolationWindowWidth() {
        return isolationWindowWidth;
    }

    public void setIsolationWindowWidth(double isolationWindowWidth) {
        this.isolationWindowWidth = isolationWindowWidth;
    }


    @Override
    public Iterator<Ms2Experiment> iterator() {
        return experiments.iterator();
        /*return new Iterator<Ms2Experiment>() {
            private int index;
            @Override
            public boolean hasNext() {
                return index < experiments.size();
            }

            @Override
            public Ms2Experiment next() {
                if (index < experiments.size())
                    return experiments.get(index++);
                else
                    throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };*/
    }

    public void setDatasetStatistics(DatasetStatistics statistics){
        this.statistics = statistics;
    }

    @Override
    public DatasetStatistics getDatasetStatistics() {
        return statistics;
    }


}
