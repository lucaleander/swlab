package converter;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class PngConverter {

	public static void parseFile(String filePath) {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
	
		String line = "";
		while ((line = br.readLine()) != null) {
			String[] values = line.split(",");
		}
		br.close();
	}
	
	public dumpFile(String filePath) {
		File imageFile = new File(filePath);
		BufferedImage image = new BufferedImage(result.getWidth(),
				result.getHeigth(), BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = image.getRaster();
	}
} 