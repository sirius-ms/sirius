package de.unijena.bionf.fastcosine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Iterator;

/**
 * A spectrum processed in a way that fast cosine calculation can be performed
 */
public class SearchPreparedSpectrum implements OrderedSpectrum<Peak>, Serializable {

    protected final float[] intensities;
    protected final double[] mz;
    @Getter
    private final double parentMass;
    @Getter
    private final float parentIntensity; // not used for cosine. Set to 0 if no parent peak in spectrum

    @JsonCreator
    SearchPreparedSpectrum(@JsonProperty("parentMass")  double parentMass,
                           @JsonProperty("parentIntensity") float parentIntensity,
                           @JsonProperty("mz") double[] mz,
                           @JsonProperty("intensities") float[] intensities
    ) {
        this.parentMass = parentMass;
        this.parentIntensity = parentIntensity;
        this.mz = mz;
        this.intensities = intensities;
    }

    @Override
    public double getMzAt(int index) {
        return mz[index];
    }

    @Override
    public double getIntensityAt(int index) {
        return intensities[index];
    }

    @Override
    public Peak getPeakAt(int index) {
        return new SimplePeak(mz[index], intensities[index]);
    }

    @Override
    public int size() {
        return mz.length;
    }

    @NotNull
    @Override
    public Iterator<Peak> iterator() {
        return new Iterator<Peak>() {
            int index = 0;
            int size = size();
            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public Peak next() {
                return getPeakAt(index++);
            }
        };
    }
}
