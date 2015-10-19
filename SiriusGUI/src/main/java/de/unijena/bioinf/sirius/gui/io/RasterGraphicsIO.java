package de.unijena.bioinf.sirius.gui.io;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class RasterGraphicsIO {

	public RasterGraphicsIO() {
		// TODO Auto-generated constructor stub
	}
	
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
