package converter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import data.LearningData;

public class CsvConverter extends AbstractConverter {

	/*
	 * Format:
	 * label,height,width,pixel,pixel,...
	 */
	
	public static void dumpFile(String filePath, LearningData learningData) throws IOException {
		PrintWriter writer = new PrintWriter(filePath, "UTF-8");
		for(int i = 0; i < learningData.getExamples().length; i++) {
			writer.print(learningData.getExamples()[i].getTargetValue());
			writer.print(",");
			writer.print(learningData.getExamples()[i].getImageValue().getDefinition().getRowLength());
			writer.print(",");
			writer.print(learningData.getExamples()[i].getImageValue().getDefinition().getColumnLength());
			writer.print(",");
			for(int j = 0; i < learningData.getExamples()[i].getImageValue().getImageData().length; i++) {
				writer.print(learningData.getExamples()[i].getImageValue().getImageData()[j]);
				writer.print(",");
			}
			writer.println();
		}
		writer.close();
	}
}
