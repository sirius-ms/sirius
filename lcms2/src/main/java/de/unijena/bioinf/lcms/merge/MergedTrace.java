package de.unijena.bioinf.lcms.merge;

import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.statistics.NormalizationStrategy;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.ProjectedTrace;
import de.unijena.bioinf.lcms.trace.Trace;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MergedTrace implements Trace {

    private final double[] mz;
    private final double avgMz;
    private final float[] intensity;
    private final ScanPointMapping mapping;
    private int startId, endId, apexId;
    private final MergedTrace[] isotopes;
    private final ProcessedSample[] samples;
    private final ProjectedTrace[] traces;

    public static MergedTrace collect(ProcessedSample mergedSample, Map<Integer, ProcessedSample> sampleByIdx, ProjectedTrace[] projectedTraces, ProjectedTrace[][] isotopes) {
        // collect isotopes
        MergedTrace[] isoMerged = new MergedTrace[isotopes.length];
        for (int k=0; k < isoMerged.length; ++k) {
            isoMerged[k] = collectIsotopic(mergedSample, sampleByIdx, isotopes[k], null);
        }
        return collectIsotopic(mergedSample, sampleByIdx, projectedTraces, isoMerged);
    }

    private static MergedTrace collectIsotopic(ProcessedSample mergedSample, Map<Integer, ProcessedSample> sampleByIdx, ProjectedTrace[] projectedTraces, MergedTrace[] isotopes) {
        final ScanPointMapping mergedMapping = mergedSample.getMapping();
        // first we have to define the range of the trace
        int mindex = Integer.MAX_VALUE, maxdex = Integer.MIN_VALUE;
        for (ProjectedTrace trace : projectedTraces) {
            mindex = Math.min(trace.getProjectedStartId(), mindex);
            maxdex = Math.max(trace.getProjectedEndId(), maxdex);
        }
        // now we can add all traces into one
        final double[] mz = new double[maxdex-mindex+1];
        final double[] intensity = new double[maxdex-mindex+1];
        ArrayList<ProcessedSample> samples = new ArrayList<>();
        for (ProjectedTrace trace : projectedTraces) {
            final ProcessedSample sample = sampleByIdx.get(trace.getSampleId());
            samples.add(sample);
            NormalizationStrategy.Normalizer normalizer = sample.getNormalizer();
            for (int k=trace.getProjectedStartId(); k <= trace.getProjectedEndId(); ++k) {
                final double normalizedIntensity = normalizer.normalize(trace.projectedIntensity(k));
                intensity[k-mindex] += normalizedIntensity;
                mz[k-mindex] += normalizedIntensity * trace.projectedMz(k);
            }
        }
        // now we can merge everything
        double avgMz = 0d, intSum=0d;
        int apexOffset = 0;
        for (int k=0; k < mz.length; ++k) {
            if (intensity[k]>0){
                avgMz += mz[k];
                mz[k] /= intensity[k];
                intSum += intensity[k];
                if (intensity[k]>intensity[apexOffset]) apexOffset = k;
            } else mz[k] = Double.NaN;
        }
        avgMz /= intSum;
        if (!Double.isFinite(avgMz)) {
            throw new IllegalArgumentException();
        }
        return new MergedTrace(mz, avgMz, MatrixUtils.double2float(intensity), mergedMapping, mindex, maxdex, mindex+apexOffset, isotopes, samples.toArray(ProcessedSample[]::new), projectedTraces);


    }

    private final int minMz, maxMz;

    private MergedTrace(double[] mz, double avgMz, float[] intensity, ScanPointMapping mapping, int startId, int endId, int apexId, MergedTrace[] isotopes, ProcessedSample[] samples, ProjectedTrace[] traces) {
        this.mz = mz;
        this.avgMz = avgMz;
        this.intensity = intensity;
        this.mapping = mapping;
        this.startId = startId;
        this.endId = endId;
        this.apexId = apexId;
        this.isotopes = isotopes;
        this.samples = samples;
        this.traces = traces;
        int mindex=0,maxdex=0;
        for (int k=0; k < mz.length; ++k) {
            if (mz[k]<mz[mindex])mindex=k;
            if (mz[k]>mz[maxdex]) maxdex=k;
        }
        this.minMz=mindex;
        this.maxMz=maxdex;
    }

    @Override
    public int startId() {
        return startId;
    }

    @Override
    public int endId() {
        return endId;
    }

    @Override
    public int apex() {
        return apexId;
    }

    @Override
    public double mz(int index) {
        return mz[index-startId];
    }

    @Override
    public double averagedMz() {
        return avgMz;
    }

    @Override
    public double minMz() {
        return mz[minMz];
    }

    @Override
    public double maxMz() {
        return mz[maxMz];
    }

    @Override
    public float intensity(int index) {
        return intensity[index-startId];
    }

    @Override
    public int scanId(int index) {
        return mapping.getScanIdAt(index);
    }

    @Override
    public double retentionTime(int index) {
        return mapping.getRetentionTimeAt(index);
    }

    public ScanPointMapping getMapping() {
        return mapping;
    }

    public MergedTrace[] getIsotopes() {
        return isotopes;
    }

    public ProcessedSample[] getSamples() {
        return samples;
    }

    public ProjectedTrace[] getTraces() {
        return traces;
    }

    public DoubleList getMzArrayList() {
        return new DoubleArrayList(mz);
    }
    public FloatArrayList getIntensityArrayList() {
        return new FloatArrayList(intensity);
    }

    // DEBUG
    public void debugJSON() {
        StringBuilder buf = new StringBuilder();
        buf.append("{\"mz\": ");
        buf.append(String.valueOf(averagedMz()));
        buf.append(", \"merged\": [");
        for (int k=0; k < intensity.length; ++k) {
            buf.append(String.valueOf(intensity[k]));
            if (k < intensity.length-1) buf.append(", ");
        }
        buf.append("],\n");
        buf.append("\"traces\": [");
        for (int i=0; i < traces.length; ++i) {
            buf.append("{");
            buf.append("\"normalizer\": ");
            buf.append(String.valueOf(samples[i].getNormalizer().normalize(1d)));
            buf.append(",\n");
            buf.append("\"raw\": [");
            float[] xs = traces[i].rawIntensityArrayList().toFloatArray();
            for (int k=0; k < xs.length; ++k) {
                buf.append(String.valueOf(xs[k]));
                if (k < xs.length-1) buf.append(", ");
            }
            buf.append("],\n");
            buf.append("\"projected\": [");
            xs = getvec(traces[i].projectedIntensityArrayList(), traces[i].getProjectedStartId(), traces[i].getProjectedEndId(), startId, endId);
            for (int k=0; k < intensity.length; ++k) {
                buf.append(String.valueOf(xs[k]));
                if (k < intensity.length-1) buf.append(", ");
            }
            buf.append("]\n");
            buf.append("}");
            if (i < traces.length-1) buf.append(",\n");
        }
        buf.append("]}\n");
        try (final PrintStream out = new PrintStream("/tmp/debug_json.json")) {
            out.println(buf.toString());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private float[] getvec(FloatArrayList floats, int rawStartId, int rawEndId, int startId, int endId) {
        final float[] vec = new float[endId-startId+1];
        for (int k=rawStartId; k <= rawEndId; ++k) {
            vec[k-startId] = floats.getFloat(k-rawStartId);
        }
        return vec;
    }


    public void debugJSON(AlignedFeatures[] features) {
        final int offset = startId;
        StringBuilder buf = new StringBuilder();
        buf.append("{\"mz\": ");
        buf.append(String.valueOf(averagedMz()));
        buf.append(", \"rtPerBin\": ");
        buf.append(getMapping().getRetentionTimeAt(10)-getMapping().getRetentionTimeAt(9));
        buf.append(", \"merged\": [");
        for (int k=0; k < intensity.length; ++k) {
            buf.append(String.valueOf(intensity[k]));
            if (k < intensity.length-1) buf.append(", ");
        }
        buf.append("],\n");
        buf.append("\"apexes\": [");
        IntArrayList inds = new IntArrayList();
        for (AlignedFeatures f : features) {
            inds.add(f.getTraceRef().getApex()+f.getTraceRef().getScanIndexOffsetOfTrace()  - offset);
        }
        buf.append(inds.intStream().mapToObj(Integer::toString).collect(Collectors.joining(", ")));
        buf.append("],\n");
        buf.append("\"traces\": [");
        for (int i=0; i < traces.length; ++i) {
            buf.append("{");
            buf.append("\"normalizer\": ");
            buf.append(String.valueOf(samples[i].getNormalizer().normalize(1d)));
            buf.append(",\n");
            buf.append("\"raw\": [");
            float[] xs = traces[i].rawIntensityArrayList().toFloatArray();
            for (int k=0; k < xs.length; ++k) {
                buf.append(String.valueOf(xs[k]));
                if (k < xs.length-1) buf.append(", ");
            }
            buf.append("],\n");
            buf.append("\"projected\": [");
            xs = getvec(traces[i].projectedIntensityArrayList(), traces[i].getProjectedStartId(), traces[i].getProjectedEndId(), startId, endId);
            for (int k=0; k < intensity.length; ++k) {
                buf.append(String.valueOf(xs[k]));
                if (k < intensity.length-1) buf.append(", ");
            }
            buf.append("],\n");
            buf.append("\"apexes\": [");
            inds.clear();
            for (AlignedFeatures f : features) {
                for (Feature g : f.getFeatures().get()) {
                    if (g.getRunId()==samples[i].getRun().getRunId()) {
                        inds.add(g.getTraceReference().get().getScanIndexOffsetOfTrace()+g.getTraceReference().get().getApex() - offset);
                    }
                }
            }
            buf.append(inds.intStream().mapToObj(Integer::toString).collect(Collectors.joining(", ")));
            buf.append("]");
            buf.append("}");
            if (i < traces.length-1) buf.append(",\n");
        }
        buf.append("]}\n");
        try (final PrintStream out = new PrintStream("/tmp/debug_json.json")) {
            out.println(buf.toString());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
