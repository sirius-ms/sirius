/*
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

package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class SiriusProjectDatabaseImpl<Storage extends Database<?>> implements SiriusProjectDocumentDatabase<Storage>, Closeable, AutoCloseable {

    protected final Storage storage;

    private static Set<Class<?>> relatedToAF = null;

    public SiriusProjectDatabaseImpl(Storage storage) {
        this.storage = storage;
    }

    @Override
    public Storage getStorage() {
        return storage;
    }

    @Override
    public void close() throws IOException {
        storage.close();
    }

    private Set<Class<?>> getRelatedToAF() {
        if (relatedToAF == null) {
            relatedToAF = getStorage().getAllRegisteredClasses().stream().filter(
                    clazz -> FieldUtils.getAllFieldsList(clazz).stream().anyMatch(field -> field.getName().equals("alignedFeatureId"))
            ).collect(Collectors.toSet());
        }
        return relatedToAF;
    }

    @Override
    public long cascadeDeleteCompound(long compoundId) throws IOException {
        long total = this.getStorage().write(() -> {
            long count = 0;
            for (AlignedFeatures f : getStorage().find(Filter.where("compoundId").eq(compoundId), AlignedFeatures.class)) {
                count += cascadeDeleteAlignedFeatures(f.getAlignedFeatureId());
            }
            return count;
        });
        total += getStorage().removeAll(Filter.where("compoundId").eq(compoundId), Compound.class);
        return total;
    }

    @Override
    public long cascadeDeleteAlignedFeatures(long alignedFeatureId) throws IOException {
        return this.getStorage().write(() -> {
            long count = 0;
            for (Class<?> clazz : getRelatedToAF()) {
                count += getStorage().removeAll(Filter.where("alignedFeatureId").eq(alignedFeatureId), clazz);
            }
            return count;
        });
    }


    @Override
    public long cascadeDeleteAlignedFeatures(List<Long> alignedFeatureIds) throws IOException {
        if (alignedFeatureIds.isEmpty())
            return 0;
        if (alignedFeatureIds.size() == 1)
            return cascadeDeleteAlignedFeatures(alignedFeatureIds.get(0));

        return this.getStorage().write(() -> {
            long count = 0;
            for (Class<?> clazz : getRelatedToAF()) {
                try {
                    count += getStorage().removeAll(Filter.where("alignedFeatureId").in(alignedFeatureIds.toArray(Long[]::new)), clazz);
                } finally {
                    getStorage().flush();
                    System.gc();
                }
            }
            return count;
        });
    }
}
