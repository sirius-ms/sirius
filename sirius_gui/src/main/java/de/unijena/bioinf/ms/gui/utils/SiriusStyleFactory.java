package de.unijena.bioinf.ms.gui.utils;

import javax.swing.*;
import javax.swing.plaf.synth.Region;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.plaf.synth.SynthStyle;
import javax.swing.plaf.synth.SynthStyleFactory;

public class SiriusStyleFactory extends SynthStyleFactory {
    protected static String variant = "regular";

    final SynthStyleFactory styleFactory = SynthLookAndFeel.getStyleFactory();

    static {
        SynthLookAndFeel.setStyleFactory(new SiriusStyleFactory(variant));
    }

    public SiriusStyleFactory(String variant) {
        if (variant.equals("regular") || variant.equals("mini")
                || variant.equals("small") || variant.equals("large"))
            SiriusStyleFactory.variant = variant;
    }

    @Override
    public SynthStyle getStyle(JComponent c, Region id) {
        c.putClientProperty("JComponent.sizeVariant", variant);
        return styleFactory.getStyle(c, id);
    }
}