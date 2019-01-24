package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.PossibleIonModes;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.fingerid.db.SearchableDatabases;
import de.unijena.bioinf.fingerid.db.custom.CustomDatabase;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.utils.TwoCloumnPanel;
import de.unijena.bioinf.sirius.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.sirius.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.sirius.gui.utils.jCheckboxList.JCheckboxListPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Marcus Ludwig on 12.01.17.
 */
public class SearchProfilePanel extends JPanel {

    protected Logger logger = LoggerFactory.getLogger(SearchProfilePanel.class);

    public enum Instruments {
        QTOF("Q-TOF", MsInstrumentation.Instrument.QTOF, "qtof", 10),
        ORBI("Orbitrap", MsInstrumentation.Instrument.ORBI, "orbitrap", 5),
        FTICR("FT-ICR", MsInstrumentation.Instrument.FTICR, "orbitrap", 2)

        //,QTOF_FIXED("Q-TOF (fixed)", "qtof_fixed", 10)

        ////
        , BRUKER("Q-TOF (isotopes)", MsInstrumentation.Instrument.BRUKER_MAXIS, "qtof", 10)
        //,EXP1("Exp1", "exp", 10),
        //EXP2("Exp2", "exp2", 10)
        ;

        public final String name, profile;
        public final MsInstrumentation instrument;
        public final int ppm;

        Instruments(String name, MsInstrumentation instrument, String profile, int ppm) {
            this.name = name;
            this.profile = profile;
            this.ppm = ppm;
            this.instrument = instrument;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private Window owner;

    private Vector<Instruments> instruments;
    final JCheckboxListPanel<String> ionizationPanel;
    public final JComboBox<String> formulaCombobox;
    private JComboBox<Instruments> instrumentCB;
    private JSpinner ppmSpinner;
    private SpinnerNumberModel snm;
    final JSpinner candidatesSpinner;

    public SearchProfilePanel(final Window owner, Collection<ExperimentContainer> ecs) {
        this.owner = owner;

        this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Sirius - Molecular Formula Identification"));

        //configure ionization panels
        ionizationPanel = new JCheckboxListPanel<>(new JCheckBoxList<>(), "Possible Ionizations", "Set possible ionisation for data with unknown ionization");
        ionizationPanel.checkBoxList.setPrototypeCellValue(new CheckBoxListItem<>("[M + H]+ ", false));

        add(ionizationPanel);

        instruments = new Vector<>();
        for (Instruments i : Instruments.values()) {
            instruments.add(i);
        }
        instrumentCB = new JComboBox<>(instruments);
        add(new TwoCloumnPanel(new JLabel("instrument"), instrumentCB));

        snm = new SpinnerNumberModel(10, 0.25, 20, 0.25);
        ppmSpinner = new JSpinner(this.snm);
        ppmSpinner.setMinimumSize(new Dimension(70, 26));
        ppmSpinner.setPreferredSize(new Dimension(70, 26));
        add(new TwoCloumnPanel(new JLabel("ppm"), ppmSpinner));

        final SpinnerNumberModel candidatesNumberModel = new SpinnerNumberModel(10, 1, 1000, 1);
        candidatesSpinner = new JSpinner(candidatesNumberModel);
        candidatesSpinner.setMinimumSize(new Dimension(70, 26));
        candidatesSpinner.setPreferredSize(new Dimension(70, 26));
        add(new TwoCloumnPanel(new JLabel("candidates"), candidatesSpinner));

        instrumentCB.addItemListener(e -> {
            final Instruments i = (Instruments) e.getItem();
            final double recommendedPPM = i.ppm;

            ppmSpinner.setValue(new Double(recommendedPPM)); // TODO: test
        });


        //////////
        {
            JLabel label = new JLabel("Consider ");
            final Vector<String> values = new Vector<>();
            values.add("all molecular formulas");

            values.add("all PubChem formulas");
            values.add("organic PubChem formulas");
            values.add("formulas from Bio databases");

            for (CustomDatabase customDatabase : SearchableDatabases.getCustomDatabases()) {
                values.add(customDatabase.name());
            }

            formulaCombobox = new JComboBox<>(values);
            add(new TwoCloumnPanel(label, formulaCombobox));
        }

        refreshPossibleIonizations(ecs.stream().map(it -> it.getIonization().getIonization().toString()).collect(Collectors.toSet()));
    }

    public void refreshPossibleIonizations(Set<String> ionTypes) {
        java.util.List<String> ionizations = new ArrayList<>();

        if (!ionTypes.isEmpty()) {
            if (ionTypes.contains(PrecursorIonType.unknown().getIonization().getName())) {
                ionizations.addAll(PeriodicTable.getInstance().getIonizationsAsString());
            } else {
                if (ionTypes.contains(PrecursorIonType.unknownPositive().getIonization().getName())) {
                    ionizations.addAll(PeriodicTable.getInstance().getPositiveIonizationsAsString());
                }
                if (ionTypes.contains(PrecursorIonType.unknownNegative().getIonization().getName())) {
                    ionizations.addAll(PeriodicTable.getInstance().getNegativeIonizationsAsString());
                }
            }
        }

        if (ionizations.isEmpty()) {
            ionizationPanel.checkBoxList.replaceElements(ionTypes.stream().sorted().collect(Collectors.toList()));
            ionizationPanel.checkBoxList.checkAll();
            ionizationPanel.setEnabled(false);
        } else {
            Collections.sort(ionizations);
            ionizationPanel.checkBoxList.replaceElements(ionizations);
            ionizationPanel.checkBoxList.checkAll();
            ionizationPanel.setEnabled(true);
        }
//        pack();
    }

    public Instruments getInstrument() {
        return (Instruments) instrumentCB.getSelectedItem();
    }

    public boolean hasIsotopesEnabled() {
        return getInstrument() == Instruments.BRUKER;
    }

    public PossibleIonModes getPossibleIonModes() {
        PossibleIonModes mode = new PossibleIonModes();
        for (String ioniz : ionizationPanel.checkBoxList.getCheckedItems()) {
            mode.add(ioniz, 1d);
        }
        return mode;
    }

    public double getPpm() {
        return snm.getNumber().doubleValue();
    }

    public int getNumberOfCandidates() {
        return ((Number) candidatesSpinner.getModel().getValue()).intValue();
    }

    public SearchableDatabase getFormulaSource() {
        //todo this is ugly and error prone
        if (formulaCombobox.getSelectedIndex() == 0) return null;
        else if (formulaCombobox.getSelectedIndex() <= 2) return SearchableDatabases.getPubchemDb();
        else if (formulaCombobox.getSelectedIndex() == 3) return SearchableDatabases.getBioDb();
        else {
            final String name = (String) formulaCombobox.getSelectedItem();
            for (CustomDatabase customDatabase : SearchableDatabases.getCustomDatabases()) {
                if (customDatabase.name().equals(name)) return customDatabase;
            }
            logger.error("Unknown database '" + name + "' selected.");
            return null;
        }
    }

    public boolean restrictToOrganics() {
        return formulaCombobox.getSelectedIndex() == 2; // TODO: add checkbox instead
    }
}
