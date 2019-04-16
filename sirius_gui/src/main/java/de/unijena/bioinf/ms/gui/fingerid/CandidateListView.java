package de.unijena.bioinf.ms.gui.fingerid;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.Filterator;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.MatcherEditor;
import de.unijena.bioinf.fingerid.CSVExporter;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.ms.gui.fingerid.candidate_filters.CandidateStringMatcherEditor;
import de.unijena.bioinf.ms.gui.fingerid.candidate_filters.DatabaseFilterMatcherEditor;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.ms.gui.dialogs.FilePresentDialog;
import de.unijena.bioinf.ms.io.filefilter.SupportedExportCSVFormatsFilter;
import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.gui.table.ActionListDetailView;
import de.unijena.bioinf.ms.gui.table.FilterRangeSlider;
import de.unijena.bioinf.ms.gui.table.MinMaxMatcherEditor;
import de.unijena.bioinf.ms.gui.utils.NameFilterRangeSlider;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.gui.utils.WrapLayout;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * Created by fleisch on 16.05.17.
 */
public class CandidateListView extends ActionListDetailView<FingerprintCandidateBean, Set<FingerIdResultBean>, CandidateList> {

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
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorderPainted(false);
        tb.setLayout(new WrapLayout(FlowLayout.LEFT, 0, 0));

        logPSlider = new FilterRangeSlider<>(source, source.logPStats);
        tanimotoSlider = new FilterRangeSlider<>(source, source.tanimotoStats, true);

        dbFilterPanel = new DBFilterPanel(source);
        dbFilterPanel.toggle();

        tb.add(new NameFilterRangeSlider("XLogP:", logPSlider));
        tb.addSeparator();
        tb.add(new NameFilterRangeSlider("Similarity:", tanimotoSlider));
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
        filter.doClick();


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
        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_TREE_EXPORT_PATH));
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        FileFilter csvFileFilter = new SupportedExportCSVFormatsFilter();
        jfc.addChoosableFileFilter(csvFileFilter);
        File selectedFile = null;
        while (selectedFile == null) {
            int returnval = jfc.showSaveDialog(this);
            if (returnval == JFileChooser.APPROVE_OPTION) {
                File selFile = jfc.getSelectedFile();

                Jobs.runInBackround(() ->
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().
                                setAndStoreProperty(SiriusProperties.DEFAULT_TREE_EXPORT_PATH, selFile.getParentFile().getAbsolutePath())
                );

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

            Set<FingerIdResult> datas = source.getElementList().stream().map(FingerprintCandidateBean::getData).map(FingerIdResultBean::getFingerIdResult)
                    .collect(Collectors.toSet());
            try {
                new CSVExporter().exportFingerIdResultsToFile(selectedFile, datas);
            } catch (Exception e2) {
                ErrorReportDialog fed = new ErrorReportDialog(MF, e2.getMessage());
                LoggerFactory.getLogger(this.getClass()).error(e2.getMessage(), e2);
            }
        }
    }

    @Override
    protected EventList<MatcherEditor<FingerprintCandidateBean>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf(
                new CandidateStringMatcherEditor(searchField.textField),
                new MinMaxMatcherEditor<>(logPSlider, (Filterator<Double, FingerprintCandidateBean>) (baseList, element) -> baseList.add(element.getXLogP())),
                new MinMaxMatcherEditor<>(tanimotoSlider, (Filterator<Double, FingerprintCandidateBean>) (baseList, element) -> baseList.add(element.getTanimotoScore())),
                new DatabaseFilterMatcherEditor(dbFilterPanel)
        );
    }
}
