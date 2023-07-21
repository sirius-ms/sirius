package de.unijena.bioinf.cmlSpectrumPrediction;


import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.cmlFragmentation.FragmentationPredictor;
import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.CombinatorialFragmenter;
import de.unijena.bioinf.fragmenter.MolecularGraph;

import java.util.HashMap;
import java.util.Map;


public abstract class AbstractMs2SpectrumPredictor<P extends Peak> implements SpectrumPredictor<P, Ms2Spectrum<P>>{

    protected final PrecursorIonType precursorIonType;
    protected final FragmentationPredictor fragPredictor;
    protected Ms2Spectrum<P> spectrum;
    protected HashMap<P, CombinatorialFragment> peak2fragment;

    public AbstractMs2SpectrumPredictor(FragmentationPredictor fragPredictor, PrecursorIonType precursorIonType){
        this.fragPredictor = fragPredictor;
        this.precursorIonType = precursorIonType;
        this.peak2fragment = new HashMap<>();
    }

    public abstract Ms2Spectrum<P> predictSpectrum();

    public Ms2Spectrum<P> getSpectrum(){
        return this.spectrum;
    }

    public HashMap<P, CombinatorialFragment> getPeak2FragmentMapping(){
        return this.peak2fragment;
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
