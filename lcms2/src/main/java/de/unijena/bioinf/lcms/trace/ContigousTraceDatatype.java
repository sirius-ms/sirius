package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.datatypes.CustomDataType;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import org.h2.mvstore.WriteBuffer;

import java.nio.ByteBuffer;

public class ContigousTraceDatatype extends CustomDataType {
    @Override
    public int getMemory(Object obj) {
        ContiguousTrace trace = (ContiguousTrace)obj;
        int mem = 16+24+trace.mz.length*8 + trace.intensity.length*4;
        if (trace instanceof MergedTrace) mem += trace.intensity.length*4;
        return mem;
    }

    @Override
    public void write(WriteBuffer buff, Object obj) {
        ContiguousTrace trace = (ContiguousTrace)obj;
        writeFixedLenInt(buff, new int[]{trace.getUid(), trace.startId(), trace.endId(), trace.apex()});
        writeFixedLenDouble(buff, new double[]{trace.averagedMz(), trace.minMz(), trace.maxMz()});
        writeFixedLenDouble(buff, trace.mz);
        writeFixedLenFloat(buff, trace.intensity);
        if (trace instanceof MergedTrace) {
            buff.put((byte)1);
            writeFixedLenInt(buff, ((MergedTrace)trace).numberOfMergedScanPoints);
        } else buff.put((byte)-1);
    }

    @Override
    public Object read(ByteBuffer buff) {
        int[] ints = readFixedLenInt(buff, 4);
        double[] doubles = readFixedLenDouble(buff, 3);
        int len = ints[2]-ints[1]+1;
        double[] mz = readFixedLenDouble(buff, len);
        float[] intenss = readFixedLenFloat(buff, len);
        byte kltype = buff.get();
        if (kltype==-1) {
            return new ContiguousTrace(null, ints[0], ints[1], ints[2], ints[3], doubles[0], doubles[1], doubles[2], mz, intenss);
        } else {
            int[] count = readFixedLenInt(buff, len);
            return new MergedTrace(null, ints[0], ints[1], ints[2], ints[3], doubles[0], doubles[1], doubles[2], mz, intenss, count);
        }
    }
}
