package de.unijena.bioinf.sirius.gui.settings;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.sirius.gui.utils.TwoCloumnPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Properties;

public class AdductSettingsPanel extends TwoCloumnPanel implements SettingsPanel {
    private Properties props;
    private boolean restart = false;
    private final DefaultListModel<String> positiveList = new DefaultListModel<>();
    private final DefaultListModel<String> negativeList = new DefaultListModel<>();

    public AdductSettingsPanel(Properties props) {
        this.props = props;
        // fill lists from periodic table
        for (PrecursorIonType ionType : PeriodicTable.getInstance().getIons()) {
            if (ionType.getCharge() > 0) {
                positiveList.addElement(ionType.toString());
            } else {
                negativeList.addElement(ionType.toString());
            }
        }

        // create adduct lists
        JPanel lists = new JPanel(new BorderLayout());
        lists.add(createListPanel(positiveList, "Positive"), BorderLayout.WEST);
        lists.add(createListPanel(negativeList, "Negative"), BorderLayout.EAST);
        add(lists);

        //ceate custom adduct field
        JTextField text = new JTextField();
        JButton addButton = new JButton("add");
        addButton.addActionListener(event -> {
            String toAdd = text.getText();
            try {
                PrecursorIonType ionType = PeriodicTable.getInstance().ionByName(toAdd);
                if (ionType != null && !ionType.isIntrinsicalCharged()) {
                    if (ionType.getCharge() > 0)
                        positiveList.addElement(ionType.toString());
                    else
                        negativeList.addElement(ionType.toString());
                } else {
                    throw new IllegalArgumentException("Illegal IonType");
                }
            } catch (Exception e) {
                new ExceptionDialog(MainFrame.MF, "Invalid Adduct Syntax");
            }
        });

        JPanel addPanel = new JPanel(new BorderLayout());
        addPanel.add(text, BorderLayout.CENTER);
        addPanel.add(addButton, BorderLayout.EAST);
        add(new JLabel("Custom Adduct:"), addPanel);
    }

    private <E> String makeStringRepresentation(DefaultListModel<E> model) {
        if (model.isEmpty()) return "";

        StringBuilder builder = new StringBuilder();
        Enumeration<E> e = model.elements();
        builder.append(e.nextElement());
        while (e.hasMoreElements()) {
            builder.append(',').append(e.nextElement());
        }

        return builder.toString();
    }

    @Override
    public void saveProperties() {
        props.setProperty("de.unijena.bioinf.sirius.chem.adducts.positive",
                makeStringRepresentation(positiveList));

        props.setProperty("de.unijena.bioinf.sirius.chem.adducts.negative",
                makeStringRepresentation(negativeList));
    }

    @Override
    public void reloadChanges() {
        PeriodicTable.getInstance().loadKnownIonTypes();
    }

    @Override
    public String name() {
        return "Adducts";
    }


    private <E> JPanel createListPanel(final DefaultListModel<E> model, final String header) {
        TextHeaderBoxPanel l = new TextHeaderBoxPanel(header);
        JList<E> jList = new JList<>(model);
        JButton remove = new JButton("Remove");
        remove.addActionListener(e -> {
            for (int index : jList.getSelectedIndices()) {
                model.removeElementAt(index);
            }
        });

        l.add(new JScrollPane(jList));
        l.add(remove);
        return l;
    }
}
