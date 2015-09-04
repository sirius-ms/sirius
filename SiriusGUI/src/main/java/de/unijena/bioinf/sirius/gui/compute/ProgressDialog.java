package de.unijena.bioinf.sirius.gui.compute;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.*;

import sun.awt.AWTAccessor.WindowAccessor;
import antlr.actions.cpp.ActionLexerTokenTypes;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IsotopePatternHandling;
import de.unijena.bioinf.sirius.Progress;
import de.unijena.bioinf.sirius.Sirius;

public class ProgressDialog extends JDialog implements Progress, ActionListener{
	
	private JProgressBar progBar;
//	private double minVal, maxVal;
	private RunThread rt;
	private Thread t;
	private JTextArea details;
	private StringBuilder sb;
	private Lock lock;
	private JButton abort;
	private long starttime;
	private JLabel timel;
	private SimpleDateFormat sdf;
	private int step;
	private boolean successful;

	public ProgressDialog(JDialog owner) {
		super(owner,true);
		this.setTitle("Progress");
		
		this.successful = false;
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBorder(BorderFactory.createEtchedBorder());
		this.add(centerPanel, BorderLayout.CENTER);
		
		progBar = new JProgressBar(JProgressBar.HORIZONTAL,0,1000);
		progBar.setValue(0);
		
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
	
	public void start(Sirius sirius,Ms2Experiment exp,Set<MolecularFormula> whiteset){
		sirius.setProgress(this);
		this.setSize(new Dimension(700,210));
		this.successful = false;
		sb = new StringBuilder();
		step = 0;
		lock = new ReentrantLock(true);
		starttime = System.currentTimeMillis();
		rt = new RunThread(sirius, exp, whiteset,this);
		t = new Thread(rt);
		t.start();
//		System.out.println("mache sichtbar");
		this.setVisible(true);
//		System.out.println("ist sichtbar");
	}

	@Override
	public void finished() {
//		lock.lock();
//		sb.append(sdf.format(new Date())+" : step "+step+" finished.\n");
//		details.setText(sb.toString());
//		lock.unlock();
	}

	@Override
	public void info(String info) {
		lock.lock();
		sb.append(sdf.format(new Date())+" : "+info+"\n");
		details.setText(sb.toString());
		lock.unlock();
	}

	@Override
	public void init(double maxVal) {
		lock.lock();
		progBar.setValue(0);
		step++;
		if(step==1){
			sb.append(sdf.format(new Date())+" : Start tree computation.\n");
			details.setText(sb.toString());
		}
		timel.setText("");
		lock.unlock();
	}

	@Override
	public void update(double current, double max, String details) {
		lock.lock();
		int val = (int) (1000*current/max);
//		System.out.println("update "+current+" "+max+" "+details+" "+val);
		progBar.setValue(val);
		timel.setText("compute element "+((int)current)+" of "+((int)max));
		lock.unlock();		
//		System.err.println(this.getSize().getWidth()+" "+this.getSize().getHeight());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==this.abort){
			try{
				t.stop();
			}catch(Exception e2){
				e2.printStackTrace();
			}
			this.setVisible(false);
		}
		
	}
	
	public void computationComplete(){
		System.out.println("computationComplete");
		this.successful = true;
		System.out.println("Anzahl Ergebnisse: "+rt.getResults().size());
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
	private List<IdentificationResult> results;
	private Ms2Experiment exp;
	private Set<MolecularFormula> whiteset;
	private ProgressDialog pd;
	
	RunThread(Sirius sirius,Ms2Experiment exp,Set<MolecularFormula> whiteset,ProgressDialog pd){
		this.sirius = sirius;
		this.results = null;
		this.exp = exp;
		this.whiteset = whiteset;
		this.pd = pd;
	}

	@Override
	public void run() {
//		System.out.println("starte Berechnung");
//		try{
//			Thread.sleep(500); //damit der Prozessdialog angezeigt wird bevor die Berechnung startet
//		}catch(Exception e){
//			
//		}
		results = sirius.identify(exp, 10, true, IsotopePatternHandling.omit, whiteset);
		pd.computationComplete();
	}
	
	List<IdentificationResult> getResults(){
		return this.results;
	}
	
	
}
