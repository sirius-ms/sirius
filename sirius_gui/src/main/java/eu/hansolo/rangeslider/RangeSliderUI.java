package eu.hansolo.rangeslider;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;


/**
 * @author Gerrit Grunwald <han.solo at muenster.de>
 *
 *     http://harmoniccode.blogspot.de/2011/11/friday-fun-component-xiv.html
 */
class RangeSliderUI extends BasicSliderUI {
    // <editor-fold defaultstate="collapsed" desc="Variable declarations">
    private static final Color INDICATOR_COLOR = new Color(0.3f, 0.3f, 0.3f, 0.9f);
    private static final float[] TRACK_FRAME_FRACTIONS = { 0.0f, 0.25f, 0.75f, 1.0f };
    private static final float[] BRIGHT_TRACK_FRACTIONS = { 0.0f, 0.96f, 1.0f };
    private static final float[] DARK_TRACK_FRACTIONS = { 0.0f, 0.3f, 0.75f, 1.0f };
    private static final float[] RANGE_FRACTIONS = { 0.0f, 0.3f, 0.94f, 1.0f };
    private static final Color[] TRACK_FRAME_COLORS = {
        UIManager.getColor("control").darker().darker().darker(),
        UIManager.getColor("control").darker().darker(),
        UIManager.getColor("control").darker().darker(),
        UIManager.getColor("control").brighter()
    };
    private static final Color[] BRIGHT_TRACK_COLORS = {
        UIManager.getColor("control").darker().darker(),
        UIManager.getColor("control").darker(),
        UIManager.getColor("control").darker().darker()
    };
    private static final Color[] DARK_TRACK_COLORS = {
        new Color(0x131313),
        new Color(0x242424),
        new Color(0x242424),
        new Color(0x1E1E1E)
    };
    private Rectangle upperThumbRect;
    private boolean upperThumbSelected;
    private boolean lowerDragging;
    private boolean upperDragging;
    private boolean rangeDragging;
    private int formerExtent;
    private RangeSlider.ThumbShape thumbShape = RangeSlider.ThumbShape.ROUND;
    private RangeSlider.ThumbDesign thumbDesign = RangeSlider.ThumbDesign.BRIGHT;
    private BufferedImage thumbImage = createThumbImage(getThumbSize().width, getThumbSize().height, false);
    private BufferedImage hoveredThumbImage = createThumbImage(getThumbSize().width, getThumbSize().height, true);
    private BufferedImage lowerIndicatorImage = createIndicatorImage(getThumbSize().width, getThumbSize().height, INDICATOR_COLOR, true, true);
    private BufferedImage upperIndicatorImage = createIndicatorImage(getThumbSize().width, getThumbSize().height, INDICATOR_COLOR, false, true);
    private boolean lowerThumbHover;
    private boolean upperThumbHover;
    private final RoundRectangle2D TRACK_FRAME;
    private final RoundRectangle2D TRACK;
    private final RoundRectangle2D RANGE;
    private Color[] rangeColors;
    private boolean darkTrack;
    private boolean indicatorsVisible;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public RangeSliderUI(final RangeSlider RANGE_SLIDER) {
        super(RANGE_SLIDER);
        thumbShape = RANGE_SLIDER.getThumbShape() != null ? RANGE_SLIDER.getThumbShape() : RangeSlider.ThumbShape.ROUND;
        thumbDesign = RANGE_SLIDER.getThumbDesign() != null ? RANGE_SLIDER.getThumbDesign() : RangeSlider.ThumbDesign.DARK;
        final Color RANGE_COLOR = RANGE_SLIDER.getRangeColor() != null ? RANGE_SLIDER.getRangeColor() : new Color(51, 204, 255);
        final int RED = RANGE_COLOR.getRed();
        final int GREEN = RANGE_COLOR.getGreen();
        final int BLUE = RANGE_COLOR.getBlue();
        if ((RED == GREEN) && (GREEN == BLUE)) {
            rangeColors = new Color[]{
                new Color(Color.HSBtoRGB(0.0f, 0.0f, 0.95f)),
                new Color(Color.HSBtoRGB(0.0f, 0.0f, 0.88f)),
                new Color(Color.HSBtoRGB(0.0f, 0.0f, 0.67f)),
                new Color(Color.HSBtoRGB(0.0f, 0.0f, 0.49f))
            };
        } else {
            final float RANGE_COLOR_HUE = Color.RGBtoHSB(RED, GREEN, BLUE, null)[0];
            rangeColors = new Color[]{
                new Color(Color.HSBtoRGB(RANGE_COLOR_HUE, 0.85f, 0.95f)),
                new Color(Color.HSBtoRGB(RANGE_COLOR_HUE, 0.85f, 0.88f)),
                new Color(Color.HSBtoRGB(RANGE_COLOR_HUE, 0.85f, 0.67f)),
                new Color(Color.HSBtoRGB(RANGE_COLOR_HUE, 0.85f, 0.49f))
            };
        }
        darkTrack = RANGE_SLIDER.isDarkTrack();
        slider = RANGE_SLIDER;
        initThumbs();
        lowerThumbHover = false;
        upperThumbHover = false;
        TRACK_FRAME = new RoundRectangle2D.Double();
        TRACK = new RoundRectangle2D.Double();
        RANGE = new RoundRectangle2D.Double();
        switch(thumbShape) {
            case ROUND:
                indicatorsVisible = true;
                break;
            case SQUARE:
                indicatorsVisible = true;
                break;
            default:
                indicatorsVisible = false;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Initialization">
    private void initThumbs() {
        int thumbResize = 0;
        if (((RangeSlider) slider).getTrackWidth() == RangeSlider.TrackWidth.THICK) {
            thumbResize = 0;
        }
        if (thumbImage != null) {
            thumbImage.flush();
        }
        thumbImage = createThumbImage(getThumbSize().width - thumbResize, getThumbSize().height - thumbResize, false);
        if (hoveredThumbImage != null) {
            hoveredThumbImage.flush();
        }
        hoveredThumbImage = createThumbImage(getThumbSize().width - thumbResize, getThumbSize().height - thumbResize, true);
        if (lowerIndicatorImage != null) {
            lowerIndicatorImage.flush();
        }
        lowerIndicatorImage = createIndicatorImage(getThumbSize().width, getThumbSize().height, INDICATOR_COLOR, true, ((RangeSlider) slider).getOrientation() == RangeSlider.HORIZONTAL);
        if (upperIndicatorImage != null) {
            upperIndicatorImage.flush();
        }
        upperIndicatorImage = createIndicatorImage(getThumbSize().width, getThumbSize().height, INDICATOR_COLOR, false, ((RangeSlider) slider).getOrientation() == RangeSlider.HORIZONTAL);
    }

    @Override
    public void installUI(final JComponent COMPONENT) {
        upperThumbRect = new Rectangle();
        super.installUI(COMPONENT);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Visualization">
    @Override
    public void paint(final Graphics G, final JComponent COMPONENT) {
        super.paint(G, COMPONENT);
        Rectangle clipRect = G.getClipBounds();
        if (((RangeSlider) slider).isRangeSelectionEnabled()) {
            if (upperThumbSelected) {
                // Paint lower thumb first, then threshold and upper thumb.
                if (clipRect.intersects(thumbRect)) {
                    paintLowerThumb(G);
                }
                if (clipRect.intersects(upperThumbRect)) {
                    paintUpperThumb(G);
                }
            } else {
                // Paint upper thumb first, then threshold and lower thumb.
                if (clipRect.intersects(upperThumbRect)) {
                    paintUpperThumb(G);
                }
                if (clipRect.intersects(thumbRect)) {
                    paintLowerThumb(G);
                }
            }
        } else {
            if (clipRect.intersects(thumbRect)) {
                paintLowerThumb(G);
            }
        }
    }

    @Override
    public void paintTrack(final Graphics G) {
        final Graphics2D G2 = (Graphics2D) G.create();
        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int distanceFromBorder;
        int trackDesignFactor;
        switch (((RangeSlider) slider).getTrackWidth()) {
            case MEDIUM:
                distanceFromBorder = 3;
                trackDesignFactor = 0;
                break;
            case THICK:
                distanceFromBorder = 0;
                trackDesignFactor = 1;
                break;
            case THIN:
            default:
                distanceFromBorder = 5;
                trackDesignFactor = 0;
                break;
        }
        int trackExtension = 0;
        double cornerRadius;
        switch(((RangeSlider) slider).getThumbShape()) {
            case RECTANGULAR:
                trackExtension = thumbRect.width / 4 * trackDesignFactor;
                cornerRadius = 0.25 * thumbRect.height;
                break;
            case SQUARE:
                trackExtension = thumbRect.width / 2 * trackDesignFactor + trackDesignFactor;
                cornerRadius = 0.5 * thumbRect.height;
                break;
            case DROP:
                trackExtension = 0;
                cornerRadius = 0.25 * thumbRect.height;
                break;
            case ROUND:

            default:
                trackExtension = thumbRect.width / 2 * trackDesignFactor + trackDesignFactor;
                cornerRadius = (thumbRect.height - (2 * distanceFromBorder));
                break;
        }
        // Draw track
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            TRACK_FRAME.setRoundRect(trackRect.x - trackExtension, trackRect.y + distanceFromBorder, trackRect.width - 1 +  2 * trackExtension, trackRect.height - (2 * distanceFromBorder), cornerRadius, cornerRadius);
            G2.setPaint(new LinearGradientPaint(0, (float) TRACK_FRAME.getMinY(), 0, (float) TRACK_FRAME.getMaxY(), TRACK_FRAME_FRACTIONS, TRACK_FRAME_COLORS));
            G2.fill(TRACK_FRAME);
            TRACK.setRoundRect(TRACK_FRAME.getMinX() + 1, TRACK_FRAME.getY() + 1, TRACK_FRAME.getWidth() - 2, TRACK_FRAME.getHeight() - 2, TRACK_FRAME.getArcWidth() - 0.5, TRACK_FRAME.getArcHeight() - 0.5);
            if (darkTrack) {
                G2.setPaint(new LinearGradientPaint(0, (float) TRACK.getMinY(), 0, (float) TRACK.getMaxY(), DARK_TRACK_FRACTIONS, DARK_TRACK_COLORS));
            } else {
                G2.setPaint(new LinearGradientPaint(0, (float) TRACK.getMinY(), 0, (float) TRACK.getMaxY(), BRIGHT_TRACK_FRACTIONS, BRIGHT_TRACK_COLORS));
            }
            G2.fill(TRACK);
        } else {
            TRACK_FRAME.setRoundRect(trackRect.x + distanceFromBorder, trackRect.y - trackExtension, trackRect.width - (2 * distanceFromBorder), trackRect.height - 1 + 2 * trackExtension, cornerRadius, cornerRadius);
            G2.setPaint(new LinearGradientPaint((float) TRACK_FRAME.getMinX(), 0, (float) TRACK_FRAME.getMaxX(), 0, TRACK_FRAME_FRACTIONS, TRACK_FRAME_COLORS));
            G2.fill(TRACK_FRAME);
            TRACK.setRoundRect(TRACK_FRAME.getX() + 1, TRACK_FRAME.getMinY() + 1, TRACK_FRAME.getWidth() - 2, TRACK_FRAME.getHeight() - 2, TRACK_FRAME.getArcWidth() - 0.5, TRACK_FRAME.getArcHeight() - 0.5);
            if (darkTrack) {
                G2.setPaint(new LinearGradientPaint((float) TRACK.getMinX(), 0, (float) TRACK.getMaxX(), 0, DARK_TRACK_FRACTIONS, DARK_TRACK_COLORS));
            } else {
                G2.setPaint(new LinearGradientPaint((float) TRACK.getMinX(), 0, (float) TRACK.getMaxX(), 0, BRIGHT_TRACK_FRACTIONS, BRIGHT_TRACK_COLORS));
            }
            G2.fill(TRACK);
        }

        // Draw range
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            final int LOWER_X;
            final int UPPER_X;
            if (((RangeSlider) slider).isRangeSelectionEnabled()) {
                LOWER_X = thumbRect.x + (thumbRect.width / 2);
                UPPER_X = upperThumbRect.x + (upperThumbRect.width / 2);
            } else {
                if (slider.getInverted()) {
                    LOWER_X = trackRect.x + trackRect.width;
                } else {
                    LOWER_X = trackRect.x;
                }
                UPPER_X = thumbRect.x + (thumbRect.width / 2);
            }
            final Paint OLD_PAINT = G2.getPaint();
            if (slider.getInverted()) {
                RANGE.setRoundRect(UPPER_X - trackExtension + 1, TRACK_FRAME.getY() + 1, LOWER_X - UPPER_X + 2 * trackExtension - 2, TRACK_FRAME.getHeight() - 2, TRACK_FRAME.getArcWidth() - 0.5, TRACK_FRAME.getArcHeight() - 0.5);
            } else {
                RANGE.setRoundRect(LOWER_X - trackExtension + 1, TRACK_FRAME.getY() + 1, UPPER_X - LOWER_X + 2 * trackExtension - 2, TRACK_FRAME.getHeight() - 2, TRACK_FRAME.getArcWidth() - 0.5, TRACK_FRAME.getArcHeight() - 0.5);
            }
            G2.setPaint(new LinearGradientPaint(0, (float) RANGE.getMinY(), 0, (float) RANGE.getMaxY(), RANGE_FRACTIONS, rangeColors));
            if (((RangeSlider) slider).isRangeVisible()) {
                G2.fill(RANGE);
            }
            G2.setPaint(OLD_PAINT);

        } else {
            final int LOWER_Y;
            final int UPPER_Y;
            if (((RangeSlider) slider).isRangeSelectionEnabled()) {
                LOWER_Y = thumbRect.y + (thumbRect.height / 2);
                UPPER_Y = upperThumbRect.y + (upperThumbRect.height / 2);
            } else {
                if (slider.getInverted()) {
                    LOWER_Y = trackRect.y;
                } else {
                    LOWER_Y = trackRect.y + trackRect.height;
                }
                UPPER_Y = thumbRect.y + (thumbRect.height / 2);
            }
            final Paint OLD_PAINT = G2.getPaint();
            if (slider.getInverted()) {
                RANGE.setRoundRect(TRACK_FRAME.getX() + 1, LOWER_Y - trackExtension + 1, TRACK_FRAME.getWidth() - 2, UPPER_Y - LOWER_Y + 2 * trackExtension - 2, TRACK_FRAME.getArcWidth() - 0.5, TRACK_FRAME.getArcHeight() - 0.5);
            } else {
                RANGE.setRoundRect(TRACK_FRAME.getX() + 1, UPPER_Y - trackExtension + 1, TRACK_FRAME.getWidth() - 2, LOWER_Y - UPPER_Y + 2 * trackExtension - 2, TRACK_FRAME.getArcWidth() - 0.5, TRACK_FRAME.getArcHeight() - 0.5);
            }
            G2.setPaint(new LinearGradientPaint((float) RANGE.getMinX(), 0, (float) RANGE.getMaxX(), 0, RANGE_FRACTIONS, rangeColors));
            if (((RangeSlider) slider).isRangeVisible()) {
                G2.fill(RANGE);
            }
            G2.setPaint(OLD_PAINT);
        }
        G2.dispose();
    }

    @Override
    public void paintThumb(final Graphics G) {
        // Do nothing.
    }

    private void paintLowerThumb(final Graphics G) {
        final Graphics2D G2 = (Graphics2D) G.create();
        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        G2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        G2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        if (lowerThumbHover) {
            G2.drawImage(hoveredThumbImage, thumbRect.x, thumbRect.y, null);
            slider.setToolTipText("Lower: " + Integer.toString(slider.getValue()));
        } else {
            G2.drawImage(thumbImage, thumbRect.x, thumbRect.y, null);
        }
        if (indicatorsVisible) {
            if (!((RangeSlider) slider).isRangeSelectionEnabled()) {
                if (slider.getInverted()) {
                G2.drawImage(lowerIndicatorImage, thumbRect.x, thumbRect.y, null);
                } else {
                    G2.drawImage(upperIndicatorImage, thumbRect.x, thumbRect.y, null);
                }
            } else {
                if (slider.getInverted()) {
                    G2.drawImage(upperIndicatorImage, thumbRect.x, thumbRect.y, null);
                } else {
                    G2.drawImage(lowerIndicatorImage, thumbRect.x, thumbRect.y, null);
                }
            }
        }
        G2.dispose();
    }

    private void paintUpperThumb(final Graphics G) {
        final Graphics2D G2 = (Graphics2D) G.create();
        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        G2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        G2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        if (upperThumbHover) {
            G2.drawImage(hoveredThumbImage, upperThumbRect.x, upperThumbRect.y, null);
            slider.setToolTipText("Upper: " + Integer.toString(slider.getValue() + slider.getExtent()));
        } else {
            G2.drawImage(thumbImage, upperThumbRect.x, upperThumbRect.y, null);
        }
        if (indicatorsVisible) {
            if (slider.getInverted()) {
                G2.drawImage(lowerIndicatorImage, upperThumbRect.x, upperThumbRect.y, null);
            } else  {
                G2.drawImage(upperIndicatorImage, upperThumbRect.x, upperThumbRect.y, null);
            }
        }
        G2.dispose();
    }

    @Override
    public void paintFocus(final Graphics G) {
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Listener related">
    @Override
    protected TrackListener createTrackListener(final JSlider SLIDER) {
        return new RangeTrackListener();
    }

    @Override
    protected ChangeListener createChangeListener(final JSlider SLIDER) {
        return new ChangeHandler();
    }

    private class ChangeHandler implements ChangeListener {
        @Override
        public void stateChanged(final ChangeEvent EVENT) {
            if (!lowerDragging && !upperDragging) {
                calculateThumbLocation();
                slider.repaint();
            }
        }
    }

    private class RangeTrackListener extends TrackListener {
        @Override
        public void mousePressed(final MouseEvent EVENT) {
            if (!slider.isEnabled()) {
                return;
            }
            currentMouseX = EVENT.getX();
            currentMouseY = EVENT.getY();
            if (slider.isRequestFocusEnabled()) {
                slider.requestFocus();
            }

            // Determine which thumb is pressed.  If the upper thumb is
            // selected (last one dragged), then check its position first;
            // otherwise check the position of the lower thumb first.
            boolean lowerPressed = false;
            boolean upperPressed = false;

            if (((RangeSlider) slider).isRangeSelectionEnabled()) {
                if (upperThumbSelected) {
                    if (upperThumbRect.contains(currentMouseX, currentMouseY)) {
                        upperPressed = true;
                    } else if (thumbRect.contains(currentMouseX, currentMouseY)) {
                        lowerPressed = true;
                    }
                } else {
                    if (thumbRect.contains(currentMouseX, currentMouseY)) {
                        lowerPressed = true;
                    } else if (upperThumbRect.contains(currentMouseX, currentMouseY)) {
                        upperPressed = true;
                    }
                }
            } else {
                if (thumbRect.contains(currentMouseX, currentMouseY)) {
                    lowerPressed = true;
                }
            }

            // Handle lower thumb pressed.
            if (lowerPressed) {
                switch (slider.getOrientation()) {
                    case JSlider.HORIZONTAL:
                        offset = currentMouseX - thumbRect.x;
                        break;
                    case JSlider.VERTICAL:
                        offset = currentMouseY - thumbRect.y;
                        break;
                }
                upperThumbSelected = false;
                lowerDragging = true;
                return;
            }
            lowerDragging = false;

            // Handle upper thumb pressed.
            if (upperPressed) {
                switch (slider.getOrientation()) {
                    case JSlider.HORIZONTAL:
                        offset = currentMouseX - upperThumbRect.x;
                        break;
                    case JSlider.VERTICAL:
                        offset = currentMouseY - upperThumbRect.y;
                        break;
                }
                upperThumbSelected = true;
                upperDragging = true;
                return;
            }
            upperDragging = false;

            // Following options only available if rangeSelectionEnabled
            if (((RangeSlider) slider).isRangeSelectionEnabled()) {
                // Handle range pressed
                if (RANGE.contains(EVENT.getPoint())) {
                    rangeDragging = true;
                    formerExtent = slider.getExtent();
                    switch (slider.getOrientation()) {
                        case JSlider.HORIZONTAL:
                            offset = currentMouseX - (int) RANGE.getMinX() + thumbRect.width / 2;
                            break;
                        case JSlider.VERTICAL:
                            if (slider.getInverted()) {
                                offset = currentMouseY - (int) RANGE.getMinY() + thumbRect.height / 2;
                            } else {
                                offset = currentMouseY - (int) RANGE.getMaxY() + thumbRect.height / 2;
                            }
                            break;
                    }
                }
            }
        }

        @Override
        public void mouseReleased(final MouseEvent EVENT) {
            lowerDragging = false;
            upperDragging = false;
            rangeDragging = false;
            slider.setValueIsAdjusting(false);
            super.mouseReleased(EVENT);
        }

        @Override
        public void mouseDragged(final MouseEvent EVENT) {
            if (!slider.isEnabled()) {
                return;
            }
            currentMouseX = EVENT.getX();
            currentMouseY = EVENT.getY();
            if (lowerDragging) {
                slider.setValueIsAdjusting(true);
                moveLowerThumb(0);
            } else if (upperDragging) {
                slider.setValueIsAdjusting(true);
                moveUpperThumb(0);
            } else if (rangeDragging) {
                slider.setValueIsAdjusting(true);
                if (slider.getInverted()) {
                    moveUpperThumb(0);
                    if (slider.getOrientation() == JSlider.HORIZONTAL) {
                        moveLowerThumb((int) RANGE.getWidth());
                    } else {
                        moveLowerThumb(0);
                    }
                } else {
                    moveLowerThumb(0);
                    if (slider.getOrientation() == JSlider.HORIZONTAL) {
                        moveUpperThumb((int) RANGE.getWidth());
                    } else {
                        moveUpperThumb((int) RANGE.getHeight());
                    }
                }
                slider.setExtent(formerExtent);
            }
        }

        @Override
        public void mouseMoved(final MouseEvent EVENT) {
            if (thumbRect.contains(EVENT.getPoint())) {
                lowerThumbHover = true;
                slider.repaint(thumbRect);
            } else {
                lowerThumbHover = false;
                slider.repaint(thumbRect);
            }

            if (upperThumbRect.contains(EVENT.getPoint())) {
                upperThumbHover = true;
                slider.repaint(upperThumbRect);
            } else {
                upperThumbHover = false;
                slider.repaint(upperThumbRect);
            }

            if (((RangeSlider) slider).isRangeSelectionEnabled() && RANGE.contains(EVENT.getPoint())) {
                slider.setToolTipText("Range: " + Integer.toString(slider.getExtent()));
            }
        }

        @Override
        public boolean shouldScroll(final int DIRECTION) {
            return false;
        }

        private void moveLowerThumb(final int ADDITIONAL_OFFSET) {
            int thumbMiddle = 0;

            switch (slider.getOrientation()) {
                case JSlider.HORIZONTAL:
                    int halfThumbWidth = thumbRect.width / 2;
                    int thumbLeft = currentMouseX - offset + ADDITIONAL_OFFSET;
                    int trackLeft = trackRect.x;
                    int trackRight = trackRect.x + (trackRect.width - 1);
                    int hMax;
                    if (((RangeSlider) slider).isRangeSelectionEnabled()) {
                        hMax = xPositionForValue(slider.getValue() + slider.getExtent());
                    } else {
                        hMax = xPositionForValue(slider.getMaximum());
                    }
                    // Apply bounds to thumb position.
                    if (drawInverted()) {
                        trackLeft = hMax;
                    } else {
                        trackRight = hMax;
                    }
                    thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
                    thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);
                    setThumbLocation(thumbLeft, thumbRect.y);
                    // Update slider value.
                    thumbMiddle = thumbLeft + halfThumbWidth;
                    slider.setValue(valueForXPosition(thumbMiddle));
                    break;

                case JSlider.VERTICAL:
                    int halfThumbHeight = thumbRect.height / 2;
                    int thumbTop = currentMouseY - offset + ADDITIONAL_OFFSET;
                    int trackTop = trackRect.y;
                    int trackBottom = trackRect.y + (trackRect.height - 1);
                    int vMax;
                    if (((RangeSlider) slider).isRangeSelectionEnabled()) {
                        vMax = yPositionForValue(slider.getValue() + slider.getExtent());
                    } else {
                        vMax = yPositionForValue(slider.getMaximum());
                    }
                    // Apply bounds to thumb position.
                    if (drawInverted()) {
                        trackBottom = vMax;
                    } else {
                        trackTop = vMax;
                    }
                    thumbTop = Math.max(thumbTop, trackTop - halfThumbHeight);
                    thumbTop = Math.min(thumbTop, trackBottom - halfThumbHeight);
                    setThumbLocation(thumbRect.x, thumbTop);
                    // Update slider value.
                    thumbMiddle = thumbTop + halfThumbHeight;
                    slider.setValue(valueForYPosition(thumbMiddle));
                    break;

                default:
                    return;
            }
        }

        private void moveUpperThumb(final int ADDITIONAL_OFFSET) {
            int thumbMiddle = 0;

            switch (slider.getOrientation()) {
                case JSlider.HORIZONTAL:
                    int halfThumbWidth = thumbRect.width / 2;
                    int thumbLeft = currentMouseX - offset + ADDITIONAL_OFFSET;
                    int trackLeft = trackRect.x;
                    int trackRight = trackRect.x + (trackRect.width - 1);
                    int hMin = xPositionForValue(slider.getValue());
                    // Apply bounds to thumb position.
                    if (drawInverted()) {
                        trackRight = hMin;
                    } else {
                        trackLeft = hMin;
                    }
                    thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
                    thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);
                    setUpperThumbLocation(thumbLeft, thumbRect.y);

                    // Update slider extent.
                    thumbMiddle = thumbLeft + halfThumbWidth;
                    slider.setExtent(valueForXPosition(thumbMiddle) - slider.getValue());
                    break;

                case JSlider.VERTICAL:
                    int halfThumbHeight = thumbRect.height / 2;
                    int thumbTop = currentMouseY - offset + ADDITIONAL_OFFSET;
                    int trackTop = trackRect.y;
                    int trackBottom = trackRect.y + (trackRect.height - 1);
                    int vMin = yPositionForValue(slider.getValue());
                    // Apply bounds to thumb position.
                    if (drawInverted()) {
                        trackTop = vMin;
                    } else {
                        trackBottom = vMin;
                    }
                    thumbTop = Math.max(thumbTop, trackTop - halfThumbHeight);
                    thumbTop = Math.min(thumbTop, trackBottom - halfThumbHeight);
                    setUpperThumbLocation(thumbRect.x, thumbTop);
                    // Update slider extent.
                    thumbMiddle = thumbTop + halfThumbHeight;
                    slider.setExtent(valueForYPosition(thumbMiddle) - slider.getValue());
                    break;

                default:
                    return;
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Thumb related">
    @Override
    protected void calculateThumbSize() {
        // Call superclass method for lower thumb size.
        super.calculateThumbSize();

        // Set upper thumb size
        upperThumbRect.setSize(thumbRect.width, thumbRect.height);
    }

    @Override
    protected void calculateThumbLocation() {
        calculateLowerThumbLocation();
        calculateUpperThumbLocation();
    }

    protected void calculateLowerThumbLocation() {
        if (slider.getSnapToTicks()) {
            int sliderValue = slider.getValue();
            int snappedValue = sliderValue;
            int majorTickSpacing = slider.getMajorTickSpacing();
            int minorTickSpacing = slider.getMinorTickSpacing();
            int tickSpacing = 0;

            if (minorTickSpacing > 0) {
                tickSpacing = minorTickSpacing;
            } else if (majorTickSpacing > 0) {
                tickSpacing = majorTickSpacing;
            }

            if (tickSpacing != 0) {
                // If it's not on a tick, change the value
                if ((sliderValue - slider.getMinimum()) % tickSpacing != 0) {
                    float temp = (float) (sliderValue - slider.getMinimum()) / (float) tickSpacing;
                    int whichTick = Math.round(temp);
                    snappedValue = slider.getMinimum() + (whichTick * tickSpacing);
                }

                if (snappedValue != sliderValue) {
                    slider.setValue(snappedValue);
                }
            }
        }
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            int valuePosition = xPositionForValue(slider.getValue());
            thumbRect.x = valuePosition - (thumbRect.width / 2);
            thumbRect.y = trackRect.y;
        } else {
            int valuePosition = yPositionForValue(slider.getValue());

            thumbRect.x = trackRect.x;
            thumbRect.y = valuePosition - (thumbRect.height / 2);
        }
    }

    protected void calculateUpperThumbLocation() {
        if (slider.getSnapToTicks()) {
            int sliderValue = slider.getValue();
            int snappedValue = sliderValue;
            int majorTickSpacing = slider.getMajorTickSpacing();
            int minorTickSpacing = slider.getMinorTickSpacing();
            int tickSpacing = 0;

            if (minorTickSpacing > 0) {
                tickSpacing = minorTickSpacing;
            } else if (majorTickSpacing > 0) {
                tickSpacing = majorTickSpacing;
            }

            if (tickSpacing != 0) {
                // If it's not on a tick, change the value
                if ((sliderValue - slider.getMinimum()) % tickSpacing != 0) {
                    float temp = (float) (sliderValue - slider.getMinimum()) / (float) tickSpacing;
                    int whichTick = Math.round(temp);
                    snappedValue = slider.getMinimum() + (whichTick * tickSpacing);
                }

                if (snappedValue != sliderValue) {
                    slider.setValue(snappedValue);
                }
            }
        }

        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            int valuePosition = xPositionForValue(slider.getValue() + slider.getExtent());
            upperThumbRect.x = valuePosition - (upperThumbRect.width / 2);
            upperThumbRect.y = trackRect.y;
        } else {
            int valuePosition = yPositionForValue(slider.getValue() + slider.getExtent());
            upperThumbRect.x = trackRect.x;
            upperThumbRect.y = valuePosition - (upperThumbRect.height / 2);
        }
    }

    private void setUpperThumbLocation(final int X, final int Y) {
        Rectangle upperUnionRect = new Rectangle();
        upperUnionRect.setBounds(upperThumbRect);
        upperThumbRect.setLocation(X, Y);
        SwingUtilities.computeUnion(upperThumbRect.x, upperThumbRect.y, upperThumbRect.width, upperThumbRect.height, upperUnionRect);
        slider.repaint(upperUnionRect.x, upperUnionRect.y, upperUnionRect.width, upperUnionRect.height);
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(16, 16);
    }

    private BufferedImage createThumbImage(final int WIDTH, final int HEIGHT, final boolean HOVER) {
        final GraphicsConfiguration GFX_CONF = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        if (WIDTH <= 0 || HEIGHT <= 0) {
            return GFX_CONF.createCompatibleImage(1, 1, java.awt.Transparency.TRANSLUCENT);
        }
        final BufferedImage IMAGE = GFX_CONF.createCompatibleImage(WIDTH, HEIGHT, Transparency.TRANSLUCENT);
        final Graphics2D G2 = IMAGE.createGraphics();
        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        G2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        G2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        if (thumbShape != RangeSlider.ThumbShape.NONE) {
            final int IMAGE_WIDTH = IMAGE.getWidth();
            final int IMAGE_HEIGHT = IMAGE.getHeight();
            final Shape BACKGROUND;
            final Shape INNER_FRAME;
            final Shape STANDARD;
            final Point2D CENTER = new Point2D.Double();

            switch (thumbShape) {
                case DROP:
                    final GeneralPath BACKGROUND_PATH = new GeneralPath();
                    final GeneralPath INNER_FRAME_PATH = new GeneralPath();
                    final GeneralPath STANDARD_PATH = new GeneralPath();

                    switch (slider.getOrientation()) {
                        case JSlider.VERTICAL:
                            BACKGROUND_PATH.setWindingRule(Path2D.WIND_EVEN_ODD);
                            BACKGROUND_PATH.moveTo(0.375 * IMAGE_WIDTH, 0.0625 * IMAGE_HEIGHT);
                            BACKGROUND_PATH.curveTo(0.5 * IMAGE_WIDTH, 0.0625 * IMAGE_HEIGHT, IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT);
                            BACKGROUND_PATH.curveTo(IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, 0.9375 * IMAGE_HEIGHT, 0.375 * IMAGE_WIDTH, 0.9375 * IMAGE_HEIGHT);
                            BACKGROUND_PATH.curveTo(0.1875 * IMAGE_WIDTH, 0.9375 * IMAGE_HEIGHT, 0.0, 0.75 * IMAGE_HEIGHT, 0.0, 0.5 * IMAGE_HEIGHT);
                            BACKGROUND_PATH.curveTo(0.0, 0.25 * IMAGE_HEIGHT, 0.25 * IMAGE_WIDTH, 0.0625 * IMAGE_HEIGHT, 0.375 * IMAGE_WIDTH, 0.0625 * IMAGE_HEIGHT);
                            BACKGROUND_PATH.closePath();

                            INNER_FRAME_PATH.setWindingRule(Path2D.WIND_EVEN_ODD);
                            INNER_FRAME_PATH.moveTo(0.375 * IMAGE_WIDTH, 0.125 * IMAGE_HEIGHT);
                            INNER_FRAME_PATH.curveTo(0.5 * IMAGE_WIDTH, 0.125 * IMAGE_HEIGHT, 0.9375 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, 0.9375 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT);
                            INNER_FRAME_PATH.curveTo(0.9375 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, 0.875 * IMAGE_HEIGHT, 0.375 * IMAGE_WIDTH, 0.875 * IMAGE_HEIGHT);
                            INNER_FRAME_PATH.curveTo(0.25 * IMAGE_WIDTH, 0.875 * IMAGE_HEIGHT, 0.0625 * IMAGE_WIDTH, 0.75 * IMAGE_HEIGHT, 0.0625 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT);
                            INNER_FRAME_PATH.curveTo(0.0625 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT, 0.25 * IMAGE_WIDTH, 0.125 * IMAGE_HEIGHT, 0.375 * IMAGE_WIDTH, 0.125 * IMAGE_HEIGHT);
                            INNER_FRAME_PATH.closePath();

                            STANDARD_PATH.setWindingRule(Path2D.WIND_EVEN_ODD);
                            STANDARD_PATH.moveTo(0.375 * IMAGE_WIDTH, 0.1875 * IMAGE_HEIGHT);
                            STANDARD_PATH.curveTo(0.5 * IMAGE_WIDTH, 0.1875 * IMAGE_HEIGHT, 0.875 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, 0.875 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT);
                            STANDARD_PATH.curveTo(0.875 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, 0.8125 * IMAGE_HEIGHT, 0.375 * IMAGE_WIDTH, 0.8125 * IMAGE_HEIGHT);
                            STANDARD_PATH.curveTo(0.25 * IMAGE_WIDTH, 0.8125 * IMAGE_HEIGHT, 0.125 * IMAGE_WIDTH, 0.6875 * IMAGE_HEIGHT, 0.125 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT);
                            STANDARD_PATH.curveTo(0.125 * IMAGE_WIDTH, 0.3125 * IMAGE_HEIGHT, 0.3125 * IMAGE_WIDTH, 0.1875 * IMAGE_HEIGHT, 0.375 * IMAGE_WIDTH, 0.1875 * IMAGE_HEIGHT);
                            STANDARD_PATH.closePath();
                            CENTER.setLocation(BACKGROUND_PATH.getBounds2D().getWidth() * 0.375, BACKGROUND_PATH.getBounds2D().getCenterY());
                            break;

                        case JSlider.HORIZONTAL:

                        default:
                            BACKGROUND_PATH.setWindingRule(Path2D.WIND_EVEN_ODD);
                            BACKGROUND_PATH.moveTo(0.0625 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                            BACKGROUND_PATH.curveTo(0.0625 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, IMAGE_HEIGHT);
                            BACKGROUND_PATH.curveTo(0.5 * IMAGE_WIDTH, IMAGE_HEIGHT, 0.9375 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, 0.9375 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                            BACKGROUND_PATH.curveTo(0.9375 * IMAGE_WIDTH, 0.1875 * IMAGE_HEIGHT, 0.75 * IMAGE_WIDTH, 0.0, 0.5 * IMAGE_WIDTH, 0.0);
                            BACKGROUND_PATH.curveTo(0.25 * IMAGE_WIDTH, 0.0, 0.0625 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT, 0.0625 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                            BACKGROUND_PATH.closePath();

                            INNER_FRAME_PATH.setWindingRule(Path2D.WIND_EVEN_ODD);
                            INNER_FRAME_PATH.moveTo(0.125 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                            INNER_FRAME_PATH.curveTo(0.125 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, 0.9375 * IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, 0.9375 * IMAGE_HEIGHT);
                            INNER_FRAME_PATH.curveTo(0.5 * IMAGE_WIDTH, 0.9375 * IMAGE_HEIGHT, 0.875 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, 0.875 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                            INNER_FRAME_PATH.curveTo(0.875 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT, 0.75 * IMAGE_WIDTH, 0.0625 * IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, 0.0625 * IMAGE_HEIGHT);
                            INNER_FRAME_PATH.curveTo(0.25 * IMAGE_WIDTH, 0.0625 * IMAGE_HEIGHT, 0.125 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT, 0.125 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                            INNER_FRAME_PATH.closePath();

                            STANDARD_PATH.setWindingRule(Path2D.WIND_EVEN_ODD);
                            STANDARD_PATH.moveTo(0.1875 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                            STANDARD_PATH.curveTo(0.1875 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, 0.875 * IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, 0.875 * IMAGE_HEIGHT);
                            STANDARD_PATH.curveTo(0.5 * IMAGE_WIDTH, 0.875 * IMAGE_HEIGHT, 0.8125 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, 0.8125 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                            STANDARD_PATH.curveTo(0.8125 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT, 0.6875 * IMAGE_WIDTH, 0.125 * IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, 0.125 * IMAGE_HEIGHT);
                            STANDARD_PATH.curveTo(0.3125 * IMAGE_WIDTH, 0.125 * IMAGE_HEIGHT, 0.1875 * IMAGE_WIDTH, 0.3125 * IMAGE_HEIGHT, 0.1875 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                            STANDARD_PATH.closePath();
                            CENTER.setLocation(BACKGROUND_PATH.getBounds2D().getCenterX(), BACKGROUND_PATH.getBounds2D().getHeight() * 0.375);
                            break;
                    }
                    BACKGROUND = BACKGROUND_PATH;
                    INNER_FRAME = INNER_FRAME_PATH;
                    STANDARD = STANDARD_PATH;
                    break;

                case RECTANGULAR:
                    switch (slider.getOrientation()) {
                        case JSlider.VERTICAL:
                            BACKGROUND = new RoundRectangle2D.Double(0.0 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT, 1.0 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT, 0.25 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT);
                            INNER_FRAME = new RoundRectangle2D.Double(0.0625 * IMAGE_WIDTH, 0.3125 * IMAGE_HEIGHT, 0.875 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT, 0.1875 * IMAGE_WIDTH, 0.1875 * IMAGE_HEIGHT);
                            STANDARD = new RoundRectangle2D.Double(0.125 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT, 0.75 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT, 0.125 * IMAGE_WIDTH, 0.125 * IMAGE_HEIGHT);
                            break;

                        case JSlider.HORIZONTAL:

                        default:
                            BACKGROUND = new RoundRectangle2D.Double(0.25 * IMAGE_WIDTH, 0.0 * IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, 1.0 * IMAGE_HEIGHT, 0.25 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT);
                            INNER_FRAME = new RoundRectangle2D.Double(0.3125 * IMAGE_WIDTH, 0.0625 * IMAGE_HEIGHT, 0.375 * IMAGE_WIDTH, 0.875 * IMAGE_HEIGHT, 0.1875 * IMAGE_WIDTH, 0.1875 * IMAGE_HEIGHT);
                            STANDARD = new RoundRectangle2D.Double(0.375 * IMAGE_WIDTH, 0.125 * IMAGE_HEIGHT, 0.25 * IMAGE_WIDTH, 0.75 * IMAGE_HEIGHT, 0.125 * IMAGE_WIDTH, 0.125 * IMAGE_HEIGHT);
                            break;
                    }
                    CENTER.setLocation(BACKGROUND.getBounds2D().getCenterX(), BACKGROUND.getBounds2D().getCenterY());
                    break;

                case SQUARE:
                    BACKGROUND = new RoundRectangle2D.Double(0.0, 0.0, IMAGE_WIDTH, IMAGE_HEIGHT, 0.5 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT);
                    INNER_FRAME = new RoundRectangle2D.Double(0.0625 * IMAGE_WIDTH, 0.0625 * IMAGE_HEIGHT, 0.875 * IMAGE_WIDTH, 0.875 * IMAGE_HEIGHT, 0.4375 * IMAGE_WIDTH, 0.4375 * IMAGE_HEIGHT);
                    STANDARD = new RoundRectangle2D.Double(0.125 * IMAGE_WIDTH, 0.125 * IMAGE_HEIGHT, 0.75 * IMAGE_WIDTH, 0.75 * IMAGE_HEIGHT, 0.375 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                    CENTER.setLocation(BACKGROUND.getBounds2D().getCenterX(), BACKGROUND.getBounds2D().getCenterY());
                    break;
                case ROUND:

                default:
                    BACKGROUND = new Ellipse2D.Double(0.0, 0.0, IMAGE_WIDTH, IMAGE_HEIGHT);
                    INNER_FRAME = new Ellipse2D.Double(0.0625 * IMAGE_WIDTH, 0.0625 * IMAGE_HEIGHT, 0.875 * IMAGE_WIDTH, 0.875 * IMAGE_HEIGHT);
                    STANDARD = new Ellipse2D.Double(0.125 * IMAGE_WIDTH, 0.125 * IMAGE_HEIGHT, 0.75 * IMAGE_WIDTH, 0.75 * IMAGE_HEIGHT);
                    CENTER.setLocation(BACKGROUND.getBounds2D().getCenterX(), BACKGROUND.getBounds2D().getCenterY());
                    break;
            }

            final float[] BACKGROUND_FRACTIONS;
            final float[] INNER_FRAME_FRACTIONS;
            final float[] FOREGROUND_FRACTIONS;

            final Color[] BACKGROUND_COLORS;
            final Color[] INNER_FRAME_COLORS;
            final Color[] FOREGROUND_COLORS;

            final Paint BACKGROUND_PAINT;
            final Paint INNER_FRAME_PAINT;
            final Paint FOREGROUND_PAINT;

            switch(thumbDesign) {
                case DARK:
                    BACKGROUND_FRACTIONS = new float[]{
                        0.0f,
                        1.0f };
                    BACKGROUND_COLORS = new Color[]{
                        UIManager.getColor("control").darker().darker(),
                        UIManager.getColor("control").darker().darker().darker()
                    };
                    INNER_FRAME_FRACTIONS = new float[]{
                        0.0f,
                        0.39f,
                        1.0f
                    };
                    INNER_FRAME_COLORS = new Color[]{
                        UIManager.getColor("control"),
                        UIManager.getColor("control").darker(),
                        UIManager.getColor("control").brighter()
                    };
                    FOREGROUND_FRACTIONS = new float[]{
                        0.0f,
                        1.0f
                    };
                    if (HOVER) {
                        FOREGROUND_COLORS = new Color[] {
                            UIManager.getColor("control"),
                            UIManager.getColor("control").brighter()
                        };
                    } else {
                        FOREGROUND_COLORS = new Color[] {
                            UIManager.getColor("control").darker(),
                            UIManager.getColor("control")
                        };
                    }
                    BACKGROUND_PAINT = new LinearGradientPaint(
                        new Point2D.Double(0, BACKGROUND.getBounds2D().getMinY()),
                        new Point2D.Double(0, BACKGROUND.getBounds2D().getMaxY()),
                        BACKGROUND_FRACTIONS,
                        BACKGROUND_COLORS);
                    INNER_FRAME_PAINT = new LinearGradientPaint(
                        new Point2D.Double(0, INNER_FRAME.getBounds2D().getMinY()),
                        new Point2D.Double(0, INNER_FRAME.getBounds2D().getMaxY()),
                        INNER_FRAME_FRACTIONS,
                        INNER_FRAME_COLORS);
                    FOREGROUND_PAINT = new LinearGradientPaint(
                        new Point2D.Double(0, STANDARD.getBounds2D().getMinY()),
                        new Point2D.Double(0, STANDARD.getBounds2D().getMaxY()),
                        FOREGROUND_FRACTIONS,
                        FOREGROUND_COLORS);
                    break;

                case STAINLESS:
                    BACKGROUND_FRACTIONS = new float[] {
                        0.0f,
                        0.25f,
                        0.51f,
                        0.76f,
                        1.0f
                    };
                    BACKGROUND_COLORS = new Color[] {
                        new Color(90, 91, 92, 255),
                        new Color(127, 127, 128, 255),
                        new Color(81, 82, 83, 255),
                        new Color(104, 105, 105, 255),
                        new Color(63, 64, 65, 255)
                    };
                    INNER_FRAME_FRACTIONS = new float[] {
                        0.0f,
                        40.0f,
                        90.0f,
                        140.0f,
                        220.0f,
                        270.0f,
                        320.0f
                    };
                    if (HOVER) {
                        INNER_FRAME_COLORS = new Color[]{
                            new Color(0xC2C2C2),
                            new Color(0x727678),
                            new Color(0xC2C2C2),
                            new Color(0x727678),
                            new Color(0xC2C2C2),
                            new Color(0x727678),
                            new Color(0xC2C2C2)
                        };
                    } else {
                        INNER_FRAME_COLORS = new Color[]{
                            new Color(0xF2F2F2),
                            new Color(0x8F9396),
                            new Color(0xF2F2F2),
                            new Color(0x8F9396),
                            new Color(0xF2F2F2),
                            new Color(0x8F9396),
                            new Color(0xF2F2F2)
                        };
                    }
                    FOREGROUND_FRACTIONS = new float[] {
                        0.0f,
                        1.0f
                    };
                    FOREGROUND_COLORS = new Color[] {
                        new Color(1.0f, 1.0f, 1.0f, 0.0f),
                        new Color(1.0f, 1.0f, 1.0f, 0.0f)
                    };
                    BACKGROUND_PAINT = new LinearGradientPaint(
                        new Point2D.Double(0, BACKGROUND.getBounds2D().getMinY()),
                        new Point2D.Double(0, BACKGROUND.getBounds2D().getMaxY()),
                        BACKGROUND_FRACTIONS,
                        BACKGROUND_COLORS);
                    INNER_FRAME_PAINT = new ConicalGradientPaint(true, CENTER, 0f, INNER_FRAME_FRACTIONS, INNER_FRAME_COLORS);
                    FOREGROUND_PAINT = new LinearGradientPaint(
                        new Point2D.Double(0, STANDARD.getBounds2D().getMinY()),
                        new Point2D.Double(0, STANDARD.getBounds2D().getMaxY()),
                        FOREGROUND_FRACTIONS,
                        FOREGROUND_COLORS);
                    break;

                case DARK_STAINLESS:
                    BACKGROUND_FRACTIONS = new float[] {
                        0.0f,
                        1.0f
                    };
                    BACKGROUND_COLORS = new Color[] {
                        new Color(1.0f, 1.0f, 1.0f),
                        new Color(0.1f, 0.1f, 0.1f)
                    };
                    INNER_FRAME_FRACTIONS = new float[]{
                        0.0f,
                        40.0f,
                        90.0f,
                        140.0f,
                        220.0f,
                        270.0f,
                        320.0f
                    };
                    if (HOVER) {
                        INNER_FRAME_COLORS = new Color[]{
                            new Color(0xC2C2C2),
                            new Color(0x727678),
                            new Color(0xC2C2C2),
                            new Color(0x727678),
                            new Color(0xC2C2C2),
                            new Color(0x727678),
                            new Color(0xC2C2C2)
                        };
                    } else {
                        INNER_FRAME_COLORS = new Color[]{
                            new Color(0xAAACAD),
                            new Color(0x424242),
                            new Color(0x85888A),
                            new Color(0x474848),
                            new Color(0x9EA5A7),
                            new Color(0x4D4F4F),
                            new Color(0xAAACAD)
                        };
                    }
                    FOREGROUND_FRACTIONS = new float[] {
                        0.0f,
                        1.0f
                    };
                    FOREGROUND_COLORS = new Color[] {
                        new Color(1.0f, 1.0f, 1.0f, 0.0f),
                        new Color(1.0f, 1.0f, 1.0f, 0.0f)
                    };
                    BACKGROUND_PAINT = new LinearGradientPaint(
                        new Point2D.Double(0, BACKGROUND.getBounds2D().getMinY()),
                        new Point2D.Double(0, BACKGROUND.getBounds2D().getMaxY()),
                        BACKGROUND_FRACTIONS,
                        BACKGROUND_COLORS);
                    INNER_FRAME_PAINT = new ConicalGradientPaint(true, CENTER, 0f, INNER_FRAME_FRACTIONS, INNER_FRAME_COLORS);
                    FOREGROUND_PAINT = new LinearGradientPaint(
                        new Point2D.Double(0, STANDARD.getBounds2D().getMinY()),
                        new Point2D.Double(0, STANDARD.getBounds2D().getMaxY()),
                        FOREGROUND_FRACTIONS,
                        FOREGROUND_COLORS);
                    break;

                case BRIGHT:

                default:
                    BACKGROUND_FRACTIONS = new float[]{
                        0.0f,
                        1.0f };
                    BACKGROUND_COLORS = new Color[]{
                        UIManager.getColor("control").darker(),
                        UIManager.getColor("control").darker().darker()
                    };
                    INNER_FRAME_FRACTIONS = new float[]{
                        0.0f,
                        0.39f,
                        1.0f
                    };
                    INNER_FRAME_COLORS = new Color[]{
                        UIManager.getColor("control").brighter(),
                        UIManager.getColor("control"),
                        UIManager.getColor("control").brighter().brighter()
                    };
                    FOREGROUND_FRACTIONS = new float[]{
                        0.0f,
                        1.0f
                    };
                    if (HOVER) {
                        FOREGROUND_COLORS = new Color[] {
                            UIManager.getColor("control").brighter(),
                            UIManager.getColor("control").brighter().brighter()
                        };
                    } else {
                        FOREGROUND_COLORS = new Color[] {
                            UIManager.getColor("control"),
                            UIManager.getColor("control").brighter()
                        };
                    }
                    BACKGROUND_PAINT = new LinearGradientPaint(
                        new Point2D.Double(0, BACKGROUND.getBounds2D().getMinY()),
                        new Point2D.Double(0, BACKGROUND.getBounds2D().getMaxY()),
                        BACKGROUND_FRACTIONS,
                        BACKGROUND_COLORS);
                    INNER_FRAME_PAINT = new LinearGradientPaint(
                        new Point2D.Double(0, INNER_FRAME.getBounds2D().getMinY()),
                        new Point2D.Double(0, INNER_FRAME.getBounds2D().getMaxY()),
                        INNER_FRAME_FRACTIONS,
                        INNER_FRAME_COLORS);
                    FOREGROUND_PAINT = new LinearGradientPaint(
                        new Point2D.Double(0, STANDARD.getBounds2D().getMinY()),
                        new Point2D.Double(0, STANDARD.getBounds2D().getMaxY()),
                        FOREGROUND_FRACTIONS,
                        FOREGROUND_COLORS);
                    break;
            }

            // Fill background
            G2.setPaint(BACKGROUND_PAINT);
            G2.fill(BACKGROUND);

            // Fill inner frame
            G2.setPaint(INNER_FRAME_PAINT);
            G2.fill(INNER_FRAME);

            // Fill standard or hover
            G2.setPaint(FOREGROUND_PAINT);
            G2.fill(STANDARD);
        }
        G2.dispose();
        return IMAGE;
    }

    public BufferedImage createIndicatorImage(final int WIDTH, final int HEIGHT, final Color COLOR, final boolean LOWER, final boolean HORIZONTAL) {
        final GraphicsConfiguration GFX_CONF = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        if (WIDTH <= 0 || HEIGHT <= 0) {
            return GFX_CONF.createCompatibleImage(1, 1, java.awt.Transparency.TRANSLUCENT);
        }
        final BufferedImage IMAGE = GFX_CONF.createCompatibleImage(WIDTH, HEIGHT, Transparency.TRANSLUCENT);
        final Graphics2D G2 = IMAGE.createGraphics();
        G2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        G2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        G2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        if (COLOR == null) {
            G2.setPaint(new Color(0.4f, 0.4f, 0.4f, 1f));
        } else {
            G2.setPaint(COLOR);
        }

        final int IMAGE_WIDTH = IMAGE.getWidth();
        final int IMAGE_HEIGHT = IMAGE.getHeight();
        
        final GeneralPath INDICATOR = new GeneralPath();
        INDICATOR.setWindingRule(Path2D.WIND_EVEN_ODD);
        if (HORIZONTAL) {
            if (LOWER) {
                INDICATOR.moveTo(0.625 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT);
                INDICATOR.lineTo(0.3125 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT);
                INDICATOR.lineTo(0.625 * IMAGE_WIDTH, 0.75 * IMAGE_HEIGHT);
                INDICATOR.lineTo(0.625 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT);
                INDICATOR.closePath();
            } else {
                INDICATOR.moveTo(0.375 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT);
                INDICATOR.lineTo(0.6875 * IMAGE_WIDTH, 0.5 * IMAGE_HEIGHT);
                INDICATOR.lineTo(0.375 * IMAGE_WIDTH, 0.75 * IMAGE_HEIGHT);
                INDICATOR.lineTo(0.375 * IMAGE_WIDTH, 0.25 * IMAGE_HEIGHT);
                INDICATOR.closePath();
            }
        } else {
            if (LOWER) {
                INDICATOR.moveTo(0.75 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                INDICATOR.lineTo(0.5 * IMAGE_WIDTH, 0.6875 * IMAGE_HEIGHT);
                INDICATOR.lineTo(0.25 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                INDICATOR.lineTo(0.75 * IMAGE_WIDTH, 0.375 * IMAGE_HEIGHT);
                INDICATOR.closePath();
            } else {
                INDICATOR.moveTo(0.75 * IMAGE_WIDTH, 0.625 * IMAGE_HEIGHT);
                INDICATOR.lineTo(0.5 * IMAGE_WIDTH, 0.3125 * IMAGE_HEIGHT);
                INDICATOR.lineTo(0.25 * IMAGE_WIDTH, 0.625 * IMAGE_HEIGHT);
                INDICATOR.lineTo(0.75 * IMAGE_WIDTH, 0.625 * IMAGE_HEIGHT);
                INDICATOR.closePath();
            }
        }
        G2.fill(INDICATOR);
        G2.dispose();
        return IMAGE;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Misc">
    @Override
    public void scrollByBlock(final int DIRECTION) {
        synchronized (slider) {
            int blockIncrement = (slider.getMaximum() - slider.getMinimum()) / 10;
            if (blockIncrement <= 0 && slider.getMaximum() > slider.getMinimum()) {
                blockIncrement = 1;
            }
            int delta = blockIncrement * ((DIRECTION > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);

            if (upperThumbSelected) {
                int oldValue = ((RangeSlider) slider).getUpperValue();
                ((RangeSlider) slider).setUpperValue(oldValue + delta);
            } else {
                int oldValue = slider.getValue();
                slider.setValue(oldValue + delta);
            }
        }
    }

    @Override
    public void scrollByUnit(final int DIRECTION) {
        synchronized (slider) {
            int delta = 1 * ((DIRECTION > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);

            if (upperThumbSelected) {
                int oldValue = ((RangeSlider) slider).getUpperValue();
                ((RangeSlider) slider).setUpperValue(oldValue + delta);
            } else {
                int oldValue = slider.getValue();
                slider.setValue(oldValue + delta);
            }
        }
    }
    // </editor-fold>
}
