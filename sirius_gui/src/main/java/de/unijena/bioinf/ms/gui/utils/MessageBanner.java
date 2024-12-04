package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ms.gui.configs.Colors;

import javax.swing.*;

public class MessageBanner extends JLabel {
    public enum BannerType {GOOD, INFO, WARNING, ERROR}
    public MessageBanner(String message, BannerType bannerType) {
        super(message);
        setBorder(BorderFactory.createEmptyBorder(3, GuiUtils.SMALL_GAP, 3, GuiUtils.SMALL_GAP));
        setOpaque(true);
        setForeground(Colors.Themes.Light.FOREGROUND);
        switch (bannerType) {
            case GOOD -> setBackground(Colors.GOOD);
            case INFO -> setBackground(Colors.INFO);
            case WARNING -> setBackground(Colors.WARN);
            case ERROR -> setBackground(Colors.ERROR);
        }
    }
}
