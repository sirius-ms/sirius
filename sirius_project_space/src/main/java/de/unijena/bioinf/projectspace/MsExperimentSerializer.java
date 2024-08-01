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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.FeatureGroup;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class MsExperimentSerializer implements ComponentSerializer<CompoundContainerId, CompoundContainer, Ms2Experiment> {

    @Override
    public Ms2Experiment read(ProjectReader reader, CompoundContainerId id, CompoundContainer container) throws IOException {
        if (!reader.exists(SiriusLocations.MS2_EXPERIMENT))
            return null;

        final Ms2Experiment exp = reader.textFile(SiriusLocations.MS2_EXPERIMENT, (b) -> new JenaMsParser().parse(b, Path.of(id.getDirectoryName(), SiriusLocations.MS2_EXPERIMENT).toUri()));

        if (exp != null) {
            id.getDetectedAdducts().ifPresent(pa -> exp.setAnnotation(DetectedAdducts.class, pa));
            if (id.getGroupId().isPresent() || id.getGroupRt().isPresent()) {
                exp.setAnnotation(FeatureGroup.class, FeatureGroup.builder()
                        .groupId(id.getGroupId().map(Long::parseLong).orElse(-1L))
                        .groupRt(id.getGroupRt().orElse(null))
                        .groupName(id.getGroupName().orElse(null))
                        .build());
            }
        }
        return exp;
    }

    @Override
    public void write(ProjectWriter writer, CompoundContainerId id, CompoundContainer container, Optional<Ms2Experiment> optEx) throws IOException {
        Ms2Experiment experiment = optEx.orElseThrow(() -> new RuntimeException("Could not find Experiment for FormulaResult with ID: " + id));
        writer.textFile(SiriusLocations.MS2_EXPERIMENT, (w) -> new JenaMsWriter().write(w, experiment));

        // actualize optional values in ID
        id.setIonMass(experiment.getIonMass());
        id.setIonType(experiment.getPrecursorIonType());
        id.setRt(experiment.getAnnotationOrNull(RetentionTime.class));
        id.setDetectedAdducts(experiment.getAnnotationOrNull(DetectedAdducts.class));
        experiment.getAnnotation(FeatureGroup.class).ifPresent(fg -> {
            id.setGroupRt(fg.getGroupRt());
            id.setGroupId(String.valueOf(fg.getGroupId()));
            id.setGroupName(fg.getGroupName());
        });
        //todo nightsky: serialize compound quality information

        writer.keyValues(SiriusLocations.COMPOUND_INFO, id.asKeyValuePairs());
    }

    @Override
    public void delete(ProjectWriter writer, CompoundContainerId id) throws IOException {
        writer.deleteIfExists(SiriusLocations.MS2_EXPERIMENT);
        writer.deleteIfExists(SiriusLocations.COMPOUND_INFO);
    }
}
