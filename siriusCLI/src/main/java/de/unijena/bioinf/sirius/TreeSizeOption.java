package de.unijena.bioinf.sirius;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *  --enforce="-0.5->1.5 (80%, 12)"
 *
 */
public class TreeSizeOption {

    public double minTreeSize, maxTreeSize, enforcedIntensity;
    public int minNumberOfPeaks;

    public TreeSizeOption() {

    }

    // 1: min tree size
    // 2: max tree size
    // 3: min explained intensity
    // 4. min number of explained peaks
    Pattern REGEXP = Pattern.compile("([+-]?\\d+(?:\\.\\d*)?)\\s*->\\s*([+-]?\\d+(?:\\.\\d*)?)\\s*\\(([+-]?\\d+(?:\\.\\d*)?)\\s*%\\s*(?:,\\s*(\\d+)\\s*)?\\)");

    public TreeSizeOption(String s) {
        final Matcher m = REGEXP.matcher(s);
    }

}
