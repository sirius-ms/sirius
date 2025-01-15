package de.unijena.bionf.fastcosine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Iterator;

// A spectrum processed in a way that fast cosine calculation can be performed
public class ReferenceLibrarySpectrum implements OrderedSpectrum<Peak>, Serializable {

    protected final float[] intensities;
    protected final double[] mz;
    private final double parentMass;

    @JsonCreator ReferenceLibrarySpectrum(@JsonProperty("parentMass")  double parentMass, @JsonProperty("mz") double[] mz,  @JsonProperty("intensities") float[] intensities) {
        this.parentMass = parentMass;
        this.mz = mz;
        this.intensities = intensities;
    }

    public double getParentMass() {
        return parentMass;
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
