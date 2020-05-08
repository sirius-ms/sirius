package de.unijena.bioinf.ms.rest.model.worker;

public enum WorkerState {
    //this are the states a worker can have -> stopping mean it will shut down itself
    STARTING, RUNNING, STOPPING;
}
