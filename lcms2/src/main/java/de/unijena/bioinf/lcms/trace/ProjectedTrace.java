package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

import java.io.Serializable;
import java.util.Arrays;

public class ProjectedTrace implements Serializable {

    private int sampleId;

    private int rawStartId, rawEndId, rawApex, projectedStartId, projectedEndId, projectedApex;
    private double averagedMz;

    private float[] rawMz, projectedMz;
    private float[] rawIntensities, projectedIntensities;

    private int[] ms2Ids = new int[0];

    public ProjectedTrace(int sampleId, int rawStartId, int rawEndId, int rawApex, int projectedStartId, int projectedEndId, int projectedApex, double averagedMz, float[] rawMz, float[] projectedMz, float[] rawIntensities, float[] projectedIntensities) {
        this.sampleId = sampleId;
        this.rawStartId = rawStartId;
        this.rawEndId = rawEndId;
        this.rawApex = rawApex;
        this.projectedStartId = projectedStartId;
        this.projectedEndId = projectedEndId;
        this.projectedApex = projectedApex;
        this.averagedMz = averagedMz;
        this.rawMz = rawMz;
        this.projectedMz = projectedMz;
        this.rawIntensities = rawIntensities;
        this.projectedIntensities = projectedIntensities;
    }

    public ProjectedTrace(int sampleId, int rawStartId, int rawEndId, int rawApex, int projectedStartId, int projectedEndId, int projectedApex, double[] rawMz, double[] projectedMz, float[] rawIntensities, float[] projectedIntensities) {
        this.sampleId = sampleId;
        this.rawStartId = rawStartId;
        this.rawEndId = rawEndId;
        this.rawApex = rawApex;
        this.projectedStartId = projectedStartId;
        this.projectedEndId = projectedEndId;
        this.projectedApex = projectedApex;

        this.averagedMz = Arrays.stream(projectedMz).filter(Double::isFinite).average().orElse(0d);
        this.rawMz = new float[rawMz.length];
        for (int k=0; k < rawMz.length; ++k) {
            if (Double.isFinite(rawMz[k])) {
                this.rawMz[k] = (float) (rawMz[k] - this.averagedMz);
            } else {
                // this happens when a trace spans multiple peaks which are islands, i.e. there are zero intensity
                // regions between them
                // it is important to always check the intensity, which is 0 at these places
                rawMz[k] = 0;
            }
        }
        this.projectedMz = new float[projectedMz.length];
        for (int k=0; k < projectedMz.length; ++k) {
            if (Double.isFinite(projectedMz[k])) {
                this.projectedMz[k] = (float) (projectedMz[k] - this.averagedMz);
            } else {
                // this happens when a trace spans multiple peaks which are islands, i.e. there are zero intensity
                // regions between them
                // it is important to always check the intensity, which is 0 at these places
                projectedMz[k] = 0;
            }
        }
        this.rawIntensities = rawIntensities;
        this.projectedIntensities = projectedIntensities;
        if (projectedMz.length>= 100*rawMz.length) {
            throw new RuntimeException("Something weird is going on here oO");
        }
    }

    public int getSampleId() {
        return sampleId;
    }

    public int getRawStartId() {
        return rawStartId;
    }

    public int getRawEndId() {
        return rawEndId;
    }

    public int getRawApex() {
        return rawApex;
    }

    public int getProjectedStartId() {
        return projectedStartId;
    }

    public int getProjectedEndId() {
        return projectedEndId;
    }

    public int getProjectedApex() {
        return projectedApex;
    }

    public double getAveragedMz() {
        return averagedMz;
    }

    public double rawMz(int k) {
        return rawMz[k-rawStartId]+averagedMz;
    }

    public double projectedMz(int k) {
        return projectedMz[k-projectedStartId]+averagedMz;
    }

    public float rawIntensity(int k) {
        return rawIntensities[k-rawStartId];
    }

    public float projectedIntensity(int k) {
        return projectedIntensities[k-projectedStartId];
    }

    public int[] getMs2Ids() {
        return ms2Ids;
    }

    public void setMs2Ids(int[] ms2Ids) {
        this.ms2Ids = ms2Ids;
    }

    public FloatArrayList projectedIntensityArrayList() {
        return new FloatArrayList(projectedIntensities);
    }
    public FloatArrayList rawIntensityArrayList() {
        return new FloatArrayList(rawIntensities);
    }

    public Trace projected(ScanPointMapping mergedMapping) {
        return new Trace() {
            @Override
            public int startId() {
                return projectedStartId;
            }

            @Override
            public int endId() {
                return projectedEndId;
            }

            @Override
            public int apex() {
                return projectedApex;
            }

            @Override
            public double mz(int index) {
                return averagedMz+projectedMz[index-projectedStartId];
            }

            @Override
            public double averagedMz() {
                return averagedMz;
            }

            @Override
            public double minMz() {
                float minMz = 0f;
                for (float v : projectedMz) minMz = Math.min(minMz,v);
                return averagedMz+minMz;
            }

            @Override
            public double maxMz() {
                float maxMz = 0f;
                for (float v : projectedMz) maxMz = Math.max(maxMz,v);
                return averagedMz+maxMz;
            }

            @Override
            public float intensity(int index) {
                return projectedIntensities[index-projectedStartId];
            }

            @Override
            public int scanId(int index) {
                return mergedMapping.getScanIdAt(index);
            }

            @Override
            public double retentionTime(int index) {
                return mergedMapping.getRetentionTimeAt(index);
            }
        };
    }

    public Trace raw(ScanPointMapping rawMapping) {
        return new Trace() {
            @Override
            public int startId() {
                return rawStartId;
            }

            @Override
            public int endId() {
                return rawEndId;
            }

            @Override
            public int apex() {
                return rawApex;
            }

            @Override
            public double mz(int index) {
                return averagedMz+rawMz[index-rawStartId];
            }

            @Override
            public double averagedMz() {
                return averagedMz;
            }

            @Override
            public double minMz() {
                float minMz = 0f;
                for (float v : rawMz) minMz = Math.min(minMz,v);
                return averagedMz+minMz;
            }

            @Override
            public double maxMz() {
                float maxMz = 0f;
                for (float v : rawMz) maxMz = Math.max(maxMz,v);
                return averagedMz+maxMz;
            }

            @Override
            public float intensity(int index) {
                return rawIntensities[index-rawStartId];
            }

            @Override
            public int scanId(int index) {
                return rawMapping.getScanIdAt(index);
            }

            @Override
            public double retentionTime(int index) {
                return rawMapping.getRetentionTimeAt(index);
            }
        };
    }

    public boolean inProjectedRange(int i) {
        return i >= projectedStartId &&  i <= projectedEndId;
    }
}
