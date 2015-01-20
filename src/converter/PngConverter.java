package converter;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.imageio.ImageIO;

public class PngConverter {

	public static byte[] loadImage(String filePath) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		BufferedImage img = ImageIO.read(new File(filePath));
		ImageIO.write(img, "png", byteStream);
		byteStream.flush();
 
		return byteStream.toByteArray();
	}
} 