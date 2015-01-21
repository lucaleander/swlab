package data;

import org.garret.perst.Persistent;

public class Schema {
	private IntTargetDefinition intTargetDefiniton;
	private ImageDefinition imageDefinition;
	
	public Schema(IntTargetDefinition intTargetDefition, ImageDefinition imageDefinition) {
		this.intTargetDefiniton = intTargetDefition;
		this.imageDefinition = imageDefinition;
	}
	
	public IntTargetDefinition getIntTargetDefinition() {
		return this.intTargetDefiniton;
	}
	
	public ImageDefinition getImageDefinition() {
		return this.imageDefinition;
	}
}
