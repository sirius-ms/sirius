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

package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectSpaceContainer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CompoundContainer extends ProjectSpaceContainer<CompoundContainerId> {

    private final Annotations<DataAnnotation> annotations;

    protected final Map<String, FormulaResultId> results;
    private final CompoundContainerId id;

    public CompoundContainer(CompoundContainerId id/*, Class<? extends FormulaScore> resultScore*/) {
        this.annotations = new Annotations<>();
        this.results = new ConcurrentHashMap<>();
        this.id = id;
    }

    public Map<String, FormulaResultId> getResults() {
        return results;
    }

    public boolean hasResult() {
        return !getResults().isEmpty();
    }

    public boolean containsResult(FormulaResultId id) {
        return id != null && id == (results.get(id.fileName()));
    }

    public Optional<FormulaResultId> findResult(String id) {
        return Optional.ofNullable(results.get(id));
    }

    @Override
    public CompoundContainerId getId() {
        return id;
    }

    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }

    public boolean contains(FormulaResultId fid) {
        if (!fid.getParentId().getDirectoryName().equals(getId().getDirectoryName()))
            return false;
        return results.containsKey(fid.fileName());
    }
}
