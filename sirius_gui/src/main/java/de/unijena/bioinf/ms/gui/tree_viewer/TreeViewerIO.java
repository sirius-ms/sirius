package de.unijena.bioinf.ms.gui.tree_viewer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.commons.io.IOUtils;
import org.apache.fop.svg.PDFTranscoder;
// import org.apache.batik.transcoder.wmf.tosvg.WMFTranscoder;

public class TreeViewerIO {

    public static void writeSVG(File file, String svg){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(svg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writePDF(File file, String svg){
        Transcoder transcoder = new PDFTranscoder();
        TranscoderInput transcoderInput = new TranscoderInput(IOUtils.toInputStream(svg));
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
