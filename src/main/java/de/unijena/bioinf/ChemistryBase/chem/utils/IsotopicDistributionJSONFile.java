package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

public class IsotopicDistributionJSONFile extends DistributionReader {

	public IsotopicDistribution read(Reader json) throws IOException{
		final IsotopicDistribution dist = new IsotopicDistribution(PeriodicTable.getInstance());
        final BufferedReader reader = new BufferedReader(json);
		final StringBuilder buffer = new StringBuilder();
		while (reader.ready()) {
			buffer.append(reader.readLine()).append("\n");
		}
		JSONObject obj;
		try {
			obj = new JSONObject(buffer.toString());
			for (Iterator<?> keys = obj.keys(); keys.hasNext();) {
				final String key = keys.next().toString();
                final Isotopes prev = PeriodicTable.getInstance().getDistribution().getIsotopesFor(key);
				//final JSONObject element = (JSONObject)obj.get(key);
                //final JSONArray jmasses = (JSONArray)obj.get("masses");
                final JSONArray jabundances = (JSONArray)obj.get(key);
                final String elementSymbol = key;
                final double[] abundances = new double[jabundances.length()];
                final double[] masses = new double[jabundances.length()];
                for (int i=0; i < prev.getNumberOfIsotopes(); ++i) {  // TODO: fix!!!
                    abundances[i] = jabundances.getDouble(i);
                    masses[i] = prev.getMass(i);
                }
                dist.addIsotope(key, masses, abundances);
			}
		} catch (JSONException e) {
			throw new IOException("Can't parse json file. Invalid JSON syntax");
		}
        return dist;
	}
}
