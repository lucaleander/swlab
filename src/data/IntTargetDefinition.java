package data;

public class IntTargetDefinition {
	private int minimum;
	private int maximum;

	public IntTargetDefinition(int minimum, int maximum) {
		this.minimum = minimum;
		this.maximum = maximum;
	}
	
	public int getClasses() {
		return maximum - minimum;
	}
	
	public boolean inRange(int value) {
		return value <= this.maximum && value >= this.minimum;
	}
}
