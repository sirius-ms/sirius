/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.spectra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms2IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.fragmenter.*;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.middleware.model.annotations.IsotopePatternAnnotation;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.sirius.Ms2Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Spectrums {
    private static <S extends AbstractSpectrum<?>> S decorateMsMs(S spectrum, @NotNull Ms2Spectrum<Peak> sourceSpectrum) {
        spectrum.setPrecursorMz(sourceSpectrum.getPrecursorMz());
        if (sourceSpectrum.getCollisionEnergy() != null && sourceSpectrum.getCollisionEnergy() != CollisionEnergy.none() && !sourceSpectrum.getCollisionEnergy().equals(CollisionEnergy.none())) {
            spectrum.setCollisionEnergy(CollisionEnergy.copyWithoutCorrection(sourceSpectrum.getCollisionEnergy()));
            spectrum.setName("MS2 " + sourceSpectrum.getCollisionEnergy().toString());
        } else {
            spectrum.setName("MS2");
        }

        spectrum.setMsLevel(2);
        spectrum.setScanNumber(((MutableMs2Spectrum) sourceSpectrum).getScanNumber());

        return spectrum;
    }

    private static <S extends AbstractSpectrum<?>> S decorateMsMs(S spectrum, @NotNull MergedMSnSpectrum sourceSpectrum) {
        spectrum.setPrecursorMz(sourceSpectrum.getMergedPrecursorMz());
        if (sourceSpectrum.getMergedCollisionEnergy() != null && !sourceSpectrum.getMergedCollisionEnergy().equals(CollisionEnergy.none())) {
            spectrum.setCollisionEnergy(CollisionEnergy.copyWithoutCorrection(sourceSpectrum.getMergedCollisionEnergy()));
            spectrum.setName("MS2 " + sourceSpectrum.getMergedCollisionEnergy().toString());
        } else {
            spectrum.setName("MS2");
        }

        spectrum.setMsLevel(2);

        return spectrum;
    }

    public static BasicSpectrum createMergedMsMs(@NotNull SimpleSpectrum sourceSpectrum, double mz) {
        BasicSpectrum spectrum = new BasicSpectrum(sourceSpectrum);
        spectrum.setPrecursorMz(mz);
        spectrum.setMsLevel(2);
        spectrum.setName("MS2 merged");
        return spectrum;
    }

    private static <S extends AbstractSpectrum<?>> S decorateMergedMsMs(S spectrum, @Nullable List<Ms2Spectrum<Peak>> sourceSpectra) {
        spectrum.setMsLevel(2);
        spectrum.setName("MS2 merged");
        if (sourceSpectra != null && !sourceSpectra.isEmpty())
            spectrum.setPrecursorMz(sourceSpectra.iterator().next().getPrecursorMz());
        return spectrum;
    }

    public static BasicSpectrum createMs2ReferenceSpectrum(@NotNull Ms2ReferenceSpectrum refSpec) {
        BasicSpectrum spec = new BasicSpectrum(refSpec.getSpectrum());
        spec.setMsLevel(2);
        spec.setName(refSpec.getName());
        spec.setPrecursorMz(refSpec.getPrecursorMz());
        spec.setCollisionEnergy(refSpec.getCollisionEnergy());
        return spec;
    }



    public static BasicSpectrum createMs1(@NotNull Spectrum<Peak> spectrum) {
        BasicSpectrum ms1 = new BasicSpectrum(spectrum);
        ms1.setMsLevel(1);
        ms1.setName("MS1");
        //todo add more meta information if available.
        return ms1;
    }

    public static BasicSpectrum createMergedMs1(@NotNull Ms2Experiment exp) {
        Spectrum<Peak> mergedMs1 = exp.getMergedMs1Spectrum();
        if (mergedMs1 == null && !exp.getMs1Spectra().isEmpty())
            mergedMs1 = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.mergeSpectra(exp.getMs1Spectra());
        if (mergedMs1 == null)
            return null;
        return createMs1(mergedMs1);
    }

    public static BasicSpectrum createMsMs(@NotNull MergedMSnSpectrum x) {
        return decorateMsMs(new BasicSpectrum(x.getPeaks()), x);
    }
    public static BasicSpectrum createMsMs(@NotNull Ms2Spectrum<Peak> x) {
        return decorateMsMs(new BasicSpectrum(x), x);
    }

    public static BasicSpectrum createMergedMsMs(@NotNull Ms2Experiment exp) {
        final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
        final ProcessedInput processedInput = preprocessor.preprocess(exp);
        return decorateMergedMsMs(new BasicSpectrum(processedInput.getMergedPeaks().stream().map(SimplePeak::new).collect(Collectors.toCollection(() -> new ArrayList<>()))), exp.getMs2Spectra());
    }

    public static AnnotatedSpectrum createMergedMsMsWithAnnotations(@NotNull Ms2Experiment exp, @Nullable FTree ftree) {
        return createMergedMsMsWithAnnotations(exp, ftree, null);
    }

    public static AnnotatedSpectrum createMergedMsMsWithAnnotations(@NotNull Ms2Experiment exp, @Nullable FTree ftree, @Nullable String candidateSmiles) {
        if (exp.getMs2Spectra() == null || exp.getMs2Spectra().isEmpty())
            return null;
        final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
        final ProcessedInput processedInput = preprocessor.preprocess(exp);
        List<ProcessedPeak> processedPeaks = processedInput.getMergedPeaks();
        List<AnnotatedPeak> peaks = processedPeaks.stream()
                .map(p -> new AnnotatedPeak(p.getMass(), p.getIntensity(), null)).toList();

        AnnotatedSpectrum spectrum = decorateMergedMsMs(new AnnotatedSpectrum(peaks), exp.getMs2Spectra());

        if (ftree == null || peaks.isEmpty())
            return spectrum;

        //map tree to spectrum
        processedInput.mapTreeToInput(ftree);
        return makeMsMsWithAnnotations(spectrum, ftree, ftree, candidateSmiles);

    }

    public static AnnotatedSpectrum createMergedMsMsWithAnnotations(@NotNull List<? extends Ms2Spectrum<?>> msmsSpectra, @Nullable FTree ftree, @Nullable String candidateSmiles) {
        //todo create fake experiment to annotate spectrum
        return null;
    }


    public static List<AnnotatedSpectrum> createMsMsWithAnnotations(@NotNull Ms2Experiment exp, @Nullable FTree ftree, @Nullable String candidateSmiles) {
        if (exp.getMs2Spectra() == null)
            return List.of();
        return exp.getMs2Spectra().stream().map(s -> createMsMsWithAnnotations(s, ftree, candidateSmiles)).toList();
    }

    public static AnnotatedSpectrum createMsMsWithAnnotations(@NotNull Ms2Spectrum<Peak> specSource, @Nullable FTree ftree, @Nullable String candidateSmiles) {
        AnnotatedSpectrum spectrum = decorateMsMs(new AnnotatedSpectrum(specSource), specSource);
        if (ftree == null)
            return spectrum;
        Fragment[] fragments = annotateFragmentsToSingleMsMs(specSource, ftree);
        return makeMsMsWithAnnotations(spectrum, ftree, Arrays.asList(fragments), candidateSmiles);
    }


    private static AnnotatedSpectrum makeMsMsWithAnnotations(@NotNull AnnotatedSpectrum spectrum, @NotNull FTree ftree, @NotNull Iterable<Fragment> fragments, @Nullable String candidateSmiles) {
        //compute substructure annotations //todo nightsky: do we want to do this somewhere else?
        final InsilicoFragmentationResult structureAnno = candidateSmiles == null ? null
                : SiriusJobs.runInBackground(new InsilicoFragmentationPeakAnnotator().makeJJob(ftree, candidateSmiles)
                .asType(JJob.JobType.TINY_BACKGROUND)).takeResult(); //executed as tiny background job to be computed instantly for immediate response
        setSpectrumAnnotation(spectrum, ftree, structureAnno, candidateSmiles);
        setPeakAnnotations(spectrum, ftree, fragments, structureAnno);
        return spectrum;
    }

    private static void setPeakAnnotations(@NotNull AnnotatedSpectrum spectrum, @NotNull FTree ftree, @NotNull Iterable<Fragment> fragments, @Nullable InsilicoFragmentationResult structureAnno) {
        List<AnnotatedPeak> peaks = spectrum.getPeaks();

        for (Fragment f : fragments) {
            if (f != null) {
                int vertexId = f.getVertexId();
                PeakAnnotation.PeakAnnotationBuilder peakAnno = PeakAnnotation.builder();
                if (f.getFormula() != null && f.getIonization() != null) {
                    peakAnno.molecularFormula(f.getFormula().toString())
                            .ionization(f.getIonization().toString())
                            .exactMass(f.getIonization().addToMass(f.getFormula().getMass()))
                            .fragmentId(vertexId);
                }

                // deviation (from FTJsonWriter tree2json)
                {
                    Deviation dev = ftree.getMassError(f);
                    if (f.isRoot() && dev.equals(Deviation.NULL_DEVIATION))
                        dev = ftree.getMassErrorTo(f, spectrum.getPrecursorMz());

                    Deviation rdev = ftree.getRecalibratedMassError(f);
                    if (f.isRoot() && rdev.equals(Deviation.NULL_DEVIATION))
                        rdev = ftree.getMassErrorTo(f, spectrum.getPrecursorMz());


                    if (!dev.equals(Deviation.NULL_DEVIATION))
                        peakAnno.massDeviationMz(dev.getAbsolute())
                                .massDeviationPpm(dev.getPpm());
                    if (!rdev.equals(Deviation.NULL_DEVIATION))
                        peakAnno.recalibratedMassDeviationMz(rdev.getAbsolute())
                                .recalibratedMassDeviationPpm(rdev.getPpm());
                }

                // we only store incoming edges because references are ugly for serialization
                f.getIncomingEdges().stream().findFirst().ifPresent(l ->
                        peakAnno.parentPeak(ParentPeak.builder()
                                .lossFormula(l.getFormula().toString())
                                .parentIdx((int) l.getSource().getPeakId())
                                .parentFragmentId(l.getSource().getVertexId())
                                .build()));

                if (structureAnno != null) {
                    Optional.ofNullable(structureAnno.getFragmentMapping().get(f))
                            .map(List::stream).flatMap(Stream::findFirst)
                            .ifPresent(subStr -> annotateSubstructure(
                                    peakAnno, f.getFormula(), subStr, structureAnno.getSubtree()));
                }

                //add annotations to corresponding peak
                peaks.get(f.getPeakId()).setPeakAnnotation(peakAnno.build());
            }
        }
    }

    private static void setSpectrumAnnotation(AnnotatedSpectrum spectrum, @Nullable FTree ftree,
                                              @Nullable InsilicoFragmentationResult structureAnno,
                                              @Nullable String candidateSmiles
    ) {
        if (ftree == null)
            return;
        // create formula/ftree based spectrum annotation
        SpectrumAnnotation.SpectrumAnnotationBuilder specAnno = SpectrumAnnotation.builder();

        if (ftree.getRoot().getFormula() != null && ftree.getRoot().getIonization() != null) {
            specAnno.molecularFormula(ftree.getRoot().getFormula().toString())
                    .ionization(ftree.getRoot().getIonization().toString())
                    .exactMass(ftree.getRoot().getIonization().addToMass(ftree.getRoot().getFormula().getMass()));
        }

        Deviation dev = ftree.getMassErrorTo(ftree.getRoot(), spectrum.getPrecursorMz());
        specAnno.massDeviationMz(dev.getAbsolute()).massDeviationPpm(dev.getPpm());

        if (structureAnno != null) {
            specAnno.structureAnnotationSmiles(candidateSmiles)
                    .structureAnnotationScore(structureAnno.getScore());
        }
        spectrum.setSpectrumAnnotation(specAnno.build());


    }

    private static void annotateSubstructure(PeakAnnotation.PeakAnnotationBuilder peakAnno, MolecularFormula fragmentFormula, CombinatorialFragment subStructureAnno, CombinatorialSubtree subtree) {
        CombinatorialNode node = subtree.getNode(subStructureAnno.getBitSet());
        int[] bondIdx = subStructureAnno.bonds().stream().mapToInt(Integer::intValue).sorted().toArray();
        int[] atomIdx = Arrays.stream(subStructureAnno.getAtoms()).mapToInt(IAtom::getIndex).sorted().toArray();
        int[] cutIdx = Arrays.stream(subStructureAnno.getAtoms())
                .flatMap(a -> StreamSupport.stream(a.bonds().spliterator(), false))
                .distinct()
                .mapToInt(IBond::getIndex)
                .filter(b -> Arrays.binarySearch(bondIdx, b) < 0)
                .sorted()
                .toArray();

        peakAnno.substructureAtoms(atomIdx)
                .substructureBonds(bondIdx)
                .substructureBondsCut(cutIdx)
                .substructureScore(node.getTotalScore())
                .hydrogenRearrangements(subStructureAnno.hydrogenRearrangements(fragmentFormula));
    }

    private static Fragment[] annotateFragmentsToSingleMsMs(Spectrum<? extends Peak> spectrum, FTree tree) {
        final FragmentAnnotation<de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak> annotatedPeak;
        if (tree == null || (annotatedPeak = tree.getFragmentAnnotationOrNull(de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak.class)) == null)
            return null;
        Fragment[] annotatedFormulas = new Fragment[spectrum.size()];
        BitSet isIsotopicPeak = new BitSet(spectrum.size());
        final FragmentAnnotation<Ms2IsotopePattern> isoAno = tree.getFragmentAnnotationOrNull(Ms2IsotopePattern.class);
        final Deviation dev = new Deviation(1, 0.01);
        for (Fragment f : tree) {
            de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak peak = annotatedPeak.get(f);
            if (peak == null) {
                continue;
            }
            Ms2IsotopePattern isoPat = isoAno == null ? null : isoAno.get(f);
            if (isoPat != null) {
                for (Peak p : isoPat.getPeaks()) {
                    if (p.getMass() - peak.getMass() > 0.25) {
                        int i = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.getFirstPeakGreaterOrEqualThan(spectrum, p.getMass() - 1e-6);
                        for (int j = i; j < spectrum.size(); ++j) {
                            if (dev.inErrorWindow(p.getMass(), spectrum.getMzAt(j))) {
                                f.setPeakId(j);
                                annotatedFormulas[j] = f;
                                isIsotopicPeak.set(j);
                            } else break;
                        }
                    }
                }
            }
            for (Peak p : peak.getOriginalPeaks()) {
                int i = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.getFirstPeakGreaterOrEqualThan(spectrum, p.getMass() - 1e-6);
                for (int j = i; j < spectrum.size(); ++j) {
                    if (dev.inErrorWindow(p.getMass(), spectrum.getMzAt(j))) {
                        f.setPeakId(j);
                        annotatedFormulas[j] = f;
                    } else break;
                }
            }
            // due to the recalibration we might be far away from the "original" mass
            final double recalibratedMz = peak.getRecalibratedMass();
            {
                int i = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.getFirstPeakGreaterOrEqualThan(spectrum, recalibratedMz - 1e-4);
                for (int j = i; j < spectrum.size(); ++j) {
                    if (dev.inErrorWindow(recalibratedMz, spectrum.getMzAt(j))) {
                        f.setPeakId(j);
                        annotatedFormulas[j] = f;
                    } else break;
                }
            }
        }

        return annotatedFormulas;
    }


    //region isotope pattern
    @NotNull
    public static IsotopePatternAnnotation createIsotopePatternAnnotation(@NotNull SimpleSpectrum isotopePattern, @Nullable FTree tree) {
        IsotopePatternAnnotation it = new IsotopePatternAnnotation();
        it.setIsotopePattern(new BasicSpectrum(isotopePattern));
        if (tree != null)
            it.setSimulatedPattern(simulateIsotopePattern(tree, isotopePattern));
        return it;
    }

    @NotNull
    public static IsotopePatternAnnotation createIsotopePatternAnnotation(@NotNull Ms2Experiment exp, @Nullable FTree tree) {
        IsotopePatternAnnotation it = new IsotopePatternAnnotation();
        it.setIsotopePattern(extractIsotopePattern(exp, tree));
        if (tree != null && it.getIsotopePattern() != null)
            it.setSimulatedPattern(simulateIsotopePattern(tree, it.getIsotopePattern()));
        return it;
    }

    @JsonIgnore
    private static BasicSpectrum simulateIsotopePattern(@NotNull FTree tree, Spectrum<?> isotopePattern) {
        PrecursorIonType ionType = tree.getAnnotation(PrecursorIonType.class).orElseThrow();
        final MolecularFormula formula = tree.getRoot().getFormula().subtract(ionType.getInSourceFragmentation()).add(ionType.getAdduct());
        final FastIsotopePatternGenerator gen = new FastIsotopePatternGenerator(Normalization.Max);
        gen.setMinimalProbabilityThreshold(Math.min(0.005, de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.getMinimalIntensity(isotopePattern)));
        gen.setMaximalNumberOfPeaks(Math.max(4, isotopePattern.size()));
        BasicSpectrum simulatedPattern = new BasicSpectrum(gen.simulatePattern(formula,
                tree.getAnnotation(PrecursorIonType.class).orElseThrow().getIonization()));
        simulatedPattern.setName("Simulated Isotope Pattern");

        return simulatedPattern;

    }

    @JsonIgnore
    private static BasicSpectrum extractIsotopePattern(@NotNull Ms2Experiment exp, @Nullable FTree tree) {
        final IsotopePattern pattern = tree != null ? tree.getAnnotationOrNull(IsotopePattern.class) : null;
        final String name = "MS1 Isotope Pattern";
        BasicSpectrum isotopePattern = null;
        if (pattern != null) {
            isotopePattern = new BasicSpectrum(pattern.getPattern());
            isotopePattern.setName(name);
        } else {
            BasicSpectrum ms = Spectrums.createMergedMs1(exp);
            if (ms != null) {
                isotopePattern = new BasicSpectrum(de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.extractIsotopePattern(
                        ms,
                        exp.getAnnotationOrDefault(MS1MassDeviation.class),
                        exp.getIonMass(),
                        exp.getPrecursorIonType().getCharge(),
                        true));
                isotopePattern.setName(name);
            }
        }
        return isotopePattern;
    }
    //endregion


}
