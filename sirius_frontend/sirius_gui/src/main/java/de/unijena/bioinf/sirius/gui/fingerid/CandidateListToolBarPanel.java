package de.unijena.bioinf.sirius.gui.fingerid;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.Filterator;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import de.unijena.bioinf.sirius.gui.configs.Buttons;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.fingerid.candidate_filters.CandidateStringMatcherEditor;
import de.unijena.bioinf.sirius.gui.fingerid.candidate_filters.DatabaseFilterMatcherEditor;
import de.unijena.bioinf.sirius.gui.table.ActionList;
import de.unijena.bioinf.sirius.gui.table.FilterRangeSlider;
import de.unijena.bioinf.sirius.gui.table.MinMaxMatcherEditor;
import de.unijena.bioinf.sirius.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.sirius.gui.utils.NameFilterRangeSlider;
import de.unijena.bioinf.sirius.gui.utils.SearchTextField;
import de.unijena.bioinf.sirius.gui.utils.ToolbarToggleButton;

import javax.swing.*;
import java.awt.*;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Created by fleisch on 19.05.17.
 */
public class CandidateListToolBarPanel extends JToolBar {
    public CandidateListToolBarPanel(CandidateList sourceList) {
        setFloatable(false);
        setBorderPainted(false);

        FilterRangeSlider logPSlider = new FilterRangeSlider(sourceList) {
            @Override
            protected DoubleListStats getDoubleListStats(ActionList list) {
                return ((CandidateList) list).logPStats;
            }
        };

        FilterRangeSlider tanimotoSlider = new FilterRangeSlider(sourceList, true) {
            @Override
            protected DoubleListStats getDoubleListStats(ActionList list) {
                return ((CandidateList) list).tanimotoStats;
            }
        };


        add(new NameFilterRangeSlider("XLogP:", logPSlider));
        addSeparator();
        add(new NameFilterRangeSlider("Match:", tanimotoSlider));
        addSeparator();


        final JToggleButton filter = new ToolbarToggleButton(Icons.FILTER_DOWN_24, "show filter");
        filter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (dbFilterPanel.toggle()) {
                    filter.setIcon(Icons.FILTER_UP_24);
                    filter.setToolTipText("hide filter");
                } else {
                    filter.setIcon(Icons.FILTER_DOWN_24);
                    filter.setToolTipText("show filter");
                }
            }
        });
        add(filter);


        final JButton exportToCSV = Buttons.getExportButton24("export candidate list");
        exportToCSV.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doExport();
            }
        });
        add(exportToCSV);
        addSeparator();
//        add(Box.createGlue());
//        addSeparator(); //todo why doe the glue not work???
        SearchTextField f = new SearchTextField();
        add(f);

        FilterList<CompoundCandidate> fl = new FilterList<CompoundCandidate>(sourceList.getElementList(), new CompositeMatcherEditor<>(
                GlazedLists.eventListOf(
                        new CandidateStringMatcherEditor(f.textField),
                        new MinMaxMatcherEditor<>(logPSlider, new Filterator<Double, CompoundCandidate>() {
                            @Override
                            public void getFilterValues(java.util.List<Double> baseList, CompoundCandidate element) {
                                baseList.add(element.getXLogP());
                            }
                        }),
                        new MinMaxMatcherEditor<>(tanimotoSlider, new Filterator<Double, CompoundCandidate>() {
                            @Override
                            public void getFilterValues(java.util.List<Double> baseList, CompoundCandidate element) {
                                baseList.add(element.getTanimotoScore());
                            }
                        }),
                        new DatabaseFilterMatcherEditor(dbFilterPanel)
                )));
    }
}
