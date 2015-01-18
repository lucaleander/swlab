package converter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class CsvConverter {

	public static void parseFile(String filePath) throws FileNotFoundException {
		BufferedReader br = new BufferedReader(new FileReader(filePath));
	
		String line = "";
		while ((line = br.readLine()) != null) {
			String[] values = line.split(",");
		}
		br.close();
	}
	
	public static void dumpFile(String filePath, ... image) {
		
	}
}
