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
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.DataSources;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.subtools.spectra_db.SpectralDatabases;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.ms_viewer.WebViewSpectraViewer;
import de.unijena.bioinf.ms.gui.ms_viewer.data.SpectraJSONWriter;
import de.unijena.bioinf.ms.gui.table.*;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.projectspace.SpectralSearchResultBean;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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

        private JJob<Boolean> backgroundLoader = null;
        private final Lock backgroundLoaderLock = new ReentrantLock();

        public MatchList() {
            super(SpectralSearchResultBean.MatchBean.class);
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
                        elementListSelectionModel.clearSelection();
                        elementList.clear();

                        if (ec != null && ec.getSpectralSearchResults().isPresent()) {
                            SpectralSearchResultBean search = ec.getSpectralSearchResults().get();
                            List<SpectralSearchResultBean.MatchBean> matches;
                            if (candidateBean == null) {
                                matches = search.getAllResults().stream().map(SpectralSearchResultBean.MatchBean::new).toList();
                            } else {
                                Optional<List<SpectralSearchResult.SearchResult>> bestResults = search.getMatchingSpectraForFPCandidate(candidateBean.getInChiKey());
                                if (bestResults.isEmpty())
                                    return true;
                                matches = bestResults.get().stream().map(SpectralSearchResultBean.MatchBean::new).toList();
                            }
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

        private static final int COL_COUNT = 10;

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
                case 1 -> "Name";
                case 2 -> "SMILES";
                case 3 -> "Similarity";
                case 4 -> "Shared Peaks";
                case 5 -> "Ionization";
                case 6 -> "Precursor m/z";
                case 7 -> "Collision Energy";
                case 8 -> "Instrument";
                case 9 -> "DB link";
                case 10 -> "Best";
                default -> throw new IllegalStateException();
            };
        }

        @Override
        public Object getColumnValue(SpectralSearchResultBean.MatchBean baseObject, int column) {
            return switch (column) {
                case 0 -> baseObject.getMatch().getRank();
                case 1 -> baseObject.getReference().getName();
                case 2 -> baseObject.getReference().getSmiles();
                case 3 -> baseObject.getMatch().getSimilarity().similarity;
                case 4 -> baseObject.getMatch().getSimilarity().shardPeaks;
                case 5 -> baseObject.getReference().getPrecursorIonType() != null ? baseObject.getReference().getPrecursorIonType().toString() : "";
                case 6 -> baseObject.getReference().getPrecursorMz();
                case 7 -> baseObject.getReference().getCollisionEnergy() != null ? baseObject.getReference().getCollisionEnergy().toString() : "";
                case 8 -> baseObject.getReference().getInstrumentation() != null ? baseObject.getReference().getInstrumentation().toString() : "";
                case 9 -> baseObject.getReference().getSpectralDbLink();
                case 10 -> isBest.apply(baseObject);
                default -> throw new IllegalStateException();
            };
        }
    }

    private static class SpectralMatchingTableView extends ActionListDetailView<SpectralSearchResultBean.MatchBean, InstanceBean, MatchList> {

        private JJob<Boolean> backgroundLoader = null;
        private final Lock backgroundLoaderLock = new ReentrantLock();

        public SpectralMatchingTableView(MatchList source, SpectralMatchingPanel parentPanel) {
            super(source, true);
            SortedList<SpectralSearchResultBean.MatchBean> sortedSource = new SortedList<>(source.getElementList());
            final MatchResultTableFormat tf = new MatchResultTableFormat(source);
            ActionTable<SpectralSearchResultBean.MatchBean> table = new ActionTable<>(filteredSource, sortedSource, tf);

            getSource().addActiveResultChangedListener((experiment, sre, resultElements, selections) -> {
                if (experiment == null || experiment.getSpectralSearchResults().isEmpty())
                    showCenterCard(ActionList.ViewState.NOT_COMPUTED);
                else if (resultElements.isEmpty())
                    showCenterCard(ActionList.ViewState.EMPTY);
                else
                    showCenterCard(ActionList.ViewState.DATA);
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
                                String queryName = String.format("MS%d [#%d; %deV]", queryMS2.getMsLevel(), (queryMS2.getScanNumber() > -1) ? queryMS2.getScanNumber() : matchBean.getMatch().getQuerySpectrumIndex() + 1, Math.round(queryMS2.getCollisionEnergy().getMinEnergy()));

                                if (matchBean.getReference().getSpectrum() == null) {
                                    try {
                                        SpectralLibrary db = SpectralDatabases.getSpectralLibrary(Path.of(matchBean.getMatch().getDbLocation())).orElseThrow();
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

            table.getColumnModel().getColumn(3).setCellRenderer(new BarTableCellRenderer(tf.highlightColumnIndex(), 0f, 1f, true));

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
            linkRenderer.registerToTable(table, 9);

            addToCenterCard(ActionList.ViewState.DATA, new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
            showCenterCard(ActionList.ViewState.NOT_COMPUTED);
        }

        @Override
        protected JToolBar getToolBar() {
            return null;
        }

        @Override
        protected EventList<MatcherEditor<SpectralSearchResultBean.MatchBean>> getSearchFieldMatchers() {
            return GlazedLists.eventListOf();
        }

    }


}
