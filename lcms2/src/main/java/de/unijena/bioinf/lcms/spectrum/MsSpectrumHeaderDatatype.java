package de.unijena.bioinf.lcms.spectrum;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.lcms.datatypes.CustomDataType;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;

import java.nio.ByteBuffer;

public class MsSpectrumHeaderDatatype extends CustomDataType<Ms1SpectrumHeader>  {

    @Override
    public int getMemory(Ms1SpectrumHeader obj) {
        return 48; // TODO: calculate exact
    }

    @Override
    public void write(WriteBuffer buff, Ms1SpectrumHeader obj) {
        byte flags = 0;
        if (obj.polarity>0) flags |= 2;
        if (obj.polarity<0) flags |= 4;
        if (obj.centroided) flags |= 8;
        if (obj instanceof Ms2SpectrumHeader) flags |= 16;
        buff.putInt(obj.uid);
        buff.putInt(obj.scanId);
        buff.putInt(obj.sourceId.length());
        buff.putStringData(obj.sourceId, obj.sourceId.length());
        buff.put(flags);
        if (obj instanceof Ms2SpectrumHeader header2) {
            String ce = header2.energy==null ? "" : header2.energy.toString();
            buff.putInt(ce.length());
            buff.putStringData(ce, ce.length());
            buff.putDouble(header2.isolationWindow!=null ? header2.isolationWindow.getWindowOffset() : -1);
            buff.putDouble(header2.isolationWindow!=null ? header2.isolationWindow.getWindowWidth() : -1);
            buff.putInt(header2.parentId);
            buff.putDouble(header2.precursorMz);
            buff.putDouble(header2.retentionTime);
            buff.putInt(header2.msLevel);
        }
    }

    @Override
    public Ms1SpectrumHeader read(ByteBuffer buff) {
        int uid = buff.getInt();
        int scanid = buff.getInt();
        int ilen = buff.getInt();
        String sourceId = DataUtils.readString(buff, ilen);
        byte flags = buff.get();
        byte polarity = 0;
        if ((flags & 2) != 0) polarity=1;
        if ((flags & 4) != 0) polarity=-1;
        boolean centroided = (flags&8)!=0;
        if ((flags & 16) != 0) {
            int len = buff.getInt();
            String ces = DataUtils.readString(buff, len);
            CollisionEnergy ce = ces.isEmpty() ? null : CollisionEnergy.fromString(ces);
            double iso1 = buff.getDouble();
            double iso2 = buff.getDouble();
            IsolationWindow window;
            if (iso1 < 0) {
                window = null;
            } else window = new IsolationWindow(iso1, iso2);
            int pid =buff.getInt();
            double mz = buff.getDouble();
            double rt = buff.getDouble();
            int level = buff.getInt();
            return new Ms2SpectrumHeader(
                    uid, scanid, sourceId, polarity, level, centroided, ce, window, pid, mz,mz, rt
            );
        } else { // MS1 Header
            return new Ms1SpectrumHeader(uid,scanid, sourceId, polarity, centroided);
        }
    }

    @Override
    public Ms1SpectrumHeader[] createStorage(int i) {
        return new Ms1SpectrumHeader[i];
    }

}
