package converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import data.ImageDefinition;
import data.IntTargetDefinition;
import data.IntTargetValue;
import data.LearningData;
import data.Schema;

public class MinstConverter {

	@SuppressWarnings("resource")
	public static int[] loadLabelFile(IntTargetDefinition intTargetDefinition, String filePath) throws IOException {
		FileInputStream fh = new FileInputStream(filePath);
		
		byte[] bs = new byte[4];
		fh.read(bs); // magic number
		fh.read(bs); // number of items
		int numOfLabels = java.nio.ByteBuffer.wrap(bs).getInt();
		int[] intTargetValues = new int[numOfLabels];
		
		byte[] b = new byte[1];
		for(int i = 0; i < numOfLabels; i++) {
			fh.read(b);
			if(!intTargetDefinition.inRange(b[0] & 0xFF)) {
				//TODO raise Exception
			}
			intTargetValues[i] = b[0] & 0xFF;
		}
		
		return intTargetValues;
	}
	
	@SuppressWarnings("resource")
	public static int[][] loadImageFile(ImageDefinition imageDefinition, String filePath) throws IOException {
		FileInputStream fh = new FileInputStream(filePath);
		
		byte[] bs = new byte[4];
		fh.read(bs); // magic number
		fh.read(bs); // number of images
		int numOfImages = java.nio.ByteBuffer.wrap(bs).getInt();
		fh.read(bs); // number of rows
		int numberOfRows = java.nio.ByteBuffer.wrap(bs).getInt();
		fh.read(bs); // number of columns
		int numberOfColumns = java.nio.ByteBuffer.wrap(bs).getInt();
		
		//TODO check imageDefinition
		
		int[][] imageData = new int[numOfImages][numberOfRows*numberOfColumns];
		byte[] b = new byte[1];
		for(int j = 0; j < numOfImages; j++) {
			for(int i = 0; i < numberOfRows*numberOfColumns; i++) {
				fh.read(b);
				imageData[j][i] = b[0] & 0xFF;
			}
		}
		
		return imageData;
	}
	
	public static LearningData loadMinst(String labelFilePath, String imageFilePath) {
		
		return null;
	}
	
	public static void main(String[] args) {
		try {
//			IntTargetValue[] intTargetValues = MinstConverter.loadLabelFile("./data/train-labels.idx1-ubyte");
//			System.out.println(intTargetValues.length);
//			System.out.println(intTargetValues[5000].getValue());
			
//			int[][] imageData = loadImageFile("./data/train-images.idx3-ubyte");
//			System.out.println(imageData.length);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
