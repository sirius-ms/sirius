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

package de.unijena.bioinf.spectraldb.io;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.ChemistryBase.ms.Splash;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.ZippedSpectraParser;
import de.unijena.bioinf.babelms.annotations.CompoundMetaData;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class SpectralDbMsExperimentParser extends MsExperimentParser {

    private static final List<String> ANNOTATION_WHITELIST = List.of(
            DataAnnotation.getIdentifier(Splash.class),
            DataAnnotation.getIdentifier(Smiles.class),
            DataAnnotation.getIdentifier(InChI.class),
            DataAnnotation.getIdentifier(MsInstrumentation.Instrument.class),
            DataAnnotation.getIdentifier(SpectrumFileSource.class),
            DataAnnotation.getIdentifier(RetentionTime.class),
            DataAnnotation.getIdentifier(CompoundMetaData.class)
    );

    public static final Consumer<Ms2Experiment> ANNOTATION_FILTER = exp -> {
        final Iterator<Class<Ms2ExperimentAnnotation>> it = exp.annotations().iterator();
        while (it.hasNext()) {
            exp.getAnnotation(it.next()).ifPresent(old -> {
                if (!ANNOTATION_WHITELIST.contains(old.getIdentifier())) {
                    it.remove();
                }
            });
        }
    };

    @Override
    public GenericParser<Ms2Experiment> getParserByExt(String extName) {
        if (!extName.startsWith("."))
            extName = "." + extName;

        final Class<? extends Parser<Ms2Experiment>> pc = KNOWN_ENDINGS.get(extName);
        if (pc == null) return null;
        try {
            if (pc.equals(ZippedSpectraParser.class))
                return (GenericParser<Ms2Experiment>) pc.getConstructor().newInstance();

            return new GenericParser<>(pc.getConstructor().newInstance(), ANNOTATION_FILTER);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
