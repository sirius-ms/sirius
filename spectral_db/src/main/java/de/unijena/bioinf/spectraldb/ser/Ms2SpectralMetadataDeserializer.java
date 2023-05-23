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

package de.unijena.bioinf.spectraldb.ser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralMetadata;
import org.dizitart.no2.exceptions.ObjectMappingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class Ms2SpectralMetadataDeserializer extends JsonDeserializer<Ms2SpectralMetadata> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Ms2SpectralMetadata deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Map<Class<SpectrumAnnotation>, SpectrumAnnotation> annotations = new HashMap<>();
        Map<Class<Ms2ExperimentAnnotation>, Ms2ExperimentAnnotation> experimentAnnotations = new HashMap<>();
        Ms2SpectralMetadata metadata = new Ms2SpectralMetadata();
        String linkName = null;
        String linkID = null;

        JsonToken token = p.currentToken();
        while (token != null && !token.isStructStart()) {
            token = p.nextToken();
        }
        for (token = p.nextToken(); token != null && !token.isStructEnd(); token = p.nextToken()) {
            if (token == JsonToken.FIELD_NAME) {
                switch (p.currentName()) {
                    case "peaksId":
                        metadata.setPeaksId(p.nextLongValue(-1L));
                        break;

                    case "precursorIonType":
                        metadata.setPrecursorIonType(PeriodicTable.getInstance().ionByNameOrThrow(p.nextTextValue()));
                        break;
                    case "ionMass":
                        token = p.nextToken();
                        metadata.setIonMass(p.getDoubleValue());
                        break;
                    case "formula":
                        metadata.setFormula(MolecularFormula.parseOrThrow(p.nextTextValue()));
                        break;
                    case "name":
                        metadata.setName(p.nextTextValue());
                        break;
                    case "smiles":
                        metadata.setSmiles(p.nextTextValue());
                        break;
                    case "linkName":
                        linkName = p.nextTextValue();
                        break;
                    case "linkID":
                        linkID = p.nextTextValue();
                        break;
                    case "dbLinks":
                        while (token != JsonToken.START_ARRAY) {
                            token = p.nextToken();
                        }
                        for (token = p.nextToken(); token != null && token != JsonToken.END_ARRAY; token = p.nextToken()) {
                            String name = p.getText();
                            String id = p.nextTextValue();
                            metadata.addDbLink(new DBLink(name, id));
                        }
                        break;

                    case "experimentAnnotations":
                        while (!token.isStructStart()) {
                            token = p.nextToken();
                        }
                        for (token = p.nextToken(); token != null && !token.isStructEnd(); token = p.nextToken()) {
                            if (token == JsonToken.FIELD_NAME) {
                                try {
                                    @SuppressWarnings("unchecked")
                                    Class<Ms2ExperimentAnnotation> clazz = (Class<Ms2ExperimentAnnotation>) Class.forName(p.currentName());
                                    if (InChI.class.equals(clazz)) {
                                        while (token != JsonToken.START_ARRAY) {
                                            token = p.nextToken();
                                        }
                                        String key = p.nextTextValue();
                                        String in3d = p.nextTextValue();
                                        while (token != JsonToken.END_ARRAY) {
                                            token = p.nextToken();
                                        }
                                        experimentAnnotations.put(clazz, new InChI(key, in3d));
                                    } else if (MsInstrumentation.Instrument.class.equals(clazz)) {
                                        experimentAnnotations.put(clazz, MsInstrumentation.Instrument.valueOf(p.nextTextValue()));
                                    } else if (SpectrumFileSource.class.equals(clazz)) {
                                        experimentAnnotations.put(clazz, new SpectrumFileSource(URI.create(p.nextTextValue())));
                                    } else {
                                        Ms2ExperimentAnnotation annotation = clazz.getConstructor(String.class).newInstance(p.nextTextValue());
                                        experimentAnnotations.put(clazz, annotation);
                                    }
                                } catch (ObjectMappingException | ClassNotFoundException | NoSuchMethodException |
                                         InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                    throw new IOException(e);
                                }
                            }
                        }
                        break;

                    case "annotations":
                        while (!token.isStructStart()) {
                            token = p.nextToken();
                        }
                        for (token = p.nextToken(); token != null && !token.isStructEnd(); token = p.nextToken()) {
                            if (token == JsonToken.FIELD_NAME) {
                                try {
                                    @SuppressWarnings("unchecked")
                                    Class<SpectrumAnnotation> clazz = (Class<SpectrumAnnotation>) Class.forName(p.currentName());
                                    if (clazz.equals(AdditionalFields.class)) {
                                        AdditionalFields annotation = new AdditionalFields();
                                        TypeReference<Map<String, String>> typeRef = new TypeReference<>() {};
                                        Map<String, String> map = mapper.readValue(p.nextTextValue(), typeRef);
                                        annotation.putAll(map);
                                        annotations.put(clazz, annotation);
                                    } else {
                                        try {
                                            token = p.nextToken();
                                            SpectrumAnnotation annotation = p.readValueAs(clazz);
                                            annotations.put(clazz, annotation);
                                        } catch (ObjectMappingException e) {
                                            throw new IOException(clazz + " deserialization is not supported.");
                                        }
                                    }
                                } catch (ClassNotFoundException e) {
                                    throw new IOException(e);
                                }
                            }
                        }
                        break;
                    case "header":
                        while (!token.isStructStart()) {
                            token = p.nextToken();
                        }
                        for (token = p.nextToken(); token != null && !token.isStructEnd(); token = p.nextToken()) {
                            if (token == JsonToken.FIELD_NAME) {
                                switch (p.currentName()) {
                                    case "precursorMz":
                                        token = p.nextToken();
                                        metadata.setPrecursorMz(p.getDoubleValue());
                                        break;
                                    case "collisionEnergy":
                                        metadata.setCollisionEnergy(CollisionEnergy.fromString(p.nextTextValue()));
                                        break;
                                    case "totalIonCount":
                                        token = p.nextToken();
                                        metadata.setTotalIonCount(p.getDoubleValue());
                                        break;
                                    case "ionization":
                                        metadata.setIonization(PeriodicTable.getInstance().ionByNameOrThrow(p.nextTextValue()).getIonization());
                                        break;
                                    case "msLevel":
                                        metadata.setMsLevel(p.nextIntValue(2));
                                        break;
                                    case "scanNumber":
                                        metadata.setScanNumber(p.nextIntValue(-1));
                                        break;
                                }
                            }
                        }
                        break;
                }
            }
        }

        if (linkName != null && linkID != null) {
            metadata.setSpectralDbLink(new DBLink(linkName, linkID));
        }

        metadata.addExperimentAnnotationsFrom(experimentAnnotations);
        metadata.addAnnotationsFrom(annotations);

        return metadata;
    }

}
