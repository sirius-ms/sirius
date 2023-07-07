package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.ft.AbstractFragmentationGraph;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import org.apache.commons.math3.special.Erf;
import org.slf4j.LoggerFactory;

@Called("Mass Deviation")
public class MassDeviationEdgeScorer implements LossScorer<Object> {

    private double weight = 1d;

    public Deviation deviation; // just for debugging

    @Override
    public Object prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
        return null;
    }

    @Override
    public double score(Loss loss, ProcessedInput input, Object precomputed) {
        if (loss.isArtificial() || loss.getSource().getPeakId()<0 || loss.getTarget().getPeakId()<0) return 0d;
        final ProcessedPeak parent = input.getMergedPeaks().get(loss.getSource().getPeakId());
        final ProcessedPeak child = input.getMergedPeaks().get(loss.getTarget().getPeakId());

        final double delta = parent.getMass()-child.getMass();
        final double theoreticalDelta = loss.getSource().getIonization().addToMass(loss.getSource().getFormula().getMass()) - loss.getTarget().getIonization().addToMass(loss.getTarget().getFormula().getMass());
        final Deviation dev;
        if (deviation==null) {
            final double ppm = input.getExperimentInformation().getAnnotationOrDefault(MS2MassDeviation.class).standardMassDeviation.getPpm();
            dev =new Deviation(ppm, 100*1e-6*ppm);
        } else {
            dev = deviation;
        }
        final double sd = dev.absoluteFor(delta);
        double score = weight * Math.log(Erf.erfc(Math.abs(delta-theoreticalDelta)/(sd * Math.sqrt(2))));
        if (score < -100 || !Double.isFinite(score)) {
            if (Math.abs(delta-theoreticalDelta) < 3*sd) {
                LoggerFactory.getLogger(MassDeviationVertexScorer.class).warn(input.getExperimentInformation().getSourceString() + "\nEdge " + delta + " has a too large mass deviation of " + Math.abs(delta - theoreticalDelta) + " for molecular formula " + loss.getFormula());
            }
            score = -100;
        }
        return score;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

    }
}
