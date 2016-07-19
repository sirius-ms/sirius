package eu.hansolo.rangeslider;

import javax.swing.*;
import java.awt.*;


/**
 * @author Gerrit Grunwald <han.solo at muenster.de>
 *
 *     http://harmoniccode.blogspot.de/2011/11/friday-fun-component-xiv.html
 *
 */
public class RangeSlider extends JSlider {
    // <editor-fold defaultstate="collapsed" desc="Variable declarations">
    private int lowerValue;
    private int upperValue;
    private boolean rangeVisible;
    private boolean rangeSelectionEnabled;
    public static enum ThumbShape { ROUND, SQUARE, RECTANGULAR, DROP, NONE }
    private ThumbShape thumbShape = ThumbShape.ROUND;
    public static enum ThumbDesign { BRIGHT, DARK, STAINLESS, DARK_STAINLESS }
    private ThumbDesign thumbDesign = ThumbDesign.DARK;
    public static enum TrackWidth { THIN, MEDIUM, THICK}
    private TrackWidth trackWidth = TrackWidth.THIN;
    private boolean darkTrack;
    private Color rangeColor;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public RangeSlider() {
        this(0, 100);
    }

    public RangeSlider(int min, int max) {
        super(min, max);
        rangeVisible = true;
        rangeSelectionEnabled = true;
        rangeColor = new Color(51, 204, 255);
        darkTrack = false;
        setOpaque(false);
        setFocusTraversalKeysEnabled(true);
        initSlider();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Initialization">
    private void initSlider() {
        setOrientation(HORIZONTAL);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Visualization">
    @Override
    public void updateUI() {
        setUI(new RangeSliderUI(this));
        // Update UI of the slider.
        updateLabelUIs();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    @Override
    public int getValue() {
        return super.getValue();
    }

    @Override
    public void setValue(int value) {
        int oldValue = getValue();
        if (oldValue == value) {
            return;
        }
        int oldExtent = getExtent();
        int newValue = Math.min(Math.max(getMinimum(), value), oldValue + oldExtent);
        int newExtent = oldExtent + oldValue - newValue;
        lowerValue = newValue;
        getModel().setRangeProperties(newValue, newExtent, getMinimum(), getMaximum(), getValueIsAdjusting());
    }

    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(orientation);
        updateUI();
    }

    public boolean isRangeVisible() {
        return rangeVisible;
    }

    public void setRangeVisible(final boolean RANGE_VISIBLE) {
        rangeVisible = RANGE_VISIBLE;
        updateUI();
    }

    public boolean isRangeSelectionEnabled() {
        return rangeSelectionEnabled;
    }

    public void setRangeSelectionEnabled(final boolean RANGE_SELECTION_ENABLED) {
        rangeSelectionEnabled = RANGE_SELECTION_ENABLED;
        if (!RANGE_SELECTION_ENABLED) {
            rangeVisible = false;
            setUpperValue(getMaximum());
        }
        updateUI();
    }

    public int getLowerValue() {
        return lowerValue;
    }

    public void setLowerValue(final int LOWER_VALUE) {
        setValue(LOWER_VALUE);
    }

    public int getUpperValue() {
        return upperValue;
    }

    public void setUpperValue(final int UPPER_VALUE) {
        lowerValue = getValue();
        int newExtent = Math.min(Math.max(0, UPPER_VALUE - lowerValue), getMaximum() - lowerValue);
        setExtent(newExtent);
        upperValue = UPPER_VALUE > lowerValue ? lowerValue + newExtent : lowerValue;
        getModel().setExtent(newExtent);
    }

    public ThumbShape getThumbShape() {
        return thumbShape;
    }

    public void setThumbShape(final ThumbShape THUMB_SHAPE) {
        thumbShape = THUMB_SHAPE;
        updateUI();
    }

    public ThumbDesign getThumbDesign() {
        return thumbDesign;
    }

    public void setThumbDesign(final ThumbDesign THUMB_DESIGN) {
        thumbDesign = THUMB_DESIGN;
        updateUI();
    }

    public TrackWidth getTrackWidth() {
        return trackWidth;
    }

    public void setTrackWidth(final TrackWidth TRACK_DESIGN) {
        trackWidth = TRACK_DESIGN;
        updateUI();
    }

    public boolean isDarkTrack() {
        return darkTrack;
    }

    public void setDarkTrack(final boolean DARK_TRACK) {
        darkTrack = DARK_TRACK;
        updateUI();
    }

    public Color getRangeColor() {
        return rangeColor;
    }

    public void setRangeColor(final Color RANGE_COLOR) {
        rangeColor = RANGE_COLOR;
        updateUI();
    }
    // </editor-fold>
}
