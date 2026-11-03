/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class MutableMs2Experiment implements Ms2Experiment {

    private PrecursorIonType precursorIonType;
    private List<SimpleSpectrum> ms1Spectra;
    private SimpleSpectrum mergedMs1Spectrum;
    private List<MutableMs2Spectrum> ms2Spectra;
    @Nullable
    private SimpleSpectrum mergedMs2Spectrum;
    private double ionMass;
    private MolecularFormula molecularFormula;
    private String name;

    @Nullable
    private String featureId;
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private Annotated.Annotations<Ms2ExperimentAnnotation> annotations;

    @Override
    public Annotations<Ms2ExperimentAnnotation> annotations() {
        return annotations;
    }

    /**
     * Copied into a list this experiment owns, rather than stored as handed over.
     * <p>
     * Everything above treats these spectra as mutable - the parsers add to them in place, and the validator drops
     * empty ones through an iterator - while a caller naturally builds the list with {@code Stream.toList()} or
     * {@code Arrays.asList}, neither of which can be changed. The result was an unsupported operation raised deep
     * inside a validator, for the one experiment that happened to contain an empty spectrum, and a whole feature
     * losing its spectral library search because of it.
     * <p>
     * An absent list stays absent: the validator distinguishes a missing MS2 list, which is an invalid experiment,
     * from an empty one.
     */
    public void setMs2Spectra(List<MutableMs2Spectrum> ms2Spectra) {
        this.ms2Spectra = ms2Spectra == null ? null : new ArrayList<>(ms2Spectra);
    }

    /** @see #setMs2Spectra(List) */
    public void setMs1Spectra(List<SimpleSpectrum> ms1Spectra) {
        this.ms1Spectra = ms1Spectra == null ? null : new ArrayList<>(ms1Spectra);
    }

    public MutableMs2Experiment() {
        this.ms1Spectra = new ArrayList<>();
        this.ms2Spectra = new ArrayList<>();
        this.annotations = new Annotations<>();
        this.name = "";
    }

    public MutableMs2Experiment(Ms2Experiment experiment) {
        this(experiment, true);
    }

    public MutableMs2Experiment(Ms2Experiment experiment, boolean cloneAnnotations) {
        this.precursorIonType = experiment.getPrecursorIonType();
        this.ms1Spectra = new ArrayList<>();
        for (Spectrum<Peak> spec : experiment.getMs1Spectra())
            ms1Spectra.add(new SimpleSpectrum(spec));
        this.mergedMs1Spectrum = experiment.getMergedMs1Spectrum() == null ? null : new SimpleSpectrum(experiment.getMergedMs1Spectrum());
        this.ms2Spectra = new ArrayList<>();
        int id = 0;
        for (Ms2Spectrum<Peak> ms2spec : experiment.getMs2Spectra()) {
            final MutableMs2Spectrum ms2 = new MutableMs2Spectrum(ms2spec);
            ms2.setScanNumber(id++);
            this.ms2Spectra.add(ms2);

        }
        this.annotations = cloneAnnotations ? experiment.annotations().clone() : new Annotations<>();
        this.ionMass = experiment.getIonMass();
        this.molecularFormula = experiment.getMolecularFormula();
        this.name = experiment.getName();
    }

    @Override
    public MutableMs2Experiment mutate() {
        return this;
    }

    @Override
    @Nullable
    public URI getSource() {
        final SourceLocation s = getSourceAnnotation();
        return s != null ? s.value : null;
    }

    @Override
    @Nullable
    public String getSourceString() {
        final SourceLocation s = getSourceAnnotation();
        return s != null ? s.toString() : null;
    }

    @Nullable
    public SourceLocation getSourceAnnotation() {
        if (hasAnnotation(SpectrumFileSource.class))
            return getAnnotationOrThrow(SpectrumFileSource.class);
        if (hasAnnotation(MsFileSource.class))
            return getAnnotationOrThrow(MsFileSource.class);
        return null;
    }

    public void setSource(@NotNull SourceLocation sourcelocation) {
        if (sourcelocation instanceof SpectrumFileSource)
            setAnnotation(SpectrumFileSource.class, (SpectrumFileSource) sourcelocation);
        else if (sourcelocation instanceof MsFileSource)
            setAnnotation(MsFileSource.class, (MsFileSource) sourcelocation);
    }

    @Override
    public MutableMs2Experiment clone() {
        return new MutableMs2Experiment(this);
    }
}
