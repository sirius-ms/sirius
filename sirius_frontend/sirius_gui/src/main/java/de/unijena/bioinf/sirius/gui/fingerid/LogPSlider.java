package de.unijena.bioinf.sirius.gui.fingerid;

import eu.hansolo.rangeslider.RangeSlider;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.DecimalFormat;

public class LogPSlider extends JPanel {

    protected RangeSlider rangeSlider;
    protected JLabel left, right;
    protected DecimalFormat format = new DecimalFormat("##0.00");
    protected Runnable callback;
    protected FingerIdData data;
    protected boolean isRefreshing;

    public LogPSlider() {
        this.setBorder(BorderFactory.createEtchedBorder());
        setLayout(new BorderLayout());
        rangeSlider = new RangeSlider(0, 500);
        rangeSlider.setLowerValue(0);
        rangeSlider.setUpperValue(rangeSlider.getMaximum());
        rangeSlider.setThumbShape(RangeSlider.ThumbShape.DROP);
        //rangeSlider.setTrackWidth(RangeSlider.TrackWidth.THICK);

        left = new JLabel();
        left.setText("00.00");
        right = new JLabel("00.00");
        /*
        left.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rangeSlider.setLowerValue((int)Math.round(Double.parseDouble(left.getText())*10));
            }
        });
        right.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rangeSlider.setUpperValue((int)Math.round(Double.parseDouble(right.getText())*10));
            }
        });
        */

        add(left, BorderLayout.WEST);
        add(rangeSlider, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        rangeSlider.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (isRefreshing) return;
                left.setText(format.format(getMinSelected()));
                right.setText(format.format(getMaxSelected()));
                if (data!=null) {
                    data.minLogPFilter = getMinSelected();
                    data.maxLogPFilter = getMaxSelected();
                    if (data.compounds.length>0)
                        System.out.println("SET " + data.compounds[0].inchi.key2D() +  " TO " + data.minLogPFilter + " / " + data.maxLogPFilter);
                }
                callback.run();
            }
        });


    }

    public double getMaxSelected() {
        return (rangeSlider.getLowerValue()+rangeSlider.getModel().getExtent())/10d;
    }

    public double getMinSelected() {
        return rangeSlider.getLowerValue()/10d;
    }

    public Runnable getCallback() {
        return callback;
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

    public void refresh(FingerIdData data) {
        isRefreshing=true;
        try {
        this.data = data;
        if (data==null || data.compounds==null || data.compounds.length==0) return;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Compound c : data.compounds) {
            if (!Double.isNaN(c.xlogP)) {
                min = Math.min(c.xlogP, min);
                max = Math.max(c.xlogP, max);
            }
        }
        int pmin = ((int)Math.floor(min/2d)*2)*10;
        int pmax = ((int)Math.ceil(max/2d)*2)*10;
        rangeSlider.setMinimum(pmin);
        rangeSlider.setMaximum(pmax);
        if (data!=null) {
            if (!Double.isInfinite(data.maxLogPFilter)) {
                rangeSlider.setUpperValue(Math.min(pmax, (int)Math.ceil(data.maxLogPFilter*10)));
            } else {
                rangeSlider.setUpperValue(pmax);
            }
            if (!Double.isInfinite(data.minLogPFilter)) {
                rangeSlider.setLowerValue(Math.max(pmin, (int)Math.floor(data.minLogPFilter*10)));
            } else {
                rangeSlider.setLowerValue(pmin);
            }

        } else {
            rangeSlider.setUpperValue(pmax);
            rangeSlider.setLowerValue(pmin);
        }
        System.out.println(pmin  + "/ " +pmax);
        left.setText(format.format(getMinSelected()));
        right.setText(format.format(getMaxSelected()));
        } finally {
                isRefreshing=false;
            }
    }
}
