/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.spectra;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.fragmenter.*;
import de.unijena.bioinf.sirius.Ms2Preprocessor;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Spectrums {

    public static BasicSpectrum createMergedMsMs(@NotNull Ms2Experiment exp) {
        final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
        final ProcessedInput processedInput = preprocessor.preprocess(exp);
        return new BasicSpectrum(List.copyOf(processedInput.getMergedPeaks()));
    }

    public static AnnotatedSpectrum createMergedMsMsWithAnnotations(@NotNull Ms2Experiment exp, @Nullable FTree ftree) {
        return createMergedMsMsWithAnnotations(exp, ftree, null);
    }

    public static AnnotatedSpectrum createMergedMsMsWithAnnotations(@NotNull Ms2Experiment exp, @Nullable FTree ftree, @Nullable String candidateSmiles) {
        final Ms2Preprocessor preprocessor = new Ms2Preprocessor();
        final ProcessedInput processedInput = preprocessor.preprocess(exp);
        List<ProcessedPeak> processedPeaks = processedInput.getMergedPeaks();
        List<AnnotatedPeak> peaks = processedPeaks.stream()
                .map(p -> new AnnotatedPeak(p.getMass(), p.getIntensity(), null)).toList();

        AnnotatedSpectrum spectrum = new AnnotatedSpectrum(peaks);
        if (peaks.isEmpty() || exp.getMs2Spectra().isEmpty())
            return spectrum;

        spectrum.setPrecursorMz(exp.getMs2Spectra().get(0).getPrecursorMz());
        if (ftree != null) {
            //map tree to spectrum
            processedInput.mapTreeToInput(ftree);
            //compute substructure annotations //todo do we want to do this somewhere else?
            final InsilicoFragmentationResult structureAnno = candidateSmiles == null ? null
                    : new InsilicoFragmentationPeakAnnotator().fragmentAndAnnotate(ftree, candidateSmiles);


            SpectrumAnnotation.SpectrumAnnotationBuilder specAnno = SpectrumAnnotation.builder();
            { // create formula/ftree based spectrum annotation
                if (ftree.getRoot().getFormula() != null && ftree.getRoot().getIonization() != null) {
                    specAnno.formula(ftree.getRoot().getFormula().toString())
                            .ionization(ftree.getRoot().getIonization().toString())
                            .exactMass(ftree.getRoot().getIonization().addToMass(ftree.getRoot().getFormula().getMass()));
                }

                Deviation dev = ftree.getMassErrorTo(ftree.getRoot(), spectrum.getPrecursorMz());
                specAnno.massDeviationMz(dev.getAbsolute()).massDeviationPpm(dev.getPpm());

                if (structureAnno != null) {
                    specAnno.structureAnnotationSmiles(candidateSmiles)
                            .structureAnnotationScore(structureAnno.getScore());
                }
            }

            for (Fragment f : ftree) {
                short i = f.getPeakId();
                if (i >= 0) {
                    PeakAnnotation.PeakAnnotationBuilder peakAnno = PeakAnnotation.builder();
                    if (f.getFormula() != null && f.getIonization() != null) {
                        peakAnno.formula(f.getFormula().toString());
                        peakAnno.ionization(f.getIonization().toString());
                        peakAnno.exactMass(f.getIonization().addToMass(f.getFormula().getMass()));
                    }

                    // deviation (from FTJsonWriter tree2json)
                    {
                        Deviation dev = ftree.getMassError(f);
                        if (f.isRoot() && dev.equals(Deviation.NULL_DEVIATION))
                            dev = ftree.getMassErrorTo(f, spectrum.getPrecursorMz());
                        Deviation rdev = ftree.getRecalibratedMassError(f);
                        if (f.isRoot() && dev.equals(Deviation.NULL_DEVIATION))
                            rdev = ftree.getMassErrorTo(f, spectrum.getPrecursorMz());

                        peakAnno.massDeviationMz(dev.getAbsolute())
                                .massDeviationPpm(dev.getPpm())
                                .recalibratedMassDeviationMz(rdev.getAbsolute())
                                .recalibratedMassDeviationPpm(rdev.getPpm());
                    }

                    // we only store incoming edges because references are ugly for serialization
                    f.getIncomingEdges().stream().findFirst().ifPresent(l ->
                            peakAnno.parentPeak(ParentPeak.builder()
                                    .lossFormula(l.getFormula().toString())
                                    .parentIdx((int) l.getSource().getPeakId())
                                    .build()));

                    if (structureAnno != null) {
                        Optional.ofNullable(structureAnno.getFragmentMapping().get(f))
                                .map(List::stream).flatMap(Stream::findFirst)
                                .ifPresent(subStr -> annotateSubstructure(
                                        peakAnno, f.getFormula(), subStr, structureAnno.getSubtree()));
                    }

                    //add annotations to corresponding peak
                    peaks.get(i).setPeakAnnotation(peakAnno.build());
                }
            }
        }

        return spectrum;
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
}
