package data;

import java.util.ArrayList;

public class Example {
	private IntTargetValue intTargetValue;
	private ImageValue imageValue;
	
	public Example(ImageValue imageValue) {
		this.imageValue = imageValue;
	}
	
	public Example(IntTargetValue intTargetValue, ImageValue imageValue) {
		this.intTargetValue = intTargetValue;
		this.imageValue = imageValue;
	}
	
	public IntTargetDefinition getIntTargetDefinition() {
		return this.intTargetValue.getIntTargetDefinition();
	}
	
	public int getTargetValue() {
		return this.intTargetValue.getValue();
	}
	
	public ImageValue getImageValue() {
		return this.imageValue;
	}
	
	public static int[] getClassesByCount(ArrayList<Example> examples) {
		if(examples.size() == 0) {
			return null;
		}
		
		int[] classes = new int[examples.get(0).getIntTargetDefinition().getClasses()];
		for(Example example:examples) {
			classes[example.getTargetValue()] += 1;
		}
		
		return classes;
	}

	public void setTargetValue(IntTargetValue targetValue){
		this.intTargetValue = targetValue;
	}
}
