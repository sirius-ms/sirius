/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.WrapLayout;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.FeatureAnnotations;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.unijena.bioinf.fingerid.AddExternalStructureJJob.SKETCHED_DB_NAME;

@Slf4j
public class DBFilterPanel extends JPanel implements ActiveElementChangedListener<FingerprintCandidateBean, InstanceBean>, CustomDataSources.DataSourceChangeListener {
    public final static Set<String> NON_FILTERABLE = Set.of(
            DataSource.ALL.name(), DataSource.TRAIN.name()
    );

    public final static Color specifiedDbBgColor = Colors.Menu.ICON_BLUE;
    public final static Color fallbackDbBgColor = Colors.EXPANSIVE_SEARCH_WARNING;

    private final Queue<FilterChangeListener> listeners = new ConcurrentLinkedQueue<>();

    protected long bitSet;
    protected List<JCheckBox> checkboxes;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    private JCheckBox sketchedCheckbox;


    public DBFilterPanel(StructureList sourceList) {
        setLayout(new WrapLayout(FlowLayout.LEFT, 5, 1));
        setBorder(BorderFactory.createEmptyBorder(0, 0, GuiUtils.SMALL_GAP, 0));
        this.checkboxes = new ArrayList<>(CustomDataSources.size());
        for (CustomDataSources.Source source : CustomDataSources.sources()) {
            if (!NON_FILTERABLE.contains(source.name())){
                JCheckBox b = new JCheckBox(source.displayName());
                b.setName(source.name());
                checkboxes.add(b);
            }
        }
        addBoxes();
        CustomDataSources.addListener(this);
        sourceList.addActiveResultChangedListener(this);
    }

    public void addFilterChangeListener(FilterChangeListener listener) {
        listeners.add(listener);
    }

    public void fireFilterChangeEvent() {
        listeners.forEach(l -> l.fireFilterChanged(bitSet, sketchedCheckbox.isSelected()));
    }

    protected void addBoxes() {
        sketchedCheckbox = new JCheckBox(SKETCHED_DB_NAME);
        sketchedCheckbox.addChangeListener(e -> {
            if (!isRefreshing.get()) {
                fireFilterChangeEvent();
            }
        });
        add(sketchedCheckbox);

        checkboxes.sort(Comparator.comparing(o -> o.getText().toUpperCase()));

        this.bitSet = 0L;
        for (final JCheckBox box : checkboxes) {
            if (box.isSelected())
                this.bitSet |= CustomDataSources.getSourceFromName(box.getName()).flag();
            add(box);
            box.addChangeListener(e -> {
                if (!isRefreshing.get()) {
                    if (box.isSelected())
                        bitSet |= CustomDataSources.getSourceFromName(box.getName()).flag();
                    else
                        bitSet &= ~CustomDataSources.getSourceFromName(box.getName()).flag();
                    fireFilterChangeEvent();
                }
            });
        }
    }

    protected void reset(InstanceBean parent, List<FingerprintCandidateBean> resultElements) {
        Set<String> specifiedDBs = Optional.ofNullable(parent)
                .map(p -> p.getSourceFeature().getTopAnnotations())
                .map(FeatureAnnotations::getSpecifiedDatabases)
                .map(HashSet::new)
                .orElse(new HashSet<>());

        Set<String> fallbackDBs = Optional.ofNullable(parent)
                .map(p -> p.getSourceFeature().getTopAnnotations())
                .map(FeatureAnnotations::getExpandedDatabases)
                .map(HashSet::new)
                .orElse(new HashSet<>());

        isRefreshing.set(true);
        sketchedCheckbox.setSelected(false);
        sketchedCheckbox.setVisible(resultElements.stream().anyMatch(FingerprintCandidateBean::isSketched));
        bitSet = 0;
        try {
            for (JCheckBox checkbox : checkboxes) {
                checkbox.setSelected(false);
                if (specifiedDBs.contains(checkbox.getName())) {
                    checkbox.setForeground(specifiedDbBgColor);
                    checkbox.setToolTipText("Was selected for searching");
                } else if (fallbackDBs.contains(checkbox.getName())) {
                    checkbox.setForeground(fallbackDbBgColor);
                    checkbox.setToolTipText("Was used as to expand search space by expansive search.");
                } else {
                    checkbox.setForeground(null);
                    checkbox.setToolTipText(null);
                }
            }
        } finally {
            fireFilterChangeEvent();
            isRefreshing.set(false);
        }
    }

    public boolean toggle() {
        setVisible(!isVisible());
        return isVisible();
    }

    @Override
    public void resultsChanged(InstanceBean elementsParent, FingerprintCandidateBean selectedElement, List<FingerprintCandidateBean> resultElements, ListSelectionModel selections) {
        reset(elementsParent, resultElements);
    }

    @Override
    public void fireDataSourceChanged(CustomDataSources.Source change, boolean removed) {
        Jobs.runEDTLater(() -> {
            isRefreshing.set(true);
            boolean c = false;

            if (removed)
                c = checkboxes.removeIf(b -> b.getName().equals(change.name()));
            else
                if (checkboxes.stream().noneMatch(b -> b.getName().equals(change.name()))){
                    JCheckBox b = new JCheckBox(change.displayName());
                    b.setName(change.name());
                    checkboxes.add(b);
                    c = true;
                } else {
                    log.warn("Got change request to add DataSource '{}', but it already exists? Ignoring!", change.name());
                }

            if (c) {
                removeAll();
                addBoxes();
                revalidate();
                repaint();
                fireFilterChangeEvent();
            }
            isRefreshing.set(false);
        });
    }

    public interface FilterChangeListener extends EventListener {
        void fireFilterChanged(long filterSet, boolean sketched);
    }
}
