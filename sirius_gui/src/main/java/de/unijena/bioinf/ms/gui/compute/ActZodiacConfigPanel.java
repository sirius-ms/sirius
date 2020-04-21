package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;

import javax.swing.*;

public class ActZodiacConfigPanel extends ActivatableConfigPanel<ZodiacConfigPanel> {

    public ActZodiacConfigPanel() {
        super("ZODIAC", Icons.NET_32, false, ZodiacConfigPanel::new);
    }
}
