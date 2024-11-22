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

package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.ChemistryBase.ms.utils.WrapperSpectrum;
import de.unijena.bioinf.fragmenter.*;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import io.sirius.ms.sdk.model.*;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
//todo this whole class is duplicated code with Spectrums class in the middleware.
// but we need to compute the annotations in the frontend to be able to cancel when user switches feature to display.
public class SpectrumAnnotationJJob extends BasicMasterJJob<AnnotatedMsMsData> {
    private FTree ftree;
    private MsData msData;
    private String smiles;

    public SpectrumAnnotationJJob(@NotNull FTree ftree, @NotNull MsData msData, @Nullable String inChIKey2d) {
        super(JobType.TINY_BACKGROUND);
        this.ftree = ftree;
        this.msData = msData;
        this.smiles = inChIKey2d;
    }

    @Override
    protected AnnotatedMsMsData compute() throws Exception {
        AnnotatedMsMsData annotatedMsMsData = null;
        try {
            checkForInterruption();
            annotatedMsMsData = new AnnotatedMsMsData();
            final InsilicoFragmentationResult structureAnno = smiles == null ? null :
                    submitSubJob(new InsilicoFragmentationPeakAnnotator().makeJJob(ftree, smiles)
                            .asType(JobType.TINY_BACKGROUND)).awaitResult();
            checkForInterruption();

            List<AnnotatedSpectrum> annotatedMsMs = msData.getMs2Spectra().stream()
                    .map(this::toAnnotatedSpectrum)
                    .peek(as -> annotateMsMs(as, ftree, structureAnno, smiles, false))
                    .toList();
            checkForInterruption();
            annotatedMsMsData.setMs2Spectra(annotatedMsMs);
            annotatedMsMsData.setMergedMs2(annotateMsMs(toAnnotatedSpectrum(msData.getMergedMs2()), ftree, structureAnno, smiles, true));
            checkForInterruption();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof Exception)
                throw (Exception) e.getCause();
            else throw e;
        }
        return annotatedMsMsData;
    }

    /**
     * Annotates peaks which are explained by fragmentation tree fragments.
     * Currently, does NOT annotate the fragment's isotope peaks.
     */
    @SneakyThrows
    private AnnotatedSpectrum annotateMsMs(@Nullable AnnotatedSpectrum as, FTree ftree, InsilicoFragmentationResult structureAnno, @Nullable String inChIKey2d, boolean isMergedMs2) {
        if (as == null)
            return null;
        checkForInterruption();
        Fragment[] fragments = annotateFragmentsToSingleMsMs(WrapperSpectrum.of(as.getPeaks(), AnnotatedPeak::getMz, AnnotatedPeak::getIntensity), ftree, as.getPrecursorMz(), isMergedMs2);
        checkForInterruption();
        setSpectrumAnnotation(as, ftree, structureAnno, inChIKey2d);
        checkForInterruption();
        setPeakAnnotations(as, ftree, Arrays.asList(fragments), structureAnno, isMergedMs2);
        return as;
    }

    private void setPeakAnnotations(@NotNull AnnotatedSpectrum spectrum,
                                    @NotNull FTree ftree, @NotNull Iterable<Fragment> fragments,
                                    @Nullable InsilicoFragmentationResult structureAnno, boolean isMergedMs2) throws InterruptedException {
        List<AnnotatedPeak> peaks = spectrum.getPeaks();

        for (Fragment f : fragments) {
            checkForInterruption();
            if (f != null) {
                setPeakAnnotations(ftree, structureAnno, f, peaks, isMergedMs2);
            }
        }
    }

    private void setPeakAnnotations(@NotNull FTree ftree, @Nullable InsilicoFragmentationResult structureAnno, Fragment f, List<AnnotatedPeak> peaks, boolean isMergedMs2) throws InterruptedException {
        int vertexId = f.getVertexId();
        PeakAnnotation peakAnno = new PeakAnnotation();
        if (f.getFormula() != null && f.getIonization() != null) {
            peakAnno.molecularFormula(f.getFormula().toString())
                    .ionization(f.getIonization().toString())
                    .exactMass(ftree.getExactMass(f))
                    .fragmentId(vertexId);
        }

        AnnotatedPeak peak = peaks.get(f.getPeakId());
        de.unijena.bioinf.ChemistryBase.ms.Deviation dev = ftree.getMassErrorTo(f, peak.getMz());

        peakAnno.massDeviationMz(dev.getAbsolute())
                .massDeviationPpm(dev.getPpm());


        if (isMergedMs2) {
            //there may be the edge case that the root fragment actually used the MS1 precursor m/z and not the m/z of any actual MS2 peak.
            //I believe in this case the recalibrated m/z is always identical to the normal m/z. I am not entirely sure if this may influence anything.
            //todo: are we using the recalibrated m/z for anything?
            de.unijena.bioinf.ChemistryBase.ms.Deviation rdev = ftree.getRecalibratedMassError(f);
            if (!rdev.equals(de.unijena.bioinf.ChemistryBase.ms.Deviation.NULL_DEVIATION))
                peakAnno.recalibratedMassDeviationMz(rdev.getAbsolute())
                        .recalibratedMassDeviationPpm(rdev.getPpm());
        }


        // we only store incoming edges because references are ugly for serialization
        f.getIncomingEdges().stream().findFirst().ifPresent(l ->
                peakAnno.parentPeak(new ParentPeak()
                        .lossFormula(l.getFormula().toString())
                        .parentIdx((int) l.getSource().getPeakId())
                        .parentFragmentId(l.getSource().getVertexId())
                ));
        if (structureAnno != null) {
            CombinatorialFragment subStr = Optional.ofNullable(structureAnno.getFragmentMapping().get(f))
                    .map(List::stream).flatMap(Stream::findFirst).orElse(null);
            if (subStr != null)
                annotateSubstructure(peakAnno, f.getFormula(), subStr, structureAnno.getSubtree());
        }
        //add annotations to corresponding peak
        peaks.get(f.getPeakId()).setPeakAnnotation(peakAnno);
    }

    private void annotateSubstructure(PeakAnnotation peakAnno, MolecularFormula fragmentFormula, CombinatorialFragment subStructureAnno, CombinatorialSubtree subtree) throws InterruptedException {
        CombinatorialNode node = subtree.getNode(subStructureAnno.getBitSet());
        checkForInterruption();

        final int[] bondIdx = subStructureAnno.bonds().stream().mapToInt(Integer::intValue).sorted().toArray();
        checkForInterruption();

        List<Integer> atomIdx = Arrays.stream(subStructureAnno.getAtoms()).mapToInt(IAtom::getIndex).boxed().toList();
        checkForInterruption();

        List<Integer> cutIdx = Arrays.stream(subStructureAnno.getAtoms())
                .flatMap(a -> StreamSupport.stream(a.bonds().spliterator(), false))
                .distinct()
                .mapToInt(IBond::getIndex)
                .filter(b -> Arrays.binarySearch(bondIdx, b) < 0)
                .sorted()
                .boxed()
                .toList();
        checkForInterruption();

        peakAnno.substructureAtoms(atomIdx)
                .substructureBonds(Arrays.stream(bondIdx).boxed().toList())
                .substructureBondsCut(cutIdx)
                .substructureScore(node.getTotalScore())
                .hydrogenRearrangements(subStructureAnno.hydrogenRearrangements(fragmentFormula));
    }


    private void setSpectrumAnnotation(@NotNull AnnotatedSpectrum spectrum, @Nullable FTree ftree,
                                       @Nullable InsilicoFragmentationResult structureAnno,
                                       @Nullable String candidateSmiles
    ) {
        if (ftree == null)
            return;
        // create formula/ftree based spectrum annotation
        SpectrumAnnotation specAnno = new SpectrumAnnotation();

        Fragment precursorRoot = IonTreeUtils.getMeasuredIonRoot(ftree);
        if (precursorRoot.getFormula() != null && precursorRoot.getIonization() != null) {
            specAnno.molecularFormula(precursorRoot.getFormula().toString())
                    .ionization(precursorRoot.getIonization().toString()) //todo should this be really an ionization and not the PrecursorIonType? In this case we need to make sure to used the correctly resolved formula or the precursor ion
                    .exactMass(ftree.getExactMass(precursorRoot));
        }

        de.unijena.bioinf.ChemistryBase.ms.Deviation dev = ftree.getMassErrorTo(precursorRoot, spectrum.getPrecursorMz());
        specAnno.massDeviationMz(dev.getAbsolute()).massDeviationPpm(dev.getPpm());
        if (dev.getAbsolute()>1) {
            logWarn("Wrong fragmentation tree fragment selected for precursor m/z. {} for m/z {}", precursorRoot, spectrum.getPrecursorMz());
        }

        if (structureAnno != null) {
            specAnno.structureAnnotationSmiles(candidateSmiles)
                    .structureAnnotationScore(structureAnno.getScore());
        }
        spectrum.setSpectrumAnnotation(specAnno);
    }

    @SneakyThrows
    private AnnotatedSpectrum toAnnotatedSpectrum(@Nullable BasicSpectrum spectrum) {
        if (spectrum == null)
            return null;
        checkForInterruption();

        List<AnnotatedPeak> peaks = spectrum.getPeaks().stream()
                .map(p -> new AnnotatedPeak().mz(p.getMz()).intensity(p.getIntensity())).toList();
        return new AnnotatedSpectrum().peaks(peaks)
                .precursorMz(spectrum.getPrecursorMz())
                .scanNumber(spectrum.getScanNumber())
                .collisionEnergy(spectrum.getCollisionEnergy())
                .absIntensityFactor(spectrum.getAbsIntensityFactor())
                .msLevel(spectrum.getMsLevel())
                .name(spectrum.getName());
    }

    /**
     * annotates
     * @param spectrum
     * @param tree
     * @param isMergedM2
     * @return
     * @throws InterruptedException
     */
    private Fragment[] annotateFragmentsToSingleMsMs(Spectrum<? extends Peak> spectrum, FTree tree, Double precursorMz, boolean isMergedM2) throws InterruptedException {
        final FragmentAnnotation<de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak> annotatedPeak;
        if (tree == null || (annotatedPeak = tree.getFragmentAnnotationOrNull(de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak.class)) == null)
            return null;
        checkForInterruption();

        Fragment[] annotatedFormulas = new Fragment[spectrum.size()];
        final de.unijena.bioinf.ChemistryBase.ms.Deviation dev = new de.unijena.bioinf.ChemistryBase.ms.Deviation(1, 0.01);

        for (Fragment f : tree) {
            checkForInterruption();

            de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak peak = annotatedPeak.get(f);
            if (peak == null || peak.isArtificial()) {
                continue;
            }

            if (isMergedM2) {
                boolean found = findCorrectPeakInMergedMs2Spectrum(tree, f, spectrum, peak, annotatedFormulas, dev, precursorMz);
                if (!found) {
                    if (f == tree.getRoot()) {
                        logWarn("Root fragment '{}' of the fragmentation tree could not be assigned to a peak in the merged MS2 spectrum.", f.getFormula());
                    } else {
                        logWarn("Fragment '{}' of the fragmentation tree could not be assigned to a peak in the merged MS2 spectrum.", f.getFormula());
                    }
                }
            } else {
                boolean found = findCorrectPeakInInputFragmentationSpectrum(f, spectrum, peak, annotatedFormulas, this::checkForInterruption);
                if (!found) {
                    logWarn("Fragment '{}' of the fragmentation tree could not be assigned to a peak in the input MS2 spectrum.", f.getFormula());
                }
            }
        }

        return annotatedFormulas;
    }

    public static boolean findCorrectPeakInInputFragmentationSpectrum(Fragment f, Spectrum<? extends Peak> spectrum, de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak peak, Fragment[] annotatedFormulas, InterruptionCheck interruptionCheck) throws InterruptedException {
        for (Peak p : peak.getOriginalPeaks()) {
            interruptionCheck.check();
            int i = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.binarySearch(spectrum, p.getMass());
            if (i >= 0) {
                f.setPeakId(i);
                annotatedFormulas[i] = f;
                return true;
            }
        }
        return false;
    }

    private boolean findCorrectPeakInMergedMs2Spectrum(FTree tree, Fragment f, Spectrum<? extends Peak> spectrum, de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak peak, Fragment[] annotatedFormulas, de.unijena.bioinf.ChemistryBase.ms.Deviation dev, Double precursorMz) {
        if (f == tree.getRoot()){
            //the precursor mass (MS1) may be used for the root fragment. First check, if there is some exact mz match. Else use most intense beak in proximity.
            int i = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.binarySearch(spectrum, peak.getMass());
            if (i < 0) {
                i = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.mostIntensivePeakWithin(spectrum, peak.getMass(), dev);
            }
            if (i >= 0) {
                f.setPeakId(i);
                annotatedFormulas[i] = f;
                return true;
            }
        } else {
            int i = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.binarySearch(spectrum, peak.getMass());
            if (i < 0) {
                //the only expected case would be a fragment corresponding to the precursor mz for a tree with insource PrecursorIonType (may be replaced by MS1 peaks).
                i = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.mostIntensivePeakWithin(spectrum, peak.getMass(), dev);
                {
                    boolean isPrecursorFragment = precursorMz != null && dev.inErrorWindow(peak.getMass(), precursorMz);
                    if (!isPrecursorFragment) logDebug("Fragment '{}' in the fragmentation tree does not correspond to an exactly matching m/z in the merged MS2 spectrum. And it is not the 'precursor' of an adduct ion type.", f.getFormula());
                }
            }
            if (i >= 0) {
                f.setPeakId(i);
                annotatedFormulas[i] = f;
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    public interface InterruptionCheck {
        void check() throws InterruptedException;
    }
}
