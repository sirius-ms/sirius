package de.unijena.bioinf.canopus;

import org.tensorflow.Tensor;

public class TrainingBatch implements AutoCloseable{

    protected Tensor platts, formulas, labels, npcLabels;

    public TrainingBatch(Tensor platts, Tensor formulas, Tensor labels) {
        this(platts,formulas,labels,null);
    }

    public TrainingBatch(Tensor platts, Tensor formulas, Tensor labels, Tensor npcLabels) {
        this.platts = platts;
        this.formulas = formulas;
        this.labels = labels;
        this.npcLabels = npcLabels;
        if (platts==null) throw new NullPointerException("platt values are null");
        if (formulas==null) throw new NullPointerException("formula values are null");
        if (labels==null) throw new NullPointerException("label values are null");
    }

    @Override
    public void close() {
        formulas.close();
        platts.close();
        labels.close();
        if (npcLabels!=null) npcLabels.close();
    }
}
