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
import de.unijena.bioinf.ms.properties.ConfigType;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.properties.SiriusConfigUtils;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    private PropertiesConfiguration configuration;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    private Parameters(@JsonProperty(value = "parametersId") long id,
                       @JsonProperty(value = "alignedFeatureId") long alignedFeatureId,
                       @JsonProperty(value = "type") ConfigType type,
                       @JsonProperty("parameters") @Nullable Map<String, String> parameters) {
        super();
        this.parametersId = id;
        this.alignedFeatureId = alignedFeatureId;
        this.type = type == null ? ConfigType.UNKNOWN : type;
        this.configuration = SiriusConfigUtils.makeConfigFromMap(parameters);
    }

    @JsonInclude // getter just for serialization
    private Map<String, String> getParameters() {
        final Map<String, String> toWrite = new HashMap<>(configuration.size());
        configuration.getKeys().forEachRemaining(key -> toWrite.put(key, configuration.getString(key)));
        return toWrite;
    }

    @JsonIgnore
    public ParameterConfig newParameterConfig() {
        return PropertyManager.DEFAULTS.newIndependentInstance(configuration, this.type.name(), false, new HashSet<>(Set.of(ConfigType.CLI.name())));
    }


    @JsonIgnore
    ImmutableConfiguration getConfiguration(){
        return configuration;
    }

    public static Parameters of(ParameterConfig config, ConfigType type, long alignedFeatureId, boolean modificationOnly) {
        Parameters parameters = of(config, type, modificationOnly);
        parameters.setAlignedFeatureId(alignedFeatureId);
        return parameters;
    }

    public static Parameters of(ParameterConfig config, ConfigType type, boolean modificationOnly) {
        PropertiesConfiguration c = modificationOnly ? (PropertiesConfiguration) config.getModifiedConfigs() : SiriusConfigUtils.makeConfigFromMap(config.toMap());
        return Parameters.builder()
                .configuration(c)
                .type(type)
                .build();
    }
}
