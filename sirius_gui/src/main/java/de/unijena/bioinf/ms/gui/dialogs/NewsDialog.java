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

package de.unijena.bioinf.ms.gui.dialogs;


import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.info.News;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Marcus Ludwig on 25.01.17.
 */
public class NewsDialog extends JDialog implements ActionListener {
    private List<News> newsList;
    private JButton ok;

    public NewsDialog(Frame owner, List<News> newsList) {
        super(owner, "We have news!");
        this.setLocationRelativeTo(owner);
        this.newsList = newsList;

        this.setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        final JPanel newsPanel = new JPanel();
        newsPanel.setLayout(new BoxLayout(newsPanel,BoxLayout.Y_AXIS));
        newsPanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        Iterator<News> iterator = newsList.iterator();
        while (iterator.hasNext()) {
            News news = iterator.next();
            final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT)); //news aligned left
            panel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
            final JLabel messageLabel = new JLabel(news.getMessage());
            panel.add(messageLabel);
            newsPanel.add(panel);
            if (iterator.hasNext()) newsPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        }

        add(newsPanel, BorderLayout.CENTER);
        final JPanel subpanel = new JPanel(new FlowLayout());
        ok = new JButton("Close");
        ok.addActionListener(this);
        subpanel.add(ok);
        subpanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        add(subpanel, BorderLayout.SOUTH);

        pack();
        setVisible(true);


    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource()==ok){
            //remember displayed news
            final String property = News.PROPERTY_KEY;
            StringBuilder knownNews = new StringBuilder(PropertyManager.getProperty(property, null, ""));
            for (News news : newsList) {
                knownNews.append(","+news.getId());
            }
            SiriusProperties.SIRIUS_PROPERTIES_FILE().setAndStoreProperty(property, knownNews.toString());

            this.dispose();
        }
    }
}
