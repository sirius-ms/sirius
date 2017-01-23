package de.unijena.bioinf.sirius.gui.dialogs;

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

public class QuestionDialog extends JDialog implements ActionListener{

	private ReturnValue rv;
	
	private JButton ok, abort;
	private JCheckBox dontAsk;

	private String property;

	public QuestionDialog(Window owner, String question) {
		this(owner, question, null);
	}

	/**
	 *
	 * @param owner
	 * @param question
	 * @param propertyKey name of the property with which the 'don't ask' flag is saved persistently
     */
	public QuestionDialog(Window owner, String question, String propertyKey) {
		super(owner,JDialog.DEFAULT_MODALITY_TYPE);

		this.property = propertyKey;

		rv = ReturnValue.Abort;
		
		this.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,20,10));
		Icon icon = UIManager.getIcon("OptionPane.questionIcon");
		northPanel.add(new JLabel(icon));
		northPanel.add(new JLabel(question));
		this.add(northPanel,BorderLayout.CENTER);
		JPanel south = new JPanel();
		south.setLayout(new BoxLayout(south,BoxLayout.X_AXIS));
		south.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

		if (propertyKey!=null){
			dontAsk = new JCheckBox();
			dontAsk.addActionListener(this);
			dontAsk.setText("Do not ask me again.");
			south.add(dontAsk);

		}

		south.add(Box.createHorizontalGlue());
		ok = new JButton("Yes");
		ok.addActionListener(this);
		abort = new JButton("No");
		abort.addActionListener(this);
		south.add(ok);
		south.add(abort);
		this.add(south,BorderLayout.SOUTH);
		this.pack();
		this.setResizable(false);
		setLocationRelativeTo(getParent());
		this.setVisible(true);
	}
	
	public ReturnValue getReturnValue(){
		return rv;
	}



	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource()==dontAsk) return;
		if(e.getSource()==ok){
			rv = ReturnValue.Success;

			if (dontAsk!=null && dontAsk.isSelected()){
				ApplicationCore.changeDefaultProptertyPersistent(property, String.valueOf(true));
			}
		} else if (e.getSource()==abort){
			rv = ReturnValue.Abort;
		}
		this.dispose();
	}

}
