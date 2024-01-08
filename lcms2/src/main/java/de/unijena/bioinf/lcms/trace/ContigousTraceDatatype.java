package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.datatypes.CustomDataType;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import org.h2.mvstore.WriteBuffer;

import java.nio.ByteBuffer;

public class ContigousTraceDatatype extends CustomDataType {
    @Override
    public int getMemory(Object obj) {
        ContiguousTrace trace = (ContiguousTrace)obj;
        int mem = 16+24+trace.mz.length*8 + trace.intensity.length*4;
        return mem;
    }

    @Override
    public void write(WriteBuffer buff, Object obj) {
        ContiguousTrace trace = (ContiguousTrace)obj;
        writeFixedLenInt(buff, new int[]{trace.getUid(), trace.startId(), trace.endId(), trace.apex()});
        writeFixedLenDouble(buff, new double[]{trace.averagedMz(), trace.minMz(), trace.maxMz()});
        writeFixedLenDouble(buff, trace.mz);
        writeFixedLenFloat(buff, trace.intensity);
        if (trace.segments==null) buff.putInt(-1);
        else {
            buff.putInt(trace.segments.length);
            final int[] segs = new int[trace.segments.length*3];
            int k=0;
            for (TraceSegment t : trace.segments) {
                segs[k++] = t.apex;
                segs[k++] = t.leftEdge;
                segs[k++] = t.rightEdge;
            }
            writeFixedLenInt(buff, segs);
        }
    }

    @Override
    public Object read(ByteBuffer buff) {
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
                segs[k] = new TraceSegment(bf[j++], bf[j++], bf[j]++);
            }
        }
        return new ContiguousTrace(null, ints[0], ints[1], ints[2], ints[3], doubles[0], doubles[1], doubles[2], mz, intenss, segs);
    }
}
