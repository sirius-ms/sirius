/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.sirius.elementdetection.prediction;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class DNNRegressionPredictor implements ElementPredictor {

    protected TrainedElementDetectionNetwork[] networks;
    protected ChemicalAlphabet alphabet;
    protected double[] modifiers;

    public DNNRegressionPredictor() {
        this.networks = readNetworks();
        this.modifiers = new double[DETECTABLE_ELEMENTS.length];
        Arrays.fill(modifiers, 0.33);
        setModifiers("S", 1d);
        setModifiers("Si", 1d);
        this.alphabet = new ChemicalAlphabet(DETECTABLE_ELEMENTS);
    }

    public void setModifiers(double modifier) {
        Arrays.fill(modifiers, modifier);
    }

    public void setModifiers(String symbol, double modifier) {
        setModifier(PeriodicTable.getInstance().getByName(symbol), modifier);
    }

    public void setModifier(Element element, double threshold) {
        for (int i=0; i < DETECTABLE_ELEMENTS.length; ++i) {
            if (DETECTABLE_ELEMENTS[i].equals(element)) {
                modifiers[i] = threshold;
                return;
            }
        }
        throw new IllegalArgumentException(element.getSymbol() + " is not predictable");
    }

    private static TrainedElementDetectionNetwork[] readNetworks() {
        try {
            final TrainedElementDetectionNetwork fivePeaks = TrainedElementDetectionNetwork.readRegressionNetwork(DNNElementPredictor.class.getResourceAsStream("/regression5.param"));
            final TrainedElementDetectionNetwork fourPeaks = TrainedElementDetectionNetwork.readRegressionNetwork(DNNElementPredictor.class.getResourceAsStream("/regression4.param"));
            final TrainedElementDetectionNetwork threePeaks = TrainedElementDetectionNetwork.readRegressionNetwork(DNNElementPredictor.class.getResourceAsStream("/regression3.param"));
            return new TrainedElementDetectionNetwork[]{fivePeaks, fourPeaks, threePeaks};
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final static Element[] DETECTABLE_ELEMENTS;
    private final static int[] UPPERBOUNDS;
    private final static Element SELENE;

    static {
        PeriodicTable T = PeriodicTable.getInstance();
        DETECTABLE_ELEMENTS = new Element[]{
                T.getByName("B"),
                T.getByName("Br"),
                T.getByName("Cl"),
                T.getByName("S"),
                T.getByName("Si"),
                T.getByName("Se"),
        };
        UPPERBOUNDS = new int[]{2,5,5,10,2,2};
        SELENE = T.getByName("Se");
    }

    @Override
    public FormulaConstraints predictConstraints(SimpleSpectrum pickedPattern) {
        final HashMap<Element, Integer> elements = new HashMap<>(10);
        // special case for selene
        if (pickedPattern.size() > 5) {
            double intensityAfterFifth = 0d;
            for (int i=pickedPattern.size()-1; i >= 5; --i) {
                intensityAfterFifth += pickedPattern.getIntensityAt(i);
            }
            double intensityBeforeFifth = 0d;
            for (int i=0; i < 5; ++i) {
                intensityBeforeFifth += pickedPattern.getIntensityAt(i);
            }
            intensityAfterFifth /= intensityBeforeFifth;
            if (intensityAfterFifth > 0.25) elements.put(SELENE, 1);
        }
        for (TrainedElementDetectionNetwork network : networks) {
            if (network.numberOfPeaks() <= pickedPattern.size() ) {
                final double[] prediction = network.predict(pickedPattern);
                for (int i=0; i < prediction.length; ++i) {
                    final Element e = DETECTABLE_ELEMENTS[i];
                    int number = (int)Math.ceil(prediction[i]-0.22);
                    if (number > 0) number = (int)Math.ceil(prediction[i]+modifiers[i]);
                    if (elements.containsKey(e)) elements.put(e, Math.max(elements.get(e), number));
                    else elements.put(e, number);
                }
                break;
            }
        }
        {
            final Iterator<Element> iter = elements.keySet().iterator();
            while (iter.hasNext()) {
                if (elements.get(iter.next())<=0) iter.remove();
            }
        }
        if (!silicon) elements.remove(PeriodicTable.getInstance().getByName("Si"));
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(elements.keySet().toArray(new Element[elements.size()]));
        final FormulaConstraints constraints = new FormulaConstraints(alphabet);
        for (int i=0; i < UPPERBOUNDS.length; ++i) {
            if (elements.containsKey(DETECTABLE_ELEMENTS[i]))
                constraints.setUpperbound(DETECTABLE_ELEMENTS[i], elements.get(DETECTABLE_ELEMENTS[i]));
        }
        return constraints;
    }

    @Override
    public ChemicalAlphabet getChemicalAlphabet() {
        return alphabet;
    }

    @Override
    public boolean isPredictable(Element element) {
        if (element.getSymbol().equals("Si") && !silicon) return false;
        for (Element detectable : DETECTABLE_ELEMENTS) {
            if (detectable.equals(element)) return true;
        }
        return false;
    }

    // workaround
    boolean silicon=true;
    public void disableSilicon() {
        silicon = false;
    }
    public void enableSilicon() {
        silicon = true;
    }
}
