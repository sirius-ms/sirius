package de.unijena.bioinf.ms.persistence.model.annotation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpectraCandidate {
    private String name;

    private String formula;

    private String adduct;

    private Double ionMass;

    /**
     * This is the representation of the structure that produced this spectrum.
     */
    private String smiles;

    /**
     * Name of the library, e,g. NIST/Massbank/Mona/GNPS/Custom ID
     */
    private String libraryName;

    /**
     * Identifier of the spectral library, e,g. NIST/Massbank/Mona/GNPS/Custom ID
     */
    private String libraryId;

    /**
     * Similarity of the hit so the best matching input spectrum
     */
    //todo to we need to store the type of similarity?
    private Double similarity;
}
