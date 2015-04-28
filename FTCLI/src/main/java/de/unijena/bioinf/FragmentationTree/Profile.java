package de.unijena.bioinf.FragmentationTree;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.babelms.json.JSONDocumentType;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Deprecated
public class Profile {

    public final FragmentationPatternAnalysis fragmentationPatternAnalysis;
    public final IsotopePatternAnalysis isotopePatternAnalysis;

    public Profile(String name) throws IOException {
        final JSONObject json = JSONDocumentType.getJSON("/profiles/" + name.toLowerCase() + ".json", name);
        final JSONDocumentType document = new JSONDocumentType();
        if (document.hasKeyInDictionary(json, "FragmentationPatternAnalysis")) this.fragmentationPatternAnalysis = FragmentationPatternAnalysis.loadFromProfile(document, json);
        else fragmentationPatternAnalysis=null;
        if (document.hasKeyInDictionary(json, "IsotopePatternAnalysis")) this.isotopePatternAnalysis = IsotopePatternAnalysis.loadFromProfile(document, json);
        else isotopePatternAnalysis=null;
    }

    public Profile(IsotopePatternAnalysis ms1, FragmentationPatternAnalysis ms2) {
        this.fragmentationPatternAnalysis = ms2;
        this.isotopePatternAnalysis = ms1;
    }

    public void writeToFile(String fileName) throws IOException  {
        writeToFile(new File(fileName));
    }

    public void writeToFile(File name) throws IOException {
        final FileWriter writer = new FileWriter(name);
        final JSONDocumentType json = new JSONDocumentType();
        final JSONObject obj = json.newDictionary();
        if (fragmentationPatternAnalysis != null) {
            fragmentationPatternAnalysis.writeToProfile(json, obj);
        }
        if (isotopePatternAnalysis != null) {
            isotopePatternAnalysis.writeToProfile(json, obj);
        }
        try {
            JSONDocumentType.writeJson(json, obj, writer);
        } finally {
            writer.close();
        }
    }

}
