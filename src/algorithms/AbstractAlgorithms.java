package algorithms;

import java.util.List;

public abstract class AbstractAlgorithms {
	protected double points[][];
	protected int values[];

	
	/*
	 * Returns the most common element of an Integer List as an int.
	 */
	public static <T> int mostCommon(List<T> list){
		Integer[] classes = list.toArray(new Integer[0]);
		int count = 1;
		int temp = 0;
		int tempCount;
		int common = (int) classes[0];
		
		for (int i = 0; i < (classes.length-1); i++){
			temp = (int) classes[i];
			tempCount=0;
			for (int j = 0; j < classes.length; j++){
				//System.out.println(tempCount + ", " + classes[j] + ", " + temp);
				if (temp == (int) classes[j]){
					tempCount++;
				}
			}
		if (tempCount>count)
			count=tempCount;
			common=temp;
		}
	return common;	
	}

	
	public double[][] getPoints() {
	return points;
	}


	public void setPoints(double points[][]) {
	this.points = points;
	}

}
