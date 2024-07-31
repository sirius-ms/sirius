package de.unijena.bioinf.lcms.debuggui;

import org.apache.commons.lang3.Range;
import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.io.lcms.MzMLParser;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.lcms.*;
import de.unijena.bioinf.lcms.ionidentity.CorrelationGroupScorer;
import de.unijena.bioinf.lcms.peakshape.*;
import de.unijena.bioinf.model.lcms.*;
import gnu.trove.list.array.TDoubleArrayList;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class GUI extends JFrame implements KeyListener, ClipboardOwner {

    boolean showadducts = true; boolean showraw=true;
    int offset = 0;
    SpecViewer specViewer;
    ProcessedSample sample;
    LCMSProccessingInstance instance;
    JComboBox<SavitzkyGolayFilter> filterBox;

    SavitzkyGolayFilter[] filters = new SavitzkyGolayFilter[]{
            SavitzkyGolayFilter.Window1Polynomial1,
            SavitzkyGolayFilter.Window2Polynomial2,
            SavitzkyGolayFilter.Window3Polynomial2,
            SavitzkyGolayFilter.Window4Polynomial2,
            SavitzkyGolayFilter.Window8Polynomial2,
            SavitzkyGolayFilter.Window16Polynomial2,
            SavitzkyGolayFilter.Window32Polynomial3
    };

    public GUI(LCMSProccessingInstance i, ProcessedSample sample) {
        instance = i;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        sample.ions.sort(Comparator.comparingDouble(FragmentedIon::getMass));
        /*
        for (offset=0; offset < sample.ions.size(); ++offset) {
            if (sample.ions.get(offset).getMass()>=625)
                break;
        }
         */
        specViewer = new SpecViewer(sample.ions.get(offset));
        this.sample = sample;

        final JButton left = new JButton("<-");
        left.setAction(new AbstractAction("<-") {
            @Override
            public void actionPerformed(ActionEvent e) {
                GUI.this.previousIon();
            }
        });
        final JButton right = new JButton("->");
        right.setAction(new AbstractAction("->") {
            @Override
            public void actionPerformed(ActionEvent e) {
                GUI.this.nextIon();
            }
        });

        final JButton info = new JButton("show experiment");
        info.setAction(new AbstractAction("show experiment") {
            @Override
            public void actionPerformed(ActionEvent e) {
                GUI.this.info();
            }
        });

        JCheckBox ad = new JCheckBox("Show adducts");
        ad.setSelected(showadducts);
        ad.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                boolean v = ad.isSelected();
                if (v!=showadducts) {
                    showadducts = v;
                    specViewer.repaint();
                }
            }
        });

        JCheckBox raw = new JCheckBox("show raw");
        raw.setSelected(showraw);
        ad.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                boolean v = raw.isSelected();
                if (v!=showraw) {
                    showraw = v;
                    specViewer.repaint();
                }
            }
        });

        filterBox = new JComboBox<>(filters);
        filterBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                specViewer.repaint();
            }
        });

        getContentPane().add(left,BorderLayout.WEST);
        getContentPane().add(right,BorderLayout.EAST);

        JSlider slider = new JSlider(0, GUI.this.sample.ions.size());
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final int value = slider.getValue();
                if (value >= 0 && value < GUI.this.sample.ions.size()) {
                    GUI.this.jumpTo(value);
                }
            }
        });

        final Box verticalBox = Box.createVerticalBox();
        verticalBox.add(slider);
        final Box horBox = Box.createHorizontalBox();
        verticalBox.add(horBox);
        horBox.add(info);
        horBox.add(Box.createHorizontalStrut(8));
        horBox.add(ad);
        horBox.add(Box.createHorizontalStrut(8));
        horBox.add(raw);
        horBox.add(Box.createHorizontalStrut(8));
        horBox.add(filterBox);

        getContentPane().add(verticalBox,BorderLayout.SOUTH);

        getContentPane().add(specViewer,BorderLayout.CENTER);
        addKeyListener(this);
        setPreferredSize(new Dimension(1300,868));
        pack();

        {
            for (int k=0; k < 4; ++k) {
                System.out.println("Isotope peak # " + k);
                final ArrayList<CorrelationGroup> groups = new ArrayList<>();
                for (FragmentedIon ion : sample.ions) {
                    var xs = ion.getIsotopes();
                    if (xs.size()>k) {
                        groups.add(xs.get(k));
                    }
                }
                System.out.println("n = " + groups.size());
                if (groups.size()<=5)
                    break;
                // cosine
                groups.sort(Comparator.comparingDouble(CorrelationGroup::getCosine));
                System.out.printf("Median cosine = %f\n", groups.get(groups.size()/2).getCosine() );
                System.out.printf("Avg cosine = %f\n", groups.stream().mapToDouble(CorrelationGroup::getCosine).average().getAsDouble() );

                System.out.printf("15%% quantile = %f\n", groups.get((int)(groups.size()*0.15)).getCosine() );
                // correlation
                groups.sort(Comparator.comparingDouble(CorrelationGroup::getCorrelation));
                System.out.printf("Median correlation = %f\n", groups.get(groups.size()/2).getCorrelation() );
                System.out.printf("Avg correlation = %f\n", groups.stream().mapToDouble(CorrelationGroup::getCorrelation).average().getAsDouble() );
                System.out.printf("15%% quantile = %f\n", groups.get((int)(groups.size()*0.15)).getCorrelation() );
                // maximum likelihood
                groups.sort(Comparator.comparingDouble(u -> u.score));
                System.out.printf("median ML = %f\n", groups.get(groups.size()/2).score );
                System.out.printf("Avg ML = %f\n", groups.stream().mapToDouble(x->x.score).average().getAsDouble() );
                System.out.printf("15%% quantile = %f\n", groups.get((int)(groups.size()*0.15)).score );

                // LENGTH
                System.out.printf("Average Length = %f\n", groups.stream().mapToInt(x->x.getNumberOfCorrelatedPeaks()).average().getAsDouble());
            }
        }
        System.out.println("================== DECOY ===============================");
        {
            // compare with random correlations
            final ArrayList<FragmentedIon> A = new ArrayList<>(sample.ions), B = new ArrayList<>(sample.ions);
            TDoubleArrayList cosines = new TDoubleArrayList(), correlations = new TDoubleArrayList(), mls = new TDoubleArrayList();
            double avgLen = 0d;
            int counter = 0;
            while (counter < 5000) {
                Collections.shuffle(A);
                Collections.shuffle(B);
                for (int k = 0; k < A.size(); ++k) {
                    final ChromatographicPeak.Segment a = A.get(k).getSegment();
                    final ChromatographicPeak.Segment b = B.get(k).getSegment();
                    final TDoubleArrayList as = new TDoubleArrayList(), bs = new TDoubleArrayList();
                    final Range<Integer> l = a.calculateFWHM(0.15);
                    final Range<Integer> r = b.calculateFWHM(0.15);
                    int lenL = Math.min(a.getApexIndex()-l.getMinimum(), b.getApexIndex()-r.getMinimum());
                    int lenR = Math.min(l.getMaximum()-a.getApexIndex(), r.getMaximum()-b.getApexIndex());
                    if (lenL>=1 && lenR >= 1 && (lenL+lenR)>=4) {
                        ++counter;
                        avgLen += (lenL+lenR);
                        as.add(a.getApexIntensity());
                        bs.add(b.getApexIntensity());
                        for (int x=1; x  <lenR; ++x) {
                            as.add(a.getPeak().getIntensityAt(a.getApexIndex()+x));
                            bs.add(b.getPeak().getIntensityAt(b.getApexIndex()+x));
                        }
                        for (int x=1; x  <lenL; ++x) {
                            as.insert(0,a.getPeak().getIntensityAt(a.getApexIndex()-x));
                            bs.insert(0, b.getPeak().getIntensityAt(b.getApexIndex()-x));
                        }

                        cosines.add(CorrelatedPeakDetector.cosine(as,bs));
                        correlations.add(CorrelatedPeakDetector.pearson(as,bs));
                        mls.add(new CorrelationGroupScorer().predictProbability(as,bs));
                    }
                }
            }
            avgLen /= counter;
            // cosine
            cosines.sort();
            System.out.printf("Median cosine = %f\n", cosines.get(cosines.size()/2));
            System.out.printf("Avg cosine = %f\n", cosines.sum()/cosines.size());
            System.out.printf("15%% quantile = %f\n", cosines.get((int)(cosines.size()*0.15)));
            // correlation
            correlations.sort();
            System.out.printf("Median correlation = %f\n", correlations.get(correlations.size()/2));
            System.out.printf("Avg correlation = %f\n", correlations.sum()/correlations.size());
            System.out.printf("15%% quantile = %f\n", correlations.get((int)(correlations.size()*0.15)));
            // maximum likelihood
            mls.sort();
            System.out.printf("Median ml = %f\n", mls.get(mls.size()/2));
            System.out.printf("Avg ml = %f\n", mls.sum()/mls.size());
            System.out.printf("15%% quantile = %f\n", mls.get((int)(mls.size()*0.15)));

            System.out.printf("Average Length = %f\n", avgLen);

        }

        setVisible(true);
    }

    public static void main(String[] args) {

        final File mzxmlFile = new File("/home/mel/lcms-data/polluted_citrus/G79624_5x_BH4_01_18895.mzML");
                //new File("/home/kaidu/analysis/lcms/examples").listFiles()[0];
        InMemoryStorage storage= new InMemoryStorage();
        final LCMSProccessingInstance i = new LCMSProccessingInstance();
        try {
            final LCMSRun parse = (mzxmlFile.getName().toLowerCase(Locale.US).endsWith(".mzml") ? new MzMLParser() : new MzXMLParser()).parse(mzxmlFile, storage);
            final ProcessedSample sample = i.addSample(parse, storage, false);

            i.detectFeatures(sample);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("/home/mel/lcms-data/polluted_citrus/SIRIUS/1_features.csv"))) {
                sample.ions.forEach(ion -> {
                    try {
                        writer.write((ion.getRetentionTime()/1000d)+ ", " + ion.getMass() + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

//            final GUI gui = new GUI(i, sample);

        } catch (IOException| InvalidInputData e) {
            e.printStackTrace();
        }

    }

    @Override
    public void keyTyped(KeyEvent e) {
        keyReleased(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    private void previousIon() {
        --offset;
        if (offset<0) offset = sample.ions.size()+offset;
        specViewer.ion = sample.ions.get(offset);
        refr();
        specViewer.repaint();
    }
    private void nextIon() {
        ++offset;
        if (offset >= sample.ions.size()) offset = 0;
        specViewer.ion = sample.ions.get(offset);
        refr();
        specViewer.repaint();
    }
    private void jumpTo(int offset) {
        this.offset = offset;
        if (offset >= sample.ions.size()) offset = 0;
        specViewer.ion = sample.ions.get(offset);
        refr();
        specViewer.repaint();
    }

    public void refr() {
        MutableChromatographicPeak peak = specViewer.ion.getPeak();
        final double[] intensities = new double[peak.numberOfScans()];
        for (int k=0; k <intensities.length; ++k) {
            intensities[k] = peak.getIntensityAt(k);
        }
        final SavitzkyGolayFilter prpf = Extrema.getProposedFilter3(intensities);
        for (int k=0; k < filters.length; ++k) {
            if (filters[k].equals(prpf)) {
                this.filterBox.setSelectedIndex(k);
            }
        }
    }

    private void info() {
        final Ms2Experiment exp = instance.makeFeature(sample,specViewer.ion,false).toMsExperiment("", null);
        final StringWriter s = new StringWriter();
        try (final BufferedWriter bw = new BufferedWriter(s)) {
            new JenaMsWriter().write(bw,exp);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println(s.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s.toString()), this);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode()==KeyEvent.VK_LEFT) {
            previousIon();
        } else if (e.getKeyCode()==KeyEvent.VK_RIGHT) {
            nextIon();
        } else if (e.getKeyCode()==KeyEvent.VK_ENTER) {
            info();
        } else if (e.getKeyCode()==KeyEvent.VK_SPACE) {
            System.out.println("Debug Mode");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final FragmentedIon ION = specViewer.ion;
                    System.out.println("Debug Mode");
                }
            });
        }
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {

    }

    protected class SpecViewer extends Canvas {
        FragmentedIon ion;
        public SpecViewer(FragmentedIon ion) {
            this.ion = ion;
            setSize(new Dimension(1300, 868));
        }

        protected Font small = new Font("Helvetica",Font.PLAIN, 16);
        protected Font medium = new Font("Helvetica",Font.PLAIN, 24);

        final BasicStroke dashed =
                new BasicStroke(1.0f,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER,
                        10.0f, new float[]{10.0f}, 0.0f);

        @Override
        public void paint(Graphics g_) {
            super.paint(g_);

            long start = ion.getPeak().getRetentionTimeAt(0);
            long end = ion.getPeak().getRetentionTimeAt(ion.getPeak().numberOfScans()-1);
            final double span = (end-start);
            final double deltaRT = span/1000d;

            start -= deltaRT*50;
            end += deltaRT*50;

            double intensity = 0d;
            for (int k=0; k < ion.getPeak().numberOfScans(); ++k)
                intensity = Math.max(intensity,ion.getPeak().getIntensityAt(k));

            final double deltaInt = intensity / 700d;

            // draw axes
            Graphics2D g = (Graphics2D)g_;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(small);
            g.translate(12, 34);
            g.drawLine(0, 700, (int)Math.ceil((end-start)/deltaRT), 700);
            g.drawLine(0, (int)(intensity/deltaInt)+50, 0, 0);


            {
                // get magnitude
                final double magnitude = Math.pow(10, ((int)Math.ceil(Math.log10(intensity))) - 2);
                int maxTick = Math.min(100, (int)Math.ceil(intensity/magnitude));
                for (int k=0; k < maxTick; k++) {
                    double intensityValue = magnitude * k;

                    int posY = 700 - (int)(intensityValue/deltaInt);
                    g.drawLine(-4, posY, 4, posY);
                    if (k % 5 == 0) {
                        g.drawString(String.valueOf((long)(intensityValue)), 8, posY);
                    }
                }
            }

            {
                long startSeconds = (int)Math.floor(start/1000d);
                long endSeconds = (int)Math.ceil(end/1000d);
                for (long k=startSeconds; k <= endSeconds; ++k) {

                    int posX = (int)Math.round((k-startSeconds)*1000d/deltaRT);
                    g.drawLine(posX, 696, posX, 704);
                    g.drawString(String.format(Locale.US, "%d", (int)(k)), posX, 716);
                }
            }




            // draw data points
            ChromatographicPeak.Segment prev = null;
            Point2D prevPoint = null;
            boolean prevWasSeg = false;

            ///
            double[] values = new double[ion.getPeak().numberOfScans()];
            for (int k=0; k < ion.getPeak().numberOfScans(); ++k) {
                values[k] = ion.getPeak().getIntensityAt(k);
            }
            //
            final SavitzkyGolayFilter filter = smooth(values);
            final int GAPS = filter.getNumberOfDataPointsPerSide();
            {
                double[] xs = new double[values.length + GAPS*2];
                System.arraycopy(values, 0, xs, GAPS, values.length);
                values = xs;
            }
            double[] smoothed = filter.apply(values);

            for (int k=0; k < ion.getPeak().numberOfScans(); ++k) {
                final double xx = (ion.getPeak().getRetentionTimeAt(k)-start)/deltaRT;
                final double yy = ion.getPeak().getIntensityAt(k)/deltaInt;
                final ChromatographicPeak.Segment s = ion.getPeak().getSegmentForScanId(ion.getPeak().getScanNumberAt(k)).orElse(null);
                Point2D dp = new Point((int)xx, 700-(int)yy);
                g.setColor(Color.BLACK);
                if (prevPoint!=null && s!=prev) {
                    g.setStroke(new BasicStroke(3f));
                    g.drawLine((int)prevPoint.getX(), 700, (int)prevPoint.getX(), 0);
                    g.setStroke(new BasicStroke(1f));
                }
                prev = s;
                if (prevPoint!=null) {
                    g.setColor(s==ion.getSegment() ? Color.RED : Color.BLACK);
                    g.drawLine((int)prevPoint.getX(), (int)prevPoint.getY(), (int)dp.getX(), (int)dp.getY());
                }
                prevPoint = dp;
                if (s == ion.getSegment()) {
                    g.setColor(Color.RED);
                    prevWasSeg = true;
                } else {
                    g.setColor(Color.BLACK);
                    prevWasSeg = false;
                }
                if (showraw) g.fillOval((int)xx-5, 700 - ((int)yy-5), 10, 10);
            }

            prevPoint=null;
            for (int k=0; k < ion.getPeak().numberOfScans(); ++k) {
                final int VI = k+GAPS;
                final double xx = (ion.getPeak().getRetentionTimeAt(k)-start)/deltaRT;
                final double yy = smoothed[VI]/deltaInt;
                final ChromatographicPeak.Segment s = ion.getPeak().getSegmentForScanId(ion.getPeak().getScanNumberAt(k)).orElse(null);
                Point2D dp = new Point((int)xx, 700-(int)yy);
                g.setStroke(new BasicStroke(3f));
                g.setColor(Color.BLUE);
                g.setStroke(new BasicStroke(1f));
                if (prevPoint!=null) {
                    g.drawLine((int)prevPoint.getX(), (int)prevPoint.getY(), (int)dp.getX(), (int)dp.getY());
                    if (VI > 0 && VI+1 < values.length) {
                        double a = smoothed[VI-1],b = smoothed[VI], c=smoothed[VI+1];
                        if (b > a && b > c && filter.getDegree()>= 2) {
                            final double deriv = filter.computeSecondOrderDerivative(values, VI);
                            //g.drawString(String.format(Locale.US, "%.4f", deriv), (int)dp.getX(), (int)dp.getY());


                        }
                    }
                }
                prevPoint = dp;
            }
            g.setColor(Color.BLACK);

            // draw noise line
            {
                final Stroke def = g.getStroke();
                g.setStroke(dashed);
                g.setColor(Color.BLACK);
                double a = -1, b=-1;
                int c=-1;
                for (int k=0; k < ion.getPeak().numberOfScans(); ++k) {
                    int scanNumber = ion.getPeak().getScanNumberAt(k);
                    double noise = sample.ms1NoiseModel.getNoiseLevel(scanNumber, ion.getMass());
                    double signal = sample.ms1NoiseModel.getSignalLevel(scanNumber, ion.getMass());
                    int rt = (int)Math.round((ion.getPeak().getRetentionTimeAt(k)-start)/deltaRT);
                    if (a<0) {
                        a=noise;
                        b=signal;
                        c = rt;
                    } else {
                        g.drawLine(c, 700-(int)Math.round(a/deltaInt), rt, 700-(int)Math.round(noise/deltaInt));
                        g.drawLine(c, 700-(int)Math.round(b/deltaInt), rt, 700-(int)Math.round(signal/deltaInt));
                        a=noise;
                        b=signal;
                        c = rt;
                    }
                }
                g.setStroke(def);
            }

            // draw MS/MS
            int scanStart = ion.getPeak().getScanNumberAt(0);
            int scanEnd = ion.getPeak().getScanNumberAt(ion.getPeak().numberOfScans()-1);
            for (Scan s : GUI.this.sample.run.getScans(scanStart,scanEnd).values()) {
                if (s.isMsMs() && Math.abs(s.getPrecursor().getMass() - ion.getMass()) < 0.01) {
                    final long retentionTime = s.getRetentionTime();
                    double prec = s.getPrecursor().getIntensity();
                    if (prec == 0d) {
                        prec = 1000d;
                    }

                    int posX = (int) Math.round((retentionTime - start) / deltaRT);
                    int posY = (int) Math.round(prec / deltaInt);
                    g.setColor(Color.BLUE);
                    if (showraw) g.fillOval((int) posX - 5, 700 - ((int) posY - 5), 10, 10);
                    g.drawLine(posX, 0, posX, 700);
                    System.out.println("MSMS at " + retentionTime + " with intensity = " + prec);

                }
            }

            g.setColor(Color.BLACK);
            g.setFont(medium);
            g.drawString(ion.getSegment().toString(), 50,800);
            g.drawString(String.valueOf(ion.getMass()), 50, 850);
            if (showadducts) {
                // draw correlated peaks
                for (CorrelationGroup c : ion.getIsotopes()) {
                    final ChromatographicPeak.Segment rightSegment = c.getRightSegment();
                    final ChromatographicPeak p = c.getRight();
                    prevPoint = null;
                    for (int i = rightSegment.getStartIndex(); i < rightSegment.getEndIndex(); ++i) {
                        final long rt = p.getRetentionTimeAt(i);
                        final double intens = p.getIntensityAt(i);
                        int posX = (int) Math.round((rt - start) / deltaRT);
                        int posY = (int) Math.round(intens / deltaInt);
                        g.setColor(Color.GREEN);
                        g.fillOval((int) posX - 5, 700 - ((int) posY - 5), 10, 10);
                        if (prevPoint != null) {
                            g.drawLine((int) prevPoint.getX(), (int) prevPoint.getY(), posX, 700 - posY);
                        }
                        prevPoint = new Point(posX, 700 - posY);
                    }

                }
                for (CorrelatedIon c : ion.getAdducts()) {
                    final ChromatographicPeak.Segment rightSegment = c.correlation.getRightSegment();
                    final ChromatographicPeak p = c.correlation.getRight();
                    prevPoint = null;
                    for (int i = rightSegment.getStartIndex(); i < rightSegment.getEndIndex(); ++i) {
                        final long rt = p.getRetentionTimeAt(i);
                        final double intens = p.getIntensityAt(i);
                        int posX = (int) Math.round((rt - start) / deltaRT);
                        int posY = (int) Math.round(intens / deltaInt);
                        g.setColor(Color.MAGENTA);
                        g.fillOval((int) posX - 5, 700 - ((int) posY - 5), 10, 10);
                        if (prevPoint != null) {
                            g.drawLine((int) prevPoint.getX(), (int) prevPoint.getY(), posX, 700 - posY);
                        }
                        prevPoint = new Point(posX, 700 - posY);
                    }
                    if (c.correlation.getAnnotation() != null) {
                        int yyy = (int) Math.round(p.getIntensityAt(rightSegment.getApexIndex()) / deltaInt) - 16;
                        int xxx = (int) Math.round((p.getRetentionTimeAt(rightSegment.getApexIndex()) - start) / deltaRT);
                        g.drawString(String.format(Locale.US, "%s %d %%, %d %%", c.correlation.getAnnotation(), (int) Math.round(100 * c.correlation.getCorrelation()), (int) Math.round(100 * c.correlation.score)), xxx, 700 - yyy);
                    }
                }
            }

            // draw Gaussian

            final GaussianShape shapeGauss = new GaussianFitting().fit(sample, ion.getPeak(), ion.getSegment());
            final LaplaceShape shapeLaplace = new LaplaceFitting().fit(sample, ion.getPeak(), ion.getSegment());
            final PeakShape shape = shapeGauss.getScore()>shapeLaplace.getScore() ? shapeGauss : shapeLaplace;
            if (false){
                g.setColor(Color.ORANGE);
                int xxP = 0; int yyP = (int)Math.round(shape.expectedIntensityAt(start)/deltaInt);
                for (long k=start+1; k < end; k += deltaRT) {
                    final int xxx = (int)Math.round((k-start)/deltaRT);
                    final int yyy = (int)Math.round(shape.expectedIntensityAt(k)/deltaInt);
                    g.drawLine(xxP,700-yyP,xxx,700-yyy);
                    xxP = xxx;
                    yyP = yyy;
                }
                int apex = (int)Math.round((shape.getLocation()-start)/deltaRT);
                int apey = 700 - (int)Math.round(shape.expectedIntensityAt(Math.round(shape.getLocation()))/deltaInt);
                apey = Math.max(16, apey);
                g.drawString(shape.getClass().getSimpleName() + " (" + shape.getScore() + ")", apex, apey);
            }


        }
    }

    private SavitzkyGolayFilter smooth(double[] smoothedFunction) {
        /*
        if (smoothedFunction.length < 10) {
            return SavitzkyGolayFilter.Window1Polynomial1;
        } else if (smoothedFunction.length < 30) {
            return SavitzkyGolayFilter.Window2Polynomial2;
        } else if (smoothedFunction.length < 60){
            return SavitzkyGolayFilter.Window3Polynomial3;
        } else return SavitzkyGolayFilter.Window4Polynomial3;
         */
        return (SavitzkyGolayFilter)filterBox.getSelectedItem();
    }

}
