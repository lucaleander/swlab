package client;
/**
 * Created by luca on 17.01.15.
 */

import java.io.File;
import java.io.IOException;

import converter.CsvConverter;
import converter.MinstConverter;
import converter.ParserException;
import converter.PngConverter;
import data.Example;
import data.ImageDefinition;
import data.IntTargetDefinition;
import data.IntTargetValue;
import data.LearningData;
import data.Schema;

public class Workbench {
    private LearningData learningData;
	
	public Workbench() {
    }
    
    public void importMinst(File labelFile, File imageFile, int begin, int end) throws IOException, ParserException {
    	this.learningData = MinstConverter.loadMinst(new Schema(new IntTargetDefinition(0,9), new ImageDefinition(28, 28)), begin, end, labelFile, imageFile);
    }
    
    public void importPng(File file) throws IOException, ParserException {
    	learningData.addExample(new Example(PngConverter.loadImage(this.learningData.getSchema().getImageDefinition(), file)));
    }
    
    public void importPng(File file, int label) throws IOException, ParserException {
    	learningData.addExample(new Example(new IntTargetValue(this.learningData.getSchema().getIntTargetDefinition(), label), PngConverter.loadImage(this.learningData.getSchema().getImageDefinition(), file)));
    }
    
    public void exportCsv(File file) throws IOException {
    	CsvConverter.dumpFile(file, learningData);
    }
    
    public String[] execute (int alg, int k, int nperclass, int dist){
        String[] stats = {};
        return stats;
    }
}


