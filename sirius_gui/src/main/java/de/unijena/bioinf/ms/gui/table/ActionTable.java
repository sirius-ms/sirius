package de.unijena.bioinf.ms.gui.table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 24.01.17.
 */

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import de.unijena.bioinf.ms.frontend.core.AbstractEDTBean;

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ActionTable<T extends AbstractEDTBean> extends JTable {
    public final TableComparatorChooser comparatorChooser;


    public ActionTable(SortedList<T> sorted, TableFormat<T> format, EventList<MatcherEditor<T>> matcher) {
        this(new FilterList<T>(sorted, new CompositeMatcherEditor<>(matcher)), format);
    }

    public ActionTable(FilterList<T> filtered, TableFormat<T> format) {
        this(filtered, new SortedList<>(filtered), format);
    }

    public ActionTable(FilterList<T> filtered, SortedList<T> sorted, TableFormat<T> format) {
        setModel(new DefaultEventTableModel(filtered, format));
        comparatorChooser = TableComparatorChooser.install(this, sorted, AbstractTableComparatorChooser.SINGLE_COLUMN);
    }

    /*public static void main(String[] args) {

        ActionTable<SiriusResultElement> issuesJTable = new ActionTable<>((new ObservableElementList<>(GlazedLists.eventList(new ArrayList<SiriusResultElement>()), GlazedLists.beanConnector(SiriusResultElement.class))), new SiriusResultTableFormat());
        JScrollPane issuesTableScrollPane = new JScrollPane(issuesJTable);
//        panel.add(issuesTableScrollPane, new GridBagConstraints(...));

        // create a frame with that panel
        JFrame frame = new JFrame("Issues");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(540, 380);
        frame.getContentPane().add(issuesTableScrollPane);
        frame.show();
    }*/
}
