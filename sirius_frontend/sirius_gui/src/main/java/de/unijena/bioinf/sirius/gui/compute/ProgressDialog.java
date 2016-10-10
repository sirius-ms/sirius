package de.unijena.bioinf.sirius.gui.compute;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.FormulaCandidate;
import de.unijena.bioinf.sirius.*;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.utils.SwingUtils;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class ProgressDialog extends JDialog implements Progress, ActionListener{
	
	private JProgressBar progBar;
//	private double minVal, maxVal;
	private RunThread rt;
	private Thread t;
	private JTextArea details;
	private StringBuilder sb;
	private JButton abort;
	private long starttime;
	private JLabel timel;
	private SimpleDateFormat sdf;
	private int step;
    private Exception exception;
	private volatile boolean successful;
	private volatile boolean cancelComputation;

	public ProgressDialog(JDialog owner) {
		super(owner,true);
		this.setTitle("Progress");
		this.cancelComputation = false;
		this.successful = false;
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBorder(BorderFactory.createEtchedBorder());
		this.add(centerPanel, BorderLayout.CENTER);
		
		progBar = new JProgressBar(JProgressBar.HORIZONTAL,0,1000);
		progBar.setValue(0);
		progBar.setStringPainted(true);

		
		JPanel progressPanel = new JPanel(new BorderLayout());
		progressPanel.add(progBar,BorderLayout.NORTH);
		timel = new JLabel("");
		JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		timePanel.add(timel);
		progressPanel.add(timePanel,BorderLayout.SOUTH);
		progressPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
//		progressPanel.setBorder(BorderFactory.createEtchedBorder());
		centerPanel.add(progressPanel,BorderLayout.NORTH);
		JPanel detailsPanel = new JPanel(new BorderLayout());
		detailsPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
//		detailsPanel.setBorder(BorderFactory.createEtchedBorder());
		
		details = new JTextArea(3,20);
		details.setEditable(false);
		
		JScrollPane jsp = new JScrollPane(details,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		detailsPanel.add(jsp);
		centerPanel.add(detailsPanel,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		abort = new JButton("Abort");
		abort.addActionListener(this);
		buttonPanel.add(abort);
		this.add(buttonPanel, BorderLayout.SOUTH);
		
		sdf = new SimpleDateFormat("hh:mm:ss");
	}

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public void start(Sirius sirius, ExperimentContainer ec, Ms2Experiment exp, FormulaConstraints constraints, int candidates, FormulaSource formulaSource){
		sirius.setProgress(this);
		this.setSize(new Dimension(700,210));
		this.successful = false;
		sb = new StringBuilder();
		step = 0;
		starttime = System.currentTimeMillis();
		rt = new RunThread(sirius, ec, exp,candidates, constraints, this, formulaSource);
		t = new Thread(rt);
		t.start();
		setLocationRelativeTo(getParent());
		this.setVisible(true);
	}

	@Override
	public void finished() {

	}

	@Override
	public void info(final String info) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				sb.append(sdf.format(new Date())+" : "+info+"\n");
				details.setText(sb.toString());
			}
		});
	}

	@Override
	public void init(double maxVal) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				progBar.setValue(0);
				step++;
				if(step==1){
					sb.append(sdf.format(new Date())+" : Start tree computation.\n");
					details.setText(sb.toString());
				}
				timel.setText("");
			}
		});

	}

	@Override
	public void update(final double current, final double max, final String details, final Feedback feedback) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				int val = (int) (1000*current/max);
				progBar.setValue(val);
				timel.setText("compute element "+((int)current)+" of "+((int)max));
			}
		});
		if (cancelComputation) {
			feedback.cancelComputation();
			successful=false;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==this.abort){
			cancelComputation = true;
		}
		
	}
	
	public void computationComplete(boolean success){
		this.successful = success;
		this.setVisible(false);
		this.dispose();
		
		
	}
	
	public boolean isSucessful(){
		return this.successful;
	}
	
	public List<IdentificationResult> getResults(){
		if(!this.successful||rt==null){
			return null;
		}
		return rt.getResults();
	}

}

class RunThread implements Runnable{
	
	private Sirius sirius;
	private volatile List<IdentificationResult> results;
	private Ms2Experiment exp;
	private ProgressDialog pd;
	private int candidates;
	private FormulaConstraints constraints;
    private ExperimentContainer ec;
	private FormulaSource formulaSource;

	RunThread(Sirius sirius, ExperimentContainer ec, Ms2Experiment exp, int candidates, FormulaConstraints constraints, ProgressDialog pd, FormulaSource formulaSource){
		this.sirius = sirius;
		this.results = null;
		this.exp = exp;
		this.pd = pd;
		this.candidates = candidates;
		this.constraints = constraints;
        this.ec = ec;
		this.formulaSource = formulaSource;
	}

	@Override
	public void run() {
		boolean success;
		boolean hasMS2 = exp.getMs2Spectra().size()!=0;
		try {
			sirius.setFormulaConstraints(constraints);
			if (formulaSource!= FormulaSource.ALL_POSSIBLE){

				PrecursorIonType ionType = exp.getPrecursorIonType();
				PrecursorIonType[] allowedIons;
				if (ionType.isIonizationUnknown()) {
					allowedIons = ionType.getCharge()>0 ? WebAPI.positiveIons : WebAPI.negativeIons;
				} else {
					allowedIons = new PrecursorIonType[]{ionType};
				}

				final HashSet<MolecularFormula> whitelist = new HashSet<>();
				for (List<FormulaCandidate> candidates : WebAPI.getRESTDb(formulaSource==FormulaSource.BIODB ? BioFilter.ONLY_BIO : BioFilter.ALL).lookupMolecularFormulas(exp.getIonMass(), sirius.getMs2Analyzer().getDefaultProfile().getAllowedMassDeviation(), allowedIons)) {
                    for (FormulaCandidate f : candidates) whitelist.add(f.getFormula());
                }
                results = hasMS2 ? sirius.identify(exp, candidates, true, IsotopePatternHandling.score, whitelist) : sirius.identifyByIsotopePattern(exp, candidates, whitelist);
			} else if (exp.getPrecursorIonType().isIonizationUnknown()) {
				results = hasMS2 ? sirius.identifyPrecursorAndIonization(exp, candidates, true, IsotopePatternHandling.score) : sirius.identifyByIsotopePattern(exp, candidates);
			} else {
                results = hasMS2 ? sirius.identify(exp, candidates, true, IsotopePatternHandling.score) : sirius.identifyByIsotopePattern(exp, candidates);
            }
			success = (results!=null && results.size()>0);
		} catch (final Exception e) {
			success = false;
			LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					pd.info("Error: " + e.getMessage());
                    ec.setComputeState(ComputingStatus.FAILED);
                    ec.setErrorMessage(e.getMessage());
                    pd.setException(e);
				}
			});
		}
		final boolean s = success;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				pd.computationComplete(s);
			}
		});
	}
	
	List<IdentificationResult> getResults(){
		return this.results;
	}
	
	
}
