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

package de.unijena.bioinf.io.lcms;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.SpectrumStorage;
import de.unijena.bioinf.model.lcms.*;
import gnu.trove.list.array.TByteArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.function.Function;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

class MzXMLSaxParser extends DefaultHandler {

    private final List<Handler> stack;
    private Handler handler;
    private StringBuilder buffer;
    private LCMSRun lcms;
    private SpectrumStorage storage;
    private final DatatypeFactory datatypeFactory;

    private final TByteArrayList totalBuffer = new TByteArrayList(1024);
    private byte[] bytebuffer;

    public MzXMLSaxParser(LCMSRun run, SpectrumStorage storage) throws IOException {
        this.stack = new ArrayList<>();
        this.lcms = run;
        this.handler = new RootHandler();
        this.storage = storage;
        this.buffer = new StringBuilder();
        this.bytebuffer = new byte[1024];
        try {
            this.datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        handler.enterElement(qName,attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (handler.listening) handler.leaveElement(qName, buffer.toString());
        else handler.leaveElement(qName,"");
        buffer.delete(0,buffer.length());
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (handler.listening) {
            buffer.append(ch, start,length);
        }
    }



    public abstract class Handler {

        protected boolean listening;

        public Handler() {
            this.listening = false;
        }

        public void listen() {
            this.listening = true;
        }

        public void listen(boolean value){
            this.listening = value;
        }

        protected boolean isTrue(Attributes attrs, String name) {
            final String val = attrs.getValue(name);
            return val != null && !val.equals("false") && !val.equals("0");
        }

        protected <T> T get(Attributes a, String name, T defaultValue, Function<String, T> f) {
            String val = a.getValue(name);
            if (val==null) return defaultValue;
            else return f.apply(val);
        }

        public abstract void enterElement(String elementName, Attributes attrs);

        public abstract void leaveElement(String elementName, String content);

        public void push(Handler h) {
            stack.add(this);
            handler = h;
        }

        public void pop() {
            handler = stack.remove(stack.size()-1);
        }
    }

    public class RootHandler extends Handler {

        @Override
        public void enterElement(String elementName, Attributes attrs) {
            switch (elementName) {
                case "msInstrument":
                    push(new MsInstrumentHandler());
                    return;
                case "dataProcessing":
                    dataProcessing(attrs);
                    return;
                case "scan":
                    push(new ScanHandler(attrs));
            }
        }

        private void dataProcessing(Attributes attrs) {
            if (isTrue(attrs,"centroided")) {
                lcms.getProcessings().add(MsDataProcessing.CENTROIDED);
            }
            if (isTrue(attrs,"deisotoped")) {
                lcms.getProcessings().add(MsDataProcessing.DEISOTOPED);
            }
            if (isTrue(attrs,"chargeDeconvoluted")) {
                lcms.getProcessings().add(MsDataProcessing.CHARGE_DECONVOLUTED);
            }
        }

        @Override
        public void leaveElement(String elementName, String content) {

        }
    }

    public class MsInstrumentHandler extends Handler {

        @Override
        public void enterElement(String elementName, Attributes attrs) {
            switch (elementName) {
                case "msModel":
                    final String value = attrs.getValue("value");
                    if (value.toLowerCase().contains("maxis")) {
                        lcms.setInstrument(MsInstrumentation.Instrument.BRUKER_MAXIS);
                    }
                    return;
                case "msMassAnalyzer":
                    final String v = attrs.getValue("value");
                    if (lcms.getInstrument()==MsInstrumentation.Unknown) {
                        for (MsInstrumentation i : MsInstrumentation.Instrument.values()) {
                            if (i.isInstrument(v)) {
                                lcms.setInstrument(i);
                                return;
                            }
                        }
                    }
            }
        }

        @Override
        public void leaveElement(String elementName, String content) {
            if (elementName.equals("msInstrument"))
                pop();
        }
    }

    public class ScanHandler extends Handler {

        Polarity polarity;
        long retentionTime;
        CollisionEnergy collisionEnergy ;
        int msLevel;
        int npeaks;
        int scanNumber;
        boolean centroided;

        int precursorScanNumber, charge;
        double precursorIntensity, isolationWindowWidth, precursorMz;

        double[] mzArray, intensityArray;

        boolean zlib;

        int precision; // 0=unknown, 1=32, 2=64

        public ScanHandler(Attributes attr) {
            polarity = get(attr, "polarity", Polarity.UNKNOWN, (s)-> s.equals("+") ? Polarity.POSITIVE : (s.equals("-") ? Polarity.NEGATIVE : Polarity.UNKNOWN));
            retentionTime = get(attr, "retentionTime", 0L, (s)->datatypeFactory.newDuration(s).getTimeInMillis(Calendar.getInstance()));
            collisionEnergy = get(attr,"collisionEnergy", CollisionEnergy.none(), x->new CollisionEnergy(Double.parseDouble(x)));
            msLevel = get(attr, "msLevel", 1, Integer::parseInt);
            npeaks = get(attr,"peaksCount",0,Integer::parseInt);
            scanNumber = get(attr, "num", -1, Integer::parseInt);
            centroided = get(attr,"centroided", lcms.getProcessings().contains(MsDataProcessing.CENTROIDED),f->f.equals("1") ||f.equalsIgnoreCase("true"));

        }

        @Override
        public void enterElement(String elementName, Attributes attrs) {
            if (elementName.equals("precursorMz")) {
                precursorScanNumber = get(attrs,"precursorScanNum", -1, Integer::parseInt);
                precursorIntensity = get(attrs,"precursorIntensity",Double.NaN,Double::parseDouble);
                charge = get(attrs,"precursorCharge", 0, Integer::parseInt);
                isolationWindowWidth = get(attrs,"windowWideness", Double.NaN, Double::parseDouble);
                listen();
            } else if (elementName.equals("peaks")) {
                zlib = get(attrs,"compressionType", false, (x)->x.equals("zlib"));
                precision = get(attrs, "precision", 0, (x)->x.equals("32") ? 1 : (x.equals("64") ? 2 : 0));
                listen();
            } else if (elementName.equals("scan")) {
                push(new ScanHandler(attrs));
            }
        }

        @Override
        public void leaveElement(String elementName, String content) {
            if (elementName.equals("precursorMz")) {
                precursorMz = Double.parseDouble(content);
                listen(false);
            } else if (elementName.equals("peaks")) {
                ByteBuffer W = ByteBuffer.wrap(zlib ? decompress(content) : Base64.getDecoder().decode(content));
                W.order(ByteOrder.BIG_ENDIAN);
                if (precision==0) {
                    // estimate precision from length...
                    if (W.capacity() == (npeaks*16))
                        precision = 2;
                    else
                        precision = 1;
                }
                if (precision==1) {
                    FloatBuffer f = W.asFloatBuffer();
                    this.mzArray = new double[f.limit()>>1];
                    this.intensityArray = new double[f.limit()>>1];
                    for (int i=0; i < mzArray.length; ++i) {
                        mzArray[i] = f.get();
                        intensityArray[i] = f.get();
                    }
                } else {
                    DoubleBuffer f = W.asDoubleBuffer();
                    this.mzArray = new double[f.limit()>>1];
                    this.intensityArray = new double[f.limit()>>1];
                    for (int i=0; i < mzArray.length; ++i) {
                        mzArray[i] = f.get();
                        intensityArray[i] = f.get();
                    }
                }
            } else if (elementName.equals("scan")) {
                SimpleSpectrum spectrum = new SimpleSpectrum(mzArray, intensityArray);
                double TIC = 0d;
                boolean zeroIntensity = false;
                for (double value : intensityArray) {
                    TIC += value;
                    zeroIntensity = zeroIntensity || value==0;
                }
                if (zeroIntensity) {
                    spectrum = Spectrums.getBaselined(spectrum,0);
                }
                Precursor prec;
                if (msLevel>1) {
                    prec = new Precursor(precursorScanNumber,precursorMz,precursorIntensity,charge,isolationWindowWidth);
                } else prec = null;
                final Scan scan = new Scan(scanNumber,polarity,retentionTime,collisionEnergy,spectrum.size(), TIC, centroided, prec);
                storage.add(scan, spectrum);
                lcms.addScan(scan);
                listen(false);
                pop();
            }
        }

        private byte[] decompress(String content) {
            totalBuffer.clearQuick();
            final Inflater inflater = new Inflater();
            inflater.setInput(Base64.getDecoder().decode(content));
            while (true){
                try {
                    int written = inflater.inflate(bytebuffer);
                    if (written==0) return totalBuffer.toArray();
                    else totalBuffer.add(bytebuffer,0,written);
                } catch (DataFormatException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

}
