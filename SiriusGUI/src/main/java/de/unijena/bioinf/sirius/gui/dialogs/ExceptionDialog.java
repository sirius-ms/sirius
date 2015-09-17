package de.unijena.bioinf.sirius.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class ExceptionDialog extends JDialog implements ActionListener{
	
	private JButton ok;
	
	public ExceptionDialog(Frame owner, String message) {
		super(owner,true);
		
		this.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,20,10));
		Icon icon = UIManager.getIcon("OptionPane.errorIcon");
		northPanel.add(new JLabel(icon));
		northPanel.add(new JLabel(message));
		this.add(northPanel,BorderLayout.CENTER);
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		ok = new JButton("Ok");
		ok.addActionListener(this);
		south.add(ok);
		this.add(south,BorderLayout.SOUTH);
		this.pack();
		this.setVisible(true);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.dispose();
	}
	
}
