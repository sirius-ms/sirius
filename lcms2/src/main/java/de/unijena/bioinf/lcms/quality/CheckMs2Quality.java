package de.unijena.bioinf.lcms.quality;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.lcms.adducts.TraceProvider;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AbstractFeature;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CheckMs2Quality implements FeatureQualityChecker{
    @Override
    public void addToReport(QualityReport report, MergedLCMSRun run, AlignedFeatures feature, TraceProvider provider) {
        // majors
        QualityReport.Category peakQuality = new QualityReport.Category(QualityReport.MS2_QUALITY);

        List<MergedMSnSpectrum> spectra;
        if (feature.getMSData().isEmpty())return;
        spectra = feature.getMSData().get().getMsnSpectra();
        if (spectra==null || spectra.isEmpty()) return;

        // 1. number of peaks above 10*noise level
        int numberOfIntensivePeaks = 0, bareMinimum = 0;
        final double threshold = run.getSampleStats().getMs2NoiseLevel()*4;
        final double threshold2 = run.getSampleStats().getMs2NoiseLevel()*2;
        findSpec:
        for (MergedMSnSpectrum spec : spectra) {
            int sofar=numberOfIntensivePeaks, bsofar=bareMinimum;
            numberOfIntensivePeaks=0; bareMinimum=0;
            SimpleSpectrum peaks = spec.getPeaks();
            for (int k=0; k < peaks.size(); ++k) {
                if (peaks.getMzAt(k) >= spec.getMergedPrecursorMz()-6)
                    break;
                if (peaks.getIntensityAt(k) >= threshold) {
                    ++numberOfIntensivePeaks;++bareMinimum;
                    if (numberOfIntensivePeaks>=8) break findSpec;
                } else if (peaks.getIntensityAt(k) >= threshold2) ++bareMinimum;
            }
            numberOfIntensivePeaks = Math.max(numberOfIntensivePeaks,sofar);
            bareMinimum = Math.max(bsofar, bareMinimum);
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
}
