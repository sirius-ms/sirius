package de.unijena.bioinf.lcms.io;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Writes a minimal but valid mzML file, so that the reader and everything behind it can be tested on acquisitions
 * that are awkward to keep as test data - an MS2 only run above all, which no real file in this repository is.
 * <p>
 * Only what the reader looks at is written: ms level, polarity, scan start time, the precursor of an MS/MS and the
 * two binary arrays. Spectra are numbered {@code scan=1,2,...} in the order they are added, which is the order they
 * have to be given in - the reader takes the retention time axis from the file rather than sorting it.
 */
public final class SyntheticMzML {

    private final List<String> spectra = new ArrayList<>();

    public static SyntheticMzML builder() {
        return new SyntheticMzML();
    }

    /** @return the scan number of the spectrum just added, for use as an MS/MS precursor reference */
    public int ms1(double retentionTimeSeconds, double[] mzValues, double[] intensities) {
        final int scanNumber = spectra.size() + 1;
        spectra.add(spectrum(scanNumber, 1, retentionTimeSeconds, mzValues, intensities, null));
        return scanNumber;
    }

    /**
     * @param precursorScanNumber the MS1 scan this fragmentation points back at, or null when the run holds no MS1
     *                            scans at all
     */
    public int ms2(double retentionTimeSeconds, double precursorMz, double isolationWindowWidth, double collisionEnergy,
            @Nullable Integer precursorScanNumber, double[] mzValues, double[] intensities) {
        final int scanNumber = spectra.size() + 1;
        spectra.add(spectrum(scanNumber, 2, retentionTimeSeconds, mzValues, intensities,
                precursor(precursorMz, isolationWindowWidth, collisionEnergy, precursorScanNumber)));
        return scanNumber;
    }

    public Path writeTo(Path file) throws IOException {
        final StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        xml.append("<mzML xmlns=\"http://psi.hupo.org/ms/mzml\" ")
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
                .append("xsi:schemaLocation=\"http://psi.hupo.org/ms/mzml http://psidev.info/files/ms/mzML/xsd/mzML1.1.0.xsd\" ")
                .append("version=\"1.1.0\" id=\"synthetic\">\n");
        xml.append("<cvList count=\"2\">")
                .append("<cv id=\"MS\" fullName=\"PSI-MS\" URI=\"http://psidev.info/ms/mzML/psi-ms.obo\"/>")
                .append("<cv id=\"UO\" fullName=\"Unit Ontology\" URI=\"http://obo.cvs.sourceforge.net/obo/obo/ontology/phenotype/unit.obo\"/>")
                .append("</cvList>\n");
        xml.append("<run id=\"synthetic_run\">\n");
        xml.append("<spectrumList count=\"").append(spectra.size()).append("\">\n");
        for (String spectrum : spectra) xml.append(spectrum);
        xml.append("</spectrumList>\n</run>\n</mzML>\n");
        Files.writeString(file, xml.toString(), StandardCharsets.UTF_8);
        return file;
    }

    private static String precursor(double precursorMz, double isolationWindowWidth, double collisionEnergy,
                                    @Nullable Integer precursorScanNumber) {
        final StringBuilder b = new StringBuilder();
        b.append("<precursorList count=\"1\"><precursor");
        if (precursorScanNumber != null) b.append(" spectrumRef=\"scan=").append(precursorScanNumber).append('"');
        b.append('>');
        b.append("<isolationWindow>")
                .append(cv("MS:1000827", "isolation window target m/z", precursorMz))
                .append(cv("MS:1000828", "isolation window lower offset", isolationWindowWidth / 2))
                .append(cv("MS:1000829", "isolation window upper offset", isolationWindowWidth / 2))
                .append("</isolationWindow>");
        b.append("<selectedIonList count=\"1\"><selectedIon>")
                .append(cv("MS:1000744", "selected ion m/z", precursorMz))
                .append("</selectedIon></selectedIonList>");
        b.append("<activation>")
                .append("<cvParam cvRef=\"MS\" accession=\"MS:1000133\" name=\"collision-induced dissociation\" value=\"\"/>")
                .append(cv("MS:1000045", "collision energy", collisionEnergy))
                .append("</activation>");
        b.append("</precursor></precursorList>");
        return b.toString();
    }

    private static String spectrum(int scanNumber, int msLevel, double retentionTimeSeconds,
                                   double[] mzValues, double[] intensities, @Nullable String precursorList) {
        if (mzValues.length != intensities.length)
            throw new IllegalArgumentException("m/z and intensity arrays differ in length");
        final StringBuilder b = new StringBuilder();
        b.append("<spectrum index=\"").append(scanNumber - 1).append("\" id=\"scan=").append(scanNumber)
                .append("\" defaultArrayLength=\"").append(mzValues.length).append("\">");
        b.append("<cvParam cvRef=\"MS\" accession=\"MS:1000511\" name=\"ms level\" value=\"").append(msLevel).append("\"/>");
        b.append("<cvParam cvRef=\"MS\" accession=\"MS:1000130\" name=\"positive scan\" value=\"\"/>");
        b.append("<cvParam cvRef=\"MS\" accession=\"MS:1000127\" name=\"centroid spectrum\" value=\"\"/>");
        b.append("<scanList count=\"1\"><scan>")
                .append("<cvParam cvRef=\"MS\" accession=\"MS:1000016\" name=\"scan start time\" value=\"")
                .append(String.format(Locale.US, "%.6f", retentionTimeSeconds))
                .append("\" unitAccession=\"UO:0000010\" unitName=\"second\" unitCvRef=\"UO\"/>")
                .append("</scan></scanList>");
        if (precursorList != null) b.append(precursorList);
        b.append("<binaryDataArrayList count=\"2\">");
        b.append(binaryDataArray(mzValues, "MS:1000514", "m/z array"));
        b.append(binaryDataArray(intensities, "MS:1000515", "intensity array"));
        b.append("</binaryDataArrayList>");
        b.append("</spectrum>\n");
        return b.toString();
    }

    private static String binaryDataArray(double[] values, String accession, String name) {
        final ByteBuffer buffer = ByteBuffer.allocate(values.length * Double.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (double value : values) buffer.putDouble(value);
        final String encoded = Base64.getEncoder().encodeToString(buffer.array());
        return "<binaryDataArray encodedLength=\"" + encoded.length() + "\">"
                + "<cvParam cvRef=\"MS\" accession=\"MS:1000523\" name=\"64-bit float\" value=\"\"/>"
                + "<cvParam cvRef=\"MS\" accession=\"MS:1000576\" name=\"no compression\" value=\"\"/>"
                + "<cvParam cvRef=\"MS\" accession=\"" + accession + "\" name=\"" + name + "\" value=\"\"/>"
                + "<binary>" + encoded + "</binary>"
                + "</binaryDataArray>";
    }

    private static String cv(String accession, String name, double value) {
        return String.format(Locale.US, "<cvParam cvRef=\"MS\" accession=\"%s\" name=\"%s\" value=\"%.6f\"/>",
                accession, name, value);
    }
}
