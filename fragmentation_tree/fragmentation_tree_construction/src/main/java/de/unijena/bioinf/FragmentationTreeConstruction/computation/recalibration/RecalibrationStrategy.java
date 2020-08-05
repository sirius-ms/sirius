
package de.unijena.bioinf.FragmentationTreeConstruction.computation.recalibration;

import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import org.apache.commons.math3.analysis.UnivariateFunction;

/**
 * A recalibration strategy. Computes a recalibration function from a measured and a reference spectrum.
 */
public interface RecalibrationStrategy {

    UnivariateFunction recalibrate(MutableSpectrum<Peak> spectrum, Spectrum<Peak> referenceSpectrum);

}
