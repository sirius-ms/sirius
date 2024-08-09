/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.ms.frontend.io;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ByteRingBufferOutputStream extends OutputStream {

    byte[] buf;
    int maxSize;
    int pos = 0;
    boolean filled = false;


    /**
     * Creates a new byte array output stream. The buffer size is
     * initially 32 bytes, though its size increases if necessary to 1024.
     */
    public ByteRingBufferOutputStream() {
        this(32, 1024);
    }


    /**
     * Creates a new byte array output stream, with a buffer size of
     * 32 bytes and a maximum of the the specified size, in bytes.
     *
     * @param maxSize the maximum sizer, after which the buffer starts overwriting from index 0.
     * @throws IllegalArgumentException if size is negative.
     */
    public ByteRingBufferOutputStream(int maxSize) {
        this(32, maxSize);
    }

    /**
     * Creates a new byte array output stream, with a buffer size of
     * the specified size, in bytes.
     *
     * @param size    the initial size.
     * @param maxSize the maximum sizer, after which the buffer starts overwriting from index 0.
     * @throws IllegalArgumentException if size is negative.
     */
    public ByteRingBufferOutputStream(int size, int maxSize) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: "
                    + size);
        }

        buf = new byte[size];
        this.maxSize = Math.max(maxSize, size);

    }


    /**
     * Increases the capacity if necessary to ensure that it can hold
     * at least the number of elements specified by the minimum
     * capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     * @throws OutOfMemoryError if {@code minCapacity < 0}.  This is
     *                          interpreted as a request for the unsatisfiably large capacity
     *                          {@code (long) Integer.MAX_VALUE + (minCapacity - Integer.MAX_VALUE)}.
     */
    private void ensureCapacity(int minCapacity) {
        if (minCapacity > buf.length) {
            if (minCapacity > maxSize) {
                if (buf.length < maxSize)
                    grow(maxSize);
                filled = true;
                pos = 0;
            } else {
                grow(minCapacity);
            }
        }
    }

    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buf.length;
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity > maxSize)
            newCapacity = maxSize;
        buf = Arrays.copyOf(buf, newCapacity);
    }


    @Override
    public synchronized void write(int b) {
        ensureCapacity(pos + 1);
        buf[pos++] = (byte) b;
    }


    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this byte array output stream.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     */
    @Override
    public synchronized void write(byte b[], int off, int len) {
        if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) - b.length > 0)) {
            throw new IndexOutOfBoundsException();
        }
        final int oldPos = pos;
        ensureCapacity(pos + len);

        int free = buf.length - oldPos;
        int toInsert = Math.min(free,len);
        System.arraycopy(b, off, buf, oldPos, toInsert);
        pos = oldPos + toInsert;
        if (free < len) {
            pos = len - free;
            System.arraycopy(b, off + free, buf, 0, pos);
        }
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this byte array output stream.
     *
     * @param b   the data.
     */
    @Override
    public synchronized void write(byte b[]) {
        write(b,0,b.length);
    }



    public synchronized byte toByteArray()[] {
        if (!filled)
            return Arrays.copyOf(buf, pos);
        byte[] ret = new byte[buf.length];
        System.arraycopy(buf, pos, ret, 0, buf.length - pos);
        System.arraycopy(buf, 0, ret, buf.length - pos, pos);
        return ret;
    }

    /**
     * Converts the buffer's contents into a string by decoding the bytes using
     * the named {@link java.nio.charset.Charset charset}. The length of the new
     * <code>String</code> is a function of the charset, and hence may not be equal
     * to the length of the byte array.
     * <p>
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string. The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param charsetName the name of a supported
     *                    {@link java.nio.charset.Charset charset}
     * @return String decoded from the buffer's contents.
     * @throws UnsupportedEncodingException If the named charset is not supported
     * @since JDK1.1
     */
    public synchronized String toString(String charsetName)
            throws UnsupportedEncodingException {
        return new String(toByteArray(), charsetName);
    }
}