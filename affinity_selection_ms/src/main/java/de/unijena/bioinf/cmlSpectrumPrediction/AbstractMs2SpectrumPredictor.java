package de.unijena.bioinf.cmlSpectrumPrediction;


import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.cmlFragmentation.FragmentationPredictor;
import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.MolecularGraph;
import lombok.Getter;

import java.util.HashMap;


public abstract class AbstractMs2SpectrumPredictor<P extends Peak> implements SpectrumPredictor<P, Ms2Spectrum<P>>{

    /**
     * The considered ion type of the precursor molecule.
     */
    @Getter
    protected final PrecursorIonType precursorIonType;

    /**
     * The fragmentation predictor for predicting the fragmentation process and thus, the fragments of the molecule.
     * The corresponding fragment masses determine where to expect a peak in the spectrum.
     */
    protected final FragmentationPredictor fragPredictor;

    /**
     * The predicted tandem mass spectrum for the given molecular structure.
     */
    @Getter
    protected Ms2Spectrum<P> spectrum;

    /**
     * A mapping which maps each peak in the predicted spectrum to the corresponding fragment.
     */
    protected HashMap<P, CombinatorialFragment> peak2fragment;

    /**
     * Constructs an initial {@link AbstractMs2SpectrumPredictor}.
     *
     *
     * @param fragPredictor the {@link FragmentationPredictor} used to predict all fragments of the molecule which
     *                      are generated during the fragmentation process during MS/MS acquisition
     * @param precursorIonType the considered ion type of the precursor molecule
     */
    public AbstractMs2SpectrumPredictor(FragmentationPredictor fragPredictor, PrecursorIonType precursorIonType){
        this.fragPredictor = fragPredictor;
        this.precursorIonType = precursorIonType;
        this.peak2fragment = new HashMap<>();
    }

    /**
     * This method predicts a tandem mass spectrum for a given molecular structure.<br>
     * Note that the method {@link FragmentationPredictor#predictFragmentation()} of the fragmentation predictor
     * must be called first.
     *
     * @return the predicted MS/MS spectrum for the molecular structure of interest
     */
    public abstract Ms2Spectrum<P> predictSpectrum();

    public HashMap<P, CombinatorialFragment> getPeak2FragmentMapping(){
        return this.peak2fragment;
    }

    public FragmentationPredictor getFragmentationPredictor(){
        return this.fragPredictor;
    }

    public MolecularGraph getPrecursorMolecule(){
        return this.fragPredictor.getMolecule();
    }

}
