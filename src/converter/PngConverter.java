package converter;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.imageio.ImageIO;

import data.ImageDefinition;
import data.ImageValue;

public class PngConverter {

	public static ImageValue loadImage(ImageDefinition imageDefinition, String filePath) throws IOException, ParserException {
		BufferedImage image = ImageIO.read(new File(filePath));

		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ImageIO.write(image, "png", byteStream);
		byteStream.flush();
 
		if(imageDefinition.getRowLength() != image.getHeight() || imageDefinition.getColumnLength() != image.getWidth()) {
			throw new ParserException("image size invalid in " + filePath + ". Expected " + imageDefinition.getRowLength() + "x" + imageDefinition.getColumnLength() + ", got" + image.getHeight() + "x" + image.getWidth());
		}
		
		int[] imageData = new int[image.getHeight() * image.getWidth()];
		byte[] byteArray = byteStream.toByteArray(); 
		for(int i = 0; i < byteArray.length; i++) {
			imageData[i] = byteArray[i] & 0xFF;
		}
		
		return new ImageValue(imageDefinition, imageData);
	}
} 