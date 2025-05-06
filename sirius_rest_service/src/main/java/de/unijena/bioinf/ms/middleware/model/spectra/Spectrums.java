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
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.fragmenter.*;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.SpectrumAnnotationJJob;
import de.unijena.bioinf.ms.middleware.model.annotations.IsotopePatternAnnotation;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.sirius.annotations.NoiseThresholdSettings;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.ReferenceSpectrum;
import de.unijena.bionf.fastcosine.FastCosine;
import de.unijena.bionf.fastcosine.ReferenceLibrarySpectrum;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.renderer.color.UniColor;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Spectrums {
    private static final FastCosine FAST_COSINE = new FastCosine(new Deviation(15), false, new NoiseThresholdSettings(0.001, 60, NoiseThresholdSettings.BASE_PEAK.NOT_PRECURSOR, 0));
    private static final boolean DEBUG = false;

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

    public static <T extends AbstractSpectrum<?>>T decorateMergedMsMs(@NotNull T targeSpectrum, double mz) {
        targeSpectrum.setPrecursorMz(mz);
        targeSpectrum.setMsLevel(2);
        targeSpectrum.setName("MS2 merged");
        return targeSpectrum;
    }

    public static BasicSpectrum createMs1(@NotNull Spectrum<Peak> spectrum) {
        BasicSpectrum ms1 = new BasicSpectrum(spectrum);
        ms1.setMsLevel(1);
        ms1.setName("MS1");
        return ms1;
    }

    public static BasicSpectrum createMsMs(@NotNull MergedMSnSpectrum x, boolean asCosineQuery) {
        double precursorMz = x.getMergedPrecursorMz();
        SimpleSpectrum ms2Peaks = x.getPeaks();

        BasicSpectrum basicSpec;
        if (asCosineQuery){
            ReferenceLibrarySpectrum query = FAST_COSINE.prepareQuery(precursorMz, ms2Peaks);
            basicSpec = decorateMsMs(new BasicSpectrum(query), x);
            if (query.getParentIntensity() > 0)
                basicSpec.setPrecursorPeak(new SimplePeak(query.getParentMass(), query.getParentIntensity()));
        } else {
            basicSpec = decorateMsMs(new BasicSpectrum(ms2Peaks), x);
            int precursorIdx = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.mostIntensivePeakWithin(ms2Peaks, precursorMz, FAST_COSINE.getMaxDeviation());
            if (precursorIdx >= 0)
                basicSpec.setPrecursorPeak(new SimplePeak(ms2Peaks.getPeakAt(precursorIdx)));
        }
        basicSpec.setCosineQuery(asCosineQuery);

        return basicSpec;
    }

    public static BasicSpectrum createMergedMsMs(Spectrum<Peak> mergedMs2Peaks, double precursorMz, boolean asCosineQuery) {
        BasicSpectrum basicSpec;

        if (asCosineQuery){
            ReferenceLibrarySpectrum query = FAST_COSINE.prepareQuery(precursorMz, mergedMs2Peaks);
            basicSpec = decorateMergedMsMs(new BasicSpectrum(query), precursorMz);
            if (query.getParentIntensity() > 0)
                basicSpec.setPrecursorPeak(new SimplePeak(query.getParentMass(), query.getParentIntensity()));
        } else {
            basicSpec = decorateMergedMsMs(new BasicSpectrum(mergedMs2Peaks), precursorMz);
            int precursorIdx = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.mostIntensivePeakWithin(mergedMs2Peaks, precursorMz, FAST_COSINE.getMaxDeviation());
            if (precursorIdx >= 0)
                basicSpec.setPrecursorPeak(new SimplePeak(mergedMs2Peaks.getPeakAt(precursorIdx)));
        }
        basicSpec.setCosineQuery(asCosineQuery);
        return basicSpec;
    }

    public static BasicSpectrum createReferenceMsMs(ReferenceSpectrum ref) {
        return createReferenceMsMs(ref, true);
    }

    /**
     *
     * @param ref
     * @param renormalize if true, the square root transformation that was applied on library spectra is removed
     * @return
     */
    public static BasicSpectrum createReferenceMsMs(ReferenceSpectrum ref, boolean renormalize) {
        Spectrum<Peak> s = ref.getQuerySpectrum();
        if (renormalize) {
            SimpleMutableSpectrum buf = new SimpleMutableSpectrum(s);
            for (int j=0; j < buf.size(); ++j) {
                buf.setIntensityAt(j, buf.getIntensityAt(j)*buf.getIntensityAt(j));
            }
            s = buf;
        }

        BasicSpectrum spec = new BasicSpectrum(s);
        // basic information
        spec.setMsLevel(2);
        spec.setName(ref.getName());
        spec.setPrecursorMz(ref.getPrecursorMz());
        // extended information
        if (ref instanceof Ms2ReferenceSpectrum) {
            Ms2ReferenceSpectrum ms2ref = (Ms2ReferenceSpectrum) ref;
            if (ms2ref.getInstrumentation() != null) {
                spec.setInstrument(ms2ref.getInstrumentation().description());
            } else if (ms2ref.getInstrumentType() != null && ms2ref.getInstrument() != null
                    && !ms2ref.getInstrumentType().isBlank() && !ms2ref.getInstrument().isBlank()) {
                spec.setInstrument(ms2ref.getInstrumentType() + " (" + ms2ref.getInstrument() + ")");
            } else if (ms2ref.getInstrumentType() != null && !ms2ref.getInstrumentType().isBlank()) {
                spec.setInstrument(ms2ref.getInstrumentType());
            } else if (ms2ref.getInstrument() != null && !ms2ref.getInstrument().isBlank()) {
                spec.setInstrument(ms2ref.getInstrument());
            }
            if (ms2ref.getCollisionEnergy() != null) {
                spec.setCollisionEnergy(ms2ref.getCollisionEnergy());
            } else {
                spec.setCollisionEnergyStr(ms2ref.getCe());
            }
            spec.setCollisionEnergy(ms2ref.getCollisionEnergy());;
        } else if (ref instanceof MergedReferenceSpectrum) {
            spec.setCollisionEnergy(CollisionEnergy.none());
        }
        return spec;
    }


    public static AnnotatedSpectrum createReferenceMsMsWithAnnotations(@NotNull ReferenceSpectrum refSpectrum, @Nullable FTree ftree) {
        return createReferenceMsMsWithAnnotations(refSpectrum, ftree, true);
    }

    @SneakyThrows
    public static AnnotatedSpectrum createReferenceMsMsWithAnnotations(@NotNull ReferenceSpectrum refSpectrum, @Nullable FTree ftree, boolean renormalize) {
        ReferenceLibrarySpectrum specSource = refSpectrum.getQuerySpectrum();

        final AnnotatedSpectrum spectrum;
        if (renormalize) {
            SimpleMutableSpectrum renormalized = new SimpleMutableSpectrum(specSource);
            for (int j = 0; j < renormalized.size(); ++j) {
                renormalized.setIntensityAt(j, renormalized.getIntensityAt(j) * renormalized.getIntensityAt(j));
            }
            spectrum = new AnnotatedSpectrum(renormalized);
        }else {
            spectrum = new AnnotatedSpectrum(specSource);
        }

        spectrum.setName(refSpectrum.getName());
        spectrum.setCosineQuery(true);
        spectrum.setPrecursorPeak(new SimplePeak(specSource.getParentMass(), specSource.getParentIntensity()));
        spectrum.setPrecursorMz(refSpectrum.getPrecursorMz());
        if (refSpectrum instanceof Ms2ReferenceSpectrum  ms2Ref) {
            spectrum.setMsLevel(ms2Ref.getMsLevel());
            spectrum.setCollisionEnergy(ms2Ref.getCollisionEnergy());
        }else {
            spectrum.setMsLevel(2);
            spectrum.setCollisionEnergy(CollisionEnergy.none());
        }

        if (ftree == null)
            return spectrum;

        Fragment[] fragments = annotateFragmentsToSingleMsMs(specSource, ftree, specSource.getParentMass(), true);
        return makeMsMsWithAnnotations(spectrum, ftree, Arrays.asList(fragments),  refSpectrum.getSmiles(), refSpectrum.getName());
    }


    public static AnnotatedSpectrum createMergedMsMsWithAnnotations(double precursorMz,
                                                                    @NotNull Spectrum<Peak> mergedMs2Peaks,
                                                                    @Nullable FTree ftree,
                                                                    boolean asCosineQuery
    ) {
        return createMergedMsMsWithAnnotations(precursorMz, mergedMs2Peaks, ftree, null, null, asCosineQuery);
    }

    @SneakyThrows
    public static AnnotatedSpectrum createMergedMsMsWithAnnotations(double precursorMz,
                                                                    @NotNull Spectrum<Peak> mergedMs2Peaks,
                                                                    @Nullable FTree ftree,
                                                                    @Nullable String candidateSmiles,
                                                                    @Nullable String candidateName,
                                                                    boolean asCosineQuery
    ) {
        AnnotatedSpectrum annotatedPeaks;
        if (asCosineQuery){
            ReferenceLibrarySpectrum query = FAST_COSINE.prepareQuery(precursorMz, mergedMs2Peaks);
            annotatedPeaks = decorateMergedMsMs(new AnnotatedSpectrum(query), precursorMz);
            if (query.getParentIntensity() > 0)
                annotatedPeaks.setPrecursorPeak(new SimplePeak(query.getParentMass(), query.getParentIntensity()));
        } else {
            annotatedPeaks = decorateMergedMsMs(new AnnotatedSpectrum(mergedMs2Peaks), precursorMz);
            int precursorIdx = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.mostIntensivePeakWithin(mergedMs2Peaks, precursorMz, FAST_COSINE.getMaxDeviation());
            if (precursorIdx >= 0)
                annotatedPeaks.setPrecursorPeak(new SimplePeak(mergedMs2Peaks.getPeakAt(precursorIdx)));
        }
        annotatedPeaks.setCosineQuery(asCosineQuery);

        return createMsMsWithAnnotations(
                annotatedPeaks,
                ftree,
                candidateSmiles,
                candidateName,
                asCosineQuery
        );
    }

    @SneakyThrows
    public static AnnotatedSpectrum createMsMsWithAnnotations(@NotNull MergedMSnSpectrum specSource,
                                                              @Nullable FTree ftree,
                                                              @Nullable String candidateSmiles,
                                                              @Nullable String candidateName,
                                                              boolean asCosineQuery
    ) {
        double precursorMz = specSource.getMergedPrecursorMz();
        AnnotatedSpectrum annotatedPeaks;
        if (asCosineQuery){
            ReferenceLibrarySpectrum query = FAST_COSINE.prepareQuery(precursorMz, specSource.getPeaks());
            annotatedPeaks = decorateMsMs(new AnnotatedSpectrum(query), specSource);
            if (query.getParentIntensity() > 0)
                annotatedPeaks.setPrecursorPeak(new SimplePeak(query.getParentMass(), query.getParentIntensity()));
        } else {
            annotatedPeaks = decorateMsMs(new AnnotatedSpectrum(specSource.getPeaks()), specSource);
            int precursorIdx = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.mostIntensivePeakWithin(specSource.getPeaks(), precursorMz, FAST_COSINE.getMaxDeviation());
            if (precursorIdx >= 0)
                annotatedPeaks.setPrecursorPeak(new SimplePeak(specSource.getPeaks().getPeakAt(precursorIdx)));
        }
        annotatedPeaks.setCosineQuery(asCosineQuery);

        return createMsMsWithAnnotations(
                annotatedPeaks,
                ftree,
                candidateSmiles,
                candidateName,
                asCosineQuery
        );
    }


    @SneakyThrows
    private static AnnotatedSpectrum createMsMsWithAnnotations(@NotNull AnnotatedSpectrum spectrum,
                                                               @Nullable FTree ftree,
                                                               @Nullable String candidateSmiles,
                                                               @Nullable String candidateName,
                                                               boolean isModifiedMs2
    ) {
        if (ftree == null)
            return spectrum;
        Fragment[] fragments = annotateFragmentsToSingleMsMs(spectrum, ftree, spectrum.getPrecursorMz(), isModifiedMs2);
        return makeMsMsWithAnnotations(spectrum, ftree, Arrays.asList(fragments), candidateSmiles, candidateName);
    }

    private static AnnotatedSpectrum makeMsMsWithAnnotations(@NotNull AnnotatedSpectrum spectrum,
                                                             @NotNull FTree ftree,
                                                             @NotNull Collection<Fragment> fragments,
                                                             @Nullable String candidateSmiles,
                                                             @Nullable String candidateName
    ) throws CDKException {
        //compute substructure annotations //todo do we want to do this somewhere else? We need a cancellable job in the api anyways
        final InsilicoFragmentationResult structureAnno = candidateSmiles == null ? null
                : SiriusJobs.runInBackground(new InsilicoFragmentationPeakAnnotator().makeJJob(ftree, candidateSmiles)
                .asType(JJob.JobType.TINY_BACKGROUND)).takeResult(); //executed as tiny background job to be computed instantly for immediate response
        setSpectrumAnnotation(spectrum, ftree, structureAnno, candidateSmiles, candidateName);
        setPeakAnnotations(spectrum, ftree, fragments, structureAnno);

        // DEBUGGING/VALIDATION: Show tree annotations stats
        if (DEBUG) {
            long annotatedPeaks = spectrum.getPeaks().stream().filter(p -> p.getPeakAnnotation() != null).count();
            int peaks = spectrum.getPeaks().size();
            int extractedFragments = fragments.size();
            int treeSize = ftree.getFragmentsWithoutRoot().size();

            System.out.printf("%s [%s] ==>  OverallPeaks: %s; AnnotatedPeaks: %s; ExtractedFragments: %s; TreeFragments: %s.", spectrum.getName(), spectrum.isCosineQuery(), peaks, annotatedPeaks, extractedFragments, treeSize);
            System.out.println();
        }
        return spectrum;
    }

    /*
    duplicate code in SpectrumAnnotationJJob.setPeakAnnotations (require different AnnotatedSpectrum/Peaks). Both need to be changed consistently.
     */
    private static void setPeakAnnotations(@NotNull AnnotatedSpectrum spectrum, @NotNull FTree ftree, @NotNull Iterable<Fragment> fragments, @Nullable InsilicoFragmentationResult structureAnno) {
        List<AnnotatedPeak> peaks = spectrum.getPeaks();

        for (Fragment f : fragments) {
            if (f != null) {
                setPeakAnnotations(ftree, structureAnno, f, peaks);
            }
        }
    }

    /*
    duplicate code in SpectrumAnnotationJJob.setPeakAnnotations (require different AnnotatedSpectrum/Peaks). Both need to be changed consistently.
     */
    private static void setPeakAnnotations(@NotNull FTree ftree, @Nullable InsilicoFragmentationResult structureAnno, Fragment f, List<AnnotatedPeak> peaks) {
        int vertexId = f.getVertexId();
        PeakAnnotation.PeakAnnotationBuilder peakAnno = PeakAnnotation.builder();
        if (f.getFormula() != null && f.getIonization() != null) {
            peakAnno.molecularFormula(f.getFormula().toString())
                    .adduct(ftree.getAdduct(f).toString())
                    .exactMass(ftree.getExactMass(f))
                    .fragmentId(vertexId);
        }

        AnnotatedPeak peak = peaks.get(f.getPeakId());
        de.unijena.bioinf.ChemistryBase.ms.Deviation dev = ftree.getMassErrorTo(f, peak.getMass());

        peakAnno.massDeviationMz(dev.getAbsolute())
                .massDeviationPpm(dev.getPpm());


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

    /*
    duplicate code in SpectrumAnnotationJJob.setSpectrumAnnotation (require different AnnotatedSpectrum/Peaks). Both need to be changed consistently.
     */
    private static void setSpectrumAnnotation(AnnotatedSpectrum spectrum,
                                              @Nullable FTree ftree,
                                              @Nullable InsilicoFragmentationResult structureAnno,
                                              @Nullable String candidateSmiles,
                                              @Nullable String candidateName
    ) throws CDKException {
        if (ftree == null)
            return;
        // create formula/ftree based spectrum annotation
        SpectrumAnnotation.SpectrumAnnotationBuilder specAnno = SpectrumAnnotation.builder();

        Fragment precursorRoot = IonTreeUtils.getMeasuredIonRoot(ftree);
        MolecularFormula compoundFormula = IonTreeUtils.getCompoundMolecularFormula(ftree);
        PrecursorIonType ionType = ftree.getAnnotation(PrecursorIonType.class,
                () -> precursorRoot.getIonization() != null ? PrecursorIonType.getPrecursorIonType(precursorRoot.getIonization()) : null);
        if (precursorRoot.getFormula() != null && precursorRoot.getIonization() != null) {
            specAnno.molecularFormula(compoundFormula.toString())
                    .adduct(ionType.toString())
                    .exactMass(ftree.getExactMass(precursorRoot));
        }

        de.unijena.bioinf.ChemistryBase.ms.Deviation dev = ftree.getMassErrorTo(precursorRoot, spectrum.getPrecursorMz());
        specAnno.massDeviationMz(dev.getAbsolute()).massDeviationPpm(dev.getPpm());
        if (dev.getAbsolute()>1) {
            LoggerFactory.getLogger(Spectrums.class).warn("Wrong fragmentation tree fragment selected for precursor m/z. {} for m/z {}", precursorRoot, spectrum.getPrecursorMz());
        }

        if (structureAnno != null) {
            specAnno.structureAnnotationSmiles(candidateSmiles)
                    .structureAnnotationName(candidateName)
                    .structureAnnotationScore(structureAnno.getScore());
            specAnno.structureAnnotationSvg(smilesToSVG(candidateSmiles));
        }
        spectrum.setSpectrumAnnotation(specAnno.build());
    }

    /*
    duplicate code in SpectrumAnnotationJJob.annotateSubstructure (require different AnnotatedSpectrum/Peaks). Both need to be changed consistently.
     */
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

    /**
     * This method annotates peaks with fragment annotations.
     * Only works for fragmentation spectra that were used as input for the fragmentation tree computation.
     * Merged spectra are generated and annotated separately by the method 'createMergedMsMsWithAnnotations'.
     */
    private static Fragment[] annotateFragmentsToSingleMsMs(Spectrum<? extends Peak> spectrum, FTree tree, double precursorMz, boolean isModifiedMs2) {
        final FragmentAnnotation<de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak> annotatedPeak;
        if (tree == null || (annotatedPeak = tree.getFragmentAnnotationOrNull(de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak.class)) == null)
            return null;
        Fragment[] annotatedFormulas = new Fragment[spectrum.size()];

        final de.unijena.bioinf.ChemistryBase.ms.Deviation dev = new de.unijena.bioinf.ChemistryBase.ms.Deviation(10, 0.01);

        for (Fragment f : tree) {
            de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak peak = annotatedPeak.get(f);
            if (peak == null)
                continue;

            try {
                if (isModifiedMs2) {  // use approximate mz matching
                    boolean found = SpectrumAnnotationJJob.findCorrectPeakMs2Spectrum(tree, f, spectrum, peak, annotatedFormulas, dev, precursorMz, LoggerFactory.getLogger(Spectrums.class)::debug);
                    if (!found) {
                        if (f == tree.getRoot()) {
                            LoggerFactory.getLogger(Spectrums.class).debug("Root fragment '{}' of the fragmentation tree could not be assigned to a peak in the merged MS2 spectrum.", f.getFormula());
                        } else {
                            LoggerFactory.getLogger(Spectrums.class).debug("Fragment '{}' of the fragmentation tree could not be assigned to a peak in the merged MS2 spectrum.", f.getFormula());
                        }
                    }
                } else { // use exact mz matching
                    boolean found = SpectrumAnnotationJJob.findCorrectPeakInInputFragmentationSpectrum(f, spectrum, peak, annotatedFormulas, () -> {
                    });
                    if (!found) {
                        //can still be normal behaviour. The fragment peak might be indeed only contained in a subset of all MS2 spectra
                        LoggerFactory.getLogger(Spectrums.class).debug("Fragment '{}' of the fragmentation tree could not be assigned to a peak in the input MS2 spectrum. Could be that fragment is just not contained in this particular spectrum.", f.getFormula());
                    }
                }
            } catch (InterruptedException e) {
                LoggerFactory.getLogger(Spectrums.class).error("Annotating spectrum peak was interrupted.", e);
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


    @JsonIgnore
    private static BasicSpectrum simulateIsotopePattern(@NotNull FTree tree, Spectrum<?> isotopePattern) {
        final MolecularFormula formula = IonTreeUtils.getPrecursorFormula(tree);
        final FastIsotopePatternGenerator gen = new FastIsotopePatternGenerator(Normalization.Max);
        gen.setMinimalProbabilityThreshold(Math.min(0.005, de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.getMinimalIntensity(isotopePattern)));
        gen.setMaximalNumberOfPeaks(Math.max(4, isotopePattern.size()));
        BasicSpectrum simulatedPattern = new BasicSpectrum(gen.simulatePattern(formula,
                tree.getAnnotation(PrecursorIonType.class).orElseThrow().getIonization()));
        simulatedPattern.setName("Simulated Isotope Pattern");

        return simulatedPattern;

    }
    //endregion


    public static String smilesToSVG(String smiles) throws CDKException {
        final MolecularGraph graph = new MolecularGraph(
                new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles)
        );
        return new DepictionGenerator()
                .withAtomColors(new UniColor(Colors.FOREGROUND_INTERFACE))
                .withBackgroundColor(Colors.BACKGROUND)
                .depict(graph.getMolecule()).toSvgStr();

    }

}
