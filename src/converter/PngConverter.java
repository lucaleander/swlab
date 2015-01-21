package converter;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import data.ImageDefinition;
import data.ImageValue;
import data.IntTargetDefinition;
import data.LearningData;
import data.Schema;

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
			imageData[i] = byteArray[i];
		}
		
		return new ImageValue(imageDefinition, imageData);
	}
	
	public static void main(String[] args) {
		try {
			
			LearningData learningData = MinstConverter.loadMinst(new Schema(new IntTargetDefinition(0, 9), new ImageDefinition(28, 28)), 0, 200, new File("./data/train-labels.idx1-ubyte"), new File("./data/train-images.idx3-ubyte"));
			
			ImageValue imageValue = PngConverter.loadImage(new ImageDefinition(28, 28), new File("./data/test_2.png"));
			//ImageValue imageValue = learningData.getExamples().get(4).getImageValue();

			byte[] byteArray = new byte[imageValue.getImageData().length];
			for(int i = 0; i < imageValue.getImageData().length; i++) {
				byteArray[i] = (byte) imageValue.getImageData()[i];
			}
			
			BufferedImage fBufferedImage = new BufferedImage (28, 28, BufferedImage.TYPE_BYTE_GRAY);
			WritableRaster wr = fBufferedImage.getRaster();
			wr.setDataElements(0, 0, 28, 28, byteArray);
			ImageIO.write(fBufferedImage, "png", new File("./data/test_3.png"));
 
//			InputStream in = new ByteArrayInputStream(imageInByte);
//			BufferedImage bImageFromConvert = ImageIO.read(in);
 
//			ImageIO.write(imageValue.getBufferedImage(), "png", new File("./data/test_2.png"));
 
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
} 