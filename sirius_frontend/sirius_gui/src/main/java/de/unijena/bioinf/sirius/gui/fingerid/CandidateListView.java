package de.unijena.bioinf.sirius.gui.fingerid;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.Filterator;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.MatcherEditor;
import de.unijena.bioinf.sirius.gui.configs.Buttons;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.sirius.gui.dialogs.FilePresentDialog;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedExportCSVFormatsFilter;
import de.unijena.bioinf.sirius.gui.fingerid.candidate_filters.CandidateStringMatcherEditor;
import de.unijena.bioinf.sirius.gui.fingerid.candidate_filters.DatabaseFilterMatcherEditor;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.table.ActionList;
import de.unijena.bioinf.sirius.gui.table.ActionListDetailView;
import de.unijena.bioinf.sirius.gui.table.FilterRangeSlider;
import de.unijena.bioinf.sirius.gui.table.MinMaxMatcherEditor;
import de.unijena.bioinf.sirius.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.sirius.gui.utils.NameFilterRangeSlider;
import de.unijena.bioinf.sirius.gui.utils.ToolbarToggleButton;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static de.unijena.bioinf.sirius.gui.mainframe.MainFrame.MF;

/**
 * Created by fleisch on 16.05.17.
 */
public class CandidateListView extends ActionListDetailView<CompoundCandidate, Set<FingerIdData>, CandidateList> {

    private FilterRangeSlider logPSlider;
    private FilterRangeSlider tanimotoSlider;
    private DBFilterPanel dbFilterPanel;

    public CandidateListView(CandidateList source) {
        super(source);
    }


    @Override
    protected JPanel getNorth() {
        final JPanel north = super.getNorth();
        north.add(dbFilterPanel, BorderLayout.SOUTH);
        return north;
    }

    @Override
    protected JToolBar getToolBar() {
        JToolBar tb =  new JToolBar();
        tb.setFloatable(false);
        tb.setBorderPainted(false);

        logPSlider = new FilterRangeSlider(source) {
            @Override
            protected DoubleListStats getDoubleListStats(ActionList list) {
                return ((CandidateList) list).logPStats;
            }
        };

        tanimotoSlider = new FilterRangeSlider(source, true) {
            @Override
            protected DoubleListStats getDoubleListStats(ActionList list) {
                return ((CandidateList) list).tanimotoStats;
            }
        };

        dbFilterPanel = new DBFilterPanel(source);
        dbFilterPanel.toggle();

        tb.add(new NameFilterRangeSlider("XLogP:", logPSlider));
        tb.addSeparator();
        tb.add(new NameFilterRangeSlider("Match:", tanimotoSlider));
        tb.addSeparator();





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
        tb.add(filter);


        final JButton exportToCSV = Buttons.getExportButton24("export candidate list");
        exportToCSV.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doExport();
            } //todo export action
        });
        tb.add(exportToCSV);

        return tb;
    }

    private void doExport() {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(Workspace.CONFIG_STORAGE.getDefaultTreeExportPath());
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        FileFilter csvFileFilter = new SupportedExportCSVFormatsFilter();
        jfc.addChoosableFileFilter(csvFileFilter);
        File selectedFile = null;
        while (selectedFile == null) {
            int returnval = jfc.showSaveDialog(this);
            if (returnval == JFileChooser.APPROVE_OPTION) {
                File selFile = jfc.getSelectedFile();

                Workspace.CONFIG_STORAGE.setDefaultCompoundsExportPath(selFile.getParentFile());

                if (selFile.exists()) {
                    FilePresentDialog fpd = new FilePresentDialog(MF, selFile.getName());
                    ReturnValue rv = fpd.getReturnValue();
                    if (rv == ReturnValue.Success) {
                        selectedFile = selFile;
                    }
                } else {
                    selectedFile = selFile;
                    if (!selectedFile.getName().endsWith(".csv"))
                        selectedFile = new File(selectedFile.getAbsolutePath() + ".csv");
                }
            } else {
                break;
            }
        }

        if (selectedFile != null) {
            Set<FingerIdData> datas = new HashSet<>();
            for (CompoundCandidate candidate : source.getElementList()) {
                datas.add(candidate.data);
            }

            try {
                new CSVExporter().exportToFile(selectedFile, new ArrayList<>(datas));
            } catch (Exception e2) {
                ErrorReportDialog fed = new ErrorReportDialog(MF, e2.getMessage());
                LoggerFactory.getLogger(this.getClass()).error(e2.getMessage(), e2);
            }
        }
    }

    @Override
    protected EventList<MatcherEditor<CompoundCandidate>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf(
                (MatcherEditor<CompoundCandidate>) new CandidateStringMatcherEditor(searchField.textField),
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
        );
    }
}
