package data;

import java.util.ArrayList;

import org.garret.perst.Persistent;

public class LearningData extends Persistent {
	private Schema schema;
	private ArrayList<Example> examples = new ArrayList<Example>();
	
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

	public Schema getSchema() {
		return schema;
	}
}
