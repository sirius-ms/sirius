/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.dialogs.input;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DragAndDropOpenDialog extends JDialog implements ActionListener{
	public enum ReturnValue {
		oneExperimentForAll, oneExperimentPerFile, abort
	}

	private JButton batchB, expB, abortB;
	
	private ReturnValue rv;

	public DragAndDropOpenDialog(JFrame owner) {
		super(owner,true);
		
		rv = ReturnValue.abort;
		
		this.setLayout(new BorderLayout());
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,20,10));
		Icon icon = UIManager.getIcon("OptionPane.questionIcon");
		northPanel.add(new JLabel(icon));
		northPanel.add(new JLabel("How should the files be imported?"));
		
		
		this.add(northPanel,BorderLayout.CENTER);
		JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		expB = new JButton("As one experiment");
		expB.addActionListener(this);
		batchB = new JButton("As multiple experiments");
		batchB.addActionListener(this);
		abortB = new JButton("Abort");
		abortB.addActionListener(this);
		south.add(expB);
		south.add(batchB);
		south.add(abortB);
		this.add(south,BorderLayout.SOUTH);
		this.pack();
		setLocationRelativeTo(getParent());
		this.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==abortB){
			this.rv = ReturnValue.abort;
		}else if(e.getSource()==batchB){
			this.rv = ReturnValue.oneExperimentPerFile;
		}else if(e.getSource()==expB){
			this.rv = ReturnValue.oneExperimentForAll;
		}
		this.dispose();
	}
	
	public ReturnValue getReturnValue(){
		return this.rv;
	}

}
