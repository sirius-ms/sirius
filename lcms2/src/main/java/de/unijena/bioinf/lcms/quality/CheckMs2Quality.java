package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.lcms.adducts.TraceProvider;
import de.unijena.bioinf.lcms.utils.MultipleCharges;
import de.unijena.bioinf.ChemistryBase.utils.DefaultQualityCategory;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import org.apache.commons.lang3.Range;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CheckMs2Quality implements FeatureQualityChecker{

    /**
     * How much ion current a data independent spectrum has to carry, as a multiple of the run's MS2 noise level,
     * before it is worth anything.
     * <p>
     * Only applied to data independent spectra, and the reason is the acquisition rather than a preference: there the
     * peaks attributed to a precursor are the outcome of a correlation over a handful of scan points, so a spectrum
     * can consist entirely of signal barely above the noise while looking perfectly ordinary once normalised. A
     * targeted spectrum cannot fail that way - what was isolated was measured - so the same bound there would only
     * punish compounds that are genuinely faint.
     * <p>
     * The numbers are quartile boundaries of the quantity's own distribution, measured against a DDA run of the same
     * sample: of the spectra below five, 90 % share less than a fifth of their peaks with the reference; below twenty,
     * 76 %; below a hundred and fifty, 61 %; above it, 42 %. So they mark real changes in what a spectrum is worth
     * rather than round numbers, and no single one of them separates cleanly - this ranks spectra, it does not
     * partition them.
     */
    private static final double[] DIA_ION_COUNT_OVER_NOISE = {5d, 20d, 150d};

    private static final String[] mostCommonLosses = {"C2H2", "CO", "C2H4", "H2O", "CHN", "C3H2", "C3H4", "CH4", "CH3", "CH2O", "C2H2O", "C4H4", "C4H2", "HN", "C2H3", "C3H6", "C2H4O", "C2H3N", "CH3N", "H3N", "CH2N", "C4H6", "CHO"};
    private static final double[] massDeltas = Arrays.stream(mostCommonLosses).mapToDouble(x-> MolecularFormula.parseOrThrow(x).getMass()).toArray();


    @Override
    public void addToReport(QualityReport report, MergedLCMSRun run, AlignedFeatures feature, TraceProvider provider) {
        // majors
        QualityReport.Category peakQuality = new QualityReport.Category(DefaultQualityCategory.MS2_QUALITY);

        List<MergedMSnSpectrum> spectra;
        if (feature.getMSData().isEmpty())return;
        spectra = feature.getMSData().get().getMsnSpectra();
        if (spectra==null || spectra.isEmpty()) return;

        // 1. number of peaks above 10*noise level
        int numberOfIntensivePeaks = 0, bareMinimum = 0, signal=0;
        final double threshold = run.getSampleStats().getMs2NoiseLevel()*4;
        final double threshold2 = run.getSampleStats().getMs2NoiseLevel()*2;
        PeakStats massDeltaStats = new PeakStats(0,0f,0f);
        findSpec:
        for (MergedMSnSpectrum spec : spectra) {
            massDeltaStats = massDeltaStats.mergeMax(peaksWithCommonMassDeltas(spec.getMergedPrecursorMz(), spec.getPeaks()));
            Range<Double> intSpan = Spectrums.getIntensityRange(spec.getPeaks());
            final double threshold3 = Math.max(intSpan.getMaximum()*0.05, intSpan.getMinimum()*5);
            int sofar=numberOfIntensivePeaks, bsofar=bareMinimum;
            numberOfIntensivePeaks=0; bareMinimum=0;
            int signalPeaks=0;
            SimpleSpectrum peaks = spec.getPeaks();
            for (int k=0; k < peaks.size(); ++k) {
                if (peaks.getMzAt(k) >= spec.getMergedPrecursorMz()-6)
                    break;
                if (peaks.getIntensityAt(k) >= threshold3)
                    ++signalPeaks;
                if (peaks.getIntensityAt(k) >= threshold) {
                    ++numberOfIntensivePeaks;++bareMinimum;
                } else if (peaks.getIntensityAt(k) >= threshold2) ++bareMinimum;
            }
            numberOfIntensivePeaks = Math.max(numberOfIntensivePeaks,sofar);
            bareMinimum = Math.max(bsofar, bareMinimum);
            signal = Math.max(signal, signalPeaks);
        }

        if (bareMinimum<3) {
            peakQuality.getItems().add(new QualityReport.Item(
                    bareMinimum==0 ? "There are no intensive peaks in the spectrum "
                                              : "There are no intensive " + numberOfIntensivePeaks + " peaks in the spectrum",
                    DataQuality.LOWEST, QualityReport.Weight.MAJOR
            ));
        } else if (numberOfIntensivePeaks<3) {
            peakQuality.getItems().add(new QualityReport.Item(
                    numberOfIntensivePeaks==0 ? "All MS/MS peaks are just slightly above noise level."
                                                : "There are only " + numberOfIntensivePeaks + " peaks above noise level in the spectrum.",
                    DataQuality.BAD, QualityReport.Weight.MAJOR
            ));
        } else if (numberOfIntensivePeaks<=5) {
            peakQuality.getItems().add(new QualityReport.Item(
                    "There are only " + numberOfIntensivePeaks + " peaks above noise level in the spectrum.",
                    DataQuality.DECENT, QualityReport.Weight.MAJOR
            ));
        } else {
            peakQuality.getItems().add(new QualityReport.Item(
                    "There are " + numberOfIntensivePeaks + " peaks above noise level in the spectrum.",
                    DataQuality.GOOD, QualityReport.Weight.MAJOR
            ));
        }
        addIonCountItems(peakQuality, run, spectra);
        if (signal < 1) {
            peakQuality.getItems().add(new QualityReport.Item(
                    "All peaks in the spectrum have similar intensities, which is usually an indication for a noise spectrum",
                    DataQuality.LOWEST, QualityReport.Weight.MAJOR
            ));
        } else if (signal <= 3) {
            peakQuality.getItems().add(new QualityReport.Item(
                    "There are few peaks with intensities considerably larger than the lowest intensities in the spectrum. This is an indication for a noisy spectrum.",
                    DataQuality.BAD, QualityReport.Weight.MAJOR
            ));
        }
        if (massDeltaStats.numberOfPeaks<3 || massDeltaStats.ratioOfIntensity < 0.2) {
            peakQuality.getItems().add(new QualityReport.Item(
                    "Most peaks in the spectrum have uncommon mass deltas, indicating noise peaks.",
                    DataQuality.LOWEST, QualityReport.Weight.MINOR
            ));
        } else if (massDeltaStats.numberOfPeaks < 6 || massDeltaStats.ratioOfIntensity < 0.33) {
            peakQuality.getItems().add(new QualityReport.Item(
                    "Many peaks in the spectrum have uncommon mass deltas, indicating noise peaks.",
                    DataQuality.BAD, QualityReport.Weight.MINOR
            ));
        } else if (massDeltaStats.ratioOfIntensity < 0.66) {
            peakQuality.getItems().add(new QualityReport.Item(
                    "Some peaks in the spectrum have uncommon mass deltas, indicating noise peaks.",
                    DataQuality.DECENT, QualityReport.Weight.MINOR
            ));
        } else {
            peakQuality.getItems().add(new QualityReport.Item(
                    "Peaks in the spectrum have common mass deltas, indicating signal peaks.",
                    DataQuality.GOOD, QualityReport.Weight.MINOR
            ));
        }

        // check multiple charges
        MultipleCharges.Decision decision = MultipleCharges.checkForMultipleCharges(spectra);
        if (decision== MultipleCharges.Decision.LIKELY) {
            peakQuality.getItems().add(new QualityReport.Item(
                    "Many peaks in the spectrum can only be explained via multiple charges, indicating either a noisy spectrum or a multiple charged ion.",
                    DataQuality.LOWEST, QualityReport.Weight.MAJOR)
            );
        }


        // check collision energies
        if (spectra.stream().map(MergedMSnSpectrum::getMergedCollisionEnergy).collect(Collectors.toSet()).size()>1) {
            peakQuality.getItems().add(new QualityReport.Item(
                    "There are multiple collision energies recorded for this feature.",
                    DataQuality.GOOD, QualityReport.Weight.MINOR
            ));
        } else if (spectra.stream().anyMatch(x->x.getMergedCollisionEnergy().isRamp())) {
            peakQuality.getItems().add(new QualityReport.Item(
                    "The spectra are recorded with ramp collision energy.",
                    DataQuality.GOOD, QualityReport.Weight.MINOR
            ));
        } else {
            peakQuality.getItems().add(new QualityReport.Item(
                    "There is only one collision energy recorded for this feature",
                    DataQuality.BAD, QualityReport.Weight.MINOR
            ));
        }

        // check chimeric pollution
        double maxPollution = Double.NEGATIVE_INFINITY;
        for (MergedMSnSpectrum spec : spectra) {
            if (spec.getChimericPollutionRatio()!=null) {
                maxPollution = Math.max(maxPollution, spec.getChimericPollutionRatio());
            }
        }
        if (Double.isFinite(maxPollution)) {
            if (maxPollution<=0.05) {
                peakQuality.getItems().add(new QualityReport.Item(
                        "There are no other peaks within the isolation window of the MS/MS.",
                        DataQuality.GOOD, QualityReport.Weight.MINOR
                ));
            } else if (maxPollution<=0.2) {
                peakQuality.getItems().add(new QualityReport.Item(
                        "There are barely other peaks within the isolation window of the MS/MS.",
                        DataQuality.GOOD, QualityReport.Weight.MINOR
                ));
            } else if (maxPollution <= 0.5) {
                peakQuality.getItems().add(new QualityReport.Item(
                        String.format(Locale.US,
                                "There are other peaks within the isolation window of the MS/MS, suming up to %d %% of the precursor intensity.",
                                (int)(maxPollution*100)),
                        DataQuality.DECENT, QualityReport.Weight.MINOR
                ));
            } else if (maxPollution < 1) {
                peakQuality.getItems().add(new QualityReport.Item(
                        String.format(Locale.US,
                                "There are other peaks within the isolation window of the MS/MS, suming up to %d %% of the precursor intensity.",
                                (int) (maxPollution * 100)),
                        DataQuality.BAD, QualityReport.Weight.MINOR
                ));
            } else {
                peakQuality.getItems().add(new QualityReport.Item(
                        String.format(Locale.US,
                                "There are other peaks within the isolation window of the MS/MS, suming up to %d %% of the precursor intensity.",
                                (int) (maxPollution * 100)),
                        DataQuality.LOWEST, QualityReport.Weight.MINOR
                ));
            }
        }


        report.addCategory(peakQuality);


    }

    public static record PeakStats(int numberOfPeaks, float ratioOfPeaks, float ratioOfIntensity) {
        public PeakStats mergeMax(PeakStats s) {
            return new PeakStats(Math.max(numberOfPeaks,s.numberOfPeaks), Math.max(ratioOfPeaks,s.ratioOfPeaks), Math.max(ratioOfIntensity,s.ratioOfIntensity));
        }
    }

    public static PeakStats signalPeaks(SimpleSpectrum spec) {
        Range<Double> intSpan = Spectrums.getIntensityRange(spec);
        if (intSpan.getMaximum() / intSpan.getMinimum() < 10) return new PeakStats(0,0f,0f);
        int numberOfPeaks = 0;
        double peakIntens = 0f;
        double totalIntens = 0d;
        final double threshold = Math.max(intSpan.getMaximum()*0.05, intSpan.getMinimum()*5);
        for (int k=0; k < spec.size(); ++k) {
            totalIntens += spec.getIntensityAt(k);
            if (spec.getIntensityAt(k)>=threshold) {
                peakIntens += spec.getIntensityAt(k);
                ++numberOfPeaks;
            }
        }
        return new PeakStats(numberOfPeaks, (float)(numberOfPeaks/(float)spec.size()), (float)(peakIntens/totalIntens));
    }

    public static PeakStats peaksWithCommonMassDeltas(double precursorMz, SimpleSpectrum spectrum) {
        Deviation dev = new Deviation(5);
        double maxInt=0.0, intSum=0.0;
        int count=0;
        double intensity=0, minTensity=Double.POSITIVE_INFINITY;
        final double mzThreshold = precursorMz-10;
        for (int i=0; i < spectrum.size(); ++i) {
            if (spectrum.getMzAt(i)>mzThreshold) break;
            maxInt = Math.max(spectrum.getIntensityAt(i), maxInt);
            intSum += spectrum.getIntensityAt(i);
            for (double d : massDeltas) {
                if (Spectrums.indexOfFirstPeakWithin(spectrum, spectrum.getMzAt(i) + d, dev)>=0 || Spectrums.indexOfFirstPeakWithin(spectrum, spectrum.getMzAt(i) - d, dev)>=0 ) {
                    intensity += spectrum.getIntensityAt(i);
                    count++;
                    minTensity = Math.min(spectrum.getIntensityAt(i), minTensity);
                    break;
                }
            }
        }
        return new PeakStats(count, count / (float)spectrum.size(), (float)(intensity/intSum));
    }

    /**
     * How much measured ion current a data independent spectrum was built from, relative to the run's MS2 noise.
     * <p>
     * Not answerable from the peaks: a merged peak's intensity is weighted by the merge score and the spectrum a
     * reader sees is normalised, so this has to come from {@link MergedMSnSpectrum#getTotalIonCount()}, recorded
     * before either happened. Nothing is added for a targeted spectrum, or for a project written before the field
     * existed.
     */
    private void addIonCountItems(QualityReport.Category peakQuality, MergedLCMSRun run,
                                  List<MergedMSnSpectrum> spectra) {
        final double noise = run.getSampleStats().getMs2NoiseLevel();
        if (!(noise > 0)) return;
        double total = 0d;
        boolean dataIndependent = false, recorded = false;
        for (MergedMSnSpectrum spectrum : spectra) {
            if (!Boolean.TRUE.equals(spectrum.getDataIndependent())) continue;
            dataIndependent = true;
            if (spectrum.getTotalIonCount() == null) continue;
            total += spectrum.getTotalIonCount();
            recorded = true;
        }
        if (!dataIndependent || !recorded) return;

        final double overNoise = total / noise;
        if (overNoise < DIA_ION_COUNT_OVER_NOISE[0]) {
            peakQuality.getItems().add(new QualityReport.Item(String.format(Locale.US,
                    "The fragments assigned to this precursor carry only %.1f times the noise level in total. At "
                            + "that level the assignment is not supported by measurable signal.", overNoise),
                    DataQuality.LOWEST, QualityReport.Weight.MAJOR));
        } else if (overNoise < DIA_ION_COUNT_OVER_NOISE[1]) {
            peakQuality.getItems().add(new QualityReport.Item(String.format(Locale.US,
                    "The fragments assigned to this precursor carry %.0f times the noise level in total, which is "
                            + "little for a spectrum assembled from co-isolated fragments.", overNoise),
                    DataQuality.BAD, QualityReport.Weight.MAJOR));
        } else if (overNoise < DIA_ION_COUNT_OVER_NOISE[2]) {
            peakQuality.getItems().add(new QualityReport.Item(String.format(Locale.US,
                    "The fragments assigned to this precursor carry %.0f times the noise level in total.", overNoise),
                    DataQuality.DECENT, QualityReport.Weight.MAJOR));
        } else {
            peakQuality.getItems().add(new QualityReport.Item(String.format(Locale.US,
                    "The fragments assigned to this precursor carry %.0f times the noise level in total, which is "
                            + "ample signal for an assignment.", overNoise),
                    DataQuality.GOOD, QualityReport.Weight.MAJOR));
        }
    }
}
