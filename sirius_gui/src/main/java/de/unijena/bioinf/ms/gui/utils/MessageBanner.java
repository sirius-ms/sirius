package de.unijena.bioinf.ms.gui.utils;

import de.unijena.bioinf.ms.gui.configs.Colors;

import javax.swing.*;

public class MessageBanner extends JLabel {
    public enum BannerType {GOOD, INFO, WARNING, ERROR}

    /** the running auto hide of {@link #showTransient}, so a second message replaces the first one's timer */
    private Timer autoHide;
    public MessageBanner() {
        this(true);
    }

    public MessageBanner(boolean visible) {
        this(null, BannerType.INFO, visible);
    }

    public MessageBanner(String message, BannerType bannerType) {
        this(message, bannerType, true);
    }
    public MessageBanner(String message, BannerType bannerType, boolean visible) {
        super(message);
        setBorder(BorderFactory.createEmptyBorder(3, GuiUtils.SMALL_GAP, 3, GuiUtils.SMALL_GAP));
        setOpaque(true);
        setForeground(Colors.Themes.Light.FOREGROUND);
        setBannerType(bannerType);
        setVisible(visible);
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

    /**
     * Shows a message that disappears again on its own. For confirmations the user should notice but never
     * have to dismiss - a modal dialog for every successful save would be in the way.
     *
     * @param millis how long the banner stays visible
     */
    public void showTransient(String message, BannerType bannerType, int millis) {
        update(message, bannerType, true);
        stopAutoHide();
        autoHide = new Timer(millis, e -> setVisible(false));
        autoHide.setRepeats(false);
        autoHide.start();
    }

    /** Hides the banner and cancels a pending {@link #showTransient} auto hide. */
    public void hideBanner() {
        stopAutoHide();
        setVisible(false);
    }

    private void stopAutoHide() {
        if (autoHide != null) {
            autoHide.stop();
            autoHide = null;
        }
    }

    public void setBannerType(BannerType bannerType) {
        switch (bannerType) {
            //a banner sits behind text; Colors.GOOD is the saturated signal green used for icons and borders
            case GOOD -> setBackground(Colors.GOOD_IS_GREEN_PALE);
            case INFO -> setBackground(Colors.INFO);
            case WARNING -> setBackground(Colors.WARN);
            case ERROR -> setBackground(Colors.ERROR);
        }
    }
}
