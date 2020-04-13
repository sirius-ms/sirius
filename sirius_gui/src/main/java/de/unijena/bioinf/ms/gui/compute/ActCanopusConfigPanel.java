package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.frontend.subtools.canopus.CanopusOptions;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */

public class ActCanopusConfigPanel extends ActivatableConfigPanel<SubToolConfigPanel<CanopusOptions>>{
    public ActCanopusConfigPanel() {
        super("CANOPUS", Icons.BUG_32, true, () -> {
            SubToolConfigPanel<CanopusOptions> p =  new SubToolConfigPanel<>(CanopusOptions.class) {};
            p.add(new JLabel("Parameter-Free! Nothing to set up here. =)"));
//            l.setBorder(BorderFactory.createEmptyBorder(0, GuiUtils.LARGE_GAP, 0, 0));
            return p;
        });
    }
}
