package de.unijena.bioinf.spectraldb;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.ReferenceSpectrum;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
@Setter
public class LibraryHit implements Comparable<LibraryHit> {

    private int queryIndex;
    private SpectrumType spectrumType;
    private boolean analog;
    private SpectralSimilarity similarity;
    private String dbName, dbId;
    private long uuid;
    private String splash;

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.MolecularFormulaDeserializer.class)
    private MolecularFormula molecularFormula;

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.PrecursorIonTypeDeserializer.class)
    private PrecursorIonType adduct;
    private double exactMass;
    private String smiles;

    public LibraryHit(int queryIndex, SpectralSimilarity similarity, Ms2ReferenceSpectrum spectrum, boolean analog) {
        this(queryIndex, similarity, (ReferenceSpectrum) spectrum, analog);
        // Ms2Reference Data
        this.spectrumType = SpectrumType.SPECTRUM;
        this.dbId = spectrum.getLibraryId();
        this.splash = spectrum.getSplash();
    }

    public LibraryHit(int queryIndex, SpectralSimilarity similarity, MergedReferenceSpectrum spectrum, boolean analog) {
        this(queryIndex, similarity, (ReferenceSpectrum) spectrum, analog);
        //Merged Spectrum data
        this.spectrumType = SpectrumType.MERGED_SPECTRUM;
        this.dbId = null;
        this.splash = null;
    }

    private LibraryHit(int queryIndex, SpectralSimilarity similarity, ReferenceSpectrum spectrum, boolean analog) {
        this.queryIndex = queryIndex;
        this.similarity = similarity;
        this.uuid = spectrum.getUuid();
        this.analog = analog;
        this.dbName = spectrum.getLibraryName();
        this.molecularFormula = spectrum.getFormula();
        this.adduct = spectrum.getPrecursorIonType();
        this.exactMass = spectrum.getExactMass();
        this.smiles = spectrum.getSmiles();
        this.candidateInChiKey = spectrum.getCandidateInChiKey();
    }

    /**
     * This is the inchikey of the corresponding structure candidate
     */
    private String candidateInChiKey;

    @Override
    public int compareTo(@NotNull LibraryHit o) {
        return similarity.compareTo(o.similarity);
    }

}
