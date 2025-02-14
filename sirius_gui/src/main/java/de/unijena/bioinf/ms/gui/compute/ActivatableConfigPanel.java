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
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourElement;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourDecorator;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfo;
import de.unijena.bioinf.ms.properties.PropertyManager;
import io.sirius.ms.sdk.model.ConnectionCheck;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isConnected;

public abstract class ActivatableConfigPanel<C extends ConfigPanel> extends TwoColumnPanel {

    public static final String DO_NOT_SHOW_TOOL_AUTOENABLE = "de.unijena.bioinf.sirius.computeDialog.autoEnable.dontAskAgain";

    protected ToolbarToggleButton activationButton;
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

    protected ActivatableConfigPanel(@NotNull SiriusGui gui, String toolname, Icon buttonIcon, Supplier<C> contentSuppl, SoftwareTourInfo tourInfo) {
        this(gui, toolname, buttonIcon, false, contentSuppl, tourInfo);
    }

    protected ActivatableConfigPanel(@NotNull SiriusGui gui, String toolname, Icon buttonIcon, boolean checkServerConnection, Supplier<C> contentSuppl, SoftwareTourInfo tourInfo) {
        this(gui, toolname, null, buttonIcon, checkServerConnection, contentSuppl, tourInfo);
    }

    protected ActivatableConfigPanel(@NotNull SiriusGui gui, String toolname, String toolDescription, Icon buttonIcon, boolean checkServerConnection, Supplier<C> contentSuppl, SoftwareTourInfo tourInfo) {
        super();
        left.anchor = GridBagConstraints.NORTH;
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
        if (tourInfo != null) {
            add(SoftwareTourDecorator.decorate(activationButton, tourInfo), content);
        } else {
            add(activationButton, content);
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
     * @param upstreamResultAvailable function that checks if the existing results of the upstream tool can be used
     */
    public void addToolDependency(ActivatableConfigPanel<?> upstreamTool, Supplier<Boolean> upstreamResultAvailable) {
        this.addToolDependencyListener((c, enabled) -> {
            if (enabled && !upstreamTool.isToolSelected() && !upstreamResultAvailable.get()) {
                upstreamTool.activationButton.doClick(0);
                showAutoEnableInfoDialog("The '" + upstreamTool.toolName + "' tool is enabled because not all selected features contain its results, but the '" + this.toolName + "' tool needs them as input.");
            }
        });
        upstreamTool.addToolDependencyListener((c, enabled) -> {
            if (!enabled && this.isToolSelected() && !upstreamResultAvailable.get()) {
                this.activationButton.doClick(0);
                showAutoEnableInfoDialog("The '" + this.toolName + "' tool is also disabled because it needs the results from the '" + upstreamTool.toolName + "' tool as input.");
            }
        });
    }

    public void showAutoEnableInfoDialog(String message) {
        if (!PropertyManager.getBoolean(DO_NOT_SHOW_TOOL_AUTOENABLE, false)) {
            new InfoDialog(gui.getMainFrame(), message, DO_NOT_SHOW_TOOL_AUTOENABLE);
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

