package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

public abstract class ActivatableConfigPanel<C extends ConfigPanel> extends TwoColumnPanel {

    protected ToolbarToggleButton activationButton;
    protected final String toolName;
    protected final C content;

    public ActivatableConfigPanel(String toolname, Icon buttonIcon, boolean needsCSIConnection, Supplier<C> contentSuppl) {
        this(toolname, null, buttonIcon, needsCSIConnection, contentSuppl);
    }

    public ActivatableConfigPanel(String toolname, String toolDescription, Icon buttonIcon, boolean needsCSIConnection, Supplier<C> contentSuppl) {
        super();
        this.toolName = toolname;
        this.content = contentSuppl.get();

        activationButton = new ToolbarToggleButton(this.toolName, buttonIcon);
        activationButton.setPreferredSize(new Dimension(110, 60));
        activationButton.setMaximumSize(new Dimension(110, 60));
        activationButton.setMinimumSize(new Dimension(110, 60));
        if (toolDescription != null)
            activationButton.setToolTipText(toolDescription);
        else if (content instanceof SubToolConfigPanel)
            activationButton.setToolTipText(GuiUtils.formatToolTip(((SubToolConfigPanel<?>) content).toolDescription()));
        add(activationButton, content);


        if (needsCSIConnection) {
            MainFrame.CONNECTION_MONITOR.addConectionStateListener(evt -> setButtonEnabled(((ConnectionMonitor.ConnectionStateEvent) evt).getConnectionCheck().isConnected()));
            setButtonEnabled(MainFrame.MF.isFingerid());
        } else {
            setButtonEnabled(true);
        }

        activationButton.addActionListener(e -> {
            setComponentsEnabled(activationButton.isSelected());
            //todo do we want switch?
            //            activationButton.setToolTipText((activationButton.isSelected() ? "Disable " + this.toolName : "Enable " + this.toolName));
        });

        activationButton.setSelected(false);
        setComponentsEnabled(activationButton.isSelected());
    }

    protected void setComponentsEnabled(final boolean enabled){
        GuiUtils.setEnabled(content,enabled);
    }


    protected void setButtonEnabled(final boolean enabled) {
        setButtonEnabled(enabled, null);
    }

    protected void setButtonEnabled(final boolean enabled, @Nullable String toolTip) {
//todo do we want switch?
        //        activationButton.setToolTipText(toolTip != null ? toolTip :
//                (enabled ? "Enable " + toolName : toolName + " Not available!")
//        );

        if (enabled) {
            activationButton.setEnabled(true);
        } else {
            activationButton.setEnabled(false);
            activationButton.setSelected(false);
        }
    }


    public boolean isToolSelected() {
        return activationButton == null || activationButton.isSelected();
    }

    public List<String> asParameterList() {
        return content.asParameterList();
    }


}

