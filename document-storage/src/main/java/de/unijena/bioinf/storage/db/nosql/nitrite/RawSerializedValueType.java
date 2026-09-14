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

package de.unijena.bioinf.storage.db.nosql.nitrite;

import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;
import org.h2.mvstore.type.ObjectDataType;

import java.nio.ByteBuffer;

/**
 * Reads stored values as the bytes they already are, instead of as the objects they encode.
 * <p>
 * MVStore writes a serialised value as {@code [tag][length][payload]}, so an entry says how long it
 * is and can be lifted out and put back without ever being turned into an object:
 * <pre>
 *     buff.put((byte) TYPE_SERIALIZED_OBJECT).putVarInt(data.length).put(data);
 * </pre>
 * Everything else - null, the boxed primitives, String, UUID, Date, arrays - is left to
 * {@link ObjectDataType}, which is what wrote it. Those carry no application classes and are small.
 * <p>
 * This is for {@link PageLayout#rewrite}, which reads every record of a store and writes it into a
 * new one. Doing that through {@link ObjectDataType} means rebuilding every stored object graph and
 * serialising it again, which costs two things.
 * <p>
 * It requires every class in those graphs to be on the classpath. A Nitrite document keeps its field
 * values as the types they were, so a project written years ago can name a class this build no longer
 * has, and the rewrite then fails with {@code ClassNotFoundException} where a plain compaction would
 * have succeeded. That is the reason this type exists; passing bytes through cannot fail that way.
 * <p>
 * And it costs time. A 1.0 GiB project rewrites in 14.5 s instead of 39.5 s, and four million records
 * from the largest maps of a 48 GiB one copy in 80 s instead of 275 s - between two and a half and
 * three and a half times faster, with the file coming out around 2% smaller either way. End to end on
 * a project of tens of GiB the saving has not been measured under controlled conditions.
 * <p>
 * Only values, never keys: keys decide where a record goes in the B-tree, and serialised bytes do
 * not sort like the objects they encode. Keys stay with {@link ObjectDataType}, which is cheap and
 * safe for them - Nitrite keys its maps by its own {@code NitriteId} and index values.
 * <p>
 * Nothing about this reaches the file. A map's persisted metadata records its name, creation version
 * and map type, never its data types - whoever opens a map supplies them - so a store rewritten
 * through this reads back through Nitrite's ordinary types, byte for byte what it would have been.
 */
final class RawSerializedValueType extends BasicDataType<Object> {

    /** {@code ObjectDataType.TYPE_SERIALIZED_OBJECT}, which is package-private there. */
    private static final int TYPE_SERIALIZED_OBJECT = 19;

    /**
     * What MVStore charges a page for a record it holds, per byte of payload.
     * <p>
     * Two, because that is what {@code ObjectDataType} charges - "we say they are larger, because
     * these objects use quite a lot of disk space" - and the page budget was chosen against that
     * accounting. The difference here is that it is charged against the payload actually in hand
     * rather than against the running average {@code ObjectDataType} keeps, so a rewrite splits
     * pages on what the records measure instead of on what they are guessed to measure.
     */
    private static final int MEMORY_PER_BYTE = 2;

    private final ObjectDataType delegate = new ObjectDataType();

    /** A value carried through as the bytes it is stored as, payload only - the framing is re-emitted. */
    record Raw(byte[] payload) {
    }

    @Override
    public Object read(ByteBuffer buff) {
        // absolute get: look at the tag without consuming it, so the delegate still sees a whole entry
        if ((buff.get(buff.position()) & 0xff) == TYPE_SERIALIZED_OBJECT) {
            buff.get();
            byte[] payload = new byte[DataUtils.readVarInt(buff)];
            buff.get(payload);
            return new Raw(payload);
        }
        return delegate.read(buff);
    }

    @Override
    public void write(WriteBuffer buff, Object obj) {
        if (obj instanceof Raw raw) {
            buff.put((byte) TYPE_SERIALIZED_OBJECT).putVarInt(raw.payload().length).put(raw.payload());
        } else {
            delegate.write(buff, obj);
        }
    }

    @Override
    public int getMemory(Object obj) {
        return obj instanceof Raw raw ? raw.payload().length * MEMORY_PER_BYTE : delegate.getMemory(obj);
    }

    /**
     * No, so MVStore asks {@link #getMemory} for every record rather than averaging a few - the size
     * is known here and costs nothing to give, which makes averaging it pointless rather than wrong.
     * <p>
     * It does not change what gets written: measured against the same rewrite with averaging left on,
     * the output matched to within 0.1% on a 1.0 GiB project, and the records per leaf of the largest
     * maps of a 48 GiB one were identical to two decimals. Page fill in these stores is set by the
     * records themselves and by {@code keysPerPage}, not by how their size is arrived at.
     */
    @Override
    public boolean isMemoryEstimationAllowed() {
        return false;
    }

    @Override
    public Object[] createStorage(int size) {
        return new Object[size];
    }

    @Override
    public int compare(Object a, Object b) {
        throw new UnsupportedOperationException("Stored values are never ordered; only keys are, and keys "
                + "do not use this type.");
    }
}
