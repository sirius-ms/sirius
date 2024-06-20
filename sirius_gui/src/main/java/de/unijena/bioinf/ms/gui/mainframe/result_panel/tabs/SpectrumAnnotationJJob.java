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
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms2IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.utils.WrapperSpectrum;
import de.unijena.bioinf.fragmenter.*;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.ms.nightsky.sdk.model.*;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;

import java.util.Arrays;
import java.util.BitSet;
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
                    .peek(as -> annotateMsMs(as, ftree, structureAnno, smiles))
                    .toList();
            checkForInterruption();
            annotatedMsMsData.setMs2Spectra(annotatedMsMs);
            annotatedMsMsData.setMergedMs2(annotateMsMs(toAnnotatedSpectrum(msData.getMergedMs2()), ftree, structureAnno, smiles));
            checkForInterruption();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof Exception)
                throw (Exception) e.getCause();
            else throw e;
        }
        return annotatedMsMsData;
    }

    @SneakyThrows
    private AnnotatedSpectrum annotateMsMs(@Nullable AnnotatedSpectrum as, FTree ftree, InsilicoFragmentationResult structureAnno, @Nullable String inChIKey2d) {
        if (as == null)
            return null;
        checkForInterruption();
        Fragment[] fragments = annotateFragmentsToSingleMsMs(WrapperSpectrum.of(as.getPeaks(), AnnotatedPeak::getMz, AnnotatedPeak::getIntensity), ftree);
        checkForInterruption();
        setSpectrumAnnotation(as, ftree, structureAnno, inChIKey2d);
        checkForInterruption();
        setPeakAnnotations(as, ftree, Arrays.asList(fragments), structureAnno);
        return as;
    }

    private void setPeakAnnotations(@NotNull AnnotatedSpectrum spectrum,
                                    @NotNull FTree ftree, @NotNull Iterable<Fragment> fragments,
                                    @Nullable InsilicoFragmentationResult structureAnno) throws InterruptedException {
        List<AnnotatedPeak> peaks = spectrum.getPeaks();

        for (Fragment f : fragments) {
            checkForInterruption();
            if (f != null) {
                int vertexId = f.getVertexId();
                PeakAnnotation peakAnno = new PeakAnnotation();
                if (f.getFormula() != null && f.getIonization() != null) {
                    peakAnno.molecularFormula(f.getFormula().toString())
                            .ionization(f.getIonization().toString())
                            .exactMass(f.getIonization().addToMass(f.getFormula().getMass()))
                            .fragmentId(vertexId);
                }

                // deviation (from FTJsonWriter tree2json)
                {
                    de.unijena.bioinf.ChemistryBase.ms.Deviation dev = ftree.getMassError(f);
                    if (f.isRoot() && dev.equals(de.unijena.bioinf.ChemistryBase.ms.Deviation.NULL_DEVIATION))
                        dev = ftree.getMassErrorTo(f, spectrum.getPrecursorMz());

                    de.unijena.bioinf.ChemistryBase.ms.Deviation rdev = ftree.getRecalibratedMassError(f);
                    if (f.isRoot() && rdev.equals(de.unijena.bioinf.ChemistryBase.ms.Deviation.NULL_DEVIATION))
                        rdev = ftree.getMassErrorTo(f, spectrum.getPrecursorMz());


                    if (!dev.equals(de.unijena.bioinf.ChemistryBase.ms.Deviation.NULL_DEVIATION))
                        peakAnno.massDeviationMz(dev.getAbsolute())
                                .massDeviationPpm(dev.getPpm());
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
        }
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

        if (ftree.getRoot().getFormula() != null && ftree.getRoot().getIonization() != null) {
            specAnno.molecularFormula(ftree.getRoot().getFormula().toString())
                    .ionization(ftree.getRoot().getIonization().toString())
                    .exactMass(ftree.getRoot().getIonization().addToMass(ftree.getRoot().getFormula().getMass()));
        }

        de.unijena.bioinf.ChemistryBase.ms.Deviation dev = ftree.getMassErrorTo(ftree.getRoot(), spectrum.getPrecursorMz());
        specAnno.massDeviationMz(dev.getAbsolute()).massDeviationPpm(dev.getPpm());

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

    private Fragment[] annotateFragmentsToSingleMsMs(Spectrum<? extends Peak> spectrum, FTree tree) throws InterruptedException {
        final FragmentAnnotation<de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak> annotatedPeak;
        if (tree == null || (annotatedPeak = tree.getFragmentAnnotationOrNull(de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak.class)) == null)
            return null;
        checkForInterruption();

        Fragment[] annotatedFormulas = new Fragment[spectrum.size()];
        BitSet isIsotopicPeak = new BitSet(spectrum.size());
        final FragmentAnnotation<Ms2IsotopePattern> isoAno = tree.getFragmentAnnotationOrNull(Ms2IsotopePattern.class);
        final de.unijena.bioinf.ChemistryBase.ms.Deviation dev = new de.unijena.bioinf.ChemistryBase.ms.Deviation(1, 0.01);
        for (Fragment f : tree) {
            checkForInterruption();

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
                checkForInterruption();

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
}
