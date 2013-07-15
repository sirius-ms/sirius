package de.unijena.bioinf.FragmentationTree;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.babelms.json.JSONDocumentType;
import org.json.JSONObject;

import java.io.*;

public class Profile {

    public static FragmentationPatternAnalysis getFTAnalysisProfile(String name) throws IOException {
        // 1. check for resource with same name
        final InputStream stream = Profile.class.getResourceAsStream("/profiles/" + name.toLowerCase() + ".json");
        if (stream != null) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            try {
                final JSONObject obj = JSONDocumentType.read(reader);
                return FragmentationPatternAnalysis.loadFromProfile(new JSONDocumentType(), obj);
            } finally {
                reader.close();
            }
        } else {
            // 2. check for file
            return FragmentationPatternAnalysis.loadFromProfile(new JSONDocumentType(), JSONDocumentType.readFromFile(new File(name)));
        }
    }

}
