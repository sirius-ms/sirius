package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ms.gui.configs.Colors;

import javax.swing.*;

public class MessageBanner extends JLabel {
    public enum BannerType {GOOD, INFO, WARNING, ERROR}
    public MessageBanner() {
        this(null, BannerType.INFO);
    }

    public MessageBanner(String message, BannerType bannerType) {
        super(message);
        setBorder(BorderFactory.createEmptyBorder(3, GuiUtils.SMALL_GAP, 3, GuiUtils.SMALL_GAP));
        setOpaque(true);
        setBannerType(bannerType);
    }

    public void update(String message, BannerType bannerType) {
        setMessage(message);
        setBannerType(bannerType);
    }
    public void update(String message, BannerType bannerType, boolean visible) {
        update(message, bannerType);
        setVisible(visible);
    }

    public void setMessage(String message) {
        setText(message);
    }

    public void setBannerType(BannerType bannerType) {
        switch (bannerType) {
            case GOOD -> setBackground(Colors.TEXT_GOOD);
            case INFO -> setBackground(Colors.TEXT_LINK);
            case WARNING -> setBackground(Colors.TEXT_WARN);
            case ERROR -> setBackground(Colors.TEXT_ERROR);
        }
    }
}
