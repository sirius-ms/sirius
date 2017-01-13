package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

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

    private static final String[] defaultElementSymbols = new String[]{"C", "H", "N", "O", "P"};
    private final Element[] defaultElements;
    private JCheckBox sulfur, bromine, boron, selenium, chlorine, iodine, fluorine;
    private JTextField elementTF;
    private JButton elementButton;
    private HashMap<Element, JCheckBox> element2Checkbox;

    private TreeSet<String> additionalElements;

    public ElementsPanel(Window owner){
        this.owner = owner;
        additionalElements = new TreeSet<>();

        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "elements beside CHNOP"));
        this.setLayout(new BoxLayout(this,BoxLayout.LINE_AXIS));

        JPanel mainP = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
        this.add(mainP);

        JPanel elements = new JPanel();
        elements.setLayout(new BoxLayout(elements,BoxLayout.LINE_AXIS));

        sulfur = new JCheckBox("sulfur");
        bromine = new JCheckBox("bromine");
        boron = new JCheckBox("boron");
        selenium = new JCheckBox("selenium");
        chlorine = new JCheckBox("chlorine");
        iodine = new JCheckBox("iodine");
        fluorine = new JCheckBox("fluorine");

        elements.add(sulfur);
        elements.add(bromine);
        elements.add(boron);
        elements.add(chlorine);
        elements.add(fluorine);
        elements.add(iodine);
        elements.add(selenium);
        sulfur.setSelected(true);
        mainP.add(elements);

        element2Checkbox = new HashMap<>();
        final PeriodicTable T = PeriodicTable.getInstance();
        defaultElements = new Element[defaultElementSymbols.length];
        for (int i = 0; i < defaultElementSymbols.length; i++) {
            defaultElements[i] = T.getByName(defaultElementSymbols[i]);

        }
        element2Checkbox.put(T.getByName("S"), sulfur);
        element2Checkbox.put(T.getByName("Br"), bromine);
        element2Checkbox.put(T.getByName("B"), boron);
        element2Checkbox.put(T.getByName("Cl"), chlorine);
        element2Checkbox.put(T.getByName("I"), iodine );
        element2Checkbox.put(T.getByName("F"), fluorine);
        element2Checkbox.put(T.getByName("Se"), selenium);

        elementTF = new JTextField(10);
        elementTF.setEditable(false);
        elementButton = new JButton("More elements");
        elementButton.addActionListener(this);

        JPanel elements2 = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        elements2.add(elementTF);
        elements2.add(elementButton);
        mainP.add(elements2);

    }

    public FormulaConstraints getElementConstraints(){
        HashSet<String> eles = new HashSet<>();
        if (sulfur.isSelected()) eles.add("S");
        if (boron.isSelected()) eles.add("B");
        if (bromine.isSelected()) eles.add("Br");
        if (chlorine.isSelected()) eles.add("Cl");
        if (fluorine.isSelected()) eles.add("F");
        if (iodine.isSelected()) eles.add("I");
        if (selenium.isSelected()) eles.add("Se");
        eles.addAll(additionalElements);
        Element[] elems = Arrays.copyOf(defaultElements, defaultElements.length+eles.size());
        int k = 0;
        final PeriodicTable tb = PeriodicTable.getInstance();
        for (String s : eles) {
            final Element elem = tb.getByName(s);
            if (elem != null)
                elems[defaultElementSymbols.length+k++] = elem;
        }
        if (k+defaultElements.length < elems.length) elems = Arrays.copyOf(elems, k+defaultElements.length);

        return new FormulaConstraints(new ChemicalAlphabet(elems));

    }
    

    
    private void excludeAndSetElements(TreeSet<String> elements){
        if (elements.contains("S")) {
            sulfur.setSelected(true);
            elements.remove("S");
        } else {
            sulfur.setSelected(false);
        }
        if (elements.contains("B")) {
            boron.setSelected(true);
            elements.remove("B");
        } else {
            boron.setSelected(false);
        }
        if (elements.contains("Br")) {
            bromine.setSelected(true);
            elements.remove("Br");
        } else {
            bromine.setSelected(false);
        }
        if (elements.contains("Cl")) {
            chlorine.setSelected(true);
            elements.remove("Cl");
        } else {
            chlorine.setSelected(false);
        }
        if (elements.contains("F")) {
            fluorine.setSelected(true);
            elements.remove("F");
        } else {
            fluorine.setSelected(false);
        }
        if (elements.contains("I")) {
            iodine.setSelected(true);
            elements.remove("I");
        } else {
            iodine.setSelected(false);
        }
        if (elements.contains("Se")) {
            selenium.setSelected(true);
            elements.remove("Se");
        } else {
            selenium.setSelected(false);
        }
    }
    
    public void setSelectedElements(Set<String> eles){
        additionalElements = new TreeSet<>(eles);
        excludeAndSetElements(additionalElements);
    }


    public void enableElementSelection(boolean enabled) {
        if (enabled) {
            for (JCheckBox b : Arrays.asList(sulfur, boron, bromine, chlorine, fluorine, iodine, selenium)) {
                b.setEnabled(true);
            }
            elementButton.setEnabled(true);
            elementTF.setEnabled(true);
        } else {
            for (JCheckBox b : Arrays.asList(sulfur, boron, bromine, chlorine, fluorine, iodine, selenium)) {
                b.setEnabled(false);
            }
            elementButton.setEnabled(false);
            elementTF.setEnabled(false);
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.elementButton) {
            HashSet<String> eles = new HashSet<>();
            if (sulfur.isSelected()) eles.add("S");
            if (boron.isSelected()) eles.add("B");
            if (bromine.isSelected()) eles.add("Br");
            if (chlorine.isSelected()) eles.add("Cl");
            if (fluorine.isSelected()) eles.add("F");
            if (iodine.isSelected()) eles.add("I");
            if (selenium.isSelected()) eles.add("Se");
            eles.addAll(additionalElements);
            AdditionalElementDialog diag = new AdditionalElementDialog(owner, eles);
            if (diag.successful()) {
                additionalElements = new TreeSet<>(diag.getSelectedElements());
                excludeAndSetElements(additionalElements);
                StringBuilder newText = new StringBuilder();

                Iterator<String> it = additionalElements.iterator();
                while (it.hasNext()) {
                    newText.append(it.next());
                    if (it.hasNext()) newText.append(",");
                }
                elementTF.setText(newText.toString());


            }
        }
    }
}
