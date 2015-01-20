package data;

public class IntTargetValue {
	private IntTargetDefinition intTargetDefinition;
	private int value;
	
	public IntTargetValue(IntTargetDefinition intTargetDefinition, int value) {
		this.intTargetDefinition = intTargetDefinition;
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}
