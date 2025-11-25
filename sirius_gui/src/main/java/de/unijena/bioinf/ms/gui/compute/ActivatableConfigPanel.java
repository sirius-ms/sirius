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

package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.dialogs.InfoDialog;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfo;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.ComputedSubtools;
import io.sirius.ms.sdk.model.ConnectionCheck;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isConnected;

public abstract class ActivatableConfigPanel<C extends ConfigPanel> extends JPanel {

    public static final String DO_NOT_SHOW_TOOL_AUTOENABLE = "de.unijena.bioinf.sirius.computeDialog.autoEnable.dontAskAgain";
    public static final String DO_NOT_SHOW_PARTIAL_RESULTS_DOWNSTREAM = "de.unijena.bioinf.sirius.computeDialog.partialResultsDownstream.dontAskAgain";
    public static final String DO_NOT_SHOW_PARTIAL_RESULTS_UPSTREAM = "de.unijena.bioinf.sirius.computeDialog.partialResultsUpstream.dontAskAgain";

    protected ToolbarToggleButton activationButton;
    protected JLabel countLabel;
    protected final String toolName;
    protected final String[] toolDescription;
    @Getter
    protected final C content;
    protected PropertyChangeListener listener;
    protected final SiriusGui gui;

    protected boolean suppressDependencyListeners = false;

    protected LinkedHashSet<EnableChangeListener<C>> listeners = new LinkedHashSet<>();

    protected Set<String> disabledReasons = new HashSet<>();
    protected String notConnectedMessage = "Cannot connect to the server";  // Can be overridden in subclasses

    protected final long totalCompounds;
    @Getter
    protected final long computedCompounds;

    protected ActivatableConfigPanel(@NotNull SiriusGui gui, String toolname, Icon buttonIcon, Supplier<C> contentSuppl, List<InstanceBean> compounds, SoftwareTourInfo tourInfo) {
        this(gui, toolname, null, buttonIcon, false, contentSuppl, compounds, tourInfo);
    }

    protected ActivatableConfigPanel(@NotNull SiriusGui gui, String toolname, Icon buttonIcon, boolean checkServerConnection, Supplier<C> contentSuppl, List<InstanceBean> compounds, SoftwareTourInfo tourInfo) {
        this(gui, toolname, null, buttonIcon, checkServerConnection, contentSuppl, compounds, tourInfo);
    }

    protected ActivatableConfigPanel(@NotNull SiriusGui gui, String toolname, String toolDescription, Icon buttonIcon, boolean checkServerConnection, Supplier<C> contentSuppl, List<InstanceBean> compounds, SoftwareTourInfo tourInfo) {
        super(new MigLayout("insets 0", "[left]10[left]","[top]"));

        this.toolName = toolname;
        this.content = contentSuppl.get();
        this.gui = gui;

        activationButton = new ToolbarToggleButton(this.toolName, buttonIcon);
        activationButton.setPreferredSize(new Dimension(110, 60));
        activationButton.setMaximumSize(new Dimension(110, 60));
        activationButton.setMinimumSize(new Dimension(110, 60));
        activationButton.setBackground(this.getBackground());
        activationButton.setRolloverEnabled(true);
        if (toolDescription != null)
            this.toolDescription = new String[]{toolDescription};
        else if (content instanceof SubToolConfigPanel)
            this.toolDescription = ((SubToolConfigPanel<?>) content).toolDescription();
        else
            this.toolDescription = new String[]{};

        activationButton.setToolTipText(GuiUtils.formatAndStripToolTip(this.toolDescription));

        totalCompounds = compounds.size();
        computedCompounds = compounds.stream().map(InstanceBean::getComputedTools).filter(this::isComputed).count();

        countLabel = new JLabel();
        updateCountLabel();

        add(activationButton,"cell 0 0, split 2, flowy, alignx center, aligny top");
        add(countLabel, "alignx center, gaptop 2");

        add(content, "cell 1 0, growx, wrap");

        if (tourInfo != null) {
            activationButton.putClientProperty(SoftwareTourInfoStore.TOUR_ELEMENT_PROPERTY_KEY, tourInfo);
        }

        if (checkServerConnection) {
            listener = evt -> processConnectionCheck(((ConnectionMonitor.ConnectionEvent) evt).getConnectionCheck());
            gui.getConnectionMonitor().addConnectionListener(listener);
            @Nullable ConnectionCheck check = gui.getConnectionMonitor().getCurrentCheckResult();
            if (check != null) {
                processConnectionCheck(check);
            }
        }

        activationButton.addActionListener(e -> setComponentsEnabled(activationButton.isSelected()));
        activationButton.setSelected(false);
        setComponentsEnabled(activationButton.isSelected());
    }

    protected void updateCountLabel() {
        countLabel.setText(String.format("<html><small>%s / %s computed</small></html>", computedCompounds, totalCompounds));
        countLabel.setToolTipText(String.format("%s of %s selected features already have %s results", computedCompounds, totalCompounds, toolName));
    }

    protected abstract boolean isComputed(@NotNull ComputedSubtools computedSubtools);

    protected boolean allComputed() {
        return getComputedCompounds() == totalCompounds;
    }

    protected void processConnectionCheck(ConnectionCheck check) {
        setButtonEnabled(isConnected(check), notConnectedMessage);
    }

    protected void setComponentsEnabled(final boolean enabled) {
        GuiUtils.setEnabled(content, enabled);
        listeners.forEach(e -> {
            if (e instanceof ActivatableConfigPanel.ToolDependencyListener<C> && suppressDependencyListeners) return;
            e.onChange(content, enabled);
        });
    }

    protected void setButtonEnabled(final boolean enabled, @Nullable String reason) {
        if (enabled) {
            disabledReasons.remove(reason);
        } else {
            disabledReasons.add(reason);
            activationButton.setSelected(false);
        }
        activationButton.setEnabled(disabledReasons.isEmpty());
        activationButton.setToolTipText(GuiUtils.formatAndStripToolTip(Stream.concat(disabledReasons.stream().map(r -> "Disabled: " + r), Arrays.stream(this.toolDescription)).toList()));
    }

    public void destroy(){
        if (listener != null)
            gui.getConnectionMonitor().removePropertyChangeListener(listener);
    }

    boolean isToolSelected() {
        return activationButton == null || activationButton.isSelected();
    }

    public List<String> asParameterList() {
        return content.asParameterList();
    }

    @NotNull
    public Map<String, String> asConfigMap() {
        return content.asConfigMap();
    }

    public boolean removeEnableChangeListener(EnableChangeListener<C> listener) {
        return listeners.remove(listener);
    }

    public void addEnableChangeListener(EnableChangeListener<C> listener) {
        listeners.add(listener);
    }

    /**
     * Add a listener for auto enabling tools
     */
    public void addToolDependencyListener(ToolDependencyListener<C> listener) {
        addEnableChangeListener(listener);
    }


    @FunctionalInterface
    public interface EnableChangeListener<C extends ConfigPanel> {
        void onChange(C content, boolean enabled);
    }

    /**
     * Separate interface to distinguish and suppress dependency listeners
     */
    public interface ToolDependencyListener<C extends ConfigPanel> extends EnableChangeListener<C> {}

    /**
     * Add listeners that enable the upstream tool if this gets enabled, and disable this if the upstream gets disabled
     * @param upstreamTool the tool which produces the data required for this tool
     */
    public void addToolDependency(ActivatableConfigPanel<?> upstreamTool) {
        this.addToolDependencyListener((c, enabled) -> {
            if (enabled && !upstreamTool.isToolSelected() && !upstreamTool.allComputed()) {
                if (upstreamTool.getComputedCompounds() == 0) {
                    upstreamTool.activationButton.doClick(0);
                } else {
                    showPartialResultsUpstreamInfoDialog(String.format("<html>Results from upstream tool(s) are needed for the tool you selected but are only available for %s of %s features.</html>", upstreamTool.getComputedCompounds(), upstreamTool.totalCompounds));
                }
            }
        });
        upstreamTool.addToolDependencyListener((c, enabled) -> {
            if (!enabled && this.isToolSelected() && !upstreamTool.allComputed()) {
                if (upstreamTool.getComputedCompounds() == 0) {
                    this.activationButton.doClick(0);
                } else {
                    showPartialResultsDownstreamInfoDialog(String.format("<html>Results from the tool you deactivated are needed for downstream tool(s) but are only available for %s of %s features.</html>", getComputedCompounds(), totalCompounds));
                }
            }
        });
    }

    public void showAutoEnableInfoDialog(String message) {
        if (!PropertyManager.getBoolean(DO_NOT_SHOW_TOOL_AUTOENABLE, false)) {
            new InfoDialog(gui.getMainFrame(), message, DO_NOT_SHOW_TOOL_AUTOENABLE);
        }
    }

    public void showPartialResultsDownstreamInfoDialog(String message) {
        //use tutorial info mechanism to not present dialog multiple times in one session.
        if (gui.getProperties().isAskedTutorialThisSession(DO_NOT_SHOW_PARTIAL_RESULTS_DOWNSTREAM))
            return;
        else
            gui.getProperties().setTutorialKnownForThisSession(DO_NOT_SHOW_PARTIAL_RESULTS_DOWNSTREAM);

        if (!PropertyManager.getBoolean(DO_NOT_SHOW_PARTIAL_RESULTS_DOWNSTREAM, false)) {
            new InfoDialog(gui.getMainFrame(), message, DO_NOT_SHOW_PARTIAL_RESULTS_DOWNSTREAM);
        }
    }

    public void showPartialResultsUpstreamInfoDialog(String message) {
        //use tutorial info mechanism to not present dialog multiple times in one session.
        if (gui.getProperties().isAskedTutorialThisSession(DO_NOT_SHOW_PARTIAL_RESULTS_UPSTREAM))
            return;
        else
            gui.getProperties().setTutorialKnownForThisSession(DO_NOT_SHOW_PARTIAL_RESULTS_UPSTREAM);

        if (!PropertyManager.getBoolean(DO_NOT_SHOW_PARTIAL_RESULTS_UPSTREAM, false)) {
            new InfoDialog(gui.getMainFrame(), message, DO_NOT_SHOW_PARTIAL_RESULTS_UPSTREAM);
        }
    }

    /**
     * Activates/deactivates the panel and applies preset parameters to the UI
     * @throws UnsupportedOperationException if the parameter values are not compatible with the UI
     */
    public void applyValuesFromPreset(boolean enable, Map<String, String> preset) {
        if (enable != isToolSelected()) {
            suppressDependencyListeners = true;  // avoid annoying dialogs in the middle of preset activation
            activationButton.doClick(0);
            suppressDependencyListeners = false;
        }
        content.applyValuesFromPreset(preset);
    }
}

