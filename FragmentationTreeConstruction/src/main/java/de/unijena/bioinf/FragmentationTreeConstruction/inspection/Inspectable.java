package de.unijena.bioinf.FragmentationTreeConstruction.inspection;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.data.JDKDocument;

public interface Inspectable {

    public <G, D, L> void inspect(DataDocument<G, D, L> document, D dictionary);

    public static class Utils {
        private static JDKDocument javaobj = new JDKDocument();
        public static <G, D, L> void keyValues(DataDocument<G, D, L> document, D dictionary, Object... pairs) {
            String key = null;
            for (int i=0; i < pairs.length; ++i) {
                if (i % 2 == 0) {
                    key = (String)pairs[i];
                } else {
                    document.addToDictionary(dictionary, key, DataDocument.transform(javaobj, document, pairs[i]));
                }
            }
        }
    }

}
