package data;

import org.garret.perst.Persistent;

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
	
	public int getTargetValue() {
		return this.intTargetValue.getValue();
	}
	
	public ImageValue getImageValue() {
		return this.imageValue;
	}
}
