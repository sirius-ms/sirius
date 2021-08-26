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

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CompoundContainer extends ProjectSpaceContainer<CompoundContainerId> {

    private final Annotations<DataAnnotation> annotations;

    final Map<String, FormulaResultId> results;
    private final CompoundContainerId id;

    public CompoundContainer(CompoundContainerId id/*, Class<? extends FormulaScore> resultScore*/) {
        this.annotations = new Annotations<>();
        this.results = new ConcurrentHashMap<>();
        this.id = id;
    }

    /**
     * @return read only results map
     */
    public Map<String, FormulaResultId> getResultsRO() {
        return Collections.unmodifiableMap(results);
    }

    public boolean hasResults() {
        return !getResultsRO().isEmpty();
    }

    public boolean containsResult(FormulaResultId fid) {
        if (!fid.getParentId().getDirectoryName().equals(getId().getDirectoryName()))
            return false;
        return results.containsKey(fid.fileName());
    }

    boolean removeResult(FormulaResultId fid) {
        if (containsResult(fid))
            return results.remove(fid.fileName()) != null;
        return false;
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
}
