package data;

import org.garret.perst.Persistent;

public class IntTargetDefinition {
	private int minimum;
	private int maximum;

	public IntTargetDefinition(int minimum, int maximum) {
		this.minimum = minimum;
		this.maximum = maximum;
	}
	
	public boolean inRange(int value) {
		return value <= this.maximum && value >= this.minimum;
	}
}
