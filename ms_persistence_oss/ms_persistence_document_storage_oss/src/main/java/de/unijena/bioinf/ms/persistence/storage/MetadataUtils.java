/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.storage;

import de.unijena.bioinf.ChemistryBase.utils.FastUtilJson;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleSet;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongSet;

public class MetadataUtils {

    public static Metadata withFasUtilCollectionSupport() {
        return addFasUtilCollectionSupport(Metadata.build());
    }

    public static Metadata addFasUtilCollectionSupport(Metadata source) {
         return source.addSerialization(
                        LongList.class,
                        new FastUtilJson.LongCollectionSerializer<>(),
                        new FastUtilJson.LongListDeserializer())
                .addSerialization(
                        LongSet.class,
                        new FastUtilJson.LongCollectionSerializer<>(),
                        new FastUtilJson.LongSetDeserializer())
                .addSerialization(
                        DoubleList.class,
                        new FastUtilJson.DoubleCollectionSerializer<>(),
                        new FastUtilJson.DoubleListDeserializer())
                 .addSerialization(
                         FloatList.class,
                         new FastUtilJson.FloatCollectionSerializer<>(),
                         new FastUtilJson.FloatListDeserializer())
                .addSerialization(
                        DoubleSet.class,
                        new FastUtilJson.DoubleCollectionSerializer<>(),
                        new FastUtilJson.DoubleSetDeserializer())
                .addSerialization(
                        IntList.class,
                        new FastUtilJson.IntCollectionSerializer<>(),
                        new FastUtilJson.IntListDeserializer())
                .addSerialization(
                        IntSet.class,
                        new FastUtilJson.IntCollectionSerializer<>(),
                        new FastUtilJson.IntSetDeserializer()
                );
    }
}
