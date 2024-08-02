/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MutableMs2Experiment implements Ms2Experiment {

    private PrecursorIonType precursorIonType;
    private List<SimpleSpectrum> ms1Spectra;
    private SimpleSpectrum mergedMs1Spectrum;
    private List<MutableMs2Spectrum> ms2Spectra;

    private double ionMass;
    private MolecularFormula molecularFormula;
    private String name;

    @Nullable
    private String featureId;


    private Annotated.Annotations<Ms2ExperimentAnnotation> annotations;

    @Override
    public Annotations<Ms2ExperimentAnnotation> annotations() {
        return annotations;
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

    public void setName(String name) {
        this.name = name;
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
    public String getName() {
        return name;
    }

    @Override
    public PrecursorIonType getPrecursorIonType() {
        return precursorIonType;
    }

    @Override
    public List<SimpleSpectrum> getMs1Spectra() {
        return ms1Spectra;
    }

    @Override
    public SimpleSpectrum getMergedMs1Spectrum() {
        return mergedMs1Spectrum;
    }

    @Override
    public List<MutableMs2Spectrum> getMs2Spectra() {
        return ms2Spectra;
    }

    @Override
    public double getIonMass() {
        return ionMass;
    }

    @Override
    public MolecularFormula getMolecularFormula() {
        return molecularFormula;
    }

    public void setPrecursorIonType(PrecursorIonType precursorIonType) {
        this.precursorIonType = precursorIonType;
    }

    public void setMs1Spectra(List<SimpleSpectrum> ms1Spectra) {
        this.ms1Spectra = ms1Spectra;
    }

    public void setMergedMs1Spectrum(SimpleSpectrum mergedMs1Spectrum) {
        this.mergedMs1Spectrum = mergedMs1Spectrum;
    }

    public void setMs2Spectra(List<MutableMs2Spectrum> ms2Spectra) {
        this.ms2Spectra = ms2Spectra;
    }

    public void setIonMass(double ionMass) {
        this.ionMass = ionMass;
    }

    public void setMolecularFormula(MolecularFormula molecularFormula) {
        this.molecularFormula = molecularFormula;
    }

    @Nullable
    @Override
    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    @Override
    public MutableMs2Experiment clone() {
        return new MutableMs2Experiment(this);
    }
}
