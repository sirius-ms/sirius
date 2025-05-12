package de.unijena.bioinf.spectraldb.entities;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bionf.fastcosine.SearchPreparedMergedSpectrum;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MergedReferenceSpectrum implements ReferenceSpectrum {

    @Id
    private long uuid;

    /**
     * The uuids of spectra used to generate this merged spectra.
     * We use an array here cause at the time the merged spectrum is generated it knows its child spectra.
     */
    private long[] individualSpectraUIDs;

    /**
     * This is the InChiKey (2D) to map spectra to a standardized SIRIUS structure candidate.
     * In rare cases ot might not match the actual smiles of the measured compound due to standardization.
     * NOTE: Indexed field (mandatory)
     */
    private String candidateInChiKey;

    /**
     * The adduct / ion type / precursor type
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.PrecursorIonTypeDeserializer.class)
    private PrecursorIonType precursorIonType;

    /**
     * NOTE: Indexed field (mandatory)
     */
    private double precursorMz;

    /**
     * NOTE: Indexed field (mandatory)
     */
    private double exactMass;

    /**
     * Molecular formula of the measured compound. Must match candidateInChiKey and smiles
     * NOTE: Indexed field (mandatory)
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.MolecularFormulaDeserializer.class)
    private MolecularFormula formula;

    /**
     * This is the representation of the structure that produced this spectrum.
     */
    private String smiles;

    /**
     * Name of the spectrum.
     * Usually contains the name of the measured compound
     */
    private String name;

    /**
     * NOTE: filled while querying a SpectralLibrary
     */
    @JsonIgnore
    private String libraryName;

    /**
     * the merged query spectrum
     */
    @JsonAlias("querySpectrum") //allows us to read old databases as well (Old name was never released but already used a lot internally).
    private SearchPreparedMergedSpectrum searchPreparedSpectrum;
}
