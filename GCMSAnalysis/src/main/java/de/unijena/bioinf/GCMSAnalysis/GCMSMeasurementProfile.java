package de.unijena.bioinf.GCMSAnalysis;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.EIIntensityDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MeasurementProfile for EI uses EIIntensityDeviation and introduces special elements like halogens, TMS or PFB if necessary.
 */
public class GCMSMeasurementProfile implements MeasurementProfile{

    private final double smallErrorPpm = 7;
    private final double largeErrorPPm = 25;
    private final double smallAbsError = 5e-3;
    private final double largeAbsError = 3e-2;
    private FormulaConstraints constraints;

    public GCMSMeasurementProfile(boolean useHalogens, boolean useChlorine, boolean usePFB, boolean useTMS) {
        PeriodicTable periodicTable =  PeriodicTable.getInstance();
        //introduce new Elements
        List<Element> usedElements = new ArrayList<Element>(Arrays.asList(periodicTable.getAllByName("C", "H", "N", "O", "P", "S")));
        if (useHalogens){
            usedElements.addAll(Arrays.asList(periodicTable.getAllByName("Br", "F", "I", "Cl")));
        }
        if (useChlorine && !useHalogens){
            usedElements.add(periodicTable.getByName("Cl"));
        }

        if (usePFB){
            usedElements.add(periodicTable.getByName("Pfb"));
        }
        if (useTMS){
            usedElements.add(periodicTable.getByName("Tms"));
            usedElements.add(periodicTable.getByName("Dms"));
        }

        for (Element usedElement : usedElements) {
            System.out.println("#"+usedElement.getSymbol());
        }

        ChemicalAlphabet simpleAlphabet = new ChemicalAlphabet(usedElements.toArray(new Element[0]));

        this.constraints = new FormulaConstraints(simpleAlphabet);

        //todo really wanna use this strict filter????
        FormulaFilter filter = new FormulaFilter() {
            @Override
            public boolean isValid(MolecularFormula formula) {
                if (formula.rdbe()>=0) return true;
                return false;
            }
        };
        constraints.addFilter(filter);

        //constraints.addFilter(new ValenceFilter(-0.5d));
    }

    @Override
    public EIIntensityDeviation getStandardMs1MassDeviation() {
        return new EIIntensityDeviation(smallErrorPpm, largeErrorPPm, smallAbsError, largeAbsError);
    }

    @Override
    public Deviation getAllowedMassDeviation() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Deviation getStandardMassDifferenceDeviation() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Deviation getStandardMs2MassDeviation() {
        return null;
    }

    @Override
    public double getMedianNoiseIntensity() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FormulaConstraints getFormulaConstraints() {
        return constraints;
    }

    @Override
    public double getIntensityDeviation() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
