/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.spectral_matching;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.table.*;
import de.unijena.bioinf.ms.gui.utils.NameFilterRangeSlider;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.nightsky.sdk.model.BasicSpectrum;
import de.unijena.bioinf.ms.nightsky.sdk.model.DBLink;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;

public class SpectralMatchingTableView extends ActionListDetailView<SpectralMatchBean, InstanceBean, SpectralMatchList> {

    private FilterRangeSlider<SpectralMatchList, SpectralMatchBean, InstanceBean> scoreSlider;
    private FilterRangeSlider<SpectralMatchList, SpectralMatchBean, InstanceBean> peaksSlider;

    private SortedList<SpectralMatchBean> sortedSource;

    public SpectralMatchingTableView(SpectralMatchList source) {
        super(source, true);

        getSource().addActiveResultChangedListener((experiment, sre, resultElements, selections) -> {
            filteredSelectionModel.setValueIsAdjusting(true);
            try {
                filteredSelectionModel.clearSelection();
                if (experiment == null || experiment.getNumberOfSpectralMatches() == 0)
                    showCenterCard(ActionList.ViewState.NOT_COMPUTED);
                else if (resultElements.isEmpty())
                    showCenterCard(ActionList.ViewState.EMPTY);
                else {
                    showCenterCard(ActionList.ViewState.DATA);
                }
                if (!getSource().getElementListSelectionModel().isSelectionEmpty())
                    filteredSelectionModel.setSelectionInterval(getSource().getElementListSelectionModel().getMinSelectionIndex(), getSource().getElementListSelectionModel().getMaxSelectionIndex());
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).warn("Error when resetting selection for elementList");
            } finally {
                filteredSelectionModel.setValueIsAdjusting(false);
            }
        });

        final SpectralMatchTableFormat tf = new SpectralMatchTableFormat(source.getBestFunc());
        ActionTable<SpectralMatchBean> table = new ActionTable<>(filteredSource, sortedSource, tf);

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
        linkRenderer.registerToTable(table, 9);

        addToCenterCard(ActionList.ViewState.DATA, new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        showCenterCard(ActionList.ViewState.NOT_COMPUTED);
    }

    @Override
    protected JToolBar getToolBar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorderPainted(false);
        tb.setRollover(true);

        scoreSlider = new FilterRangeSlider<>(source, source.similarityStats, true);
        peaksSlider = new FilterRangeSlider<>(source, source.sharedPeaksStats);

        tb.add(new NameFilterRangeSlider("Similarity:", scoreSlider));
        tb.addSeparator();
        tb.add(new NameFilterRangeSlider("Shared Peaks:", peaksSlider));

        int size = source.getSize();
        int total = source.getTotalSize();
        BiFunction<Integer, Integer, String> toolTipText = (s, t) -> s < t ? "Load " + (t - s) + " more reference spectr" + (t - s > 1 ? "a." : "um.") : "Load more reference spectra.";

        ToolbarToggleButton loadAll = new ToolbarToggleButton(Icons.LOAD_ALL_24, toolTipText.apply(size, total));
        loadAll.setEnabled(source.getSize() < source.getTotalSize());
        source.addSizeChangedListener((s, t) -> {
            loadAll.setToolTipText(toolTipText.apply(s, t));
            loadAll.setEnabled(s < t);
        });
        loadAll.addActionListener(e -> {
            source.setLoadAll();
            source.reloadData();
        });
        tb.add(firstGap);
        tb.add(secondGap);
        tb.add(loadAll);

        return tb;
    }

    @Override
    protected EventList<MatcherEditor<SpectralMatchBean>> getSearchFieldMatchers() {
        return GlazedLists.eventListOf(
                new TextComponentMatcherEditor<>(searchField, (baseList, element) -> {
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
