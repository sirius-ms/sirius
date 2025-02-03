package de.unijena.bioinf.spectraldb;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
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
        this.queryIndex = queryIndex;
        this.similarity = similarity;
        this.spectrumType = SpectrumType.SPECTRUM;
        this.analog = analog;
        this.dbId = spectrum.getLibraryId();
        this.dbName = spectrum.getLibraryName();
        this.uuid = spectrum.getUuid();
        this.splash = spectrum.getSplash();
        this.molecularFormula = spectrum.getFormula();
        this.adduct = spectrum.getPrecursorIonType();
        this.exactMass = spectrum.getExactMass();
        this.smiles = spectrum.getSmiles();
        this.candidateInChiKey = spectrum.getCandidateInChiKey();
    }

    public LibraryHit(int queryIndex, SpectralSimilarity similarity, MergedReferenceSpectrum spectrum, boolean analog) {
        this.queryIndex = queryIndex;
        this.similarity = similarity;
        this.spectrumType = SpectrumType.MERGED_SPECTRUM;
        this.dbId = null;
        this.dbName = spectrum.getLibraryName();
        this.uuid = spectrum.getUuid();
        this.splash = null;
        this.molecularFormula = spectrum.getFormula();
        this.adduct = spectrum.getPrecursorIonType();
        this.exactMass = spectrum.getExactMass();
        this.smiles = spectrum.getSmiles();
        this.candidateInChiKey = spectrum.getCandidateInChiKey();
        this.analog = analog;
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
