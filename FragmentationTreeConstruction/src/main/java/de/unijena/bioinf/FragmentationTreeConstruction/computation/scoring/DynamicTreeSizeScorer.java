package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import gnu.trove.list.array.TDoubleArrayList;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

public class DynamicTreeSizeScorer implements PeakScorer {

    private double signalThreshold;


    public DynamicTreeSizeScorer(double signalThreshold) {
        this.signalThreshold = signalThreshold;
    }

    public DynamicTreeSizeScorer() {
        this(0.025);
    }

    @Override
    public void score(List<ProcessedPeak> peaks, ProcessedInput input, double[] scores) {
        /*
        final double p = signalPropability(peaks);
        final double score = Math.log(p) - Math.log(1 - p);
        System.err.println(score);
        for (int i=0; i < scores.length; ++i)
            scores[i] += score;
        */
        final double[] peakscores = new double[scores.length];
        Arrays.fill(peakscores, Double.NEGATIVE_INFINITY);
        final ArrayList<Ms2Spectrum<? extends Peak>> spectra = new ArrayList<Ms2Spectrum<? extends Peak>>();
        final TDoubleArrayList sscores = new TDoubleArrayList();
        for (int k=0; k < peaks.size(); ++k) {
            final ProcessedPeak p = peaks.get(k);
            for (MS2Peak f : p.getOriginalPeaks()) {
                final Ms2Spectrum<? extends Peak> spec = f.getSpectrum();
                int index = 0;
                for (Ms2Spectrum<? extends Peak> a : spectra) {
                    if (a==spec) break;
                    ++index;
                }
                if (index >= spectra.size()) {
                    spectra.add(spec);
                    sscores.add(rawSignalPropability(spec));
                }
                peakscores[k] = Math.max(peakscores[k], sscores.get(index));
            }
        }
        for (int x=0; x < peakscores.length; ++x) {
            scores[x] += peakscores[x];
        }

        //printSig(input, spectra, sscores);
        //for (Ms2Spectrum<? extends Peak> s : spectra) System.err.print(s.getCollisionEnergy() + "\t");
        //System.err.println(sscores);
    }

/*
    static PrintStream printer = null;
    static HashSet<MolecularFormula> formulas = new HashSet<MolecularFormula>();

    protected void printSig(ProcessedInput input, final ArrayList<Ms2Spectrum<? extends Peak>> spectra, TDoubleArrayList sscores) {
        if (printer==null) try {
            printer = new PrintStream(new java.io.File("DEBUGOUT.csv"));
            printer.print("formula,0eV,10eV,20eV,40eV\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        final MolecularFormula f = input.getExperimentInformation().getMolecularFormula();
        if (formulas.contains(f)) return;
        formulas.add(f);
        printer.print(f.toString());
        final double[] evscores = new double[4];
        final int[] evs = new int[]{0,10,20,40};
        for (int x=0; x < sscores.size(); ++x) {
            final double ev = spectra.get(x).getCollisionEnergy().getMinEnergy();
            evscores[Math.min(3, Math.abs(Arrays.binarySearch(evs, (int)ev)))] = sscores.get(x);
        }
        for (int i=0; i < 4; ++i) {
            printer.print(',');
            printer.print(evscores[i]);
        }
        printer.print('\n');
        printer.flush();
    }
    */

    private double rawSignalPropability(Ms2Spectrum<? extends Peak> s) {
        int peaksAbove = 0;
        int peaksBelow = 0;
        SimpleMutableSpectrum ms = new SimpleMutableSpectrum(s);
        Spectrums.sortSpectrumByMass(ms);
        for (int k=ms.size()-1; k >= 0; --k) {
            if (ms.getMzAt(k) >= (s.getPrecursorMz()-0.5)) ms.removePeakAt(k);
            else break;
        }
        Spectrums.normalize(ms, Normalization.Max(1.0));
        for (int k=ms.size()-1; k >= 0; --k) if (ms.getIntensityAt(k) < 0.005) ms.removePeakAt(k);
        Spectrums.normalize(ms, Normalization.Sum(1.0));
        for (int k=0; k < ms.size(); ++k) {
            if (ms.getIntensityAt(k) > 0.025) {
                ++peaksAbove;
            } else {
                ++peaksBelow;
            }

        }
        if ((peaksAbove+peaksBelow)<=0) return Math.log(0.2d);
        final double p= Math.max(0.2d, Math.min(0.8d, (double) peaksAbove / (double) (peaksAbove + peaksBelow)));
        return Math.log(p) - Math.log(1 - p);
    }

    private double signalPropability(List<ProcessedPeak> peaks) {
        int peaksAbove = 0;
        int peaksBelow = 0;
        for (ProcessedPeak p : peaks) {
            if (p.getRelativeIntensity() > signalThreshold) {
                ++peaksAbove;
            } else {
                ++peaksBelow;
            }
        }
        return Math.max(0.1d, Math.min(0.9d, (double) peaksAbove / (double) (peaksAbove + peaksBelow)));
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        signalThreshold = document.getDoubleFromDictionary(dictionary, "signalThreshold");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "signalThreshold", signalThreshold);
    }
}
