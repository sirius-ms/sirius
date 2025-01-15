package de.unijena.bioinf.spectraldb.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceFragment {

    private int index;
    private int parentIndex;
    private int peakIndex; // corresponding peak in merged spectrum

    // fragment information

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.MolecularFormulaDeserializer.class)
    private MolecularFormula formula;

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.MolecularFormulaDeserializer.class)
    private MolecularFormula lossFormula;

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.MolecularFormulaDeserializer.class)
    private MolecularFormula rootLossFormula;

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = SimpleSerializers.PrecursorIonTypeDeserializer.class)
    private PrecursorIonType ionType;

    // peak information, null if no recorded mass
    private Double mz;
    private Float intensity;

    private float normalizedIntensity; // intensity for querying

    public double exactMass() {
        return ionType.neutralMassToPrecursorMass(formula.getMass());
    }
}
