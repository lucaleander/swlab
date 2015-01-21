package data;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.garret.perst.Persistent;

public class ImageValue {
	private ImageDefinition imageDefinition;
	private int[] imageData;
	
	public ImageValue(ImageDefinition imageDefinition, int[] imageData) {
		this.imageDefinition = imageDefinition;
		this.imageData = imageData;
	}
	
	public ImageDefinition getDefinition() {
		return this.imageDefinition;
	}
	
	public int[] getImageData() {
		return imageData;
	}
	
	public BufferedImage getBufferedImage() throws IOException {
		byte[] byteArray = new byte[imageData.length];
		for(int i = 0; i < imageData.length; i++) {
			byteArray[i] = (byte) imageData[i];
		}
		
		InputStream in = new ByteArrayInputStream(byteArray);
		return ImageIO.read(in);
	}
}
