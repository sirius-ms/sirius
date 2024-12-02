package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ms.gui.configs.Colors;

import javax.swing.*;

public class MessageBanner extends JLabel {
    public enum BannerType {GOOD, INFO, WARNING, ERROR}
    public MessageBanner(String message, BannerType bannerType) {
        super(message);
        setBorder(BorderFactory.createEmptyBorder(3, GuiUtils.SMALL_GAP, 3, GuiUtils.SMALL_GAP));
`        setOpaque(true);

        switch (bannerType) {
            case GOOD -> setBackground(Colors.GOOD_IS_GREEN_PALE); //todo change to text green later
            case INFO -> setBackground(Colors.TEXT_LINK);
            case WARNING -> setBackground(Colors.TEXT_WARN);
            case ERROR -> setBackground(Colors.TEXT_ERROR);
        }
    }
}
