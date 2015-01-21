package data;

public class IntTargetValue {
	private IntTargetDefinition intTargetDefinition;
	private int value;
	
	public IntTargetValue(IntTargetDefinition intTargetDefinition, int value) {
		this.setIntTargetDefinition(intTargetDefinition);
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}

	public IntTargetDefinition getIntTargetDefinition() {
		return intTargetDefinition;
	}
}
