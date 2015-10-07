/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration.RecalibrationMethod;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.MassDecomposer.Interval;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

/**
 * cleanup
 * cleanup --recalibrate --accept=explained
 * cleanup --recalibrate --accept=explainable
 */
public class CleanupSpectrum {

    public static final String USAGE = "cleanup -f <formula> <file>\ncleanup -f <formula> --recalibrate --filter=EXPLAINED <file>";

    public final static String VERSION_STRING = "FragmentationPatternAnalysis " + Main.VERSION + "\n" + Main.CITE + "\nusage:\n" + USAGE;

    public static void main(String[] args) {
        try {
            final CleanupOptions options = CliFactory.createCli(CleanupOptions.class).parseArguments(args);
            if (options.getCite() || options.getVersion() || args.length == 0) {
                System.out.println(Main.VERSION_STRING);
            } else {
                run(options);
            }
        } catch (HelpRequestedException h) {
            System.out.println(h.getMessage());
            System.exit(0);
        }
    }

    public static void run(CleanupOptions options) {
        /*final Profile profile;
        if (options.getProfile() != null) {
            try {
                profile = new Profile(options.getProfile());
            } catch (IOException e) {
                System.err.println(e);
                System.exit(1);
                return;
            }
        } else {
            try {
                profile = new Profile("default");
            } catch (IOException e) {
                System.err.println("Can't find default profile");
                return;
            }
        }
        final List<File> files = getFiles(options);
        profile.fragmentationPatternAnalysis.setRepairInput(true);
        profile.fragmentationPatternAnalysis.setRecalibrationMethod(new HypothesenDrivenRecalibration(new MedianSlope(profile.fragmentationPatternAnalysis.getDefaultProfile().getStandardMs2MassDeviation().divide(2), 7, 0.01d), 1e-8));
        ((HypothesenDrivenRecalibration) profile.fragmentationPatternAnalysis.getRecalibrationMethod()).setDeviationScale(1d);//(2d/3d);
        for (File f : files) {
            try {
                MutableMs2Experiment experiment = new MutableMs2Experiment(
                        Main.parseFile(f, new MutableMeasurementProfile(profile.fragmentationPatternAnalysis.getDefaultProfile())));

                if (options.getFormula() != null) {
                    experiment.setMolecularFormula(MolecularFormula.parse(options.getFormula()));
                } else if (experiment.getMolecularFormula() == null) {
                    System.err.println("No molecular formula is given for " + f + ". Please provide the molecular formula before cleanup the spectrum");
                    continue;
                }
                FTree tree = profile.fragmentationPatternAnalysis.computeTrees(profile.fragmentationPatternAnalysis.preprocessing(experiment)).withoutRecalibration().
                        onlyWith(Arrays.asList(experiment.getMolecularFormula())).optimalTree();
                final ProcessedInput pinput = tree.getAnnotationOrThrow(ProcessedInput.class);
                experiment =  pinput.getExperimentInformation();
                if (tree == null) {
                    System.err.println("Can't find tree for " + f + " with " + experiment.getMolecularFormula() + " as formula");
                    continue;
                }

                if (!options.getRecalibrate() && options.getFilter() == null) {
                    tree = profile.fragmentationPatternAnalysis.recalibrate(tree, true);
                    idealize(experiment, tree, profile);
                } else {
                    if (options.getRecalibrate()) {
                        recalibrate(experiment, tree, profile);
                    } else tree = profile.fragmentationPatternAnalysis.recalibrate(tree, true);
                    if (options.getFilter() == CleanupOptions.NOISE_FILTER.EXPLAINED) {
                        filterExplained(experiment, tree, profile);
                    } else if (options.getFilter() == CleanupOptions.NOISE_FILTER.EXPLAINABLE) {
                        filterPossible(experiment, tree, profile);
                    } else {
                        // do nothing
                    }
                }
                if (!options.getTarget().exists()) options.getTarget().mkdirs();
                new GenericWriter<Ms2Experiment>(new JenaMsWriter()).writeToFile(new File(options.getTarget(), f.getName()), experiment);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        */

    }

    private static void recalibrate(MutableMs2Experiment experiment, FTree tree, Profile profile) {
        final RecalibrationMethod.Recalibration rec = profile.fragmentationPatternAnalysis.getRecalibrationFromTree(tree, false);
        final UnivariateFunction f = rec.recalibrationFunction();
        if (f == null || f instanceof Identity ||
                rec.getCorrectedTree(profile.fragmentationPatternAnalysis).getAnnotationOrThrow(TreeScoring.class).getOverallScore() <=
                        tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore()) return;
        final ArrayList<MutableMs2Spectrum> spectra = new ArrayList<MutableMs2Spectrum>();
        for (int k = 0; k < experiment.getMs2Spectra().size(); ++k) {
            final MutableMs2Spectrum spec;
            {
                final Ms2Spectrum ms = experiment.getMs2Spectra().get(k);
                if (ms instanceof MutableMs2Spectrum) spec = (MutableMs2Spectrum) ms;
                else spec = new MutableMs2Spectrum(ms);
            }
            for (int j = 0; j < spec.size(); ++j) {
                spec.setMzAt(j, f.value(spec.getMzAt(j)));
            }
            spectra.add(spec);
        }
        experiment.setMs2Spectra(spectra);
    }

    private static void idealize(MutableMs2Experiment experiment, FTree tree, Profile profile) {
        final HashMap<CollisionEnergy, MutableMs2Spectrum> spectra = new HashMap<CollisionEnergy, MutableMs2Spectrum>();
        final double parentmass = experiment.getIonMass();
        final FragmentAnnotation<ProcessedPeak> peakAno = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        for (Fragment f : tree.getFragments()) {
            for (MS2Peak peak : peakAno.get(f).getOriginalPeaks()) {
                final CollisionEnergy ce = peak.getSpectrum().getCollisionEnergy();
                MutableMs2Spectrum spec = spectra.get(ce);
                if (spec == null) {
                    spec = new MutableMs2Spectrum(); spec.setCollisionEnergy(ce); spec.setPrecursorMz(parentmass);
                    spectra.put(ce, spec);
                }
                spec.addPeak(new MS2Peak(spec, peakAno.get(f).getIon().addToMass(f.getFormula().getMass()), peak.getIntensity()));
            }
        }
        for (MutableMs2Spectrum spec : spectra.values()) {
            Spectrums.sortSpectrumByMass(spec);
        }
        experiment.setMs2Spectra(new ArrayList<MutableMs2Spectrum>(spectra.values()));

    }

    private static void filterPossible(MutableMs2Experiment experiment, FTree tree, Profile profile) {
        final ArrayList<MutableMs2Spectrum> spectra = new ArrayList<MutableMs2Spectrum>();
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(tree.getRoot().getFormula().elementArray());
        final MassToFormulaDecomposer decomposer = profile.fragmentationPatternAnalysis.getDecomposerFor(alphabet);
        final Map<Element, Interval> boundaries = alphabet.toMap();
        for (Element e : alphabet.getElements()) {
            boundaries.put(e, new Interval(0, experiment.getMolecularFormula().numberOf(e)));
        }
        final Ionization ion = experiment.getPrecursorIonType().getIonization();
        for (Ms2Spectrum<? extends Peak> spec : experiment.getMs2Spectra()) {
            final MutableMs2Spectrum mspec = new MutableMs2Spectrum();
            for (Peak peak : spec) {
                if (decomposer.decomposeToFormulas(ion.subtractFromMass(peak.getMass()), profile.fragmentationPatternAnalysis.getDefaultProfile().getAllowedMassDeviation(), boundaries).size() > 0) {
                    mspec.addPeak(peak);
                }
            }
            mspec.setCollisionEnergy(spec.getCollisionEnergy());
            mspec.setTotalIonCount(spec.getTotalIonCount());
            mspec.setPrecursorMz(tree.getAnnotationOrThrow(ProcessedInput.class).getParentPeak().getMz());
            spectra.add(mspec);
        }
        experiment.setMs2Spectra(spectra);
    }

    // TODO: loose of total ion count
    private static void filterExplained(MutableMs2Experiment experiment, FTree tree, Profile profile) {
        final HashMap<CollisionEnergy, MutableMs2Spectrum> spectra = new HashMap<CollisionEnergy, MutableMs2Spectrum>();
        for (Ms2Spectrum spec : experiment.getMs2Spectra()) {
            final MutableMs2Spectrum mspec = new MutableMs2Spectrum();
            mspec.setCollisionEnergy(spec.getCollisionEnergy());
            mspec.setTotalIonCount(spec.getTotalIonCount());
            mspec.setPrecursorMz(tree.getAnnotationOrThrow(ProcessedInput.class).getParentPeak().getMz());
            if (!spectra.containsKey(spec.getCollisionEnergy()))
                spectra.put(spec.getCollisionEnergy(), mspec);
        }
        final FragmentAnnotation<ProcessedPeak> peakAno = tree.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        for (Fragment f : tree.getFragments()) {
            for (MS2Peak peak : peakAno.get(f).getOriginalPeaks()) {
                spectra.get(peak.getSpectrum().getCollisionEnergy()).addPeak(peak);
            }
        }
        experiment.setMs2Spectra(new ArrayList<MutableMs2Spectrum>());
        for (MutableMs2Spectrum spec : spectra.values()) experiment.getMs2Spectra().add(spec);
    }


    public static List<File> getFiles(CleanupOptions options) {
        final List<File> files = options.getFiles();
        final ArrayList<File> fs = new ArrayList<File>(files.size());
        for (File f : files) {
            if (f.isDirectory()) {
                fs.addAll(Arrays.asList(f.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile() && !pathname.isDirectory() && pathname.canRead();
                    }
                })));
            } else if (f.canRead()) {
                fs.add(f);
            }
        }
        return fs;
    }

}
