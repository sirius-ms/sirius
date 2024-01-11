/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.DataSources;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.subtools.spectra_search.SpectraSearchSubtoolJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.ms_viewer.WebViewSpectraViewer;
import de.unijena.bioinf.ms.gui.ms_viewer.data.SpectraJSONWriter;
import de.unijena.bioinf.ms.gui.table.*;
import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;
import de.unijena.bioinf.ms.gui.utils.NameFilterRangeSlider;
import de.unijena.bioinf.ms.gui.utils.WrapLayout;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.projectspace.SpectralSearchResult;
import de.unijena.bioinf.projectspace.SpectralSearchResultBean;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SpectralMatchingPanel extends JPanel implements PanelDescription {

    private final MatchList matchList;

    private final WebViewSpectraViewer browser;
    private final SpectraJSONWriter spectraWriter;

    public SpectralMatchingPanel(CompoundList compoundList) {
        this();

        compoundList.addChangeListener(new ExperimentListChangeListener() {
            @Override
            public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection) {
                if (!selection.isSelectionEmpty()) {
                    while (event.next()) {
                        if (selection.isSelectedIndex(event.getIndex())) {
                            matchList.changeData(event.getSourceList().get(event.getIndex()), null);
                            return;
                        }
                    }
                }
            }

            @Override
            public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection) {
                if (!selection.isSelectionEmpty())
                    matchList.changeData(selection.getSelected().get(0), null);
                else
                    matchList.changeData(null, null);
            }
        });
    }

    public SpectralMatchingPanel(final CompoundList compoundList, final FingerprintCandidateBean candidateBean) {
        this();
        EventList<InstanceBean> selected = compoundList.getCompoundListSelectionModel().getSelected();
        if (!selected.isEmpty()) {
            Jobs.runEDTLater(() -> matchList.changeData(selected.get(0), candidateBean));
        }
    }

    private SpectralMatchingPanel() {
        super(new BorderLayout());

        this.spectraWriter = new SpectraJSONWriter();
        this.browser = new WebViewSpectraViewer();

        this.matchList = new MatchList();
        SpectralMatchingTableView tableView = new SpectralMatchingTableView(matchList, this);

        JSplitPane major = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableView, browser);
        major.setDividerLocation(400);
        add(major, BorderLayout.CENTER);
    }

    @Override
    public String getDescription() {
        return "<html>"
                +"<b>Reference spectra</b>"
                +"<br>"
                + "Reference spectra from spectral libraries that match the spectra from your experiment."
                +"<br>"
                + "For the selected match in the upper panel, the bottom panel shows a comparison of the experimental and reference spectrum."
                + "</html>";
    }

    public void showMatch(SimpleSpectrum query, SimpleSpectrum reference, String queryName, String referenceName) {
            Jobs.runEDTLater(() -> {
                try {
                    String json = spectraWriter.ms2MirrorJSON(query, reference, queryName, referenceName);
                    this.browser.loadData(json, null, null);
                } catch (Exception e) {
                    LoggerFactory.getLogger(getClass()).error("Error.", e);
                }
            });
    }

    private static class MatchList extends ActionList<SpectralSearchResultBean.MatchBean, InstanceBean> {

        public final DoubleListStats scoreStats;
        public final DoubleListStats peaksStats;

        private JJob<Boolean> backgroundLoader = null;
        private final Lock backgroundLoaderLock = new ReentrantLock();

        public MatchList() {
            super(SpectralSearchResultBean.MatchBean.class);
            this.scoreStats = new DoubleListStats();
            this.peaksStats = new DoubleListStats();
        }

        public void changeData(final InstanceBean ec, final FingerprintCandidateBean candidateBean) {
            //cancel running job if not finished to not waist resources for fetching data that is not longer needed.
            try {
                backgroundLoaderLock.lock();
                setData(ec);
                final JJob<Boolean> old = backgroundLoader;
                backgroundLoader = Jobs.runInBackground(new TinyBackgroundJJob<>() {
                    @Override
                    protected Boolean compute() throws Exception {

                        if (old != null && !old.isFinished()) {
                            old.cancel(false);
                            old.getResult(); //await cancellation so that nothing strange can happen.
                        }
                        checkForInterruption();

                        Jobs.runEDTAndWait(() -> {
                            scoreStats.reset();
                            peaksStats.reset();
                            elementListSelectionModel.clearSelection();
                            elementList.clear();
                        });

                        if (ec != null && ec.getSpectralSearchResults().isPresent()) {
                            SpectralSearchResultBean search = ec.getSpectralSearchResults().get();
                            List<SpectralSearchResult.SearchResult> searchResults;

                            if (candidateBean == null) {
                                searchResults = search.getAllResults();
                            } else {
                                Optional<List<SpectralSearchResult.SearchResult>> bestResults = search.getMatchingSpectraForFPCandidate(candidateBean.getInChiKey());
                                searchResults = bestResults.orElseGet(ArrayList::new);
                            }
                            List<SpectralSearchResultBean.MatchBean> matches = searchResults.stream().map(r -> {
                                scoreStats.addValue(r.getSimilarity().similarity);
                                peaksStats.addValue(r.getSimilarity().sharedPeaks);
                                return new SpectralSearchResultBean.MatchBean(r, ec);
                            }).toList();
                            try {
                                refillElementsEDT(matches);
                            } catch (InvocationTargetException | InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return true;
                    }
                });
            } finally {
                backgroundLoaderLock.unlock();
            }
        }

    }

    private static class MatchResultTableFormat extends SiriusTableFormat<SpectralSearchResultBean.MatchBean> {

        private static final int COL_COUNT = 11;

        protected MatchResultTableFormat(MatchList source) {
            super(matchBean -> matchBean.getMatch().getRank() == source.getElementList().stream().mapToInt(m -> m.getMatch().getRank()).min().orElse(0));
        }

        @Override
        protected int highlightColumnIndex() {
            return COL_COUNT;
        }

        @Override
        public int getColumnCount() {
            return COL_COUNT;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Rank";
                case 1 -> "Query";
                case 2 -> "Name";
                case 3 -> "SMILES";
                case 4 -> "Similarity";
                case 5 -> "Shared Peaks";
                case 6 -> "Ionization";
                case 7 -> "Collision Energy";
                case 8 -> "Instrument";
                case 9 -> "Database";
                case 10 -> "DB Link";
                case 11 -> "Best";
                default -> throw new IllegalStateException();
            };
        }

        @Override
        public Object getColumnValue(SpectralSearchResultBean.MatchBean baseObject, int column) {
            return switch (column) {
                case 0 -> baseObject.getMatch().getRank();
                case 1 -> baseObject.getQueryName();
                case 2 -> baseObject.getReference().getName();
                case 3 -> baseObject.getReference().getSmiles();
                case 4 -> baseObject.getMatch().getSimilarity().similarity;
                case 5 -> baseObject.getMatch().getSimilarity().sharedPeaks;
                case 6 -> baseObject.getReference().getPrecursorIonType() != null ? baseObject.getReference().getPrecursorIonType().toString() : "";
                case 7 -> baseObject.getReference().getCollisionEnergy() != null ? baseObject.getReference().getCollisionEnergy().toString() : "";
                case 8 -> baseObject.getReference().getInstrumentation() != null ? baseObject.getReference().getInstrumentation().toString() : "";
                case 9 -> baseObject.getMatch().getDbName();
                case 10 -> baseObject.getReference().getSpectralDbLink();
                case 11 -> isBest.apply(baseObject);
                default -> throw new IllegalStateException();
            };
        }
    }

    private static class SpectralMatchingTableView extends ActionListDetailView<SpectralSearchResultBean.MatchBean, InstanceBean, MatchList> {

        private FilterRangeSlider<MatchList, SpectralSearchResultBean.MatchBean, InstanceBean> scoreSlider;
        private FilterRangeSlider<MatchList, SpectralSearchResultBean.MatchBean, InstanceBean> peaksSlider;

        private SortedList<SpectralSearchResultBean.MatchBean> sortedSource;

        private JJob<Boolean> backgroundLoader = null;
        private final Lock backgroundLoaderLock = new ReentrantLock();

        public SpectralMatchingTableView(MatchList source, SpectralMatchingPanel parentPanel) {
            super(source, true);
            sortedSource = new SortedList<>(source.getElementList());
            final MatchResultTableFormat tf = new MatchResultTableFormat(source);
            ActionTable<SpectralSearchResultBean.MatchBean> table = new ActionTable<>(filteredSource, sortedSource, tf);

            getSource().addActiveResultChangedListener((experiment, sre, resultElements, selections) -> {
                if (experiment == null || experiment.getSpectralSearchResults().isEmpty())
                    showCenterCard(ActionList.ViewState.NOT_COMPUTED);
                else if (resultElements.isEmpty())
                    showCenterCard(ActionList.ViewState.EMPTY);
                else {
                    showCenterCard(ActionList.ViewState.DATA);
                }
            });

            filteredSelectionModel.addListSelectionListener(e -> {
                final EventList<SpectralSearchResultBean.MatchBean> selected = filteredSelectionModel.getSelected();
                if (selected.isEmpty())
                    return;

                try {

                    backgroundLoaderLock.lock();
                    final JJob<Boolean> old = backgroundLoader;
                    backgroundLoader = Jobs.runInBackground(new TinyBackgroundJJob<>() {

                        @Override
                        protected Boolean compute() throws Exception {
                            if (old != null && !old.isFinished()) {
                                old.cancel(false);
                                old.getResult(); //await cancellation so that nothing strange can happen.
                            }
                            checkForInterruption();

                            final SpectralSearchResultBean.MatchBean matchBean = selected.get(0);
                            Pair<Pair<SimpleSpectrum, String>, Pair<SimpleSpectrum, String>> data = getSource().readDataByFunction(ec -> {
                                if (ec == null)
                                    return null;
                                MutableMs2Spectrum queryMS2 = ec.getMs2Spectra().get(matchBean.getMatch().getQuerySpectrumIndex());
                                SimpleSpectrum query = new SimpleSpectrum(queryMS2);
                                String queryName = SpectraSearchSubtoolJob.getQueryName(queryMS2, matchBean.getMatch().getQuerySpectrumIndex());

                                if (matchBean.getReference().getSpectrum() == null) {
                                    try {
                                        SpectralLibrary db = SearchableDatabases.getCustomDatabase(matchBean.getMatch().getDbName()).orElseThrow().toSpectralLibraryOrThrow();
                                        db.getSpectralData(matchBean.getReference());
                                    } catch (Exception exc) {
                                        LoggerFactory.getLogger(SpectralMatchingTableView.class).error("Error retrieving spectral data", exc);
                                        return null;
                                    }
                                }

                                return Pair.of(Pair.of(query, queryName), Pair.of(matchBean.getReference().getSpectrum(), matchBean.getReference().getName()));
                            });

                            if (data == null)
                                return false;

                            parentPanel.showMatch(data.getLeft().getLeft(), data.getRight().getLeft(), data.getLeft().getRight(), data.getRight().getRight());
                            return true;
                        }
                    });
                } finally {
                    backgroundLoaderLock.unlock();
                }
            });

            table.setSelectionModel(filteredSelectionModel);
            final SiriusResultTableCellRenderer defaultRenderer = new SiriusResultTableCellRenderer(tf.highlightColumnIndex());
            table.setDefaultRenderer(Object.class, defaultRenderer);

            table.getColumnModel().getColumn(4).setCellRenderer(new BarTableCellRenderer(tf.highlightColumnIndex(), 0f, 1f, true));

            LinkedSiriusTableCellRenderer linkRenderer = new LinkedSiriusTableCellRenderer(defaultRenderer, (LinkedSiriusTableCellRenderer.LinkCreator<DBLink>) dbLink -> {
                Optional<DataSource> ds = DataSources.getSourceFromName(dbLink.name);
                if (ds.isEmpty() || ds.get().URI == null || dbLink.id == null)
                    return null;
                try {
                    if (ds.get().URI.contains("%s")) {
                        return new URI(String.format(Locale.US, ds.get().URI, URLEncoder.encode(dbLink.id, StandardCharsets.UTF_8)));
                    } else {
                        return new URI(String.format(Locale.US, ds.get().URI, Integer.parseInt(dbLink.id)));
                    }
                } catch (URISyntaxException e) {
                    LoggerFactory.getLogger(getClass()).error("Error.", e);
                    return null;
                }
            });
            linkRenderer.registerToTable(table, 10);

            addToCenterCard(ActionList.ViewState.DATA, new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
            showCenterCard(ActionList.ViewState.NOT_COMPUTED);
        }

        @Override
        protected JToolBar getToolBar() {
            JToolBar tb = new JToolBar();
            tb.setFloatable(false);
            tb.setBorderPainted(false);
            tb.setLayout(new WrapLayout(FlowLayout.LEFT, 0, 0));

            scoreSlider = new FilterRangeSlider<>(source, source.scoreStats, true);
            peaksSlider = new FilterRangeSlider<>(source, source.peaksStats);

            tb.add(new NameFilterRangeSlider("Similarity:", scoreSlider));
            tb.addSeparator();
            tb.add(new NameFilterRangeSlider("Shared Peaks:", peaksSlider));
            tb.addSeparator();

            return tb;
        }

        @Override
        protected EventList<MatcherEditor<SpectralSearchResultBean.MatchBean>> getSearchFieldMatchers() {
            return GlazedLists.eventListOf(
                    new TextComponentMatcherEditor<>(searchField.textField, (baseList, element) -> {
                        baseList.add(element.getQueryName());
                        baseList.add(element.getReference().getName());
                        baseList.add(element.getReference().getSmiles());
                        if (element.getReference().getPrecursorIonType() != null)
                            baseList.add(element.getReference().getPrecursorIonType().toString());
                        if (element.getReference().getCollisionEnergy() != null)
                            baseList.add(element.getReference().getCollisionEnergy().toString());
                        if (element.getReference().getInstrumentation() != null)
                            baseList.add(element.getReference().getInstrumentation().toString());
                    }),
                    new MinMaxMatcherEditor<>(scoreSlider, (baseList, element) -> baseList.add(element.getMatch().getSimilarity().similarity)),
                    new MinMaxMatcherEditor<>(peaksSlider, (baseList, element) -> baseList.add((double) element.getMatch().getSimilarity().sharedPeaks))
            );
        }

        @Override
        protected FilterList<SpectralSearchResultBean.MatchBean> configureFiltering(EventList<SpectralSearchResultBean.MatchBean> source) {
            sortedSource = new SortedList<>(source);
            return super.configureFiltering(sortedSource);
        }

    }


}
