package de.unijena.bioinf.sirius.gui.load;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

public class CollisionEnergyDialog extends JDialog implements ActionListener, ChangeListener{

	private JSpinner minEnergy, maxEnergy, colEnergy;
	private JButton ok, abort;
	private JCheckBox cb;
	private JPanel rampPanel, mainPanel, singlePanel, cardPanel;
	private CardLayout cardLayout;
	
	private static String RAMP_S = "ramp";
	private static String SINGLE_S = "single";
	
	private ReturnValue returnVal;
	
	
	public CollisionEnergyDialog(Dialog owner,double minCE, double maxCE) {
		super(owner,true);
		returnVal = ReturnValue.Abort;
		minEnergy = new JSpinner(new SpinnerNumberModel(minCE, 0, 100, 0.1));
		maxEnergy = new JSpinner(new SpinnerNumberModel(maxCE, 0, 100, 0.1));
		minEnergy.setEditor(new JSpinner.NumberEditor(minEnergy, "##0.#"));
		maxEnergy.setEditor(new JSpinner.NumberEditor(maxEnergy, "##0.#"));
		minEnergy.addChangeListener(this);
		maxEnergy.addChangeListener(this);
		JLabel minText = new JLabel("minimal energy");
		JLabel maxText = new JLabel("maximal energy");
		ok = new JButton("ok");
		abort = new JButton("abort");
		ok.addActionListener(this);
		abort.addActionListener(this);
		
		rampPanel = new JPanel(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		JPanel temp = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		temp.add(minText);
		rampPanel.add(temp,gbc);
		
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		temp = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		temp.add(maxText);
		rampPanel.add(temp,gbc);
		
		gbc.gridheight = 1;
		gbc.gridwidth = 2;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		rampPanel.add(minEnergy,gbc);
		
		gbc.gridheight = 1;
		gbc.gridwidth = 2;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		rampPanel.add(maxEnergy,gbc);
		
		JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		cb = new JCheckBox("ramp");
		
		modePanel.add(cb);
		
		singlePanel = new JPanel(new GridBagLayout());
		
		JLabel ceLabel = new JLabel("collision Energy");
		
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		temp = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		temp.add(ceLabel);
		singlePanel.add(ceLabel,gbc);
		
		colEnergy = new JSpinner(new SpinnerNumberModel(minCE, 0, 100, 0.1));
		colEnergy.setEditor(new JSpinner.NumberEditor(colEnergy, "##0.#"));
		gbc.gridheight = 1;
		gbc.gridwidth = 2;
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		singlePanel.add(colEnergy,gbc);
		
//		gbc.gridheight = 1;
//		gbc.gridwidth = 3;
//		gbc.gridx = 0;
//		gbc.gridy = 2;
//		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(modePanel,BorderLayout.SOUTH);
		
		cardLayout = new CardLayout();
		cardPanel = new JPanel(cardLayout);
		cardPanel.add(singlePanel,SINGLE_S);
		cardPanel.add(rampPanel,RAMP_S);
		
		if(minCE==maxCE){
			cardLayout.show(cardPanel, SINGLE_S);
			cb.setSelected(false);
		}else{
			cardLayout.show(cardPanel, RAMP_S);
			cb.setSelected(true);
		}
		
		mainPanel.add(cardPanel,BorderLayout.CENTER);
		
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		controlPanel.add(ok);
		controlPanel.add(abort);
		
		this.setLayout(new BorderLayout());
		this.add(mainPanel,BorderLayout.CENTER);
		this.add(controlPanel,BorderLayout.SOUTH);
		
		cb.addActionListener(this);
		
		this.pack();
		setLocationRelativeTo(getParent());
		this.setVisible(true);
	}
	
	public double getMinCollisionEnergy(){
		if(cb.isSelected()){
			return (Double) minEnergy.getValue();
		}else{
			return (Double) colEnergy.getValue();
		}
	}
	
	public double getMaxCollisionEnergy(){
		if(cb.isSelected()){
			return (Double) maxEnergy.getValue();
		}else{
			return (Double) colEnergy.getValue();
		}
	}
	
	public ReturnValue getReturnValue(){
		return returnVal;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == cb){
			if(cb.isSelected()){
				cardLayout.show(cardPanel, RAMP_S);
			}else{
				cardLayout.show(cardPanel, SINGLE_S);
			}
			mainPanel.validate();
			this.repaint();
		}else if(e.getSource() == ok){
			this.returnVal = ReturnValue.Success;
			this.setVisible(false);
		}else if(e.getSource() == abort){
			this.returnVal = ReturnValue.Abort;
			this.setVisible(false);
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		Double val1 = (Double) minEnergy.getValue();
		Double val2 = (Double) maxEnergy.getValue();
		if(val1>val2){
			maxEnergy.setValue(val1);
		}
		
	}

}
