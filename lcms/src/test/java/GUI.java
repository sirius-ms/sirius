import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.io.lcms.MzMLParser;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.lcms.InMemoryStorage;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.peakshape.*;
import de.unijena.bioinf.model.lcms.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

public class GUI extends JFrame implements KeyListener, ClipboardOwner {


    int offset = 0;
    SpecViewer specViewer;
    ProcessedSample sample;
    LCMSProccessingInstance instance;

    public GUI(LCMSProccessingInstance i, ProcessedSample sample) {
        instance = i;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        sample.ions.sort(Comparator.comparingDouble(FragmentedIon::getMass));
        for (offset=0; offset < sample.ions.size(); ++offset) {
            if (sample.ions.get(offset).getMass()>=625)
                break;
        }
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

        getContentPane().add(left,BorderLayout.WEST);
        getContentPane().add(right,BorderLayout.EAST);
        getContentPane().add(info,BorderLayout.SOUTH);

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
                if (groups.size()<=5)
                    break;
                // cosine
                groups.sort((u,v)->Double.compare(u.getCosine(),v.getCosine()));
                System.out.printf("Median cosine = %f\n", groups.get(groups.size()/2).getCosine() );
                System.out.printf("15%% quantile = %f\n", groups.get((int)(groups.size()*0.15)).getCosine() );
                // correlation
                groups.sort((u,v)->Double.compare(u.getCorrelation(),v.getCorrelation()));
                System.out.printf("Median cosine = %f\n", groups.get(groups.size()/2).getCorrelation() );
                System.out.printf("15%% quantile = %f\n", groups.get((int)(groups.size()*0.15)).getCorrelation() );
            }
        }

        setVisible(true);
    }

    public static void main(String[] args) {

        final File mzxmlFile = new File("/home/kaidu/Downloads/180912_109.mzML");
        InMemoryStorage storage= new InMemoryStorage();
        final LCMSProccessingInstance i = new LCMSProccessingInstance();
        try {
            final LCMSRun parse = (mzxmlFile.getName().endsWith(".mzML") ? new MzMLParser() : new MzXMLParser()).parse(mzxmlFile, storage);
            final ProcessedSample sample = i.addSample(parse, storage);

            i.detectFeatures(sample);


            final GUI gui = new GUI(i, sample);

        } catch (IOException e) {
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
        specViewer.repaint();
    }
    private void nextIon() {
        ++offset;
        if (offset >= sample.ions.size()) offset = 0;
        specViewer.ion = sample.ions.get(offset);
        specViewer.repaint();
    }

    private void info() {
        final Ms2Experiment exp = instance.makeFeature(sample,specViewer.ion,false).toMsExperiment();
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
                        g.drawString(String.format(Locale.US, "%d", (int)(intensityValue)), 8, posY);
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
            for (int k=0; k < ion.getPeak().numberOfScans(); ++k) {
                final double xx = (ion.getPeak().getRetentionTimeAt(k)-start)/deltaRT;
                final double yy = ion.getPeak().getIntensityAt(k)/deltaInt;
                final ChromatographicPeak.Segment s = ion.getPeak().getSegmentForScanId(ion.getPeak().getScanNumberAt(k)).orElse(null);
                Point2D dp = new Point((int)xx, 700-(int)yy);
                g.setColor(Color.BLACK);
                if (prevPoint!=null && s!=prev) {
                    g.drawLine((int)prevPoint.getX(), 700, (int)prevPoint.getX(), 0);
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
                g.fillOval((int)xx-5, 700 - ((int)yy-5), 10, 10);
            }

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
            /*
            for (Scan s : ion.getMsMs().getScans())  {
                final long retentionTime = s.getRetentionTime();
                double prec = s.getPrecursor().getIntensity();
                if (prec == 0d) {
                    prec = 1000d;
                }

                int posX = (int)Math.round((retentionTime-start)/deltaRT);
                int posY = (int)Math.round(prec/deltaInt);
                g.setColor(Color.BLUE);
                g.fillOval((int)posX-5, 700 - ((int)posY-5), 10, 10);

            }
            */
            g.setColor(Color.BLACK);
            g.setFont(medium);
            g.drawString(ion.getSegment().toString(), 50,800);
            g.drawString(String.valueOf(ion.getMass()), 50, 850);

            // draw correlated peaks
            for (CorrelationGroup c : ion.getIsotopes()) {
                final ChromatographicPeak.Segment rightSegment = c.getRightSegment();
                final ChromatographicPeak p = c.getRight();
                prevPoint = null;
                for (int i=rightSegment.getStartIndex(); i < rightSegment.getEndIndex(); ++i) {
                    final long rt = p.getRetentionTimeAt(i);
                    final double intens = p.getIntensityAt(i);
                    int posX = (int)Math.round((rt-start)/deltaRT);
                    int posY = (int)Math.round(intens/deltaInt);
                    g.setColor(Color.GREEN);
                    g.fillOval((int)posX-5, 700 - ((int)posY-5), 10, 10);
                    if (prevPoint != null) {
                        g.drawLine((int)prevPoint.getX(), (int)prevPoint.getY(), posX, 700-posY);
                    }
                    prevPoint = new Point(posX,700-posY);
                }

            }
            for (CorrelatedIon c : ion.getAdducts()) {
                final ChromatographicPeak.Segment rightSegment = c.correlation.getRightSegment();
                final ChromatographicPeak p = c.correlation.getRight();
                prevPoint = null;
                for (int i=rightSegment.getStartIndex(); i < rightSegment.getEndIndex(); ++i) {
                    final long rt = p.getRetentionTimeAt(i);
                    final double intens = p.getIntensityAt(i);
                    int posX = (int)Math.round((rt-start)/deltaRT);
                    int posY = (int)Math.round(intens/deltaInt);
                    g.setColor(Color.MAGENTA);
                    g.fillOval((int)posX-5, 700 - ((int)posY-5), 10, 10);
                    if (prevPoint != null) {
                        g.drawLine((int)prevPoint.getX(), (int)prevPoint.getY(), posX, 700-posY);
                    }
                    prevPoint = new Point(posX,700-posY);
                }
                if (c.correlation.getAnnotation()!=null) {
                    int yyy = (int) Math.round(p.getIntensityAt(rightSegment.getApexIndex()) / deltaInt) - 16;
                    int xxx = (int) Math.round((p.getRetentionTimeAt(rightSegment.getApexIndex()) - start) / deltaRT);
                    g.drawString(String.format(Locale.US, "%s %d %%, %d %%",c.correlation.getAnnotation() , (int)Math.round(100*c.correlation.getCorrelation()), (int)Math.round(100*c.correlation.getCosine())), xxx, 700 - yyy);
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

}
