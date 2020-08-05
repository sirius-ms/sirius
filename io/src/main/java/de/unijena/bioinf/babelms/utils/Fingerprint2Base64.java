

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

package de.unijena.bioinf.babelms.utils;

import gnu.trove.list.array.TShortArrayList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class Fingerprint2Base64 {

    public static String encode(short[] fingerprint) {
        byte[] bytes = new byte[fingerprint.length*2];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        final ShortBuffer sb = buf.asShortBuffer();
        for (short v : fingerprint) {
            sb.put(v);
        }
        return Base64.encodeBytes(bytes);
    }

    public static String encode(boolean[] fingerprint) {
        TShortArrayList xs = new TShortArrayList(120);
        for (int k=0; k < fingerprint.length; ++k)
            if (fingerprint[k]) xs.add((short)k);
        return encode(xs.toArray());
    }

    public static boolean[] decodeAsBoolean(String fingerprint, int length) throws IOException {
        final boolean[] fp = new boolean[length];
        final short[] indizes = decode(fingerprint);
        for (short v : indizes) fp[v] = true;
        return fp;
    }

    public static short[] decode(String fingerprint) throws IOException {
        final byte[] bytes = Base64.decode(fingerprint);
        final ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        final ShortBuffer sb = buf.asShortBuffer();
        final short[] ary = new short[bytes.length>>1];
        for (int k=0; k < ary.length; ++k) ary[k] = sb.get();
        return ary;
    }

}
