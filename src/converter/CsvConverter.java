package converter;

import java.io.BufferedReader;
import java.io.File;
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
	
	public static void dumpFile(File file, LearningData learningData) throws IOException {
		PrintWriter writer = new PrintWriter(file, "UTF-8");
		for(int i = 0; i < learningData.getExamples().size(); i++) {
			writer.print(learningData.getExamples().get(i).getTargetValue());
			writer.print(",");
			writer.print(learningData.getExamples().get(i).getImageValue().getDefinition().getRowLength());
			writer.print(",");
			writer.print(learningData.getExamples().get(i).getImageValue().getDefinition().getColumnLength());
			writer.print(",");
			for(int j = 0; i < learningData.getExamples().get(i).getImageValue().getImageData().length; i++) {
				writer.print(learningData.getExamples().get(i).getImageValue().getImageData()[j]);
				writer.print(",");
			}
			writer.println();
		}
		writer.close();
	}
}
