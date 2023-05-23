/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.spectraldb.entities;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.babelms.massbank.MassbankFormat;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Ms2SpectralMetadata extends MutableMs2Spectrum {

    private long id = -1L;
    private long peaksId = -1L;

    private PrecursorIonType precursorIonType;

    private double ionMass;

    private MolecularFormula formula;

    private String name;

    private String smiles;

    private DBLink spectralDbLink;

    private final List<DBLink> dbLinks = new ArrayList<>();

    private final ExperimentAnnotated experimentAnnotated = new ExperimentAnnotated();

    public Ms2SpectralMetadata() {
    }

    public Ms2SpectralMetadata(MutableMs2Experiment experiment, MutableMs2Spectrum spec) {
        super(spec);
        this.precursorIonType = experiment.getPrecursorIonType();
        this.ionMass = experiment.getIonMass();
        this.formula = experiment.getMolecularFormula();
        this.name = experiment.getName();
        experiment.getAnnotation(Smiles.class).ifPresent(smiles -> {
            this.smiles = smiles.toString();
        });
        // TODO read NIST msp format
        spec.getAnnotation(AdditionalFields.class).ifPresent(fields -> {
            if (fields.containsKey(MassbankFormat.ACCESSION.k())) {
                this.spectralDbLink = new DBLink(DataSource.MASSBANK.realName, fields.get(MassbankFormat.ACCESSION.k()));
            }
            if (fields.containsKey(MassbankFormat.CH_LINK.k())) {
                String link = fields.get(MassbankFormat.CH_LINK.k());
                if (link.startsWith("PUBCHEM")) {
                    String[] split = link.strip().split(" ");
                    if (split.length > 1) {
                        this.dbLinks.add(new DBLink(DataSource.PUBCHEM.realName, split[1]));
                    }
                }
            }
        });

        this.experimentAnnotated.addAnnotationsFrom(experiment);
        this.addAnnotationsFrom(spec);
        this.peaks.clear();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPeaksId() {
        return peaksId;
    }

    public void setPeaksId(long peaksId) {
        this.peaksId = peaksId;
    }


    public PrecursorIonType getPrecursorIonType() {
        return precursorIonType;
    }

    public void setPrecursorIonType(PrecursorIonType precursorIonType) {
        this.precursorIonType = precursorIonType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    public void setFormula(MolecularFormula formula) {
        this.formula = formula;
    }

    public double getIonMass() {
        return ionMass;
    }

    public void setIonMass(double ionMass) {
        this.ionMass = ionMass;
    }

    public String getSmiles() {
        return smiles;
    }

    public void setSmiles(String smiles) {
        this.smiles = smiles;
    }

    public DBLink getSpectralDbLink() {
        return spectralDbLink;
    }

    public void setSpectralDbLink(DBLink spectralDbLink) {
        this.spectralDbLink = spectralDbLink;
    }

    public List<DBLink> getDbLinks() {
        return dbLinks;
    }

    public void addDbLink(DBLink link) {
        this.dbLinks.add(link);
    }

    public Annotations<Ms2ExperimentAnnotation> experimentAnnotations() {
        return experimentAnnotated.annotations();
    }

    public void addExperimentAnnotationsFrom(Annotated<Ms2ExperimentAnnotation> annotated) {
        experimentAnnotated.addAnnotationsFrom(annotated);
    }

    public void addExperimentAnnotationsFrom(Map<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> map) {
        experimentAnnotated.addAnnotationsFrom(map);
    }

    public static List<Pair<Ms2SpectralMetadata, SpectralData>> fromMutableMs2Experiment(MutableMs2Experiment experiment) {
        return experiment.getMs2Spectra().stream().map(s -> Pair.of(new Ms2SpectralMetadata(experiment, s), new SpectralData(s))).collect(Collectors.toList());
    }

    public static MutableMs2Spectrum toMutableMs2Spectrum(Ms2SpectralMetadata metadata, SpectralData data) {
        MutableMs2Spectrum ms2Spectrum = metadata.shallowCopy();
        ms2Spectrum.addAnnotationsFrom(metadata);
        for (Peak p : data) {
            ms2Spectrum.addPeak(p);
        }
        return ms2Spectrum;
    }

    private static final class ExperimentAnnotated implements Annotated<Ms2ExperimentAnnotation> {

        private Annotations<Ms2ExperimentAnnotation> annotations = new Annotations<>();

        @Override
        public Annotations<Ms2ExperimentAnnotation> annotations() {
            return annotations;
        }

    }

}
