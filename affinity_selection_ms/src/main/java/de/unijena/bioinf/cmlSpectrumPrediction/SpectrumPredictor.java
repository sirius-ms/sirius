package de.unijena.bioinf.cmlSpectrumPrediction;


import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.cmlFragmentation.FragmentationPredictor;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;

public abstract class SpectrumPredictor<T extends Peak> {

    private final PrecursorIonType precursorIonType;
    private final FragmentationPredictor fragPredictor;
    private Spectrum<T> spectrum;


    public SpectrumPredictor(FragmentationPredictor fragPredictor, PrecursorIonType ionType){
        this.fragPredictor = fragPredictor;
        this.precursorIonType = ionType;
    }

    public abstract Spectrum<T> predictSpectrum();

    public FragmentationPredictor getFragmentationPredictor(){
        return this.fragPredictor;
    }

    public PrecursorIonType getPrecursorIonType(){
        return this.precursorIonType;
    }

}
