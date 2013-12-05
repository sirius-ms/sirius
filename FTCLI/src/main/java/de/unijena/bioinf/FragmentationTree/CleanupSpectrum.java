package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.FormulaVisitor;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.MutableMs2Spectrum;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.MassDecomposer.Interval;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

/**
 * Created by kaidu on 12/5/13.
 */
public class CleanupSpectrum {


    public static void main(String[] args) {
        try {
            final CleanupOptions options = CliFactory.createCli(CleanupOptions.class).parseArguments(args);
            if (options.getCite() || options.getVersion() || args.length==0) {
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
        final Profile profile;
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
        for (File f : files) {
            try {
                Ms2ExperimentImpl experiment = new Ms2ExperimentImpl(
                        Main.parseFile(f, new MutableMeasurementProfile(profile.fragmentationPatternAnalysis.getDefaultProfile())));

                if (options.getFormula() != null) {
                    experiment.setMolecularFormula(MolecularFormula.parse(options.getFormula()));
                } else if (experiment.getMolecularFormula()==null) {
                    System.err.println("No molecular formula is given for " + f + ". Please provide the molecular formula before cleanup the spectrum");
                    continue;
                }
                final FragmentationTree tree = profile.fragmentationPatternAnalysis.computeTrees(profile.fragmentationPatternAnalysis.preprocessing(experiment)).
                        onlyWith(Arrays.asList(experiment.getMolecularFormula())).optimalTree();
                if (tree == null) {
                    System.err.println("Can't find tree for " + f + " with " + experiment.getMolecularFormula() + " as formula");
                    continue;
                }
                if (options.getPeakFilter() == CleanupOptions.NOISE.EXPLAINED) {
                    filterExplained(experiment, tree, profile);
                } else if (options.getPeakFilter() == CleanupOptions.NOISE.POSSIBLE) {
                    filterPossible(experiment, tree, profile);
                } else {
                    // do nothing
                }
                if (options.getMz()== CleanupOptions.MZ.IDEALIZE) {
                    idealize(experiment, tree, profile);
                } else if (options.getMz() == CleanupOptions.MZ.RECALIBRATE) {
                    recalibrate(experiment, tree, profile);
                } else {
                    // do nothing
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private static void recalibrate(Ms2ExperimentImpl experiment, FragmentationTree tree, Profile profile) {
        final UnivariateFunction f = profile.fragmentationPatternAnalysis.getRecalibrationFromTree(tree).recalibrationFunction();
        if (f==null || f instanceof Identity) return;
        for (int k=0; k < experiment.getMs2Spectra().size(); ++k) {
            final MutableMs2Spectrum spec;
            {
                final Ms2Spectrum ms = experiment.getMs2Spectra().get(k);
                if (ms instanceof MutableMs2Spectrum) spec = (MutableMs2Spectrum)ms;
                else spec = new MutableMs2Spectrum(ms);
            }
            for (int j=0; j < spec.size(); ++j) {
                spec.setMzAt(j, f.value(spec.getMzAt(j)));
            }
        }
    }

    private static void idealize(Ms2ExperimentImpl experiment, FragmentationTree tree, Profile profile) {
        final TLongObjectHashMap<List<MolecularFormula>> peaks = new TLongObjectHashMap<List<MolecularFormula>>();
        for (Fragment f : tree.getFragments()) {
            final long mz1 = (long)Math.floor(f.getPeak().getMz());
            final long mz2 = (long)Math.ceil(f.getPeak().getMz());
            peaks.putIfAbsent(mz1, new ArrayList<MolecularFormula>());
            peaks.get(mz1).add(f.getFormula());
            peaks.putIfAbsent(mz2, new ArrayList<MolecularFormula>());
            peaks.get(mz2).add(f.getFormula());
        }
        final Deviation allowedDev = experiment.getMeasurementProfile().getAllowedMassDeviation();
        final Ionization ion = experiment.getIonization();
        for (int k=0; k < experiment.getMs2Spectra().size(); ++k) {
            final MutableMs2Spectrum spec;
            {
                final Ms2Spectrum ms = experiment.getMs2Spectra().get(k);
                if (ms instanceof MutableMs2Spectrum) spec = (MutableMs2Spectrum)ms;
                else spec = new MutableMs2Spectrum(ms);
            }
            for (int j=0; j < spec.size(); ++j) {
                final long mz = Math.round(spec.getMzAt(j));
                final List<MolecularFormula> formulas = peaks.get(mz);
                for (MolecularFormula f : formulas) {
                    if (allowedDev.inErrorWindow(ion.subtractFromMass(spec.getMzAt(j)), f.getMass())) {
                        spec.setMzAt(j, ion.addToMass(f.getMass()));
                        break;
                    }
                }
            }
        }

    }

    private static void filterPossible(Ms2ExperimentImpl experiment, FragmentationTree tree, Profile profile) {
        final ArrayList<Ms2Spectrum> spectra = new ArrayList<Ms2Spectrum>();
        final ChemicalAlphabet alphabet = experiment.getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet();
        final MassToFormulaDecomposer decomposer = profile.fragmentationPatternAnalysis.getDecomposerFor(alphabet);
        final Map<Element, Interval> boundaries = alphabet.toMap();
        experiment.getMolecularFormula().visit(new FormulaVisitor<Object>() {
            @Override
            public Object visit(Element element, int amount) {
                boundaries.put(element, new Interval(0, amount));
                return null;
            }
        });
        final Ionization ion = experiment.getIonization();
        for (Ms2Spectrum<? extends Peak> spec : experiment.getMs2Spectra()) {
            final MutableMs2Spectrum mspec = new MutableMs2Spectrum();
            for (Peak peak : spec) {
                if (decomposer.decomposeToFormulas(ion.subtractFromMass(peak.getMass()), experiment.getMeasurementProfile().getAllowedMassDeviation(), boundaries ).size() > 0) {
                    mspec.addPeak(peak);
                }
            }
            mspec.setCollisionEnergy(spec.getCollisionEnergy());
            mspec.setTotalIonCurrent(spec.getTotalIonCount());
            mspec.setPrecursorMz(tree.getInput().getParentPeak().getMz());
            spectra.add(mspec);
        }
        experiment.setMs2Spectra(spectra);
    }

    // TODO: loose of total ion count
    private static void filterExplained(Ms2ExperimentImpl experiment, FragmentationTree tree, Profile profile) {
        final HashMap<CollisionEnergy, MutableMs2Spectrum> spectra = new HashMap<CollisionEnergy, MutableMs2Spectrum>();
        for (Ms2Spectrum spec : experiment.getMs2Spectra()) {
            final MutableMs2Spectrum mspec = new MutableMs2Spectrum();
            mspec.setCollisionEnergy(spec.getCollisionEnergy());
            mspec.setTotalIonCurrent(spec.getTotalIonCount());
            mspec.setPrecursorMz(tree.getInput().getParentPeak().getMz());
            if (!spectra.containsKey(spec.getCollisionEnergy()))
                spectra.put(spec.getCollisionEnergy(), mspec);
        }
        for (Fragment f : tree.getFragments()) {
            for (MS2Peak peak : f.getPeak().getOriginalPeaks()) {
                spectra.get(peak.getSpectrum().getCollisionEnergy()).addPeak(peak);
            }
        }
        experiment.setMs2Spectra(new ArrayList<Ms2Spectrum>());
        for (Ms2Spectrum spec : spectra.values()) experiment.getMs2Spectra().add(spec);
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
