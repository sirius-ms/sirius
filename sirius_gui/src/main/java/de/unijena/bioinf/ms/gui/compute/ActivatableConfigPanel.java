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
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isConnected;

public abstract class ActivatableConfigPanel<C extends ConfigPanel> extends JPanel {

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

    private boolean suppressDependencyListeners = false;

    protected LinkedHashSet<EnableChangeListener<C>> listeners = new LinkedHashSet<>();

    protected Set<String> disabledReasons = new HashSet<>();
    protected String notConnectedMessage = "Cannot connect to the server";  // Can be overridden in subclasses

    protected final long totalCompounds;
    protected final long computedCompounds;
    protected final List<ActivatableConfigPanel<?>> upstreamTools = new ArrayList<>();
    protected final List<ActivatableConfigPanel<?>> downstreamTools = new ArrayList<>();
    protected boolean optionalTool = false;

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
        countLabel.setText(String.format("<html><small>%s / %s computed</small></html>", computedCompounds, totalCompounds));
        countLabel.setToolTipText(String.format("%s of %s selected features already have %s results", computedCompounds, totalCompounds, toolName));

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

    protected abstract boolean isComputed(@NotNull ComputedSubtools computedSubtools);

    protected boolean allComputed() {
        return computedCompounds == totalCompounds;
    }

    protected void processConnectionCheck(ConnectionCheck check) {
        setButtonEnabled(isConnected(check), notConnectedMessage);
    }

    protected void setComponentsEnabled(final boolean enabled) {
        GuiUtils.setEnabled(content, enabled);

        if (!suppressDependencyListeners) {
            for (ActivatableConfigPanel<?> upstreamTool : upstreamTools) {
                if (enabled && !upstreamTool.isToolSelected() && !upstreamTool.allComputed() && !upstreamTool.optionalTool) {
                    if (upstreamTool.computedCompounds == 0) {
                        upstreamTool.activationButton.doClick(0);
                    } else {
                        showAutoEnableInfoDialog(String.format("<html>Results from upstream tool(s) are needed for the tool you selected but are only available for %s of %s features.</html>", upstreamTool.computedCompounds, upstreamTool.totalCompounds), DO_NOT_SHOW_PARTIAL_RESULTS_UPSTREAM);
                    }
                }
            }

            for (ActivatableConfigPanel<?> downstreamTool : downstreamTools) {
                if (!enabled && downstreamTool.isToolSelected() && !allComputed() && !optionalTool) {
                    if (computedCompounds == 0) {
                        downstreamTool.activationButton.doClick(0);
                    } else {
                        showAutoEnableInfoDialog(String.format("<html>Results from the tool you deactivated are needed for downstream tool(s) but are only available for %s of %s features.</html>", computedCompounds, totalCompounds), DO_NOT_SHOW_PARTIAL_RESULTS_DOWNSTREAM);
                    }
                }
            }

            if (enabled) {
                // Check if enabling this tool creates a "broken chain" either upstream or downstream and activate missing tools
                List<ActivatableConfigPanel<?>> brokenChain = checkBrokenChain(new ArrayList<>(), t -> t.upstreamTools);
                brokenChain.addAll(checkBrokenChain(new ArrayList<>(), t -> t.downstreamTools));
                for (ActivatableConfigPanel<?> missingTool : new HashSet<>(brokenChain)) {
                    missingTool.clickIgnoreDependencies();
                }
            } else {
                // Check if disabling this tool creates a broken chain, and, if so, disable the downstream tools
                if (findEnabled(t -> t.upstreamTools) != null) {
                    ActivatableConfigPanel<?> downstreamEnabled = findEnabled(t -> t.downstreamTools);
                    while (downstreamEnabled != null) {
                        downstreamEnabled.clickIgnoreDependencies();
                        downstreamEnabled = findEnabled(t -> t.downstreamTools);
                    }
                }
            }
        }

        listeners.forEach(e -> e.onChange(content, enabled));
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



    @FunctionalInterface
    public interface EnableChangeListener<C extends ConfigPanel> {
        void onChange(C content, boolean enabled);
    }

    /**
     * Add upstream tool for auto enabling/disabling
     * @param upstreamTool the tool which produces the data required for this tool
     */
    public void addToolDependency(ActivatableConfigPanel<?> upstreamTool) {
        upstreamTools.add(upstreamTool);
        upstreamTool.downstreamTools.add(this);
    }

    public void showAutoEnableInfoDialog(String message, String property) {
        //use tutorial info mechanism to not present dialog multiple times in one session.
        if (gui.getProperties().isAskedTutorialThisSession(property))
            return;
        else
            gui.getProperties().setTutorialKnownForThisSession(property);

        if (!PropertyManager.getBoolean(property, false)) {
            new InfoDialog(gui.getMainFrame(), message, property);
        }
    }

    /**
     * Activates/deactivates the panel and applies preset parameters to the UI
     * @throws UnsupportedOperationException if the parameter values are not compatible with the UI
     */
    public void applyValuesFromPreset(boolean enable, Map<String, String> preset) {
        if (enable != isToolSelected()) {
            clickIgnoreDependencies();
        }
        content.applyValuesFromPreset(preset);
    }

    /**
     * Recursively walk the tool dependency graph and check if there is a "broken chain" - deactivated tools between the current and some activated tool
     * @param currentChain the chain of deactivated tools found so far up to this
     * @param nextTools either upstream or downstream tool function
     * @return deactivated "missing" tools between this and some next active tool
     */
    private List<ActivatableConfigPanel<?>> checkBrokenChain(List<ActivatableConfigPanel<?>> currentChain, Function<ActivatableConfigPanel<?>, List<ActivatableConfigPanel<?>>> nextTools) {
        List<ActivatableConfigPanel<?>> brokenChain = new ArrayList<>();
        for (ActivatableConfigPanel<?> nextTool : nextTools.apply(this)) {
            if (nextTool.isToolSelected()) {
                brokenChain.addAll(currentChain);
            } else if (!nextTool.optionalTool) {
                List<ActivatableConfigPanel<?>> nextChain = new ArrayList<>(currentChain);
                nextChain.add(nextTool);
                brokenChain.addAll(nextTool.checkBrokenChain(nextChain, nextTools));
            }
        }
        return brokenChain;
    }

    /**
     * Recursively finds an enabled tool among next tools
     * @param nextTools either upstream or downstream tool function
     * @return enabled tool or null if all next tools are disabled
     */
    @Nullable
    private ActivatableConfigPanel<?> findEnabled(Function<ActivatableConfigPanel<?>, List<ActivatableConfigPanel<?>>> nextTools) {
        for (ActivatableConfigPanel<?> nextTool : nextTools.apply(this)) {
            if (nextTool.isToolSelected()) {
                return nextTool;
            } else if (!nextTool.optionalTool) {
                ActivatableConfigPanel<?> furtherEnabled = nextTool.findEnabled(nextTools);
                if (furtherEnabled != null) {
                    return furtherEnabled;
                }
            }
        }
        return null;
    }

    /**
     * Simulate a click to (de)activate the tool, but ignore all upstream/downstream tool logic
     */
    protected void clickIgnoreDependencies() {
        if (suppressDependencyListeners) {
            activationButton.doClick(0);
        } else {
            suppressDependencyListeners = true;
            activationButton.doClick(0);
            suppressDependencyListeners = false;
        }

    }
}

