package data;

import java.util.ArrayList;

public class LearningData {
	private Schema schema;
	private ArrayList<Example> examples;
	
	
	public LearningData(Schema schema, IntTargetValue[] intTargetValues, ImageValue[] imageValues) {
		this.schema = schema;
		for(int i = 0; i < intTargetValues.length; i++) {
			this.examples.add(new Example(intTargetValues[i], imageValues[i]));	
		}		
	}
	
	public void addExample(Example example) {
		this.examples.add(example);
	}
	
	public ArrayList<Example> getExamples() {
		return this.examples;
	}
}
