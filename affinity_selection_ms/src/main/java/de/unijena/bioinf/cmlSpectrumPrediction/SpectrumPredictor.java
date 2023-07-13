package de.unijena.bioinf.cmlSpectrumPrediction;


import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.cmlFragmentation.FragmentationPredictor;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;

public abstract class SpectrumPredictor<T extends Peak> {

    private final PrecursorIonType precursorIonType;
    private final FragmentationPredictor fragPredictor;
    private Ms2Spectrum<T> spectrum;


    public SpectrumPredictor(FragmentationPredictor fragPredictor, PrecursorIonType precursorIonType){
        this.fragPredictor = fragPredictor;
        this.precursorIonType = precursorIonType;
    }

    public abstract Ms2Spectrum<T> predictSpectrum();

    public FragmentationPredictor getFragmentationPredictor(){
        return this.fragPredictor;
    }

    public PrecursorIonType getPrecursorIonType(){
        return this.precursorIonType;
    }

}
