/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
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
