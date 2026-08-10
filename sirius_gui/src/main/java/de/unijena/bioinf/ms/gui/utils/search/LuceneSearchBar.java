/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
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

package de.unijena.bioinf.ms.gui.utils.search;

import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.PlaceholderTextField;
import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import io.sirius.ms.sdk.SiriusClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The collapsed state of the feature search bar: a compact, read-only summary of the committed
 * query in the narrow left rail. Clicking (or focusing) it expands the {@link SearchBarOverlay}
 * to the right over the result view, where the query is built with chips and autocompletion.
 * <p>
 * The committed query lives in the {@link FeatureFilterModel}'s shared search text document
 * (also shown in the filter dialog's fulltext field), so this bar needs no state of its own.
 */
public class LuceneSearchBar extends JPanel {

    private final PlaceholderTextField summaryField;
    private final FeatureFilterModel filterModel;
    private final SearchableFieldsProvider fieldsProvider;
    private final Supplier<List<ModelChip>> modelChipSupplier;
    private final Runnable openFilterDialog;

    @Nullable
    private SearchBarOverlay overlay;

    public LuceneSearchBar(@NotNull SiriusClient siriusClient, @NotNull String projectId,
                           @NotNull FeatureFilterModel filterModel,
                           @NotNull Supplier<List<ModelChip>> modelChipSupplier,
                           @NotNull Runnable openFilterDialog) {
        super(new BorderLayout());
        this.filterModel = filterModel;
        this.modelChipSupplier = modelChipSupplier;
        this.openFilterDialog = openFilterDialog;
        this.fieldsProvider = new SearchableFieldsProvider(siriusClient, projectId);

        summaryField = new PlaceholderTextField();
        summaryField.setPlaceholder("Search or add filters...");
        summaryField.setToolTipText(GuiUtils.formatToolTip(
                "Search the feature list. Click to open the query builder with autocompletion on all searchable fields."));
        summaryField.setEditable(false);
        summaryField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                openOverlay();
            }
        });
        summaryField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                openOverlay();
            }
        });
        add(summaryField, BorderLayout.CENTER);

        // keep the summary in sync with commits from anywhere (overlay, dialog, reset)
        filterModel.addUpdateCompleteListener(evt -> refreshSummary());
        refreshSummary();
    }

    /**
     * Expands the query-builder overlay anchored at this bar.
     */
    public void openOverlay() {
        if (!isShowing())
            return;
        if (overlay == null) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            if (owner == null)
                return;
            overlay = new SearchBarOverlay(owner, filterModel, fieldsProvider, modelChipSupplier,
                    openFilterDialog, this::refreshSummary);
        }
        if (!overlay.isVisible())
            overlay.openAt(this);
    }

    /**
     * Updates the collapsed one-line summary from the committed state.
     */
    public void refreshSummary() {
        String userQuery = Optional.ofNullable(filterModel.getSearchText()).orElse("");
        summaryField.setText(userQuery);
        summaryField.setCaretPosition(0);
        if (!userQuery.isEmpty())
            summaryField.setToolTipText(GuiUtils.formatToolTip("Current search query:", userQuery));
    }
}
