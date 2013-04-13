package de.unijena.bioinf.babelms.mzxml.io;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import de.unijena.bioinf.babelms.mzxml.model.*;

import javax.xml.datatype.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import de.unijena.bioinf.babelms.utils.Base64;

import static de.unijena.bioinf.babelms.mzxml.io.MzXMLParser.SaxHandler.State.*;

public class MzXMLParser {

	// this field contains an arbitrary start time, which is used for conversion of duration values
	// if a duration contains a month field, the number of days becomes arbitrary. But in practice,
	// this should never happen
	private static final Date ARBITRARY_START_TIME;
	
	static{
		Date d;
		try {
			 d = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").parse("2000.01.01 00:00:00");
		} catch (ParseException e) {
			d = new Date();
		}
		ARBITRARY_START_TIME = d;
	}
	
    public MsRun parseMsRun(File f) throws IOException {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        try {
            final SAXParser parser = factory.newSAXParser();
            final SaxHandler handler = new SaxHandler();
            parser.parse(f, handler);
            return handler.getMsRun();
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (DatatypeConfigurationException e) {
        	throw new IOException(e);
		}
    }


    public static class SaxHandler extends DefaultHandler {

        public static enum State {
            ROOT, MSRUN, MSINSTRUMENT, DATAPROCESSING, SCAN, SPOTTING, PLATE, PATTERN
        }


        private State currentState;
        private MsRun currentMsRun;
        private MsInstrument currentMsInsrument;
        private DataProcessing currentDataProcessing;
        private Spotting currentSpotting;
        private Plate currentPlate;
        private Deque<Scan> scanStack;
        private Map<String, ParentFile> filePool;
        private Map<String, Spot> spotPool;
        private Map<Integer, Plate> platePool;
        private Map<Integer, MsInstrument> msIntrPool;
        private List<Reference<String, ParentFile>> parentFileRefList;
        private List<Reference<Integer, MsInstrument>> msInstrRefList;
        private List<Reference<String, Spot>> spotRefList;
        private List<Reference<Integer, Plate>> plateRefList;
        private Attributes attributes;
        private String tagName;
        private Locator locator;
        private DatatypeFactory factory;
        private final StringBuilder buffer;
        private boolean listen;

        public SaxHandler() throws DatatypeConfigurationException {
            super();
            this.buffer = new StringBuilder();
            this.factory = DatatypeFactory.newInstance();
        }

        public MsRun getMsRun() {
            return currentMsRun;
        }

        @Override
        public void startDocument() throws SAXException {
            super.startDocument();
            this.currentMsRun = new MsRun();
            this.scanStack = new ArrayDeque<Scan>();
            this.filePool = new HashMap<String, ParentFile>();
            this.spotPool = new HashMap<String, Spot>();
            this.platePool = new HashMap<Integer, Plate>();
            this.msIntrPool = new HashMap<Integer, MsInstrument>();
            this.parentFileRefList = new ArrayList<Reference<String, ParentFile>>();
            this.spotRefList = new ArrayList<Reference<String, Spot>>();
            this.plateRefList = new ArrayList<Reference<Integer, Plate>>();
            this.msInstrRefList = new ArrayList<Reference<Integer, MsInstrument>>();
            this.currentState = ROOT;
            this.listen = false;
        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();
            solveReferences();
            currentPlate = null;
            currentSpotting = null;
            currentDataProcessing = null;
            currentMsInsrument = null;
            scanStack.clear();
            filePool.clear();
            platePool.clear();
            plateRefList.clear();
            parentFileRefList.clear();
            spotPool.clear();
            spotRefList.clear();
            msIntrPool.clear();
            msInstrRefList.clear();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            this.tagName = qName;
            this.attributes = attributes;
            switch (currentState) {
                case ROOT:
                    checkRoot(qName);
                    return;
                case MSRUN:
                    checkMsRun(qName);
                    return;
                case MSINSTRUMENT:
                    checkMsInstrument(qName);
                    return;
                case DATAPROCESSING:
                    checkDataProcessing(qName);
                    return;
                case SPOTTING:
                    checkSpotting(qName);
                    return;
                case PLATE:
                    checkPlate(qName);
                    return;
                case PATTERN:
                    checkPattern(qName);
                    return;
                case SCAN:
                    checkScan(qName);
            }
        }

        private void checkRoot(String qName) {
            if ("msRun".equals(qName)) {
                currentState = MSRUN;
                currentMsRun = new MsRun();
                currentMsRun.setScanCount(parseInt(fetchOptional("scanCount")));
                currentMsRun.setStartTime(parseDuration(fetchOptional("startTime")));
                currentMsRun.setEndTime(parseDuration(fetchOptional("endTime")));
            }
        }

        private void checkMsRun(String qName) {
            if ("parentFile".equals(qName)) {
                final ParentFile file = new ParentFile();
                file.setFileName(fetchAttribute("fileName"));
                file.setFileSha1(fetchAttribute("fileSha1"));
                file.setFileType(parseFileType(fetchAttribute("fileType")));
                currentMsRun.getParentFiles().add(file);
                if (file.getFileSha1() != null)
                    filePool.put(file.getFileSha1(), file);
            } else if ("msInstrument".equals(qName)) {
                currentState = MSINSTRUMENT;
                this.currentMsInsrument = new MsInstrument();
                currentMsRun.getMsInstruments().add(currentMsInsrument);
                currentMsInsrument.setInstrumentId(parseInt(fetchOptional("msInstrumentID")));
                if (currentMsInsrument.getInstrumentId() != null) {
                    msIntrPool.put(currentMsInsrument.getInstrumentId(), currentMsInsrument);
                }
            } else if ("dataProcessing".equals(qName)) {
                currentDataProcessing = new DataProcessing();
                currentMsRun.getDataProcessings().add(currentDataProcessing);
                currentState = DATAPROCESSING;
                currentDataProcessing.setIntensityCutoff(parseFloat(fetchOptional("intensityCutoff")));
                currentDataProcessing.setCentroided(parseBoolean(fetchOptional("centroided")));
                currentDataProcessing.setDeisotoped(parseBoolean(fetchOptional("deisotoped")));
                currentDataProcessing.setChargeDeconvoluted(parseBoolean(fetchOptional("chargeDeconvoluted")));
                currentDataProcessing.setSpotIntegration(parseBoolean(fetchOptional("spotIntegration")));
            } else if ("spotting".equals(qName)) {
                currentSpotting = new Spotting();
                currentMsRun.setSpotting(currentSpotting);
                currentState = SPOTTING;
            } else if ("sha1".equals(qName)) {
                clearBuffer();
                listen = true;
            } else if ("scan".equals(qName)) {
                final Scan scan = parseScan();
                currentMsRun.getScans().add(scan);
                scanStack.push(scan);
                currentState = SCAN;
            }
        }

        private void checkScan(String name) {
            final Scan scan = scanStack.peek();
            if ("scanOrigin".equals(name)) {
                final String sha1 = fetchAttribute("parentFileID");
                final Reference<String, ParentFile> pref = ref(parentFileRefList,
                        new Reference<String, ParentFile>(sha1));
                final SortedMap<Integer, Reference<String, ParentFile>> scanOrigs = scan.getScanOrigins();
                final Integer integ = parseInt(fetchAttribute("num"));
                if (integ == null) {
                    if (scanOrigs.isEmpty()) {
                        scanOrigs.put(0, pref);
                    } else {
                        scanOrigs.put(scanOrigs.lastKey(), pref);
                    }
                } else {
                    scanOrigs.put(integ, pref);
                }
            } else if ("precursorMz".equals(name)) {
                final PrecursorIon prec = new PrecursorIon();
                scan.getPrecursorIons().add(prec);
                prec.setPrecursorScanNum(parseInt(fetchOptional("precursorScanNum")));
                prec.setPrecursorIntensity(parseFloat(fetchAttribute("precursorIntensity")));
                prec.setPrecursorCharge(parseInt(fetchOptional("precursorCharge")));
                prec.setPossibleCharges(fetchOptional("possibleCharges"));
                prec.setWindowWideness(parseFloat(fetchOptional("windowWideness")));
                setActivationMethod(prec, fetchOptional("activationMethod"));
                listen = true;
                clearBuffer();
            } else if ("maldi".equals(name)) {
                final MaldiExperiment maldi = new MaldiExperiment();
                scan.setMaldi(maldi);
                maldi.setPlate(ref(plateRefList, new Reference<Integer, Plate>(parseInt(fetchAttribute("plateID")))));
                maldi.setSpot(ref(spotRefList, new Reference<String, Spot>(fetchAttribute("spotID"))));
                maldi.setLaserShootCount(parseInt(fetchOptional("laserShootCount")));
                maldi.setLaserFrequency(parseDuration(fetchOptional("laserFrequency")));
                maldi.setLaserIntensity(parseInt(fetchOptional("laserIntensity")));
                maldi.setCollisionGas(parseBoolean(fetchOptional("collisionGas")));
            } else if ("peaks".equals(name)) {
                scan.setUseDoublePrecision("64".equals(attributes.getValue("precision")));
                scan.setUseZlib("zlib".equalsIgnoreCase(attributes.getValue("compressionType")));
                final Integer compressedLen = parseInt(scan.isUseZlib() ? fetchAttribute("compressedLen") : fetchOptional("compressedLen"));
                if (compressedLen != null)
                    scan.setCompressedLen(compressedLen);
                listen = true;
                clearBuffer();
            } else if ("scan".equals(name)) {
                final Scan newScan = parseScan();
                scanStack.push(newScan);
                scan.getScans().add(newScan);
            }
        }

        private void checkMsInstrument(String qName) {
            if ("msManufacturer".equals(qName)) {
                currentMsInsrument.setMsManufacturer(parseOntologyEntry());
            } else if ("msModel".equals(qName)) {
                currentMsInsrument.setMsModel(parseOntologyEntry());
            } else if ("msIonisation".equals(qName)) {
                currentMsInsrument.setMsIonisation(parseOntologyEntry());
            } else if ("msMassAnalyzer".equals(qName)) {
                currentMsInsrument.setMsMassAnalyser(parseOntologyEntry());
            } else if ("msDetector".equals(qName)) {
                currentMsInsrument.setMsDetector(parseOntologyEntry());
            } else if ("msResolution".equals(qName)) {
                currentMsInsrument.setMsResolution(parseOntologyEntry());
            } else if ("operator".equals(qName)) {
                currentMsInsrument.getOperators().add(parseOperator());
            } else if ("nameValue".equals(qName)) {
                currentMsInsrument.getNameValueSet().add(parseNameValue());
            } else if ("comment".equals(qName)) {
                clearBuffer();
                listen = true;
            } else if ("software".equals(qName)) {
                currentMsInsrument.setSoftware(parseSoftware());
            }
        }

        private void checkDataProcessing(String name) {
            if ("software".equals(name)) {
                currentDataProcessing.setSoftware(parseSoftware());
            } else if ("processingOperation".equals(name)) {
                currentDataProcessing.getProcessingOperation().add(parseNameValue());
            } else if ("comment".equals(name)) {
                clearBuffer();
                listen = true;
            }
        }

        private void checkSpotting(String name) {
            if ("plate".equals(name)) {
                currentPlate = new Plate();
                currentSpotting.getPlates().add(currentPlate);
                currentPlate.setPlateId(parseInt(fetchAttribute("plateID")));
                currentPlate.setSpotXCount(parseInt(fetchAttribute("spotXCount")));
                currentPlate.setSpotYCount(parseInt(fetchAttribute("spotYCount")));
                if (currentPlate.getPlateId() != null) {
                    platePool.put(currentPlate.getPlateId(), currentPlate);
                }
                currentState = PLATE;
            } else if ("robot".equals(name)) {
                final Robot rob = new Robot();
                currentSpotting.setRobot(rob);
                rob.setTimePerSpot(parseDuration(fetchAttribute("timePerSpot")));
                rob.setDeadVolume(parseInt(fetchOptional("deadVolume")));
            } else if ("robotManufacturer".equals(name)) {
                currentSpotting.getRobot().setRobotManufacturer(parseOntologyEntry());
            } else if ("robotModel".equals(name)) {
                currentSpotting.getRobot().setRobotModel(parseOntologyEntry());
            }
        }

        private void checkPlate(String name) {
            if ("plateManufacturer".equals(name)) {
                currentPlate.setPlateManufacturer(parseOntologyEntry());
            } else if ("plateModel".equals(name)) {
                currentPlate.setPlateModel(parseOntologyEntry());
            } else if ("pattern".equals(name)) {
                currentPlate.setPattern(new SpottingPattern());
                currentState = PATTERN;
            } else if ("spot".equals(name)) {
                final Spot spot = new Spot();
                currentPlate.getSpots().add(spot);
                spot.setSpotId(fetchAttribute("spotID"));
                spot.setSpotXPosition(fetchAttribute("spotXPosition"));
                spot.setSpotYPosition(fetchAttribute("spotYPosition"));
                spot.setSpotDiameter(parseInt(fetchOptional("spotDiameter")));
                if (spot.getSpotId() != null) {
                    spotPool.put(spot.getSpotId(), spot);
                }
            } else if ("maldiMatrix".equals(name)) {
                currentPlate.getSpots().get(currentPlate.getSpots().size() - 1).setMaldiMatrix(parseOntologyEntry());
            }
        }

        private void checkPattern(String name) {
            if ("spottingPattern".equals(name)) {
                currentPlate.getPattern().setSpottingPattern(parseOntologyEntry());
            } else if ("orientation".equals(name)) {
                currentPlate.getPattern().setFirstSpot(ref(spotRefList,
                        new Reference<String, Spot>(fetchAttribute("firstSpotID"))));
                currentPlate.getPattern().setSecondSpot(ref(spotRefList,
                        new Reference<String, Spot>(fetchAttribute("secondSpotID"))));
            }
        }

        private void decodePreaks() {
            try {
                final Scan scan = scanStack.peek();
                final byte[] decoded = Base64.decode(buffer.toString().getBytes());
                final int length;
                int peaksCount;
                if (scan.getPeaksCount() != null) {
                    length = scan.getPeaksCount() * (scan.isUsingDoublePrecision() ? 16 : 8);
                    peaksCount = scan.getPeaksCount();
                } else {
                    length = decoded.length;
                    peaksCount = Integer.MAX_VALUE;
                }
                final byte[] uncompressed;
                if (scan.isUseZlib()) {
                    final ArrayList<byte[]> buffers = new ArrayList<byte[]>();
                	final Inflater inflater = new Inflater();
                    inflater.setInput(decoded);
                    int totalLength = 0;
                    int x;
                    do {
                    	final byte[] buffer = new byte[length];
                    	buffers.add(buffer);
                    	x = inflater.inflate(buffer);
                    	totalLength += x;
                    } while (x > 0 && inflater.getRemaining() > 0);
                    inflater.end();
                    uncompressed = new byte[totalLength];
                    int offset = 0;
                    for (byte[] buffer : buffers) {
                    	System.arraycopy(uncompressed, offset, buffer, 0, Math.min(buffer.length, totalLength-offset));
                    }
                } else {
                	uncompressed = decoded;
                }
                peaksCount = Math.min(peaksCount, uncompressed.length / (scan.isUsingDoublePrecision() ? 16 : 8));
                scan.setPeakMass(new double[peaksCount]);
                scan.setPeakIntensity(new double[peaksCount]);
                final ByteBuffer buffer = ByteBuffer.wrap(uncompressed);
                for (int i = 0; i < peaksCount; ++i) {
                    final double massOverCharge = (scan.isUsingDoublePrecision()) ? buffer.getDouble() : buffer.getFloat();
                    final double intensity = (scan.isUsingDoublePrecision()) ? buffer.getDouble() : buffer.getFloat();
                    scan.getPeakMass()[i] = massOverCharge;
                    scan.getPeakIntensity()[i] = intensity;
                }
            } catch (DataFormatException exc) {
                error("Error while uncompressing peaks", exc);
            } catch (IOException exc) {
                error("Error while decoding peaks", exc);
            }
        }

        public void solveReferences() {
            for (Reference<String, ParentFile> fref : parentFileRefList) {
                fref.setValue(filePool.get(fref.getKey()));
            }
            for (Reference<Integer, MsInstrument> msref : msInstrRefList) {
                msref.setValue(msIntrPool.get(msref.getKey()));
            }
            for (Reference<String, Spot> sref : spotRefList) {
                sref.setValue(spotPool.get(sref.getKey()));
            }
            for (Reference<Integer, Plate> pref : plateRefList) {
                pref.setValue(platePool.get(pref.getKey()));
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            if (currentState == MSRUN) {
                if ("sha1".equals(qName)) {
                    currentMsRun.setSha1(buffer.toString());
                    clearBuffer();
                    listen = false;
                }
            } else if (currentState == MSINSTRUMENT) {
                if ("comment".equals(qName)) {
                    currentMsInsrument.getComments().add(buffer.toString());
                    clearBuffer();
                    listen = false;
                } else if ("msInstrument".equals(qName)) {
                    currentState = MSRUN;
                }
            } else if (currentState == DATAPROCESSING) {
                if ("comment".equals(qName)) {
                    currentDataProcessing.getComments().add(buffer.toString());
                    clearBuffer();
                    listen = false;
                } else if ("dataProcessing".equals(qName)) {
                    currentState = MSRUN;
                }
            } else if (currentState == PATTERN) {
                if ("pattern".equals(qName)) {
                    currentState = PLATE;
                }
            } else if (currentState == PLATE) {
                if ("plate".equals(qName)) {
                    currentState = SPOTTING;
                }
            } else if (currentState == SPOTTING) {
                if ("spotting".equals(qName)) {
                    currentState = MSRUN;
                }
            } else if (currentState == SCAN) {
                if ("precursorMz".equals(qName)) {
                    final List<PrecursorIon> precs = scanStack.peek().getPrecursorIons();
                    precs.get(precs.size() - 1).setPrecursorMz(parseFloat(buffer.toString()));
                    clearBuffer();
                    listen = false;
                } else if ("peaks".equals(qName)) {
                    decodePreaks();
                    clearBuffer();
                    listen = false;
                } else if ("scan".equals(qName)) {
                    scanStack.pop();
                    if (scanStack.isEmpty()) {
                        currentState = MSRUN;
                    }
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
            if (listen) {
                buffer.append(ch, start, length);
            }
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            super.setDocumentLocator(locator);
            this.locator = locator;
        }

        private <K, V> Reference<K, V> ref(List<Reference<K, V>> ls, Reference<K, V> r) {
            if (r.getKey() == null) return null;
            ls.add(r);
            return r;
        }

        private Float parseFloat(String value) {
            if (value == null) return null;
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException exc) {
                warn("Illegal format of float: '" + value + "'");
                return null;
            }
        }

        private Boolean parseBoolean(String value) {
            if (value == null) return null;
            if ("1".equals(value) || value.equalsIgnoreCase("true")) {
                return true;
            } else if (value.equals("0") || value.equalsIgnoreCase("false")) {
                return false;
            } else {
                warn("Illegal format of boolean: '" + value + "'");
                return null;
            }
        }

        private void clearBuffer() {
            buffer.delete(0, buffer.length());
        }

        private Scan parseScan() {
            final Scan scan = new Scan();
            scan.setNum(parseInt(fetchAttribute("num")));
            scan.setMsLevel(parseInt(fetchAttribute("msLevel")));
            scan.setPeaksCount(parseInt(fetchAttribute("peaksCount")));
            scan.setPolarity(parsePolarity(fetchOptional("polarity")));
            scan.setScanType(fetchOptional("scanType"));
            scan.setFilterLine(fetchOptional("filterLine"));
            scan.setCentroided(parseBoolean(fetchOptional("centroided")));
            scan.setDeisotoped(parseBoolean(fetchOptional("deisotoped")));
            scan.setChargeDeconvoluted(parseBoolean(fetchOptional("chargeDeconvoluted")));
            scan.setRetentionTime(parseDuration(fetchOptional("retentionTime")));
            scan.setIonisationEnergy(parseFloat(fetchOptional("ionisationEnergy")));
            scan.setCollisionEnergy(parseFloat(fetchOptional("collisionEnergy")));
            scan.setCidGasPressure(parseFloat(fetchOptional("cidGasPressure")));
            scan.setStartMz(parseFloat(fetchOptional("startMz")));
            scan.setEndMz(parseFloat(fetchOptional("endMz")));
            scan.setLowMz(parseFloat(fetchOptional("lowMz")));
            scan.setHighMz(parseFloat(fetchOptional("highMz")));
            scan.setBasePeakMz(parseFloat(fetchOptional("basePeakMz")));
            scan.setBasePeakIntensity(parseFloat(fetchOptional("basePeakIntensity")));
            scan.setTotIonCurrent(parseFloat(fetchOptional("totIonCurrent")));
            scan.setCompensationVoltage(parseFloat(fetchOptional("compensationVoltage")));
            scan.setMsInstrument(ref(msInstrRefList,
                    new Reference<Integer, MsInstrument>(parseInt(fetchOptional("msInstrumentID")))));
            return scan;
        }

        private NameValuePair parseNameValue() {
            return new NameValuePair(fetchOptional("name"), fetchOptional("value"), fetchOptional("type"));
        }

        private Integer parseInt(String value) {
            if (value == null) return null;
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exc) {
                warn("Illegal format of integer: '" + value + "'");
                return null;
            }
        }

        private XMLGregorianCalendar parseDateTime(String value) {
            if (value == null) return null;
            try {
                return factory.newXMLGregorianCalendar(value);
            } catch (IllegalArgumentException exc) {
                warn("Illegal format of dateTime: '" + value + "'");
                return null;
            }
        }

        private Double parseDuration(String value) {
            if (value == null) return null;
            if (factory == null) return null;
            try {
                final Duration time = factory.newDuration(value);
                double t = time.getTimeInMillis(ARBITRARY_START_TIME)/1000d;
                return t;
            } catch (IllegalArgumentException ex) {
                warn("Illegal format of duration: '" + value + "'");
                return null;
            } catch (UnsupportedOperationException ex) {
                warn("Can't parse duration field.");
                return null;
            }

        }

        private Software parseSoftware() {
            final Software software = new Software();
            setSoftwareType(software, fetchAttribute("type"));
            software.setName(fetchAttribute("name"));
            software.setVersion(fetchAttribute("version"));
            software.setCompletionTime(parseDateTime(fetchOptional("completionTime")));
            return software;
        }

        private FileType parseFileType(String value) {
            if (value == null) return null;
            for (FileType ft : FileType.values()) {
                if (ft.attributeValue.equalsIgnoreCase(value)) {
                    return ft;
                }
            }
            warn("Unknown filetype '" + value + "'");
            return null;
        }

        private void setActivationMethod(PrecursorIon ion, String value) {
            if (value == null) return;
            ion.setActivationMethod(value);
            if (ion.getActivationMethod() == ActivationMethod.CUSTOM) {
            	warn("Unknown activation method '" + value + "'");
            }
        }

        private void setSoftwareType(Software software, String value) {
            if (value == null) return;
            software.setType(value);
            if (software.getType() == SoftwareType.CUSTOM) {
            	warn("Unknown software type '" + value + "'");
            }
        }

        private Polarity parsePolarity(String value) {
            if (value == null) return null;
            for (Polarity p : Polarity.values()) {
                if (p.attributeName.equalsIgnoreCase(value)) {
                    return p;
                }
            }
            warn("Unknown polarity '" + value + "'");
            return null;
        }

        private OntologyEntry parseOntologyEntry() {
            return new OntologyEntry(fetchAttribute("category"), fetchAttribute("value"));
        }

        private Contact parseOperator() {
            final Contact contact = new Contact();
            contact.setFirst(fetchAttribute("first"));
            contact.setLast(fetchAttribute("last"));
            contact.setMail(fetchOptional("email"));
            contact.setPhone(fetchOptional("phone"));
            contact.setUri(fetchOptional("uri"));
            return contact;
        }

        private String fetchOptional(String name) {
            return attributes.getValue(name);
        }

        private String fetchAttribute(String name) {
            final String attr = attributes.getValue(name);
            if (attr == null) {
                warn("Missing attribute " + name + " in <" + tagName + ">");
            }
            return attr;
        }

        private void warn(String msg) {
            System.err.println("Line #" + locator.getLineNumber() + ": " + msg);
        }

        private void error(String msg, Throwable exc) {
            System.err.println("Line #" + locator.getLineNumber() + ": " + msg + "\n" + exc);
        }
    }

}
