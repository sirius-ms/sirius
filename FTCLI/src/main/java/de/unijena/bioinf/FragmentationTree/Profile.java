package de.unijena.bioinf.FragmentationTree;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.babelms.json.JSONDocumentType;
import org.json.JSONObject;

import java.io.*;

public class Profile {

    public final FragmentationPatternAnalysis fragmentationPatternAnalysis;
    public final IsotopePatternAnalysis isotopePatternAnalysis;

    protected static JSONObject getJSON(String name) throws IOException {
        // 1. check for resource with same name
        final InputStream stream = Profile.class.getResourceAsStream("/profiles/" + name.toLowerCase() + ".json");
        if (stream != null) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            try {
                final JSONObject obj = JSONDocumentType.read(reader);
                return obj;
            } finally {
                reader.close();
            }
        } else {
            // 2. check for file
            return JSONDocumentType.readFromFile(new File(name));
        }
    }

    public Profile(String name) throws IOException {
        final JSONObject json = getJSON(name);
        final JSONDocumentType document = new JSONDocumentType();
        this.fragmentationPatternAnalysis = FragmentationPatternAnalysis.loadFromProfile(document, json);
        this.isotopePatternAnalysis = IsotopePatternAnalysis.loadFromProfile(document, json);
    }

}
