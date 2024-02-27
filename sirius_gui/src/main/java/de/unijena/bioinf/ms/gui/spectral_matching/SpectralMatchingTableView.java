/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.spectral_matching;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.SpectralMatchingPanel;
import de.unijena.bioinf.ms.gui.table.*;
import de.unijena.bioinf.ms.gui.utils.NameFilterRangeSlider;
import de.unijena.bioinf.ms.gui.utils.WrapLayout;
import de.unijena.bioinf.ms.nightsky.sdk.model.BasicSpectrum;
import de.unijena.bioinf.ms.nightsky.sdk.model.DBLink;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SpectralMatchingTableView extends ActionListDetailView<SpectralMatchBean, InstanceBean, SpectralMatchList> {

    private FilterRangeSlider<SpectralMatchList, SpectralMatchBean, InstanceBean> scoreSlider;
    private FilterRangeSlider<SpectralMatchList, SpectralMatchBean, InstanceBean> peaksSlider;

    private SortedList<SpectralMatchBean> sortedSource;

    private JJob<Boolean> backgroundLoader = null;
    private final Lock backgroundLoaderLock = new ReentrantLock();

    public SpectralMatchingTableView(SpectralMatchList source, SpectralMatchingPanel parentPanel) {
        super(source, true);

        getSource().addActiveResultChangedListener((experiment, sre, resultElements, selections) -> {
            filteredSelectionModel.setValueIsAdjusting(true);
            filteredSelectionModel.clearSelection();
            if (experiment == null || experiment.getSpectralSearchResults() == null)
                showCenterCard(ActionList.ViewState.NOT_COMPUTED);
            else if (resultElements.isEmpty())
                showCenterCard(ActionList.ViewState.EMPTY);
            else {
                showCenterCard(ActionList.ViewState.DATA);
            }
            if (!getSource().getElementListSelectionModel().isSelectionEmpty())
                filteredSelectionModel.setSelectionInterval(getSource().getElementListSelectionModel().getMinSelectionIndex(), getSource().getElementListSelectionModel().getMaxSelectionIndex());
            filteredSelectionModel.setValueIsAdjusting(false);
        });

        final SpectralMatchTableFormat tf = new SpectralMatchTableFormat(source.getBestFunc());
        ActionTable<SpectralMatchBean> table = new ActionTable<>(filteredSource, sortedSource, tf);


        filteredSelectionModel.addListSelectionListener(e -> {
            try {

                backgroundLoaderLock.lock();
                final EventList<SpectralMatchBean> selected = filteredSelectionModel.getSelected();
                if (selected.isEmpty())
                    return;
                final JJob<Boolean> old = backgroundLoader;
                backgroundLoader = Jobs.runInBackground(new TinyBackgroundJJob<>() {

                    @Override
                    protected Boolean compute() throws Exception {
                        if (old != null && !old.isFinished()) {
                            old.cancel(false);
                            old.getResult(); //await cancellation so that nothing strange can happen.
                        }
                        checkForInterruption();

                        final SpectralMatchBean matchBean = selected.get(0);
                        Pair<BasicSpectrum, BasicSpectrum> data = getSource().readDataByFunction(ec -> {
                            if (ec == null)
                                return null;
                            BasicSpectrum queryMS2 = ec.getMsData().getMs2Spectra().get(matchBean.getMatch().getQuerySpectrumIndex());

                            return matchBean.getReference().map(r -> Pair.of(queryMS2, r)).orElse(null);
                        });

                        if (data == null)
                            return false;

                        parentPanel.showMatch(data.getLeft(), data.getRight());
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

        LinkedSiriusTableCellRenderer linkRenderer = new LinkedSiriusTableCellRenderer(defaultRenderer,
                dbLink -> {
                    Optional<CustomDataSources.Source> ds = CustomDataSources.getSourceFromNameOpt(dbLink.getName());
                    if (ds.isEmpty() || ds.get().URI() == null || dbLink.getId() == null)
                        return null;
                    try {
                        if (ds.get().URI().contains("%s")) {
                            return new URI(String.format(Locale.US, ds.get().URI(), URLEncoder.encode(dbLink.getId(), StandardCharsets.UTF_8)));
                        } else {
                            return new URI(String.format(Locale.US, ds.get().URI(), Integer.parseInt(dbLink.getId())));
                        }
                    } catch (URISyntaxException e) {
                        LoggerFactory.getLogger(getClass()).error("Error.", e);
                        return null;
                    }
                }, DBLink::getId);
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

        scoreSlider = new FilterRangeSlider<>(source, source.similarityStats, true);
        peaksSlider = new FilterRangeSlider<>(source, source.sharedPeaksStats);

        tb.add(new NameFilterRangeSlider("Similarity:", scoreSlider));
        tb.addSeparator();
        tb.add(new NameFilterRangeSlider("Shared Peaks:", peaksSlider));
        tb.addSeparator();

        return tb;
    }

    @Override
    protected EventList<MatcherEditor<SpectralMatchBean>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf(
                new TextComponentMatcherEditor<>(searchField.textField, (baseList, element) -> {
                    baseList.add(element.getQueryName());
                    element.getReference().map(BasicSpectrum::getName).ifPresent(baseList::add);
                    element.getReference().map(BasicSpectrum::getCollisionEnergy).ifPresent(baseList::add);
                    baseList.add(element.getMatch().getSmiles());
                    if (element.getMatch().getAdduct() != null)
                        baseList.add(element.getMatch().getAdduct());

                    //todo nighsky: if instrumentation added enable this

//                        if (element.getReference().getInstrumentation() != null)
//                            baseList.add(element.getReference().getInstrumentation().toString());
                }),
                new MinMaxMatcherEditor<>(scoreSlider, (baseList, element) -> baseList.add(element.getMatch().getSimilarity())),
                new MinMaxMatcherEditor<>(peaksSlider, (baseList, element) -> baseList.add((double) element.getMatch().getSharedPeaks()))
        );
    }

    @Override
    protected FilterList<SpectralMatchBean> configureFiltering(EventList<SpectralMatchBean> source) {
        sortedSource = new SortedList<>(source);
        return super.configureFiltering(sortedSource);
    }

}
