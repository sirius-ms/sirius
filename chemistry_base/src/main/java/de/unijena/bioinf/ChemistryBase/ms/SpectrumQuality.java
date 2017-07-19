package de.unijena.bioinf.ChemistryBase.ms;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ge28quv on 04/07/17.
 */
public class SpectrumQuality {
    List<SpectrumProperty> properties;

    public SpectrumQuality() {
        this(null);
    }

    public SpectrumQuality(SpectrumProperty property) {
        properties = new ArrayList<>();
        if (property!=null) properties.add(property);
    }

    public void addProperty(SpectrumProperty property) {
        properties.add(property);
    }

    public List<SpectrumProperty> getProperties() {
        return properties;
    }

    public boolean isGoodQuality(){
        //todo what is standard?
        for (SpectrumProperty property : properties) {
            if (property.equals(SpectrumProperty.Good)) return true;
        }
        return false;
    }

    public boolean hasProperty(SpectrumProperty property) {
        for (SpectrumProperty p : properties) {
            if (property.equals(p)) return true;
        }
        return false;
    }

    public static boolean isNotBadQuality(Ms2Experiment experiment){
        return experiment.getAnnotation(SpectrumQuality.class, new SpectrumQuality(SpectrumProperty.Good)).isGoodQuality();
    }

    public static String getQualityString(Ms2Experiment experiment){
        SpectrumQuality quality = experiment.getAnnotation(SpectrumQuality.class);
        if (quality==null) return "";
        StringBuilder builder = new StringBuilder(",");
        for (SpectrumProperty spectrumProperty : quality.getProperties()) {
            builder.append(spectrumProperty.toString());
        }
        return builder.toString();
    }
}
