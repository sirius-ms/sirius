package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.lcms.datatypes.CustomDataType;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import org.h2.mvstore.WriteBuffer;

import java.nio.ByteBuffer;

public class ContigousTraceDatatype extends CustomDataType<ContiguousTrace> {
    @Override
    public int getMemory(ContiguousTrace obj) {
        return 16+24+obj.mz.length*8 + obj.intensity.length*4;
    }

    @Override
    public void write(WriteBuffer buff, ContiguousTrace obj) {
        writeFixedLenInt(buff, new int[]{obj.getUid(), obj.startId(), obj.endId(), obj.apex()});
        writeFixedLenDouble(buff, new double[]{obj.averagedMz(), obj.minMz(), obj.maxMz()});
        writeFixedLenDouble(buff, obj.mz);
        writeFixedLenFloat(buff, obj.intensity);
        if (obj.segments==null) buff.putInt(-1);
        else {
            buff.putInt(obj.segments.length);
            final int[] segs = new int[obj.segments.length*3];
            int k=0;
            for (TraceSegment t : obj.segments) {
                segs[k++] = t.apex;
                segs[k++] = t.leftEdge;
                segs[k++] = t.rightEdge;
            }
            writeFixedLenInt(buff, segs);
        }
    }

    @Override
    public ContiguousTrace read(ByteBuffer buff) {
        int[] ints = readFixedLenInt(buff, 4);
        double[] doubles = readFixedLenDouble(buff, 3);
        int len = ints[2]-ints[1]+1;
        double[] mz = readFixedLenDouble(buff, len);
        float[] intenss = readFixedLenFloat(buff, len);

        int size = buff.getInt();
        TraceSegment[] segs;
        if (size < 0) segs = null;
        else {
            segs = new TraceSegment[size];
            int[] bf = readFixedLenInt(buff, segs.length * 3);
            int j = 0;
            for (int k = 0; k < segs.length; ++k) {
                final int apex = bf[j++];
                final int leftEdge = bf[j++];
                final int rightEdge = bf[j++];
                segs[k] = new TraceSegment(apex, leftEdge, rightEdge);
            }
        }
        return new ContiguousTrace(null, ints[0], ints[1], ints[2], ints[3], doubles[0], doubles[1], doubles[2], mz, intenss, segs);
    }

    @Override
    public ContiguousTrace[] createStorage(int i) {
        return new ContiguousTrace[i];
    }

}
