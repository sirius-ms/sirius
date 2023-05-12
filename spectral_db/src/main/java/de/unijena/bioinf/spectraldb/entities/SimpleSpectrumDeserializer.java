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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SimpleSpectrumDeserializer extends JsonDeserializer<SimpleSpectrum> {

    @Override
    public SimpleSpectrum deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        double[] masses = null;
        double[] intensities = null;
        Map<Class<? extends SpectrumAnnotation>, SpectrumAnnotation> annotations = new HashMap<>();

        JsonToken token = p.currentToken();
        while (!token.isStructStart()) {
            token = p.nextToken();
        }
        while (token != null && !token.isStructEnd()) {
            token = p.nextToken();
            if (token == JsonToken.FIELD_NAME) {
                switch (p.currentName()) {
                    case "masses":
                        token = p.nextToken();
                        masses = p.readValueAs(double[].class);
                        break;
                    case "intensities":
                        token = p.nextToken();
                        intensities = p.readValueAs(double[].class);
                        break;
                    case "annotations":
                        while (!token.isStructStart()) {
                            token = p.nextToken();
                        }
                        while (token != null && !token.isStructEnd()) {
                            token = p.nextToken();
                            if (token == JsonToken.FIELD_NAME) {
                                try {
                                    @SuppressWarnings("unchecked")
                                    Class<? extends SpectrumAnnotation> clazz = (Class<? extends SpectrumAnnotation>) Class.forName(p.currentName());
                                    token = p.nextToken();
                                    SpectrumAnnotation annotation = p.readValueAs(clazz);
                                    annotations.put(clazz, annotation);
                                } catch (ClassNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                        break;
                }
            }
        }
        SimpleSpectrum spectrum = new SimpleSpectrum(masses, intensities);
        for (Class<? extends SpectrumAnnotation> clazz : annotations.keySet()) {
            addAnnotation(clazz, annotations.get(clazz), spectrum);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends SpectrumAnnotation> void addAnnotation(Class<? extends SpectrumAnnotation> clazz, SpectrumAnnotation annotation, SimpleSpectrum spectrum) {
        Class<T> c = (Class<T>) clazz;
        T a = (T) annotation;
        spectrum.addAnnotation(c, a);
    }

}


