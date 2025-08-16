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

package io.sirius.ms.gui.webView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.commons.io.IOUtils;
import org.apache.fop.svg.PDFTranscoder;
// import org.apache.batik.transcoder.wmf.tosvg.WMFTranscoder;

public class WebViewIO {

    public static void writeSVG(File file, String svg){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(svg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writePDF(File file, String svg){
        Transcoder transcoder = new PDFTranscoder();
        TranscoderInput transcoderInput = new TranscoderInput(IOUtils.toInputStream(svg, Charset.defaultCharset()));
        try {
            TranscoderOutput transcoderOutput = new TranscoderOutput(new FileOutputStream(file));
            transcoder.transcode(transcoderInput, transcoderOutput);
        } catch (FileNotFoundException e){
            throw new RuntimeException(e);
        } catch (TranscoderException e){
            throw new RuntimeException(e);
        }
    }

    // NOTE: does not work, probably only intended for WMF->SVG
    // public static void writeWMF(File file, String svg){
    //     Transcoder transcoder = new WMFTranscoder();
    //     TranscoderInput transcoderInput = new TranscoderInput(IOUtils.toInputStream(svg));
    //     try {
    //         TranscoderOutput transcoderOutput = new TranscoderOutput(new FileOutputStream(file));
    //         transcoder.transcode(transcoderInput, transcoderOutput);
    //     } catch (FileNotFoundException e){
    //         throw new RuntimeException(e);
    //     } catch (TranscoderException e){
    //         throw new RuntimeException(e);
    //     }
    // }
}
