package de.unijena.bioinf.sirius.gui.utils;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 24.01.17.
 */

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.result_element_detail.SiriusResultTableFormat;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import org.jdesktop.beans.AbstractBean;

import javax.swing.*;
import java.util.ArrayList;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ActionTable<T extends AbstractBean> extends JTable {
    public final FilterList<T> elements;
    public final TableComparatorChooser comparatorChooser;

    /*public ActionTable(TableFormat<T> format, MatcherEditor<T> matcher, Class<T> elementType) {
        this(new ArrayList<T>(), format, matcher, elementType);
    }*/

//    public ActionTable(TableFormat<T> format, Class<T> elementType) {
//        this(new ArrayList<T>(), format, elementType);
//    }

    public ActionTable(ObservableElementList<T> elements, TableFormat<T> format, Class<T> elementType) {
        this(elements, format, new AbstractMatcherEditor<T>() {
        }, elementType);
    }

    //todo replace with other constructor
    /*public ActionTable(List<T> elements, TableFormat<T> format, MatcherEditor<T> matcher, Class<T> elementType) {
        SortedList<T> sorted = new SortedList<>(GlazedLists.eventList(elements));
        this.elements = new FilterList<T>(new ObservableElementList<>(sorted, GlazedLists.beanConnector(elementType)), matcher);
        setModel(new DefaultEventTableModel(this.elements, format));
        comparatorChooser = TableComparatorChooser.install(this, sorted, AbstractTableComparatorChooser.SINGLE_COLUMN);
    }*/

    public ActionTable(ObservableElementList<T> elements, TableFormat<T> format, MatcherEditor<T> matcher, Class<T> elementType) {
        SortedList<T> sorted = new SortedList<>(elements);
        this.elements = new FilterList<T>(sorted, matcher);
        setModel(new DefaultEventTableModel(this.elements, format));
        comparatorChooser = TableComparatorChooser.install(this, sorted, AbstractTableComparatorChooser.SINGLE_COLUMN);
    }


    public static void main(String[] args) {

        ActionTable<SiriusResultElement> issuesJTable = new ActionTable<>((new ObservableElementList<>(GlazedLists.eventList(new ArrayList<SiriusResultElement>()), GlazedLists.beanConnector(SiriusResultElement.class))), new SiriusResultTableFormat(), SiriusResultElement.class);
        JScrollPane issuesTableScrollPane = new JScrollPane(issuesJTable);
//        panel.add(issuesTableScrollPane, new GridBagConstraints(...));

        // create a frame with that panel
        JFrame frame = new JFrame("Issues");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(540, 380);
        frame.getContentPane().add(issuesTableScrollPane);
        frame.show();
    }
}
