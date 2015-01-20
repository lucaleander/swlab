package data;

public class LearningData {
	private Schema schema;
	private Example[] examples;
	
	
	public LearningData(int rows, int columns, int[] label, int[][] imageData) {
		IntTargetDefinition intTargetDefinition = new IntTargetDefinition(0, 9);
		ImageDefinition imageDefinition = new ImageDefinition(rows, columns);
		this.schema = new Schema(intTargetDefinition, imageDefinition);
		
		this.examples = new Example[imageData.length];
		for(int i = 0; i < imageData.length; i++) {
			this.examples[i] = new Example(new IntTargetValue(intTargetDefinition, label[i]), new ImageValue(imageDefinition, imageData[i]));	
		}		
	}
	
	public Example[] getExamples() {
		return this.examples;
	}
}
