package de.unijena.bioinf.FragmentationTreeConstruction.io;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.Pipeline;
import de.unijena.bioinf.FragmentationTreeConstruction.graph.format.DotFormatter;
import de.unijena.bioinf.FragmentationTreeConstruction.graph.format.EdgeFormatter;
import de.unijena.bioinf.FragmentationTreeConstruction.graph.format.ScoringFormatter;
import de.unijena.bioinf.FragmentationTreeConstruction.graph.format.VertexFormatter;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;

import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * @author Kai Dührkop
 */
public class TreeDotWriter  {

    public transient static DecimalFormat scoreFormatter = new DecimalFormat("###0.0##");
    public transient static DecimalFormat preciseFormatter = new DecimalFormat("###0.0#####");

    private final DotFormatter<TreeFragment> formatter;
    private final FragmentationTree tree;
    public TreeDotWriter(FragmentationTree tree) {
        this(tree, new DotFormatter<TreeFragment>(FragmentationTree.getAdapter(),
                new FragmentFormatter(tree.getScore()), new LossFormatter()));
    }

    public TreeDotWriter(FragmentationTree tree, Pipeline pipeline) {
        this(tree, new DotFormatter<TreeFragment>(FragmentationTree.getAdapter(),
                new FragmentFormatter(tree.getScore(), new ScoringFormatter(pipeline.getRootScorer(), pipeline.getDecompositionScorer(),
                        pipeline.getEdgeScorer(), pipeline.getPeakPairScorer(), tree)), new LossFormatter()));
    }

    public TreeDotWriter(FragmentationTree tree, DotFormatter<TreeFragment> formatter) {
        this.formatter = formatter;
        this.tree = tree;
    }

    public void openPerDotViewer() throws IOException{
        final ProcessBuilder proc = new ProcessBuilder(new File("/home/" + System.getenv("USER") + "/bin", "dotviewer").getAbsolutePath(), "-a", "-i");
        final Process p = proc.start();
        final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
        w.write("<!dot>");w.newLine();
        w.write(tree.getInput().getOriginalInput().getName()); w.newLine();
        format(w);
        w.close();
    }

    public void openAsFile() throws IOException{
        final File tempFile = File.createTempFile("tree", "dot");
        final File svgFile = File.createTempFile("tree", "svg");
        //svgFile.deleteOnExit();
        tempFile.deleteOnExit();
        formatToFile(tempFile);
        // menno =(
        final String dotPath;
        {
           if (new File("/usr/local/bin/dot").exists()) {
                dotPath = "/usr/local/bin/dot";
            } else if (new File("/usr/bin/dot").exists()) {
                dotPath = "/usr/bin/dot";
            } else {
                throw new RuntimeException("Can't find dot");
            }
        }
        final ProcessBuilder proc = new ProcessBuilder(dotPath,  "-T", "svg",
                "-o", svgFile.getAbsolutePath(), tempFile.getAbsolutePath() );
        final Process process = proc.start();
        try {
            process.waitFor();
            Thread.sleep(1000);
            Desktop.getDesktop().open(svgFile);
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void formatToFile(File file) throws IOException {
        this.formatter.formatToFile(tree.getRoot(), file);
    }

    public String formatToString() {
        try {
            return formatter.formatToString(tree.getRoot());
        } catch (IOException e) { // StringWriter should not throw IOExceptions
            throw new RuntimeException(e);
        }
    }

    public void format(Writer writer) throws IOException{
        formatter.format(tree.getRoot(), writer);
    }

    public static class LossFormatter implements EdgeFormatter<TreeFragment> {
        @Override
        public String format(TreeFragment parent, TreeFragment child) {
            final Iterator<Loss> iter = parent.getOutgoingEdges().iterator();
            while (iter.hasNext()) {
                final Loss l = iter.next();
                if (l.getTail() == child) {
                    return l.getLoss() + "\\n" + scoreFormatter.format(l.getWeight());
                }
            }
            return "";
        }
    }

    public static class FragmentFormatter implements VertexFormatter<TreeFragment> {

        VertexFormatter<TreeFragment> extraFormatter;

        public FragmentFormatter(double rootScore,VertexFormatter<TreeFragment> extraFormatter ) {
            this.rootScore = rootScore;
            this.extraFormatter = extraFormatter;
        }

        public FragmentFormatter(double rootScore) {
            this(rootScore, null);
        }

        private String collisionEnergy(Fragment f) {
            final ArrayList<CollisionEnergy> cEs = new ArrayList<CollisionEnergy>();
            for (MS2Peak p : f.getPeak().getOriginalPeaks()) {
                if (p.getSpectrum() != null) {
                    CollisionEnergy cE = p.getSpectrum().getCollisionEnergy();
                    if (!cEs.contains(cE)) cEs.add(cE);
                }
            }
            Collections.sort(cEs, CollisionEnergy.getMinEnergyComparator());
            final String s = cEs.toString();
            return "collision: " + s.substring(1, s.length()-1);
        }

        private final double rootScore;
        @Override
        public String format(TreeFragment vertex) {
        	final double deviation = vertex.getPeak().getUnmodifiedMass() - vertex.getDecomposition().getFormula().getMass();
        	final double ppm = deviation*1e6 / vertex.getPeak().getMz();
        	final String score;
            if (vertex.isRoot()) {
                score = (vertex.getPeak().getOriginalPeaks().size()==0) ? "0 (synthetic)" : scoreFormatter.format(rootScore);
            } else {
                score = scoreFormatter.format(vertex.getParentEdge().getWeight());
            }
            return vertex.getDecomposition().getFormula().toString() + "\\n" +
            	"Score: " + score + "\\n" + preciseFormatter.format(vertex.getPeak().getMz()) + " Da" + "\\n"
            + scoreFormatter.format(vertex.getPeak().getRelativeIntensity()*100) + " %\\n" +
                    preciseFormatter.format(deviation) + " Da" + "\\n"
            + scoreFormatter.format(ppm) + " ppm\\n" + collisionEnergy(vertex) + (extraFormatter != null ? "\\n"+extraFormatter.format(vertex) : "");
        }
    }


}
