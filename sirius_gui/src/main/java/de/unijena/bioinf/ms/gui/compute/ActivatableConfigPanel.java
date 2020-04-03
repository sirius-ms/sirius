package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public abstract class ActivatableConfigPanel<C extends JComponent> extends TwoColumnPanel {

    protected ToolbarToggleButton activationButton;
    protected final String toolName;
    protected final C content;

    public ActivatableConfigPanel(String toolname, Icon buttonIcon, boolean needsCSIConnection, Supplier<C> contentSuppl ) {
        super();
        this.toolName = toolname;
        this.content = contentSuppl.get();

        activationButton = new ToolbarToggleButton(this.toolName, buttonIcon);
        activationButton.setPreferredSize(new Dimension(110, 60));
        activationButton.setMaximumSize(new Dimension(110, 60));
        activationButton.setMinimumSize(new Dimension(110, 60));
        add(activationButton, content);


        if (needsCSIConnection) {
            MainFrame.CONNECTION_MONITOR.addConectionStateListener(evt -> setButtonEnabled(((ConnectionMonitor.ConnectionStateEvent) evt).getConnectionCheck().isConnected()));
            setButtonEnabled(MainFrame.MF.isFingerid());
        } else {
            setButtonEnabled(true);
        }

        activationButton.addActionListener(e -> {
            setComponentsEnabled(activationButton.isSelected());
            activationButton.setToolTipText((activationButton.isSelected() ? "Disable " + this.toolName : "Enable " + this.toolName));
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
        activationButton.setToolTipText(toolTip != null ? toolTip :
                (enabled ? "Enable " + toolName : toolName + " Not available!")
        );

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
}

