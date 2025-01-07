import java.util.List;

public interface TransformationMethod {
    /**
     * Berechnet Biotransformationsprodukte aus einer gegebenen Eingabemolekül-Struktur.
     * Nutzt standardmäßig ein vorgegebenes Biosystem und alle verfügbaren Enzyme.
     *
     * @param inputMolecule Die Eingabemolekül-Struktur im SMILES- oder InChI-Format.
     * @return Eine Liste von Biotransformationsprodukten als SMILES- oder InChI-Strings.
     */
    //List<String> calculateTransformations(String inputMolecule);

    /**
     * Berechnet Biotransformationsprodukte aus einer gegebenen Eingabemolekül-Struktur,
     * einem spezifischen Biosystem und einer Liste von Enzymen.
     *
     * @param inputMolecule Die Eingabemolekül-Struktur im SMILES- oder InChI-Format.
     * @param biosystem      Das gewünschte Biosystem (z. B. HUMAN, GUTMICRO, ENVMICRO).
     * @param enzymes        Eine Liste der spezifischen zu verwendenden Enzyme. Wenn `null` oder leer,
     *                       werden standardmäßig alle unterstützten Enzyme verwendet.
     * @return Eine Liste von Biotransformationsprodukten als SMILES- oder InChI-Strings.
     */
    List<String> calculateTransformations(String inputMolecule, String biosystem, List<String> enzymes);
}