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

package de.unijena.bioinf.ms.frontend.io;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RasterGraphicsIO {
	
	/**
	 * 
	 * @param path
	 * @param image
	 * @param type gif, jpg oder png
	 */
	private static void write(File path,BufferedImage image,String type) throws IOException{
		ImageIO.write(image, type, path);
	}
	
	public static void writeGIF(File path,BufferedImage image) throws IOException{
		write(path, image, "GIF");
	}
	
	public static void writeJPG(File path,BufferedImage image) throws IOException{
		write(path, image, "JPEG");
	}
	
	public static void writePNG(File path,BufferedImage image) throws IOException{
		write(path, image, "PNG");
	}

}
