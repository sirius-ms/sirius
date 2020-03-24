package de.unijena.bioinf.ms.gui.fingerid;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.MatcherEditor;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.fingerid.candidate_filters.CandidateStringMatcherEditor;
import de.unijena.bioinf.ms.gui.fingerid.candidate_filters.DatabaseFilterMatcherEditor;
import de.unijena.bioinf.ms.gui.table.ActionListDetailView;
import de.unijena.bioinf.ms.gui.table.FilterRangeSlider;
import de.unijena.bioinf.ms.gui.table.MinMaxMatcherEditor;
import de.unijena.bioinf.ms.gui.utils.NameFilterRangeSlider;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.gui.utils.WrapLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * Created by fleisch on 16.05.17.
 */
public class CandidateListView extends ActionListDetailView<FingerprintCandidateBean, Set<FormulaResultBean>, CandidateList> {

    private FilterRangeSlider<CandidateList,FingerprintCandidateBean, Set<FormulaResultBean>> logPSlider;
    private FilterRangeSlider<CandidateList,FingerprintCandidateBean, Set<FormulaResultBean>> tanimotoSlider;
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
        filter.addActionListener(e -> {
            if (dbFilterPanel.toggle()) {
                filter.setIcon(Icons.FILTER_UP_24);
                filter.setToolTipText("hide filter");
            } else {
                filter.setIcon(Icons.FILTER_DOWN_24);
                filter.setToolTipText("show filter");
            }
        });
        tb.add(filter);
        filter.doClick();


//        final JButton exportToCSV = Buttons.getExportButton24("export candidate list");
//        exportToCSV.addActionListener(e -> doExport());
//        tb.add(exportToCSV);

        return tb;
    }

    /*private void doExport() {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(PropertyManager.getFile(SiriusProperties.DEFAULT_TREE_EXPORT_PATH));
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        FileFilter csvFileFilter = new ExportCSVFormatsFilter();
        jfc.addChoosableFileFilter(csvFileFilter);
        File selectedFile = null;
        while (selectedFile == null) {
            int returnval = jfc.showSaveDialog(this);
            if (returnval == JFileChooser.APPROVE_OPTION) {
                File selFile = jfc.getSelectedFile();

                Jobs.runInBackground(() ->
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

            final List<Scored<CompoundCandidate>> datas = source.getElementList().stream().map(fpc -> new Scored<CompoundCandidate>(fpc.candidate, fpc.score)).sorted(Comparator.reverseOrder()).collect(Collectors.toList());

            try {
                new StructureCSVExporter().exportFingerIdResults(Files.newBufferedWriter(selectedFile.toPath()), datas);
            } catch (Exception e2) {
                new ErrorReportDialog(MF, e2.getMessage());
                LoggerFactory.getLogger(this.getClass()).error(e2.getMessage(), e2);
            }
        }
    }*/

    @Override
    protected EventList<MatcherEditor<FingerprintCandidateBean>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf(
                new CandidateStringMatcherEditor(searchField.textField)
                ,new MinMaxMatcherEditor<>(logPSlider, (baseList, element) -> baseList.add(element.getXLogPOrNull()))
                ,new MinMaxMatcherEditor<>(tanimotoSlider, (baseList, element) -> baseList.add(element.getTanimotoScore()))
               , new DatabaseFilterMatcherEditor(dbFilterPanel)
        );
    }
}
