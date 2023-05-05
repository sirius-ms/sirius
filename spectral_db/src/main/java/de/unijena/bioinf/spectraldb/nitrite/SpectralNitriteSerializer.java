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

package de.unijena.bioinf.spectraldb.nitrite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.AbstractSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.nitrite.NitriteSerializer;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;
import de.unijena.bioinf.spectraldb.SpectralNoSQLSerializer;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.lang3.ClassUtils;
import org.dizitart.no2.Document;

public class SpectralNitriteSerializer extends NitriteSerializer implements SpectralNoSQLSerializer {

    protected static final ObjectMapper mapper = new ObjectMapper();

    // TODO spectral serialization

    @Override
    public <P extends Peak> Document serializeSpectrum(Spectrum<P> spectrum, long compoundId) throws ChemicalDatabaseException {
        Document doc = Document.createDocument("compundId", compoundId);

        // TODO spectral meta data:
        // TODO compound name?
        // TODO original smiles from spectrum (does not necessarily equal compound smiles!)
        // TODO spectrum library identifiers
        // TODO additional notes / technical parameters?
        // TODO collisionenergy

        // TODO NIST msp, massbank format

        if (ClassUtils.getAllSuperclasses(spectrum.getClass()).contains(AbstractSpectrum.class)) {
            doc.put("class", spectrum.getClass());
            DoubleList masses = new DoubleArrayList();
            DoubleList intensities = new DoubleArrayList();
            for (P peak : spectrum) {
                masses.add(peak.getMass());
                intensities.add(peak.getIntensity());
                // TODO what about peak annotations and additional fields!?
            }
            doc.put("masses", masses.toDoubleArray()).put("intensities", intensities.toDoubleArray());

            @SuppressWarnings("unchecked")
            Annotated.Annotations<SpectrumAnnotation> annotations = ((AbstractSpectrum) spectrum).annotations();
            Document annoDoc = new Document();
            try {
                annotations.forEach((clazz, annotation) -> {
                    try {
                        annoDoc.put(clazz.getName(), mapper.writeValueAsString(annotation));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (RuntimeException e) {
                throw new ChemicalDatabaseException(e);
            }

            doc.put("annotations", annoDoc);
        }

        return doc;
    }

    // TODO how to deserialize?

    private <T extends SpectrumAnnotation> void addAnnotation(Document doc, String className, SimpleSpectrum spectrum) throws ClassNotFoundException, JsonProcessingException {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) Class.forName(className);
        String json = doc.get(className, String.class);
        T value = mapper.readValue(json, clazz);
        spectrum.addAnnotation(clazz, value);
    }

    @SuppressWarnings("unchecked")
    public Spectrum<?> deserializeSpectrum(Document document) throws ChemicalDatabaseException {
        try {
            Class<? extends Spectrum<?>> spectrumClass = (Class<? extends Spectrum<?>>) Class.forName(document.get("class", String.class));

            if (ClassUtils.getAllSuperclasses(spectrumClass).contains(AbstractSpectrum.class)) {

            }

        } catch (ClassNotFoundException e) {
            throw new ChemicalDatabaseException(e);
        }

        SimpleSpectrum spectrum = new SimpleSpectrum((double[]) document.get("masses"), (double[]) document.get("intensities"));
        Document annoDoc = document.get("annotations", Document.class);
        for (String className : annoDoc.keySet()) {
            try {
                addAnnotation(annoDoc, className, spectrum);
            } catch (ClassNotFoundException | JsonProcessingException e) {
                throw  new ChemicalDatabaseException(e);
            }
        }
        return (Spectrum<?>) spectrum;
    }

}
