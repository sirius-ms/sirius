package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.sirius.gui.utils.SliderWithTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Created by Marcus Ludwig on 12.01.17.
 */
public class ElementsPanel extends JPanel implements ActionListener {

    private Window owner;

    private static final int maxNumberOfOneElements = 10;
    private static final String[] defaultElementSymbols = new String[]{"C", "H", "N", "O", "P"};
    private final Element[] defaultElements;

    private JButton elementButton;
    private HashMap<String, ElementSlider> element2Slider;
    private JPanel elementsPanel;
    private JScrollPane scrollPane;
    private JPanel mainP;

    private final PeriodicTable periodicTable;


    public ElementsPanel(Window owner, int columns){
        this.owner = owner;

        periodicTable = PeriodicTable.getInstance();
        defaultElements = new Element[defaultElementSymbols.length];
        for (int i = 0; i < defaultElementSymbols.length; i++) {
            defaultElements[i] = periodicTable.getByName(defaultElementSymbols[i]);

        }

        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "elements beside CHNOP"));
        this.setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));

        mainP = new JPanel();
        mainP.setLayout(new BoxLayout(mainP,BoxLayout.Y_AXIS));
        this.add(mainP);


        elementsPanel = new JPanel(new GridLayout(0,columns));
        element2Slider = new HashMap<>();

        Element sulfur = PeriodicTable.getInstance().getByName("S");
        ElementSlider elementSlider = new ElementSlider(sulfur, maxNumberOfOneElements, maxNumberOfOneElements);
        elementsPanel.add(elementSlider.slider);

        element2Slider.put(sulfur.getSymbol(), elementSlider);

        scrollPane = new JScrollPane(elementsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(elementsPanel.getPreferredSize().width, 2*elementsPanel.getPreferredSize().height));
        scrollPane.setSize(new Dimension(elementsPanel.getPreferredSize().width, 2*elementsPanel.getPreferredSize().height));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
;
        mainP.add(scrollPane);

        elementButton = new JButton("Add elements");
        elementButton.addActionListener(this);

        JPanel elements2 = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        elements2.add(elementButton);
        mainP.add(elements2);

    }

    public FormulaConstraints getElementConstraints(){
        Element[] elems = Arrays.copyOf(defaultElements, defaultElements.length+element2Slider.size());
        int k = 0;
        for (String s : element2Slider.keySet()) {
            final Element elem = periodicTable.getByName(s);
            if (elem != null)
                elems[defaultElementSymbols.length+k++] = elem;
        }
        if (k+defaultElements.length < elems.length) elems = Arrays.copyOf(elems, k+defaultElements.length);

        FormulaConstraints formulaConstraints =  new FormulaConstraints(new ChemicalAlphabet(elems));
        for (String s : element2Slider.keySet()) {
            int max = element2Slider.get(s).getMax();
            if (max!= maxNumberOfOneElements) formulaConstraints.setUpperbound(periodicTable.getByName(s), max);
        }
        return formulaConstraints;
    }

    
    public void setSelectedElements(Set<String> selected){
        Set<String> selectedNoDefaults = new HashSet<>(selected);
        for (String symbol : defaultElementSymbols) selectedNoDefaults.remove(symbol);

        Set<String> current = new HashSet<>(element2Slider.keySet());
        for (String symbol : current) {
            if (!selectedNoDefaults.contains(symbol)){
                elementsPanel.remove(element2Slider.get(symbol).slider);
                element2Slider.remove(symbol);
            }
        }
        for (String symbol : selectedNoDefaults) {
            if (!element2Slider.containsKey(symbol)){
                Element ele = periodicTable.getByName(symbol);
                ElementSlider slider = new ElementSlider(ele, maxNumberOfOneElements, maxNumberOfOneElements);
                element2Slider.put(ele.getSymbol(), slider);
                elementsPanel.add(slider.slider);
            }
        }

        owner.revalidate();
        owner.repaint();
    }


    public void enableElementSelection(boolean enabled) {
        for (ElementSlider elementSlider : element2Slider.values()) {
            elementSlider.slider.setEnabled(enabled);
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

    private class ElementSlider{
        private Element element;
        private SliderWithTextField slider;
        public ElementSlider(Element element, int min, int max){
            this.element = element;
            //// TODO: range upper value not working
            this.slider = new SliderWithTextField(element.getSymbol(), 0, maxNumberOfOneElements, max, -1);
        }

        int getMin(){
            return slider.getMinValue();
        }

        int getMax(){
            return slider.getMaxValue();
        }
    }
}
