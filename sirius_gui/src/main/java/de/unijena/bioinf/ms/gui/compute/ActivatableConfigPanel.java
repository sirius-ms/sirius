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
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.ms.nightsky.sdk.model.ConnectionCheck;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static de.unijena.bioinf.ms.gui.net.ConnectionChecks.isConnected;

public abstract class ActivatableConfigPanel<C extends ConfigPanel> extends TwoColumnPanel {

    protected ToolbarToggleButton activationButton;
    protected final String toolName;
    protected final String[] toolDescription;
    protected final C content;
    protected PropertyChangeListener listener;
    protected final SiriusGui gui;

    protected LinkedHashSet<EnableChangeListener<C>> listeners = new LinkedHashSet<>();

    protected ActivatableConfigPanel(@NotNull SiriusGui gui, String toolname, Icon buttonIcon, Supplier<C> contentSuppl) {
        this(gui, toolname, buttonIcon, false, contentSuppl);
    }

    protected ActivatableConfigPanel(@NotNull SiriusGui gui, String toolname, Icon buttonIcon, boolean checkServerConnection, Supplier<C> contentSuppl) {
        this(gui, toolname, null, buttonIcon, checkServerConnection, contentSuppl);
    }

    protected ActivatableConfigPanel(@NotNull SiriusGui gui, String toolname, String toolDescription, Icon buttonIcon, boolean checkServerConnection, Supplier<C> contentSuppl) {
        super();
        left.anchor = GridBagConstraints.NORTH;
        this.toolName = toolname;
        this.content = contentSuppl.get();
        this.gui = gui;

        activationButton = new ToolbarToggleButton(this.toolName, buttonIcon);
        activationButton.setPreferredSize(new Dimension(110, 60));
        activationButton.setMaximumSize(new Dimension(110, 60));
        activationButton.setMinimumSize(new Dimension(110, 60));
        if (toolDescription != null)
            this.toolDescription = new String[]{toolDescription};
        else if (content instanceof SubToolConfigPanel)
            this.toolDescription = ((SubToolConfigPanel<?>) content).toolDescription();
        else
            this.toolDescription = new String[]{};

        activationButton.setToolTipText(GuiUtils.formatAndStripToolTip(this.toolDescription));
        add(activationButton, content);


        if (checkServerConnection) {
            listener = evt -> setButtonEnabled(isConnected(((ConnectionMonitor.ConnectionStateEvent) evt).getConnectionCheck()));
            gui.getConnectionMonitor().addConnectionStateListener(listener);
            @Nullable ConnectionCheck check = gui.getConnectionMonitor().getCurrentCheckResult();
            setButtonEnabled(check == null || isConnected(check));
        } else {
            listener = null;
            setButtonEnabled(true);
        }

        activationButton.addActionListener(e -> setComponentsEnabled(activationButton.isSelected()));
        activationButton.setSelected(false);
        setComponentsEnabled(activationButton.isSelected());
    }

    protected void setComponentsEnabled(final boolean enabled){
        GuiUtils.setEnabled(content, enabled);
        listeners.forEach(e -> e.onChange(content, enabled));
    }


    protected void setButtonEnabled(final boolean enabled) {
        setButtonEnabled(enabled, null);
    }

    protected void setButtonEnabled(final boolean enabled, @Nullable String disabledMessage) {
        if (enabled != activationButton.isEnabled()) {
            if (enabled) {
                activationButton.setEnabled(true);
                activationButton.setToolTipText(GuiUtils.formatAndStripToolTip(this.toolDescription));
            } else {
                activationButton.setEnabled(false);
                activationButton.setSelected(false);
                activationButton.setToolTipText(GuiUtils.formatAndStripToolTip(Stream.concat(Stream.of("Disabled: " + disabledMessage), Arrays.stream(this.toolDescription)).toList()));
            }
        }
    }

    public void destroy(){
        if (listener != null)
            gui.getConnectionMonitor().removePropertyChangeListener(listener);
    }

    boolean isToolSelected() {
        return activationButton == null || activationButton.isSelected();
    }

    public C getContent() {
        return content;
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

}

