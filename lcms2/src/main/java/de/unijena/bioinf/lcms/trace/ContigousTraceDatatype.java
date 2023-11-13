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
        return 16+24+trace.mz.length*8 + trace.intensity.length*4;
    }

    @Override
    public void write(WriteBuffer buff, Object obj) {
        ContiguousTrace trace = (ContiguousTrace)obj;
        writeFixedLenInt(buff, new int[]{trace.getUid(), trace.startId(), trace.endId(), trace.apex()});
        writeFixedLenDouble(buff, new double[]{trace.averagedMz(), trace.minMz(), trace.maxMz()});
        writeFixedLenDouble(buff, trace.mz);
        writeFixedLenFloat(buff, trace.intensity);
    }

    @Override
    public Object read(ByteBuffer buff) {
        int[] ints = readFixedLenInt(buff, 4);
        double[] doubles = readFixedLenDouble(buff, 3);
        int len = ints[2]-ints[1]+1;
        double[] mz = readFixedLenDouble(buff, len);
        float[] intenss = readFixedLenFloat(buff, len);
        return new ContiguousTrace(null, ints[0], ints[1], ints[2], ints[3], doubles[0], doubles[1], doubles[2], mz, intenss);
    }
}
