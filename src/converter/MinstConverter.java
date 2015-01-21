package converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import data.ImageDefinition;
import data.ImageValue;
import data.IntTargetDefinition;
import data.IntTargetValue;
import data.LearningData;
import data.Schema;

public class MinstConverter extends AbstractConverter {

	@SuppressWarnings("resource")
	public static IntTargetValue[] loadLabelFile(IntTargetDefinition intTargetDefinition, int begin, int size, File file) throws IOException, ParserException {
		System.out.println(begin + " " + size);
		
		FileInputStream fh = new FileInputStream(file);
		
		byte[] bs = new byte[4];
		fh.read(bs); // magic number
		fh.read(bs); // number of items
		int numOfLabels = java.nio.ByteBuffer.wrap(bs).getInt();
		
		if(begin+size > numOfLabels) {
			throw new ParserException("parameter \"end\" is out of bound");
		}
		
		IntTargetValue[] intTargetValues = new IntTargetValue[size];	
		
		fh.read(new byte[begin-1]);
		
		byte[] b = new byte[1];
		for(int i = 0; i < size; i++) {
			fh.read(b);
			if(!intTargetDefinition.inRange(b[0] & 0xFF)) {
				throw new ParserException("integer not in range on position " + i);
			}
			intTargetValues[i] = new IntTargetValue(intTargetDefinition, b[0] & 0xFF);
		}
		
		return intTargetValues;
	}
	
	@SuppressWarnings("resource")
	public static ImageValue[] loadImageFile(ImageDefinition imageDefinition, int begin, int size, File file) throws IOException, ParserException {
		System.out.println(begin + " " + size);
		FileInputStream fh = new FileInputStream(file);
		
		byte[] bs = new byte[4];
		fh.read(bs); // magic number
		fh.read(bs); // number of images
		int numOfImages = java.nio.ByteBuffer.wrap(bs).getInt();
		fh.read(bs); // number of rows
		int rows = java.nio.ByteBuffer.wrap(bs).getInt();
		fh.read(bs); // number of columns
		int columns = java.nio.ByteBuffer.wrap(bs).getInt();
		
		if(begin+size > numOfImages) {
			throw new ParserException("parameter \"end\" is out of bound");
		}
		
		if(imageDefinition.getRowLength() != rows || imageDefinition.getColumnLength() != columns) {
			throw new ParserException("image size invalid. Expected " + imageDefinition.getRowLength() + "x" + imageDefinition.getColumnLength() + ", got" + rows + "x" + columns);
		}
		
		ImageValue[] imageValues = new ImageValue[size];
		
		fh.read(new byte[(begin-1)*rows*columns]);
		
		byte[] b = new byte[1];
		for(int i = 0; i < size; i++) {
			
			int imageData[] = new int[rows*columns];
			for(int j = 0; j < rows*columns; j++) {
				fh.read(b);
				imageData[j] = b[0] & 0xFF;
			}
			
			imageValues[i] = new ImageValue(imageDefinition, imageData);
		}
		
		return imageValues;
	}
	
	public static LearningData loadMinst(Schema schema, int begin, int end, File labelFile, File imageFile) throws IOException, ParserException {
		return new LearningData(schema, loadLabelFile(schema.getIntTargetDefinition(), begin, end, labelFile), loadImageFile(schema.getImageDefinition(), begin, end, imageFile));
	}
	
}
