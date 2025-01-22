/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.mainframe;

import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import io.sirius.ms.sdk.model.ConnectionCheck;
import io.sirius.ms.sdk.model.Subscription;
import io.sirius.ms.sdk.model.SubscriptionConsumables;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Optional;

public class WebServiceInfoPanel extends JToolBar implements PropertyChangeListener {
    private static final String INF = Character.toString('∞');
    private final JLabel license;
    private final JLabel consumedQueries;
    private final JLabel pendingJobs;

    public WebServiceInfoPanel(ConnectionMonitor monitor) {
        super("Web service info");
        setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        setPreferredSize(new Dimension(getPreferredSize().width, 16));
        setFloatable(false);

        license = new JLabel("License: ?");
        license.setToolTipText("Web service license information.");
        consumedQueries = new JLabel("Queries: 'UNLIMITED'");
        consumedQueries.setToolTipText(GuiUtils.formatToolTip("Quota Utilization. Consumed feature queries in billing period. (If subscription has a feature query quota/limit)"));
        pendingJobs = new JLabel("Jobs: ?");
        pendingJobs.setToolTipText("Number of pending jobs on web server.");

        add(license);
        add(Box.createGlue());
        add(consumedQueries);
        add(Box.createGlue());
        add(pendingJobs);
        monitor.addConnectionUpdateListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final ConnectionMonitor.ConnectionUpdateEvent cevt = (ConnectionMonitor.ConnectionUpdateEvent) evt;
        ConnectionCheck check = cevt.getConnectionCheck();

        if (check.getLicenseInfo().getSubscription() != null) {
            @Nullable Subscription sub = check.getLicenseInfo().getSubscription();
            license.setText("<html>License: <b>" + sub.getSubscriberName() + "</b>" + (check.getLicenseInfo().getSubscription() == null ? "" : " (" + check.getLicenseInfo().getSubscription().getName() + ")</html>"));
            if (Boolean.TRUE.equals(sub.isCountQueries())) {
                final boolean hasLimit = sub.getInstanceLimit() != null && sub.getInstanceLimit() > 0;
                String max = hasLimit ? String.valueOf(sub.getInstanceLimit()) : INF;
                final int consumed = Optional.ofNullable(check.getLicenseInfo().getConsumables())
                        .map(SubscriptionConsumables::getCountedCompounds).orElse(-1);

                final String current = consumed < 0 ? "N/A" : String.valueOf(consumed);

                Color fontColor = Colors.FOREGROUND_INTERFACE;
                if (hasLimit && (consumed  >= sub.getInstanceLimit()))
                    fontColor = Colors.TEXT_ERROR;
                else if (hasLimit && (consumed  >= sub.getInstanceLimit()* .8))
                    fontColor = Colors.TEXT_WARN_ORANGE;

                String limit = "<font color=\""+ Colors.asHex(fontColor) +"\"> " +  current + "/" + max + " </font>";
                consumedQueries.setText("<html>Quota: <b>" + limit + "</b> (per " + (hasLimit ? "Year" : "Month") + ")</html>");
            } else {
                consumedQueries.setText("<html>Quota: <b>UNLIMITED</b></html>");
            }
        } else {
            license.setText("License: '?'");
            consumedQueries.setText("Quota: '?'");
        }

        Optional.ofNullable(check.getLicenseInfo().getConsumables()).map(SubscriptionConsumables::getPendingJobs)
                .ifPresentOrElse(pj -> pendingJobs.setText("<html>Jobs: <b>" + pj + "</b></html>"),
                        () -> pendingJobs.setText("Jobs: ?"));

        revalidate();
        repaint();
    }
}
