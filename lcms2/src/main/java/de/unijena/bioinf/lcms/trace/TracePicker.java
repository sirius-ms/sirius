package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.model.lcms.Scan;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TracePicker {

    private final static int MIN_SCANPOINT_THRESHOLD = 3;

    protected LCMSStorage storage;

    protected Deviation allowedMassDeviation;

    protected ScanPointMapping mapping;

    public TracePicker(LCMSStorage traceStorage, ScanPointMapping scanPointMapping) {
        this.storage = traceStorage;
        this.allowedMassDeviation = new Deviation(10);
        this.mapping = scanPointMapping;
    }

    public Optional<ContiguousTrace> detectTraceAlongTimeWindow(int firstScanId, int lastScanId, double fromMz, double toMz, int innerFirstScanId, int innerLastScanId, double innerFromMz, double innerToMz) {
        if (innerFromMz < fromMz) innerFromMz=fromMz;
        if (innerToMz < toMz) innerToMz=toMz;

        final DoubleArrayList masses = new DoubleArrayList();
        final FloatArrayList intensities = new FloatArrayList();
        final IntArrayList ids = new IntArrayList();
        // find most intensive match
        int mostIntensive=-1;
        float intensity=0f;
        double mmz=-1;
        for (int k=innerFirstScanId; k <= innerLastScanId; ++k) {
            final SimpleSpectrum spectrum = storage.getSpectrum(k);
            if (spectrum==null) continue;
            int index = Spectrums.mostIntensivePeakWithin(spectrum, innerFromMz, innerToMz);
            if (index >= 0 && spectrum.getIntensityAt(index) > intensity) {
                mostIntensive = k;
                intensity = (float)spectrum.getIntensityAt(index);
                mmz = spectrum.getMzAt(index);
            }
        }
        if (mostIntensive<0) return Optional.empty();
        int id = mostIntensive;
        masses.add(mmz);
        intensities.add(intensity);
        ids.add(id);

        // extend to the left
        for (int sid = id-1; sid >= firstScanId; --sid) {
            final SimpleSpectrum spectrum = storage.getSpectrum(sid);

            final double mzA, mzB;
            if (sid < innerFirstScanId) {
                mzA = fromMz;
                mzB = toMz;
            } else {
                mzA = innerFromMz;
                mzB = innerToMz;
            }

            if (spectrum==null) break;
            int index = Spectrums.mostIntensivePeakWithin(spectrum, mzA, mzB);
            if (index < 0) {
                masses.add(0d);
                intensities.add(0f);
            } else {
                masses.add(spectrum.getMzAt(index));
                intensities.add((float) spectrum.getIntensityAt(index));
            }
            ids.add(sid);
        }

        // remove all leading and trailing zeros from the trace
        while (intensities.getFloat(intensities.size()-1)<=0) {
            intensities.popFloat();
            ids.popInt();
            masses.popDouble();
        }

        // reverse
        rev(masses);
        rev(intensities);
        rev(ids);
        // extend to the right
        for (int sid = id+1; sid <= lastScanId; ++sid) {
            final double mzA, mzB;
            if (sid > innerLastScanId) {
                mzA = fromMz;
                mzB = toMz;
            } else {
                mzA = innerFromMz;
                mzB = innerToMz;
            }


            final SimpleSpectrum spectrum = storage.getSpectrum(sid);
            if (spectrum==null) break;
            int index = Spectrums.mostIntensivePeakWithin(spectrum, mzA, mzB);
            if (index < 0) {
                masses.add(0d);
                intensities.add(0f);
            } else {
                masses.add(spectrum.getMzAt(index));
                intensities.add((float) spectrum.getIntensityAt(index));
            }
            ids.add(sid);
        }

        // remove all leading and trailing zeros from the trace
        while (intensities.getFloat(intensities.size()-1)<=0) {
            intensities.popFloat();
            ids.popInt();
            masses.popDouble();
        }

        ContiguousTrace trace = new ContiguousTrace(
                mapping, ids.getInt(0), ids.getInt(ids.size() - 1), masses.toDoubleArray(), intensities.toFloatArray());
        return Optional.of(trace);
    }


    public Optional<ContiguousTrace> detectTrace(int id, double mz) {
        // first check if we have this scan already picked
        final double delta = allowedMassDeviation.absoluteFor(mz);
        Optional<ContiguousTrace> contigousTrace = storage.getContigousTrace(mz-delta, mz+delta, id);
        if (contigousTrace.isPresent()) return contigousTrace;
        // pick trace
        return pickTrace(id, mz);
    }

    public Optional<ContiguousTrace> detectMostIntensivePeakWithin(int id, double startMz, double endMz) {
        // first pick spectrum
        final SimpleSpectrum spectrum = storage.getSpectrum(id);
        final int index = Spectrums.mostIntensivePeakWithin(spectrum, startMz, endMz);
        if (index < 0) return Optional.empty();
        // now search for the peak
        return detectTrace(id, spectrum.getMzAt(index));
    }

    public Optional<ContiguousTrace> detectMostIntensivePeak(int id, double mz) {
        final double abs = allowedMassDeviation.absoluteFor(mz);
        return detectMostIntensivePeakWithin(id, mz-abs, mz+abs);
    }

    public Optional<ContiguousTrace> detectPeakInIsolationWindowUsingGaussianShape(int id, double offset, double width) {
        // first pick spectrum
        final double radius = width/2;
        final SimpleSpectrum spectrum = storage.getSpectrum(id);
        int index = Spectrums.indexOfFirstPeakWithin(spectrum, offset-radius, offset+radius);
        if (index < 0) return Optional.empty();

        double score = Double.NEGATIVE_INFINITY;
        for (int i=index; i < spectrum.size(); ++i) {
            double delta = Math.abs(spectrum.getMzAt(i)-offset);
            if (delta > width) break;
            double prob = spectrum.getIntensityAt(i) * Math.exp(-delta/radius);
            if (prob > score) {
                score = prob;
                index = i;
            }
        }

        // now search for the peak
        return detectTrace(id, spectrum.getMzAt(index));
    }

    private Optional<ContiguousTrace> pickTrace(int id, double mz) {
        return pickTraceAlongTimeWindow(id, mz, 0, storage.numberOfScans()-1, true, true);
    }

    private Optional<ContiguousTrace> pickTraceAlongTimeWindow(int id, double mz, int firstScanId, int lastScanId, boolean stopAtZero, boolean cacheTrace) {
        final DoubleArrayList masses = new DoubleArrayList();
        final FloatArrayList intensities = new FloatArrayList();
        final IntArrayList ids = new IntArrayList();
        if (stopAtZero) {
            final SimpleSpectrum spectrum = storage.getSpectrum(id);
            int index = Spectrums.search(spectrum, mz, allowedMassDeviation);
            if (index < 0) return Optional.empty();
            masses.add(spectrum.getMzAt(index));
            intensities.add((float) spectrum.getIntensityAt(index));
            ids.add(id);
        } else {
            // find most intensive match
            int mostIntensive=-1;
            float intensity=0f;
            double mmz=-1;
            for (int k=firstScanId; k < lastScanId; ++k) {
                final SimpleSpectrum spectrum = storage.getSpectrum(k);
                if (spectrum==null) continue;
                int index = Spectrums.mostIntensivePeakWithin(spectrum, mz, allowedMassDeviation.divide(3));
                if (index >= 0 && spectrum.getIntensityAt(index) > intensity) {
                    mostIntensive = k;
                    intensity = (float)spectrum.getIntensityAt(index);
                    mmz = spectrum.getMzAt(index);
                }
            }
            if (mostIntensive<0) {
                for (int k=firstScanId; k < lastScanId; ++k) {
                    final SimpleSpectrum spectrum = storage.getSpectrum(k);
                    if (spectrum==null) continue;
                    int index = Spectrums.mostIntensivePeakWithin(spectrum, mz, allowedMassDeviation);
                    if (index >= 0 && spectrum.getIntensityAt(index) > intensity) {
                        mostIntensive = k;
                        intensity = (float)spectrum.getIntensityAt(index);
                        mmz = spectrum.getMzAt(index);
                    }
                }
            }
            if (mostIntensive<0) return Optional.empty();
            id = mostIntensive;
            masses.add(mmz);
            intensities.add(intensity);
            ids.add(id);
        }
        // extend to the left
        for (int sid = id-1; sid >= firstScanId; --sid) {
            final double massToSearch = masses.getDouble(masses.size() - 1);
            final SimpleSpectrum spectrum = storage.getSpectrum(sid);
            if (spectrum==null) break;
            int index = Spectrums.mostIntensivePeakWithin(spectrum, massToSearch, allowedMassDeviation.divide(3));
            if (index < 0) index = Spectrums.mostIntensivePeakWithin(spectrum, massToSearch, allowedMassDeviation);
            if (index < 0) {
                if (stopAtZero) break;
                else {
                    masses.add(0d);
                    intensities.add(0f);
                }
            } else {
                masses.add(spectrum.getMzAt(index));
                intensities.add((float) spectrum.getIntensityAt(index));
            }
            ids.add(sid);
        }

        // remove all leading and trailing zeros from the trace
        if (!stopAtZero) {
            while (intensities.getFloat(intensities.size()-1)<=0) {
                intensities.popFloat();
                ids.popInt();
                masses.popDouble();
            }
        }

        // reverse
        rev(masses);
        rev(intensities);
        rev(ids);
        // extend to the right
        for (int sid = id+1; sid <= lastScanId; ++sid) {
            final double massToSearch = masses.getDouble(masses.size() - 1);
            final SimpleSpectrum spectrum = storage.getSpectrum(sid);
            if (spectrum==null) break;
            int index = Spectrums.mostIntensivePeakWithin(spectrum, massToSearch, allowedMassDeviation.divide(3));
            if (index < 0) index = Spectrums.mostIntensivePeakWithin(spectrum, massToSearch, allowedMassDeviation);
            if (index < 0) {
                if (stopAtZero) break;
                else {
                    masses.add(0d);
                    intensities.add(0f);
                }
            } else {
                masses.add(spectrum.getMzAt(index));
                intensities.add((float) spectrum.getIntensityAt(index));
            }
            ids.add(sid);
        }

        // remove all leading and trailing zeros from the trace
        if (!stopAtZero) {
            while (intensities.getFloat(intensities.size()-1)<=0) {
                intensities.popFloat();
                ids.popInt();
                masses.popDouble();
            }
        }
        int traceLen;
        if (stopAtZero) traceLen = ids.size();
        else {
            if (intensities.size() < 2) return Optional.empty();
            traceLen=0;
            for (int i=1; i < intensities.size(); ++i) {
                // only consider pairs to avoid most of the noise
                if (intensities.getFloat(i)>0 && intensities.getFloat(i-1)>0) ++traceLen;
            }
            if (intensities.getFloat(0)>0 && intensities.getFloat(1)>0) ++traceLen;
        }

        if (traceLen >= MIN_SCANPOINT_THRESHOLD) {
            ContiguousTrace trace = new ContiguousTrace(
            mapping, ids.getInt(0), ids.getInt(ids.size() - 1), masses.toDoubleArray(), intensities.toFloatArray());
            if (cacheTrace) {
                return Optional.of(storage.addContigousTrace(trace));
            } else return Optional.of(trace);
        } else return Optional.empty();
    }

    private void rev(DoubleArrayList masses) {
        final int n = masses.size();
        for (int i=0; i < n/2; ++i) {
            double a = masses.getDouble(i);
            double b = masses.getDouble(n-(i+1));
            masses.set(i,b);
            masses.set(n-(i+1), a);
        }
    }
    private void rev(FloatArrayList masses) {
        final int n = masses.size();
        for (int i=0; i < n/2; ++i) {
            float a = masses.getFloat(i);
            float b = masses.getFloat(n-(i+1));
            masses.set(i,b);
            masses.set(n-(i+1), a);
        }
    }
    private void rev(IntArrayList masses) {
        final int n = masses.size();
        for (int i=0; i < n/2; ++i) {
            int a = masses.getInt(i);
            int b = masses.getInt(n-(i+1));
            masses.set(i,b);
            masses.set(n-(i+1), a);
        }
    }


}
