package de.unijena.bioinf.lcms.datatypes;


import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.BasicDataType;

import java.nio.ByteBuffer;

/**
 * Java serialization is rather slow and cannot deal with immutable types. Therefore, it makes
 * sense to define custom data types, in particular for the classes which are used a lot.
 */
public abstract class CustomDataType<T> extends BasicDataType<T> {
    @Override
    public int compare(T a, T b) {
        throw new UnsupportedOperationException();
    }

    protected void writeDouble(WriteBuffer out, double[] array) {
        out.putVarInt(array.length);
        writeFixedLenDouble(out, array);
    }
    protected void writeFixedLenDouble(WriteBuffer out, double[] array) {
        final int k = out.position()+array.length*8;
        ensureCapacity(out, k);
        out.getBuffer().asDoubleBuffer().put(array);
        out.position(k);
    }

    private static void ensureCapacity(WriteBuffer out, int size) {
        // ensure capacity
        out.limit(size);
        out.getBuffer().limit(out.getBuffer().capacity());
    }

    protected double[] readDouble(ByteBuffer buf) {
        int len = DataUtils.readVarInt(buf);
        return readFixedLenDouble(buf, len);
    }
    protected double[] readFixedLenDouble(ByteBuffer buf, int len) {
        double[] xs = new double[len];
        buf.asDoubleBuffer().get(xs);
        buf.position(buf.position()+len*8);
        return xs;
    }
    protected void writeFixedLenLong(WriteBuffer out, long[] array) {
        final int k = out.position() + array.length*8;
        ensureCapacity(out, k);
        out.getBuffer().asLongBuffer().put(array);
        out.position(k);
    }
    protected void writeLong(WriteBuffer out, long[] array) {
        out.putVarInt(array.length);
        writeFixedLenLong(out, array);
    }
    protected long[] readLong(ByteBuffer buf) {
        int len = DataUtils.readVarInt(buf);
        return readFixedLenLong(buf, len);
    }
    protected long[] readFixedLenLong(ByteBuffer buf, int len) {
        long[] xs = new long[len];
        buf.asLongBuffer().get(xs);
        buf.position(buf.position()+len*8);
        return xs;
    }
    protected void writeFixedLenFloat(WriteBuffer out, float[] array) {
        final int k = out.position()+array.length*4;
        ensureCapacity(out, k);
        out.getBuffer().asFloatBuffer().put(array);
        out.position(k);
    }
    protected void writeFloat(WriteBuffer out, float[] array) {
        out.putVarInt(array.length);
        writeFixedLenFloat(out,array);
    }
    protected float[] readFloat(ByteBuffer buf) {
        int len = DataUtils.readVarInt(buf);
        return readFixedLenFloat(buf, len);
    }
    protected float[] readFixedLenFloat(ByteBuffer buf, int len) {
        float[] xs = new float[len];
        buf.asFloatBuffer().get(xs);
        buf.position(buf.position()+len*4);
        return xs;
    }
    protected void writeFixedLenInt(WriteBuffer out, int[] array) {
        final int k = out.position()+array.length*4;
        ensureCapacity(out, k);
        out.getBuffer().asIntBuffer().put(array);
        out.position(k);
    }
    protected void writeInt(WriteBuffer out, int[] array) {
        out.putVarInt(array.length);
        writeFixedLenInt(out, array);
    }
    protected int[] readInt(ByteBuffer buf) {
        int len = DataUtils.readVarInt(buf);
        return readFixedLenInt(buf, len);
    }
    protected int[] readFixedLenInt(ByteBuffer buf, int len) {
        int[] xs = new int[len];
        buf.asIntBuffer().get(xs);
        buf.position(buf.position()+len*4);
        return xs;
    }
    protected void writeFixedLenShort(WriteBuffer out, short[] array) {
        final int k = out.position()+array.length*2;
        ensureCapacity(out, k);
        out.getBuffer().asShortBuffer().put(array);
        out.position(k);
    }
    protected void writeShort(WriteBuffer out, short[] array) {
        out.putVarInt(array.length);
        writeFixedLenShort(out, array);
    }
    protected short[] readFixedLenShort(ByteBuffer buf, int len) {
        short[] xs = new short[len];
        buf.asShortBuffer().get(xs);
        buf.position(buf.position()+len*2);
        return xs;
    }
    protected short[] readShort(ByteBuffer buf) {
        int len = DataUtils.readVarInt(buf);
        return readFixedLenShort(buf, len);
    }

}
