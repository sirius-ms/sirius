/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.actions;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class OpenPortalAction extends AbstractUserPortalAction {
    public OpenPortalAction() {
        super("Manage Account");
        putValue(Action.SHORT_DESCRIPTION, "Manage your user account in the User Portal.");
    }

    @Override
    String path() {
        return Jobs.runInBackgroundAndLoad(MF, () -> {
            try {
                String it = ApplicationCore.WEB_API.getAuthService().getRefreshTokenForQuickReuse();
                if (it != null)
                    return "auth/login/" + URLEncoder.encode(it, StandardCharsets.UTF_8);
            } catch (IOException | ExecutionException | InterruptedException e) {
                LoggerFactory.getLogger(getClass()).warn("Error when requesting token for quick reuse! You might have to re-login in the browser.",e);
            }
            return "";
        }).getResult();

    }
}