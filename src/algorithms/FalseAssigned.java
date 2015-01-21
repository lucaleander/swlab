package algorithms;

import data.Example;

public class FalseAssigned {
	private Example example;
	private int falseTargetValue;

	public FalseAssigned(Example ex, int fTV){
		this.example=ex;
		this.falseTargetValue=fTV;
	}
	
	public Example getExample(){
		return this.example;
	}
	
	public int getFalseTargetValue(){
		return this.falseTargetValue;
	}
}
