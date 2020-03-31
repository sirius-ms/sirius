package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.ms.gui.configs.Icons;

import javax.swing.*;

public class ActZodiacConfigPanel extends ActivatableConfigPanel<JLabel> {

    public ActZodiacConfigPanel() {
        super("ZODIAC", Icons.NET_32, false, () ->  new JLabel("PLEASE ADD ME SOME Parameters"));
    }

    @Override
    protected void setComponentsEnabled(boolean enabled) {
        super.setComponentsEnabled(enabled);
    }
}
