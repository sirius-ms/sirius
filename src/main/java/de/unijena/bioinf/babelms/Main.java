package de.unijena.bioinf.babelms;

import uk.ac.ebi.pride.tools.jmzreader.JMzReader;
import uk.ac.ebi.pride.tools.jmzreader.model.Spectrum;
import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLFile;
import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLParsingException;

import java.io.File;
import java.util.Iterator;

/**
 * Hello world!
 *
 */
public class Main 
{
    public static void main( String[] args )
    {

        try {
            final JMzReader reader = new MzXMLFile(new File("D:/daten/F10-Tax.mzXML"));
            final Iterator<Spectrum> iter = reader.getSpectrumIterator();
            final Spectrum spec = iter.next();
            System.out.println(spec.getMsLevel());
            System.out.println(spec.getPrecursorMZ());
            System.out.println(spec.getPrecursorCharge());
            System.out.println("!");


        } catch (MzXMLParsingException e) {
            e.printStackTrace();
        }


    }
}
