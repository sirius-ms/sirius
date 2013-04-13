package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PeriodicTableJSONReader extends PeriodicTableReader {

    private final boolean overrideElements;

    public PeriodicTableJSONReader(boolean overrideElements) {
        this.overrideElements = overrideElements;
    }

    public PeriodicTableJSONReader() {
        this(true);
    }

    public void readFromClasspath(PeriodicTable table, String name) throws IOException {
        super.fromClassPath(table, name);
    }

    public void readFromClasspath(PeriodicTable table) throws IOException {
        readFromClasspath(table, "/elements.json");
    }

    @Override
    public void read(PeriodicTable table, Reader reader) throws IOException {
        final String content = readString(reader);
        try {
            final JSONObject data = new JSONObject(content);
            for (Iterator<?> keys = data.keys(); keys.hasNext();) {
                final String key = keys.next().toString();
                final JSONObject element = (JSONObject)data.get(key);
                final String elementName = (String)element.get("name");
                final Integer elementValence = (Integer)element.get("valence");
                final JSONObject isotopes = (JSONObject)element.get("isotopes");
                final double[] masses = toDoubleArray((JSONArray) isotopes.get("mass"));
                final double[] abundances = toDoubleArray((JSONArray) isotopes.get("abundance"));
                final Isotopes iso = new Isotopes(masses, abundances);
                if (overrideElements || table.getByName(key) == null)
                    table.addElement(elementName, key, iso.getMass(0), elementValence );
                table.getDistribution().addIsotope(key, iso);
            }
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public boolean isOverrideElements() {
        return overrideElements;
    }

    private static double[] toDoubleArray(JSONArray ary) throws JSONException {
        final double[] array = new double[ary.length()];
        for (int i=0; i < ary.length(); ++i) array[i] = ary.getDouble(i);
        return array;
    }

    private static String readString(Reader reader) throws IOException {
        final StringBuilder buffer = new StringBuilder();
        final BufferedReader bufReader = new BufferedReader(reader);
        try {
            while (bufReader.ready()) {
                buffer.append(bufReader.readLine()).append("\n");
            }
        } finally {
            bufReader.close();
        }
        return buffer.toString();
    }
}
