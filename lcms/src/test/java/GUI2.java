import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.MemoryFileStorage;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.align.Cluster;
import de.unijena.bioinf.lcms.debuggui.Gradient;
import de.unijena.bioinf.lcms.peakshape.GaussianShape;
import de.unijena.bioinf.lcms.peakshape.PeakShape;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.ConsensusFeature;
import de.unijena.bioinf.model.lcms.Feature;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.model.lcms.ScanPoint;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class GUI2 extends JFrame implements KeyListener, ClipboardOwner {

    private ConsensusFeature[] features;
    private SpecViewer specViewer;
    private int offset = 0;
    LCMSProccessingInstance lcms;

    public GUI2(LCMSProccessingInstance instance, List<String> samples, ConsensusFeature[] consensusFeatures) {
        super();
        this.lcms = instance;
        Arrays.sort(consensusFeatures, Comparator.comparingDouble(ConsensusFeature::getAverageMass));
        this.features = consensusFeatures;
        double maxIntens  = 0d;
        for (ConsensusFeature f : features) {
            for (Feature g : f.getFeatures()) {
                maxIntens = Math.max(maxIntens, g.getIntensity());
            }
        }
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        final JPanel stack = new JPanel();
        stack.setLayout(new BorderLayout());
        getContentPane().add(stack, BorderLayout.SOUTH);
        specViewer = new SpecViewer(samples, consensusFeatures[0], maxIntens);
        getContentPane().add(specViewer,BorderLayout.CENTER);
        getContentPane().add(new JButton(new AbstractAction("->") {
            @Override
            public void actionPerformed(ActionEvent e) {
                inc();
            }
        }), BorderLayout.EAST);
        getContentPane().add(new JButton(new AbstractAction("<-") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dec();
            }
        }), BorderLayout.WEST);
        final JButton toggleRec = new JButton();
        toggleRec.setAction(new AbstractAction("Enable Recalibration") {
            @Override
            public void actionPerformed(ActionEvent e) {
                specViewer.recalibrate = !specViewer.recalibrate;
                specViewer.repaint();
                if (specViewer.recalibrate) toggleRec.setName("Disable Recalibration");
                else toggleRec.setName("Enable Recalibration");
            }
        });
        stack.add(toggleRec, BorderLayout.WEST);

        final JButton export = new JButton(new AbstractAction("Copy") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Ms2Experiment experiment = specViewer.feature.toMs2Experiment();
                final StringWriter wr = new StringWriter();
                try {
                    final BufferedWriter writer = new BufferedWriter(wr);
                    new JenaMsWriter().write(writer,experiment);
                    writer.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                System.out.println(wr.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(wr.toString()), GUI2.this);
            }
        });
        stack.add(export, BorderLayout.EAST);

        {
            int min = (int)features[0].getAverageMass();
            int max = (int)features[features.length-1].getAverageMass();
            final JSlider slider = new JSlider(JSlider.HORIZONTAL,
                    min, max, (int)(features[(int)(features.length*0.25)].getAverageMass()));
            stack.add(slider, BorderLayout.CENTER);
            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    gotoMass(slider.getValue());
                }
            });
        }

        final Thread background = new Thread(new Runnable() {
            @Override
            public void run() {
                try (BufferedWriter bw = FileUtils.getWriter(new File("gui2.ms"))) {
                    final JenaMsWriter writer = new JenaMsWriter();
                    for (ConsensusFeature f : features) {
                        writer.write(bw, f.toMs2Experiment());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("WRITING DONE!!!!!!!!!!!!!");
            }
        });
        background.start();

        setPreferredSize(new Dimension(1400,1200));
        pack();
        setVisible(true);

    }

    private void gotoMass(int value) {
        final double mzVal = value-0.33d;
        for (int k=0; k < features.length; ++k) {
            if (features[k].getAverageMass() >= mzVal) {
                this.offset = k;
                specViewer.feature = features[offset];
                specViewer.repaint();
                return;
            }
        }
    }


    @Override
    public void keyTyped(KeyEvent e) {
        keyReleased(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    public void dec() {
        --offset;
        if (offset<0) offset = features.length+offset;
        specViewer.feature = features[offset];
        specViewer.repaint();
    }
    public void inc() {
        ++offset;
        if (offset >= features.length) offset = 0;
        specViewer.feature = features[offset];
        specViewer.repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode()==KeyEvent.VK_LEFT) {
            dec();
        } else if (e.getKeyCode()==KeyEvent.VK_RIGHT) {
            inc();
        } else if (e.getKeyCode()==KeyEvent.VK_SPACE) {
            System.out.println("Debug Mode");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final ConsensusFeature ION = specViewer.feature;
                    specViewer.recalibrate = !specViewer.recalibrate;
                    System.out.println("Recalibration: " + specViewer.recalibrate);
                    specViewer.repaint();
                }
            });
        }
    }

    public static void main(String[] args) {

        final File mzxmlFile = new File(
                //"/home/kaidu/analysis/example"
                //"/home/kaidu/analysis/canopus/mice/raw/cecum"
                //"/home/kaidu/analysis/example"
                "/home/kaidu/data/raw/BBS"
                );
        MemoryFileStorage storage= null;
        try {
            final LCMSProccessingInstance i = new LCMSProccessingInstance();
            i.getMs2Storage().keepInMemory();
            for (File f : mzxmlFile.listFiles()) {
                storage = new MemoryFileStorage();
                final LCMSRun parse = new MzXMLParser().parse(f, storage);
                final ProcessedSample sample = i.addSample(parse, storage);
                i.detectFeatures(sample);
                storage.backOnDisc();
                storage.dropBuffer();
            }
            i.getMs2Storage().backOnDisc();
            i.getMs2Storage().dropBuffer();

            Cluster c = i.alignAndGapFilling();
            System.out.println("Gapfilling Done."); System.out.flush();

            final List<String> sampleNames = new ArrayList<>();
            addOrderedSampleNames(c, sampleNames);

            final ConsensusFeature[] consensusFeatures = i.makeConsensusFeatures(c);

            System.out.println("Done.");
            System.out.println(consensusFeatures.length + " features in total");
            int good = 0;
            for (ConsensusFeature f : consensusFeatures) {
                int countReal = 0;
                for (Feature g : f.getFeatures()) {
                    if (g.getCorrelatedFeatures().length>1) {
                        ++countReal;
                    }
                }
                if (countReal >= 10) {
                    ++good;
                }
            }
            System.out.println(good + " features are good.");
            new GUI2(i, sampleNames, consensusFeatures);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void addOrderedSampleNames(Cluster c, List<String> sampleNames) {
        if (c.getLeft()==null && c.getRight()==null) {
            sampleNames.addAll(c.getMergedSamples().stream().map(x->x.run.getIdentifier()).sorted().collect(Collectors.toList()));
        } else {
            addOrderedSampleNames(c.getLeft(), sampleNames);
            addOrderedSampleNames(c.getRight(),sampleNames);
        }
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {

    }


    protected class SpecViewer extends Canvas implements MouseMotionListener {
        ConsensusFeature feature;
        List<String> lcmsRuns;
        double maxIntensity;
        Rectangle[] rect2Feature;
        int highlighted = -1;
        public SpecViewer(List<String> runs, ConsensusFeature feature, double maxIntensity) {
            this.feature = feature;
            this.lcmsRuns = runs;
            this.maxIntensity = maxIntensity;
            rect2Feature = new Rectangle[lcmsRuns.size()];
            final int width = 1000 / lcmsRuns.size();
            for (int k=0; k < lcmsRuns.size(); ++k) {
                rect2Feature[k] = new Rectangle(12 + 1+k*width, 801 + 34, width, 50);
            }
            setSize(new Dimension(1300, 868));
            addMouseMotionListener(this);
        }

        protected Font small = new Font("Helvetica",Font.PLAIN, 16);
        protected Font medium = new Font("Helvetica",Font.PLAIN, 24);

        boolean recalibrate = false;

        final BasicStroke dashed =
                new BasicStroke(2.0f,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER,
                        10.0f, new float[]{10.0f}, 0.0f);
        final BasicStroke thicker = new BasicStroke(3f);

        @Override
        public void paint(Graphics g_) {
            super.paint(g_);

            final long min = Arrays.stream(feature.getFeatures()).mapToLong(x->recalibrate ? (long)x.getRtRecalibration().value(x.getTrace()[0].getRetentionTime()) : x.getTrace()[0].getRetentionTime()).min().orElse(0L);
            final long max = Arrays.stream(feature.getFeatures()).mapToLong(x->recalibrate ? (long)x.getRtRecalibration().value(x.getTrace()[x.getTrace().length-1].getRetentionTime()) : x.getTrace()[x.getTrace().length-1].getRetentionTime()).max().orElse(0L);

            long start = min;
            long end = max;
            final double span = (end-start);
            final double deltaRT = span/1000d;

            start -= deltaRT*50;
            end += deltaRT*50;

            final double maxIntensity = Arrays.stream(feature.getFeatures()).mapToDouble(x->x.getIntensity()).max().orElse(1d);


            double intensity = maxIntensity;

            final double deltaInt = intensity / 700d;

            // draw axes
            Graphics2D g = (Graphics2D)g_;

            BasicStroke s = new BasicStroke(2f);
            g.setStroke(s);
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
                long interval = Math.max(1, (endSeconds-startSeconds)/25);
                for (long k=startSeconds; k <= endSeconds; k+=1) {
                    int posX = (int)Math.round((k-startSeconds)*1000d/deltaRT);
                    g.setStroke(s);
                    if (k % interval == 0) {
                        g.translate(posX, 750);
                        g.rotate(-Math.PI / 2d);
                        g.drawString(String.format(Locale.US, "%.2f", (k/60d)), 0, 0);
                        g.rotate(Math.PI / 2d);
                        g.translate(-posX, -750);
                        g.setStroke(new BasicStroke(s.getLineWidth() * 2));
                    }
                    g.drawLine(posX, 696, posX, 704);
                }
            }

            g.setStroke(s);


            {
                // draw quality and charge state
                List<Quality> ms1=new ArrayList<>(), ms2=new ArrayList<>(), shape=new ArrayList<>();
                for (Feature f : feature.getFeatures()) {
                    ms1.add(f.getMs1Quality());
                    ms2.add(f.getMs2Quality());
                    shape.add(f.getPeakShapeQuality());
                }
                Collections.sort(ms1);
                Collections.sort(ms2);
                Collections.sort(shape);
                if (ms1.isEmpty()) ms1.add(Quality.UNUSABLE);
                if (ms2.isEmpty()) ms2.add(Quality.UNUSABLE);
                if (shape.isEmpty()) shape.add(Quality.UNUSABLE);
                g.setColor(Color.BLACK);
                g.fillOval(1000,0,25,25);
                g.setColor(quality2Color(shape.get((int)(shape.size()*0.75d))));
                g.fillArc(1000, 0, 25, 25, 0, 120);
                g.setColor(quality2Color(ms1.get((int)(ms1.size()*0.9d))));
                g.fillArc(1000, 0, 25, 25, 120, 120);
                g.setColor(quality2Color(ms2.get(ms2.size()-1)));
                g.fillArc(1000, 0, 25, 25, 240, 120);


            }


            // draw noise line
            {
                final Stroke def = g.getStroke();
                g.setStroke(dashed);
                g.setColor(Color.BLACK);
                g.drawLine(0, (int)(700 - (1000d/deltaInt)), (int)Math.ceil((end-start)/deltaRT), (int)(700-(1000d/deltaInt)) );
                g.setStroke(def);
            }


            final Feature[] fs = feature.getFeatures().clone();
            Arrays.sort(fs, (u,v)-> {
                int k = Integer.compare(u.getMs2Spectra().length, v.getMs2Spectra().length);
                if (k!=0) return k;
                return Integer.compare(u.getCorrelatedFeatures().length, v.getCorrelatedFeatures().length );
            });
            for (Feature f : fs) {
                g.setStroke(s);
                int beginTrace = f.getTrace()[0].getScanNumber();
                int endTrace = f.getTrace()[f.getTrace().length-1].getScanNumber();
                if (highlighted>=0 && f.getOrigin().getIdentifier().equals(lcmsRuns.get(highlighted))) {
                  g.setColor(Color.RED);
                  g.setStroke(thicker);
                } else if (f.getMs2Spectra().length>0) g.setColor(Color.BLUE);
                else if (f.getCorrelatedFeatures().length>1) g.setColor(Color.GREEN);
                else if (f.getAnnotation(PeakShape.class, ()->new GaussianShape(-1000,0,0,1d)).getPeakShapeQuality().betterThan(Quality.DECENT)) {
                    g.setColor(Color.ORANGE);
                }
                else g.setColor(Color.BLACK);
                int xxP = (int)Math.round(((recalibrate ? f.getRtRecalibration().value(f.getTrace()[0].getRetentionTime())  :
                        f.getTrace()[0].getRetentionTime())-start)/deltaRT);
                int yyP = (int)Math.round(f.getTrace()[0].getIntensity()/deltaInt);
                g.fillOval((int)xxP-5, 700 - ((int)yyP+5), 10, 10);

                final ScanPoint[] trace = f.completeTraceDebug;

                for (int k=1; k < trace.length; ++k) {

                    int xx = (int)Math.round(((recalibrate ? f.getRtRecalibration().value(trace[k].getRetentionTime()) : trace[k].getRetentionTime())-start)/deltaRT);
                    int yy = (int)Math.round(trace[k].getIntensity()/deltaInt);
                    if (trace[k].getScanNumber() <= beginTrace || trace[k].getScanNumber() > endTrace) {
                        g.setStroke(dashed);
                        g.drawOval((int)xx-5, 700 - ((int)yy+5), 10, 10);
                    } else {
                        g.fillOval((int)xx-5, 700 - ((int)yy+5), 10, 10);
                    }
                    g.drawLine(xxP, 700 - yyP, xx, 700 - yy);
                    xxP = xx;
                    yyP = yy;
                    g.setStroke(s);
                }
            }

            g.setColor(Color.BLACK);
            g.drawString("m/z = " + feature.getAverageMass(), 60, 40);
            int width = (int)(1000d / lcmsRuns.size());
            g.fillRect(0, 800, width*lcmsRuns.size()+2, 52);
            for (int k=0; k < lcmsRuns.size(); ++k) {
                final int i = g.getFontMetrics().stringWidth(lcmsRuns.get(k));
                g.translate(1 + k*width + width/2d, 852 + 10);// + i);
                g.rotate(Math.PI/2d);
                g.drawString(lcmsRuns.get(k), 0, 0);
                g.rotate(-Math.PI/2d);
                g.translate(-1 - k*width - width/2d, -852 - 10);// - i);
            }
            for (int k=0; k < feature.getFeatures().length; ++k) {
                int index = lcmsRuns.indexOf(feature.getFeatures()[k].getOrigin().getIdentifier());
                double intens = 1d - (Math.log(feature.getFeatures()[k].getIntensity()) - Math.log(this.maxIntensity))/Math.log(0.0001d);
                if (intens < 0) intens = 0d;
                if (intens >= 1) intens = 1d-1e-16;
                g.setColor(Gradient.GRADIENT_HOT[(int)Math.floor(intens*Gradient.GRADIENT_HOT.length)]);
                g.fillRect(1 + index*width, 801, width, 50);
                g.setColor(Color.BLACK);

            }


        }

        @Override
        public void mouseDragged(MouseEvent e) {

        }

        @Override
        public void mouseMoved(MouseEvent e) {
            final int x = e.getX();
            final int y = e.getY();
            int nh = -1;
            for (int k=0; k < rect2Feature.length; ++k) {
                if (rect2Feature[k].contains(x,y)) {
                    nh = k;
                    break;
                }
            }
            if (nh != highlighted) {
                highlighted = nh;
                repaint();
            }
        }
    }

    private static Color quality2Color(Quality q) {
        if (q.betterThan(Quality.DECENT)) return Color.GREEN;
        if (q.betterThan(Quality.BAD)) return Color.ORANGE;
        if (q.betterThan(Quality.UNUSABLE)) return Color.RED;
        return Color.DARK_GRAY;
    }

}
