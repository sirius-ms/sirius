package de.unijena.bioinf.babelms.descriptor;

import de.unijena.bioinf.ChemistryBase.data.DataDocument;

/**
 * This class handles the serialization of annotation objects.
 * As I do not have a final API for this yet, annotation objects are serialized in a hardcoded manner.
 * However, future versions might allow other APIs to define their own serialization routines
 * Until this point every user is encouraged to define his own Annotation classes in the ChemistryBase packacke as
 * final, immutable pojos together with a serialization route in this class.
 */
public interface Descriptor<AnnotationType> {

    /**
     * A Descriptor is tried to parse an annotation as soon as one of the keywords appear in the dictionary.
     * If the keyword list is empty, the descriptor is always used.
     *
     * @return a list of keywords.
     */
    public String[] getKeywords();

    public Class<AnnotationType> getAnnotationClass();

    public <G, D, L> AnnotationType read(DataDocument<G, D, L> document, D dictionary);

    public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, AnnotationType annotation);

}
