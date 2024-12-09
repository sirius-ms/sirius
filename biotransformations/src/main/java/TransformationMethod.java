//package sirius.transformations;

import java.util.List;

public interface TransformationMethod {
    /**
     * Berechnet Biotransformationsprodukte aus einer gegebenen Eingabemolekül-Struktur.
     *
     * @param inputMolecule Die Eingabemolekül-Struktur im SMILES- oder InChI-Format.
     * @return Eine Liste von Biotransformationsprodukten als SMILES- oder InChI-Strings.
     */
    List<String> calculateTransformations(String inputMolecule);
}
