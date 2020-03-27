package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Panel to configure SIRIUS Computations
 * Provides CONFIGS for SiriusSubTool
 *
 * @author Marcus Ludwig
 * @since 12.01.17
 */
public class FormulaIDConfigPanel extends JPanel {

    protected Logger logger = LoggerFactory.getLogger(FormulaIDConfigPanel.class);

    public enum Instrument {
        QTOF("Q-TOF", MsInstrumentation.Instrument.QTOF, "qtof", 10), ORBI("Orbitrap", MsInstrumentation.Instrument.ORBI, "orbitrap", 5), FTICR("FT-ICR", MsInstrumentation.Instrument.FTICR, "orbitrap", 2), BRUKER("Q-TOF (isotopes)", MsInstrumentation.Instrument.BRUKER_MAXIS, "qtof", 10);

        public final String name, profile;
        public final MsInstrumentation instrument;
        public final int ppm;

        Instrument(String name, MsInstrumentation instrument, String profile, int ppm) {
            this.name = name;
            this.profile = profile;
            this.ppm = ppm;
            this.instrument = instrument;
        }


        public String asProfile() {
            return profile;
        }

        @Override
        public String toString() {
            return name;
        }
    }


    protected final JCheckboxListPanel<String> ionizationPanel;
    protected final JCheckboxListPanel<SearchableDatabase> searchDBList;
    protected final JComboBox<Instrument> profileSelector;
    protected final JSpinner ppmSpinner;
    protected final SpinnerNumberModel snm;
    protected final JSpinner candidatesSpinner, candidatesPerIonSpinner;

    public FormulaIDConfigPanel(Collection<InstanceBean> ecs) {

        this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Sirius - Molecular Formula Identification"));

        //configure ionization panels
        ionizationPanel = new JCheckboxListPanel<>(new JCheckBoxList<>(), "Possible Ionizations", "Set possible ionisation for data with unknown ionization");
        ionizationPanel.checkBoxList.setPrototypeCellValue(new CheckBoxListItem<>("[M + Na]+ ", false));

        add(ionizationPanel);

        Vector<Instrument> instruments = new Vector<>();
        Collections.addAll(instruments, Instrument.values());

        profileSelector = new JComboBox<>(instruments);
        GuiUtils.assignParameterToolTip(profileSelector, "AlgorithmProfile");
        add(new TwoColumnPanel("Instrument", profileSelector));


        snm = new SpinnerNumberModel(10, 0.25, 20, 0.25);
        ppmSpinner = new JSpinner(this.snm);
        ppmSpinner.setMinimumSize(new Dimension(70, 26));
        ppmSpinner.setPreferredSize(new Dimension(70, 26));
        GuiUtils.assignParameterToolTip(ppmSpinner, "MS2MassDeviation.allowedMassDeviation");
        add(new TwoColumnPanel("Ms2MassDev (ppm)", ppmSpinner));

        final SpinnerNumberModel candidatesNumberModel = new SpinnerNumberModel(10, 1, 10000, 1);
        candidatesSpinner = new JSpinner(candidatesNumberModel);
        candidatesSpinner.setMinimumSize(new Dimension(70, 26));
        candidatesSpinner.setPreferredSize(new Dimension(70, 26));
        GuiUtils.assignParameterToolTip(candidatesSpinner, "NumberOfCandidates");
        add(new TwoColumnPanel("Candidates", candidatesSpinner));

        candidatesPerIonSpinner = new JSpinner(candidatesNumberModel);
        candidatesPerIonSpinner.setMinimumSize(new Dimension(70, 26));
        candidatesPerIonSpinner.setPreferredSize(new Dimension(70, 26));
        GuiUtils.assignParameterToolTip(candidatesPerIonSpinner, "NumberOfCandidatesPerIon");
        add(new TwoColumnPanel("Candidates per Ion", candidatesPerIonSpinner));

        profileSelector.addItemListener(e -> {
            final Instrument i = (Instrument) e.getItem();
            final double recommendedPPM = i.ppm;
            ppmSpinner.setValue(recommendedPPM);
        });

        searchDBList = new JCheckboxListPanel<>(new DBSelectionList(), "Consider only formulas in:");
        GuiUtils.assignParameterToolTip(searchDBList, "FormulaSearchDB");
        add(searchDBList);

        refreshPossibleIonizations(ecs.stream().map(it -> it.getIonization().getIonization().toString()).collect(Collectors.toSet()));
    }

    public void refreshPossibleIonizations(Set<String> ionTypes) {
        java.util.List<String> ionizations = new ArrayList<>();

        if (!ionTypes.isEmpty()) {

            if (ionTypes.contains(PrecursorIonType.unknownPositive().getIonization().getName())) {
                ionizations.addAll(PeriodicTable.getInstance().getPositiveIonizationsAsString());
            }
            if (ionTypes.contains(PrecursorIonType.unknownNegative().getIonization().getName())) {
                ionizations.addAll(PeriodicTable.getInstance().getNegativeIonizationsAsString());
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
    }

    public Instrument getInstrument() {
        return (Instrument) profileSelector.getSelectedItem();
    }

    public double getPpm() {
        return snm.getNumber().doubleValue();
    }

    public int getNumberOfCandidates() {
        return ((Number) candidatesSpinner.getModel().getValue()).intValue();
    }

    public List<SearchableDatabase> getFormulaSearchDB() {
        return searchDBList.checkBoxList.getCheckedItems();
    }

    /*public boolean restrictToOrganics() {
        return formulaCombobox.getSelectedIndex() == 2; // TODO: add checkbox instead
    }*/


}
