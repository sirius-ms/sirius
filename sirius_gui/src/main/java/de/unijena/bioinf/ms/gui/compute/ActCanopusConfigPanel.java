package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.RelativeLayout;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

public class ActCanopusConfigPanel extends ActivatableConfigPanel<JLabel>{
    public ActCanopusConfigPanel() {
        super("CANOPUS", Icons.BUG_32, true, () ->  new JLabel("Nothing to set up here =)"));
    }
}
