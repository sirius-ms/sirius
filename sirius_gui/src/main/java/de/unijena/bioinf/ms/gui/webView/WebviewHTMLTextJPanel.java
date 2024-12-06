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

package de.unijena.bioinf.ms.gui.webView;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.gui.configs.Colors;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class WebviewHTMLTextJPanel extends WebViewJPanel{
    private final String html;
    public WebviewHTMLTextJPanel(String htmlText) {
        this(htmlText, Colors.BACKGROUND);
    }
    public WebviewHTMLTextJPanel(String htmlText, Color background) {
        super("/sirius/style-light.css","/sirius/style-dark.css");
        final StringBuilder buf = new StringBuilder();
        try (final BufferedReader br = FileUtils.ensureBuffering(new InputStreamReader(WebviewHTMLTextJPanel.class.getResourceAsStream("/sirius/text.html")))) {
            String line;
            while ((line = br.readLine()) != null) buf.append(line).append('\n');
            html = buf.toString().replace("#BACKGROUND#", "#"+Integer.toHexString(background.getRGB()).substring(2)).replace("#TEXT#", htmlText);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void load() {
       load(html);
    }

    public static String styleWarningColor(String text) {
        return "<span class='warn'>"+text+"</span>";
    }

    public static String styleErrorColor(String text) {
        return "<span class='error'>"+text+"</span>";
    }

    public static String styleGoodColor(String text) {
        return "<span class='good'>"+text+"</span>";
    }
}
