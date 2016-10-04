package de.unijena.bioinf.io;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 04.10.16.
 */

import de.unijena.bioinf.sirius.gui.io.ByteRingBufferOutputStream;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ByteRingBufferOutputStreamTest {

    public byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] c = new byte[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }


    @Test
    public void testSingleWrite() {
        byte[] data = new byte[30];
        byte[] res = new byte[10];

        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }

        for (int i = 0; i < res.length; i++) {
            res[i] = (byte) (i + 20);
        }

        ByteRingBufferOutputStream buf = new ByteRingBufferOutputStream(2, 10);
        for (int i = 0; i < data.length; i++) {
            buf.write(data[i]);
        }
        byte[] bufRes = buf.toByteArray();
        Assert.assertArrayEquals(res, bufRes);
    }

    @Test
    public void testMultiWrite() {
        byte[] first = {1, 2, 3, 4, 5, 6, 7};
        byte[] second = {8, 9, 10, 11, 12, 13, 14};
        byte[] third = {15, 16, 17, 18, 19, 20, 21};
        byte[] endRes = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};

        ByteRingBufferOutputStream buf = new ByteRingBufferOutputStream(10, 20);
        buf.write(first);
        byte[] result = buf.toByteArray();
        Assert.assertArrayEquals(first, result);

        buf.write(second);
        result = buf.toByteArray();
        Assert.assertArrayEquals(concat(first, second), result);

        buf.write(third);
        result = buf.toByteArray();
        Assert.assertArrayEquals(endRes, result);
    }
}
