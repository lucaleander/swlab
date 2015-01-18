package converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MinstConverter {

	public void loadLabelFile(String filePath) throws IOException {
		FileInputStream in = new FileInputStream(new File(filePath));
		in.read();
		in.read();
		in.read();
		in.read();
		byte[] number = new byte[4];
		in.read(number);
	}
	
	public void loadImageFile(String filePath) {
		
	}
	
}
