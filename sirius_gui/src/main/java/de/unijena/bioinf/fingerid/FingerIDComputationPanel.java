package de.unijena.bioinf.fingerid;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 23.01.17.
 */

import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.sirius.gui.actions.CheckConnectionAction;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.compute.AdductSelectionList;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.utils.RelativeLayout;
import de.unijena.bioinf.sirius.gui.utils.TextHeaderBoxPanel;
import de.unijena.bioinf.sirius.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.sirius.gui.utils.jCheckboxList.JCheckBoxList;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Vector;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

//here we can show fingerid options. If it becomes to much, we can change this to a setting like tabbed pane
public class FingerIDComputationPanel extends JPanel {

    public final DBSelectionPanel dbSelectionOptions;
    public final AdductSelectionPanel adductOptions;
    private ToolbarToggleButton csiButton = null;

    public FingerIDComputationPanel(final List<SearchableDatabase> databases) {
        this(databases, null);
    }

    public FingerIDComputationPanel(final List<SearchableDatabase> databases, final JCheckBoxList<String> sourceIonization) {
        this(databases, sourceIonization, false, false);
    }

    public FingerIDComputationPanel(final List<SearchableDatabase> databases, final JCheckBoxList<String> sourceIonization, boolean horizontal, boolean button) {
        JPanel target = this;
        if (horizontal) {
            if (button) {
                setLayout(new FlowLayout(FlowLayout.LEFT));
                target = new JPanel();
                csiButton = csiButton();
                csiButton.addActionListener(e -> {
                    setComponentsEnabled(csiButton.isSelected());
                    csiButton.setToolTipText((csiButton.isSelected() ? "Disable CSI:FingerID search" : "Enable CSI:FingerID search"));
                });
                csiButton.setSelected(false);
                add(csiButton);
                add(target);
            }
            RelativeLayout rl = new RelativeLayout(RelativeLayout.X_AXIS, 15);
            rl.setAlignment(RelativeLayout.LEADING);
            target.setLayout(rl);

        } else {
            target.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        if (databases != null) {
            dbSelectionOptions = new DBSelectionPanel(databases);
            target.add(dbSelectionOptions);
        } else dbSelectionOptions = null;

        if (sourceIonization != null) {
            adductOptions = new AdductSelectionPanel(sourceIonization);
            target.add(adductOptions);
        } else adductOptions = null;


        setComponentsEnabled(csiButton == null);


    }

    private void setComponentsEnabled(final boolean enabled) {
        if (dbSelectionOptions != null) dbSelectionOptions.setEnabled(enabled);
        if (adductOptions != null) {
            adductOptions.setEnabled(enabled);
            adductOptions.adductList.setEnabled(enabled);
            adductOptions.sp.setEnabled(enabled);

        }
    }


    private ToolbarToggleButton csiButton() {
        ToolbarToggleButton b = new ToolbarToggleButton("CSI:FingerID", Icons.FINGER_32);
        if (MainFrame.MF.getCsiFingerId().isEnabled() && ((CheckConnectionAction) SiriusActions.CHECK_CONNECTION.getInstance()).isActive.get()) {
            b.setToolTipText("Enable CSI:FingerID search");
            b.setEnabled(true);
        } else {
            b.setToolTipText("Can't connect to CSI:FingerID server!");
            b.setEnabled(false);
        }
        return b;
    }

    public boolean isCSISelected() {
        return csiButton == null || csiButton.isSelected();
    }

    public PossibleAdducts getPossibleAdducts() {
        PossibleAdducts adds = new PossibleAdducts();
        for (String adductName : adductOptions.adductList.getCheckedItems()) {
            adds.addAdduct(adductName);
        }
        return adds;
    }

    public static class DBSelectionPanel extends TextHeaderBoxPanel {
        public JComboBox<SearchableDatabase> db;
        protected final List<SearchableDatabase> databases;
        protected int bioIndex, pubchemIndex;
//        private Border b = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        public DBSelectionPanel(final List<SearchableDatabase> databases) {
            super("Search in");
            this.databases = databases;
            this.db = new JComboBox<>(new Vector<>(databases));
            add(db);
            for (int k = 0; k < databases.size(); ++k) {
                if (!databases.get(k).isCustomDb()) {
                    if (databases.get(k).searchInPubchem()) {
                        pubchemIndex = k;
                    } else bioIndex = k;
                }
            }
        }

        public void setIsBioDB(boolean isBio) {
            if (isBio) {
                db.setSelectedIndex(bioIndex);
            } else {
                db.setSelectedIndex(pubchemIndex);
            }
        }

        public void setDb(SearchableDatabase database) {
            if (database != null) {
                for (int i = 0; i < db.getModel().getSize(); ++i) {
                    final SearchableDatabase d = db.getModel().getElementAt(i);
                    if (database.name().equals(d.name())) {
                        db.setSelectedIndex(i);
                        return;
                    }
                }
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            db.setEnabled(enabled);
        }

        public SearchableDatabase getDb() {
            return (SearchableDatabase) db.getSelectedItem();
        }
    }

    public static class AdductSelectionPanel extends TextHeaderBoxPanel {
        private final AdductSelectionList adductList;
        private final JScrollPane sp;

        public AdductSelectionPanel(final JCheckBoxList<String> sourceIonization) {
            super("Select adducts");
            adductList = new AdductSelectionList(sourceIonization);
            sp = new JScrollPane(adductList);
            sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            sp.setPreferredSize(new Dimension(sp.getPreferredSize().width, 125));
            add(sp);

        }
    }
}
