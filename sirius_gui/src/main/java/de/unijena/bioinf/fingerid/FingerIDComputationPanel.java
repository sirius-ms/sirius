package de.unijena.bioinf.fingerid;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 23.01.17.
 */

import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import org.jdesktop.swingx.JXTitledSeparator;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;
import java.util.Vector;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FingerIDComputationPanel extends JPanel {
    //todo her cann we show more option if we select databases
    public JComboBox<SearchableDatabase> db;
    protected final List<SearchableDatabase> databases;
    protected int bioIndex, pubchemIndex;
    private Border b = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    public FingerIDComputationPanel(final List<SearchableDatabase> databases) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        final JPanel inner2 = new JPanel();
        inner2.setLayout(new FlowLayout());
        this.databases = databases;
        this.db = new JComboBox<SearchableDatabase>(new Vector<>(databases));
        inner2.add(db);
        setBorder(b);
        add(new JXTitledSeparator("Search in"));
        add(inner2);
        for (int k=0; k < databases.size(); ++k) {
            if (!databases.get(k).isCustomDb()) {
                if (databases.get(k).searchInPubchem()) {
                    pubchemIndex = k;
                } else bioIndex = k;
            }
        }
    }

    public void setIsBioDB(boolean isBio){
        if (isBio) {
            db.setSelectedIndex(bioIndex);
        } else {
            db.setSelectedIndex(pubchemIndex);
        }
    }

    public void setDb(SearchableDatabase database) {
        if (database!=null) {
            for (int i=0; i < db.getModel().getSize(); ++i) {
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
