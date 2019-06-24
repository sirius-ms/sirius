package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Quantification implements Ms2ExperimentAnnotation {

    private final TObjectDoubleHashMap<String> quant;

    public Quantification(TObjectDoubleHashMap<String> quant) {
        this.quant = new TObjectDoubleHashMap<>(quant);
    }

    public Quantification(Map<String, Double> quant) {
        this.quant = new TObjectDoubleHashMap<>();
        for (String k : quant.keySet())
            this.quant.put(k, quant.get(k));
    }

    public double getQuantificationFor(String id) {
        return quant.get(id);
    }

    @Override
    public String toString() {
        return Arrays.stream(quant.keys()).map(k->"\"" + k + "\":(" + quant.get(k) + ")").collect(Collectors.joining(";"));
    }

    public static Quantification fromString(String s) {
        final Pattern pat = Pattern.compile("\"([^\"]+)\":\\((\\S+)\\);");
        final Matcher m = pat.matcher(s+";");
        final TObjectDoubleHashMap<String> map = new TObjectDoubleHashMap<>();
        while (m.find()) {
            map.put(m.group(1), Double.parseDouble(m.group(1)));
        }
        return new Quantification(map);
    }
}
