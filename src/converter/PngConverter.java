package converter;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import data.ImageDefinition;
import data.ImageValue;

public class PngConverter extends AbstractConverter {

	public static ImageValue loadImage(ImageDefinition imageDefinition, File file) throws IOException, ParserException {
		BufferedImage image = ImageIO.read(file);

		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ImageIO.write(image, "png", byteStream);
		byteStream.flush();
 
		if(imageDefinition.getRowLength() != image.getHeight() || imageDefinition.getColumnLength() != image.getWidth()) {
			throw new ParserException("image size invalid. Expected " + imageDefinition.getRowLength() + "x" + imageDefinition.getColumnLength() + ", got" + image.getHeight() + "x" + image.getWidth());
		}
		
		int[] imageData = new int[image.getHeight() * image.getWidth()];
		byte[] byteArray = byteStream.toByteArray(); 
		for(int i = 0; i < byteArray.length; i++) {
			imageData[i] = byteArray[i] & 0xFF;
		}
		
		return new ImageValue(imageDefinition, imageData);
	}
	
//	public static void main(String[] args) {
//		try {
//			 
//			ImageValue imageValue = PngConverter.loadImage(new ImageDefinition(28, 28), new File("./data/2.png"));
// 
////			InputStream in = new ByteArrayInputStream(imageInByte);
////			BufferedImage bImageFromConvert = ImageIO.read(in);
// 
//			ImageIO.write(imageValue.getBufferedImage(), "png", new File("./data/test_2.png"));
// 
//		} catch (IOException e) {
//			System.out.println(e.getMessage());
//		} catch (ParserException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
} 