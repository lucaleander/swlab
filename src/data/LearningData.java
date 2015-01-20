package data;

public class LearningData {
	private Schema schema;
	private Example[] examples;
	
	
	public LearningData(Schema schema, IntTargetValue[] intTargetValues, ImageValue[] imageValues) {
		this.schema = schema;
		for(int i = 0; i < intTargetValues.length; i++) {
			this.examples[i] = new Example(intTargetValues[i], imageValues[i]);	
		}		
	}
	
	public Example[] getExamples() {
		return this.examples;
	}
}
