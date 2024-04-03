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

package de.unijena.bioinf.ms.persistence.model.sirius;

import com.fasterxml.jackson.annotation.*;
import de.unijena.bioinf.ChemistryBase.ms.properties.ConfigAnnotation;
import de.unijena.bioinf.ms.properties.ConfigType;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuperBuilder
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.ANY, setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Parameters extends AlignedFeatureAnnotation {
    @Id
    @Getter
    @Setter
    private long parametersId;

    @Getter
    @Setter
    private ConfigType type;

    private ParameterConfig parameterConfig;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    private Parameters(@JsonProperty(value = "parametersId") long id,
                       @JsonProperty(value = "alignedFeatureId") long alignedFeatureId,
                       @JsonProperty(value = "type") ConfigType type,
                       @JsonProperty("parameters") @Nullable Map<String, String> parameters) {
        super();
        this.parametersId = id;
        this.alignedFeatureId = alignedFeatureId;
        this.type = type == null ? ConfigType.UNKNOWN : type;
        this.parameterConfig = PropertyManager.DEFAULTS.newIndependentInstance(parameters, this.type.name(), false, ConfigType.CLI.name());
    }

    @JsonInclude // getter just for serialization
    private Map<String, String> getParameters() {
        if (parameterConfig == null)
            return null;
        return parameterConfig.toMap();
    }

    @JsonIgnore
    public ParameterConfig getConfig() {
        return parameterConfig;
    }

    public static <T extends ConfigAnnotation> Parameters of(T configAnnotation, ConfigType type, long alignedFeatureId) {
        Parameters parameters = of(configAnnotation, type);
        parameters.setAlignedFeatureId(alignedFeatureId);
        return parameters;
    }

    public static <T extends ConfigAnnotation> Parameters of(T configAnnotation, ConfigType type) {
        return of(configAnnotation.config(), type);
    }

    public static Parameters of(ParameterConfig config, ConfigType type, long alignedFeatureId) {
        Parameters parameters = of(config, type);
        parameters.setAlignedFeatureId(alignedFeatureId);
        return parameters;
    }

    public static Parameters of(ParameterConfig config, ConfigType type) {
        return Parameters.builder()
                .parameterConfig(config)
                .type(type)
                .build();
    }
}
