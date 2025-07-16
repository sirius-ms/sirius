/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ms.gui.utils.SliderWithTextField;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

/**
 * Created by Marcus Ludwig on 12.01.17.
 */
public class ElementsPanel extends TextHeaderBoxPanel implements ActionListener {

    private Window owner;

    private static final int maxNumberOfOneElements = 20;
    private static final String[] additionalElementSymbols = new String[]{"C", "H", "N", "O", "P", "S", "B", "Br", "Cl", "F", "I", "Se", "Si", "Fe", "Mg"};

    public final boolean individualAutoDetect;
    private JButton elementButton;
    private HashMap<String, ElementSlider> element2Slider;
    private JPanel elementsPanel;
    private JScrollPane scrollPane;
    private JPanel mainP;
    public JPanel lowerPanel;

    private Set<Element> possDetectableElements;
    private Set<Element> enabledDetectableElements;

    private final PeriodicTable periodicTable;

    private final boolean setDetectablesOnly;

    public ElementsPanel(Window owner, int columns, FormulaConstraints enforcedElements) {
        this(owner, columns, null, null, enforcedElements, false);
    }

    public ElementsPanel(Window owner, int columns, Collection<Element> possibleDetectable, Collection<Element> enabledDetectable, @NotNull FormulaConstraints enforced, boolean setDetectablesOnly) {
        super(setDetectablesOnly ? "Detectable elements" : "Elements allowed in Molecular Formula");
        this.owner = owner;
        this.setDetectablesOnly = setDetectablesOnly;
        this.individualAutoDetect = (possibleDetectable != null && enabledDetectable != null);
        if (individualAutoDetect) {
            this.possDetectableElements = new HashSet<>(possibleDetectable);
            this.enabledDetectableElements = new HashSet<>(enabledDetectable);
        }

        periodicTable = PeriodicTable.getInstance();

        mainP = new JPanel();
        mainP.setLayout(new BoxLayout(mainP, BoxLayout.Y_AXIS));
        this.add(mainP);

        elementsPanel = new JPanel(new GridLayout(0, columns));
        element2Slider = new HashMap<>();

        if (!setDetectablesOnly) enforced.getChemicalAlphabet().getElements().stream().filter(e -> !element2Slider.containsKey(e.getSymbol()))
                .forEach(e -> addElementSlider(new ElementSlider(e, enforced.getLowerbound(e), enforced.getUpperbound(e))));

        if (individualAutoDetect) {
            possibleDetectable.stream().filter(e -> !element2Slider.containsKey(e.getSymbol())).map(e -> new ElementSlider(e, 0, 0)).
                    forEach(es -> {
                        addElementSlider(es);
                        es.setAutoDetectable(enabledDetectable.contains(es.element));
                    });
        }

        if (!setDetectablesOnly) {
            for (String symbol : additionalElementSymbols) {
                if (!element2Slider.containsKey(symbol)) {
                    Element element = PeriodicTable.getInstance().getByName(symbol);
                    ElementSlider elementSlider = new ElementSlider(element, 0, 0);
                    addElementSlider(elementSlider);
                }
            }
        }

        scrollPane = new JScrollPane(elementsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        int width = elementsPanel.getPreferredSize().width;
        int height = Math.max((int)Math.ceil(1.0*element2Slider.size()/columns),2)*element2Slider.values().iterator().next().getPanel().getPreferredSize().height;
        scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, height));
        scrollPane.setSize(new Dimension(width, height));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainP.add(scrollPane);

        elementButton = new JButton("Select additional elements");
        elementButton.addActionListener(this);

        lowerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        lowerPanel.add(elementButton);
        if (!setDetectablesOnly) mainP.add(lowerPanel);

    }

    public FormulaConstraints getElementConstraints(){
        Element[] elems = new Element[element2Slider.size()];
        int k = 0;
        for (String s : element2Slider.keySet()) {
            final Element elem = periodicTable.getByName(s);
            if (elem != null)
                elems[k++] = elem;
        }
        if (k < elems.length) elems = Arrays.copyOf(elems, k);

        FormulaConstraints formulaConstraints =  new FormulaConstraints(new ChemicalAlphabet(elems));
        for (String s : element2Slider.keySet()) {
            Element element = periodicTable.getByName(s);
            int min = element2Slider.get(s).getMin();
            int max = element2Slider.get(s).getMax();
            formulaConstraints.setLowerbound(element, min);
            if (max!= maxNumberOfOneElements) formulaConstraints.setUpperbound(element, max);
        }
        return formulaConstraints;
    }

    public List<Element> getElementsToAutoDetect(){
        //todo what if all shall be detected?
        if (individualAutoDetect){
            List<Element> elementsToDetect = new ArrayList<>();
            for (ElementSlider slider : element2Slider.values()) {
                if (slider.isAutoDetectable()){
                    elementsToDetect.add(slider.element);
                }
            }
            return elementsToDetect;
        } else {
            throw new RuntimeException("individual auto detect selection is not enabled");
        }
    }

    
    public void setSelectedElements(Set<String> selected){
        Set<String> selectedNoDefaults = new HashSet<>(selected);

        Set<String> current = new HashSet<>(element2Slider.keySet());
        for (String symbol : current) {
            if (!selectedNoDefaults.contains(symbol)){
                removeElementSlider(symbol);
            }
        }
        for (String symbol : selectedNoDefaults) {
            if (!element2Slider.containsKey(symbol)){
                Element ele = periodicTable.getByName(symbol);
                ElementSlider slider = new ElementSlider(ele, 0, maxNumberOfOneElements);
                addElementSlider(slider);
            }
        }

        owner.revalidate();
        owner.repaint();
    }


    public void setSelectedElements(FormulaConstraints constraints){
        Set<Element> selectedNoDefaults = new HashSet<>(constraints.getChemicalAlphabet().getElements());
        for (Element element : constraints.getChemicalAlphabet().getElements())
            if (constraints.getUpperbound(element)==0) selectedNoDefaults.remove(element);

        Set<String> current = new HashSet<>(element2Slider.keySet());
        for (String symbol : current) {
            if (!selectedNoDefaults.contains(symbol)){
                Element element = periodicTable.getByName(symbol);
                element2Slider.get(symbol).slider.setLowerValue(constraints.getLowerbound(element));
                element2Slider.get(symbol).slider.setUpperValue(constraints.getUpperbound(element));
            }
        }
        for (Element ele : selectedNoDefaults) {
            if (!element2Slider.containsKey(ele.getSymbol())){
                int min = constraints.getLowerbound(ele);
                int max = Math.min(maxNumberOfOneElements, constraints.getUpperbound(ele));
                ElementSlider slider = new ElementSlider(ele, min, max);
                addElementSlider(slider);
            }
        }

        owner.revalidate();
        owner.repaint();
    }

    private void addElementSlider(ElementSlider slider){
        element2Slider.put(slider.element.getSymbol(), slider);
        elementsPanel.add(slider.getPanel());
    }

    private void removeElementSlider(String symbol){
        elementsPanel.remove(element2Slider.get(symbol).getPanel());
        element2Slider.remove(symbol);
    }


    public void enableElementSelection(boolean enabled) {
        for (ElementSlider elementSlider : element2Slider.values()) {
            elementSlider.setEnabled(enabled);
        }
        elementButton.setEnabled(enabled);
        elementsPanel.setEnabled(enabled);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.elementButton) {
            Set<String> selectedElements = new HashSet<>(element2Slider.keySet());
            AdditionalElementDialog diag = new AdditionalElementDialog(owner, selectedElements);
            if (diag.successful()) {
                Set<String> newSelected = new HashSet<>(diag.getSelectedElements());
                setSelectedElements(newSelected);
            }
        }
    }

    private class ElementSlider implements ActionListener{
        private Element element;
        private SliderWithTextField slider;
        private JCheckBox checkBox;
        private JPanel panel;
        public ElementSlider(Element element, int min, int max){
            this.element = element;
            this.slider = new SliderWithTextField(element.getSymbol(), 0, maxNumberOfOneElements, min, max);
            Dimension dimension = slider.nameLabel.getPreferredSize();
            slider.nameLabel.setPreferredSize(new Dimension((int)(1.5*dimension.height), dimension.height));

            if (setDetectablesOnly){
                this.panel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
                JLabel symbol = new JLabel(element.getSymbol());
                panel.add(symbol);
            } else {
                this.panel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
                panel.add(slider);
            }
            if (individualAutoDetect){
                if (possDetectableElements.contains(element)) {
                    checkBox = new JCheckBox();
                    checkBox.addActionListener(this);
                    panel.add(checkBox);
                    setAutoDetectable(enabledDetectableElements.contains(element));
                }
            }
        }

        int getMin(){
            return slider.getLowerValue();
        }

        int getMax(){
            return slider.getUpperValue();
        }

        JPanel getPanel(){
            return panel;
        }

        boolean isAutoDetectable(){
            return (checkBox!=null && checkBox.isSelected());
        }


        void setEnabled(boolean enabled){
            if (!isAutoDetectable()) {
                panel.setEnabled(enabled);
                slider.setEnabled(enabled);
            } else {
                panel.setEnabled(false);
                slider.setEnabled(false);
            }

            if (checkBox!=null) checkBox.setEnabled(enabled);
        }

        void setAutoDetectable(boolean isAuto){
            checkBox.setSelected(isAuto);
            if (isAuto){
                slider.getRightTextField().setText("auto");
                slider.setEnabled(false);
            } else {
                this.slider.refreshText();
                slider.setEnabled(!setDetectablesOnly);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource()==checkBox) {
                setAutoDetectable(checkBox.isSelected());
            }
        }
    }

}
