package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.fingerid.CSIFingerIdComputation;
import de.unijena.bioinf.fingerid.db.CustomDatabase;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.sirius.gui.utils.TwoCloumnPanel;
import de.unijena.bioinf.sirius.gui.utils.jCheckboxList.JCheckBoxList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by Marcus Ludwig on 12.01.17.
 */
public class SearchProfilePanel extends JPanel {

    protected Logger logger = LoggerFactory.getLogger(SearchProfilePanel.class);

    public enum Instruments {
        QTOF("Q-TOF", "qtof", 10),
        ORBI("Orbitrap", "orbitrap", 5),
        FTICR("FT-ICR", "fticr", 2)

        //,QTOF_FIXED("Q-TOF (fixed)", "qtof_fixed", 10)

        ////
        , BRUKER("Q-TOF (isotopes)", "bruker_tof", 10)
        //,EXP1("Exp1", "exp", 10),
        //EXP2("Exp2", "exp2", 10)
        ;

        final String name, profile;
        final int ppm;

        Instruments(String name, String profile, int ppm) {
            this.name = name;
            this.profile = profile;
            this.ppm = ppm;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private Window owner;

    private Vector<String> ionizations;
    private Vector<Instruments> instruments;
    JCheckBoxList<String> ionizationCB;
    public final JComboBox<String> formulaCombobox;
    private JComboBox<Instruments> instrumentCB;
    private JSpinner ppmSpinner;
    private SpinnerNumberModel snm;
    private final JSpinner candidatesSpinner;


    public SearchProfilePanel(final Window owner, boolean enableFallback) {
        this(owner, null, false, enableFallback);
    }

    public SearchProfilePanel(final Window owner, PrecursorIonType ionType) {
        this(owner, ionType, true, false);
    }

    private SearchProfilePanel(final Window owner, PrecursorIonType ionType, boolean selectIonization, boolean enableFallback) {
        this.owner = owner;

        JPanel mainwindow;
        if (!selectIonization) {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Other"));

            JPanel otherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            mainwindow = otherPanel;
            this.add(mainwindow);

        } else {
            this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Other"));
            mainwindow = this;
        }

        //configure ionization panels
        ionizations = new Vector<>();

        if (ionType != null) {
            if (ionType.isIonizationUnknown()) {
                if (ionType.getCharge() > 0) {
                    ionizations.addAll(PeriodicTable.getInstance().getPositiveIonizations());
                } else {
                    ionizations.addAll(PeriodicTable.getInstance().getNegativeIonizations());
                }
            } else {
                String io = ionType.getIonization().toString();
                ionizations.add(io);
            }
        } else {
            ionizations.addAll(PeriodicTable.getInstance().getIonizations());
        }


        ionizationCB = new JCheckBoxList<>(ionizations);
        ionizationCB.checkAll();
        JPanel ionPanel;
        if (selectIonization) {
            ionPanel = createIonissationOptionPanel(ionizationCB);
            mainwindow.add(ionPanel);
        } else {
            if (enableFallback) {
                ionPanel = createFallbackIonissationOptionPanel(ionizationCB);
                mainwindow.add(ionPanel);
            }
        }


        instruments = new Vector<>();
        for (Instruments i : Instruments.values()) {
            instruments.add(i);
        }
        instrumentCB = new JComboBox<>(instruments);
        mainwindow.add(new TwoCloumnPanel(new JLabel("instrument"), instrumentCB));

        snm = new SpinnerNumberModel(10, 0.25, 20, 0.25);
        ppmSpinner = new JSpinner(this.snm);
        ppmSpinner.setMinimumSize(new Dimension(70, 26));
        ppmSpinner.setPreferredSize(new Dimension(70, 26));
        mainwindow.add(new TwoCloumnPanel(new JLabel("ppm"), ppmSpinner));

        final SpinnerNumberModel candidatesNumberModel = new SpinnerNumberModel(10, 1, 1000, 1);
        candidatesSpinner = new JSpinner(candidatesNumberModel);
        candidatesSpinner.setMinimumSize(new Dimension(70, 26));
        candidatesSpinner.setPreferredSize(new Dimension(70, 26));
        mainwindow.add(new TwoCloumnPanel(new JLabel("candidates"), candidatesSpinner));

        instrumentCB.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                final Instruments i = (Instruments) e.getItem();
                final double recommendedPPM = i.ppm;

                ppmSpinner.setValue(new Double(recommendedPPM)); // TODO: test
            }
        });


        //////////
        {
            JLabel label = new JLabel("Consider ");
            final Vector<String> values = new Vector<>();
            values.add("all molecular formulas");
            values.add("all PubChem formulas");
            values.add("organic PubChem formulas");
            values.add("formulas from Bio databases");

            for (CustomDatabase customDatabase : CustomDatabase.customDatabases(true)) {
                values.add(customDatabase.name());
            }

            formulaCombobox = new JComboBox<>(values);
            mainwindow.add(new TwoCloumnPanel(label, formulaCombobox));
        }
    }


    public Instruments getInstrument() {
        return (Instruments) instrumentCB.getSelectedItem();
    }

    public boolean hasIsotopesEnabled() {
        return getInstrument() == Instruments.BRUKER;
    }

    public String getIonization() {
        ArrayList<String> it = ionizationCB.getCheckedItems();
        return it == null ? null : it.get(0);
    }

    public double getPpm() {
        return snm.getNumber().doubleValue();
    }

    public int getNumberOfCandidates() {
        return ((Number) candidatesSpinner.getModel().getValue()).intValue();
    }

    public SearchableDatabase getFormulaSource() {
        final CSIFingerIdComputation csi = MainFrame.MF.getCsiFingerId();
        if (formulaCombobox.getSelectedIndex() == 0) return null;
        else if (formulaCombobox.getSelectedIndex() <= 2) return csi.getPubchemDb();
        else if (formulaCombobox.getSelectedIndex() == 3) return csi.getBioDb();
        else {
            final String name = (String) formulaCombobox.getSelectedItem();
            for (CustomDatabase customDatabase : CustomDatabase.customDatabases(true)) {
                if (customDatabase.name().equals(name)) return customDatabase;
            }
            logger.error("Unknown database '" + name + "' selected.");
            return null;
        }
    }

    public boolean restrictToOrganics() {
        return formulaCombobox.getSelectedIndex() == 2; // TODO: add checkbox instead
    }


    private JPanel createIonissationOptionPanel(final JCheckBoxList<String> ionizationCB) {
        JPanel main = new TextHeaderBoxPanel("Ionization");
        main.add(ionizationCB);

        if (ionizations.size() > 1) {
            JButton all = new JButton("all");
            all.addActionListener(e -> ionizationCB.checkAll());
            JButton none = new JButton("none");
            none.addActionListener(e -> ionizationCB.uncheckAll());
            JPanel buttons = new JPanel(new BorderLayout());
            buttons.add(all, BorderLayout.WEST);
            buttons.add(none, BorderLayout.EAST);
            main.add(buttons);
        }

        return main;
    }

    private JPanel createFallbackIonissationOptionPanel(final JCheckBoxList<String> ionizationCB) {
        ionizationCB.setToolTipText("Set fallback ionisation for unknown adduct types");

        JButton all = new JButton("all");
        all.addActionListener(e -> ionizationCB.checkAll());
        JButton pos = new JButton("+");
        pos.addActionListener(e -> {
            ionizationCB.uncheckAll();
            ionizationCB.checkAll(PeriodicTable.getInstance().getPositiveIonizations());
        });
        JButton neg = new JButton("-");
        neg.addActionListener(e -> {
            ionizationCB.uncheckAll();
            ionizationCB.checkAll(PeriodicTable.getInstance().getNegativeIonizations());
        });
        JButton none = new JButton("none");
        none.addActionListener(e -> ionizationCB.uncheckAll());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 1));
        buttons.add(all);
        buttons.add(pos);
        buttons.add(neg);
        buttons.add(none);

        JPanel main = new TextHeaderBoxPanel("Fallback ionisation(s)");
        main.add(ionizationCB);
        main.add(buttons);
        return main;
    }
}
