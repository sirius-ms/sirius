package de.unijena.bioinf.cmlSpectrumPrediction;


import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.cmlFragmentation.FragmentationPredictor;
import de.unijena.bioinf.fragmenter.MolecularGraph;


public abstract class AbstractMs2SpectrumPredictor<P extends Peak> implements SpectrumPredictor<P, Ms2Spectrum<P>>{

    private final PrecursorIonType precursorIonType;
    private final FragmentationPredictor fragPredictor;
    private Ms2Spectrum<P> spectrum;

    public AbstractMs2SpectrumPredictor(FragmentationPredictor fragPredictor, PrecursorIonType precursorIonType){
        this.fragPredictor = fragPredictor;
        this.precursorIonType = precursorIonType;
    }

    public abstract Ms2Spectrum<P> predictSpectrum();

    public Ms2Spectrum<P> getSpectrum(){
        return this.spectrum;
    }

    public FragmentationPredictor getFragmentationPredictor(){
        return this.fragPredictor;
    }

    public PrecursorIonType getPrecursorIonType(){
        return this.precursorIonType;
    }

    public MolecularGraph getPrecursorMolecule(){
        return this.fragPredictor.getMolecule();
    }

}
