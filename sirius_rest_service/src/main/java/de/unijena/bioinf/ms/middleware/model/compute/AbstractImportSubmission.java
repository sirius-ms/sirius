/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.compute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

import java.util.List;

/**
 * Parameter set for Data import.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractImportSubmission<S> {

    protected List<S> inputSources;

    protected boolean allowMs1OnlyData;
    protected boolean ignoreFormulas;

    @Delegate
    protected LcmsSubmissionParameters lcmsParameters;


    @JsonIgnore
    public abstract List<InputResource<?>> asInputResource();
    @JsonIgnore
    public abstract List<PathInputResource> asPathInputResource();

}
