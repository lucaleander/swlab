package algorithms;

import java.util.List;

public abstract class AbstractAlgorithms {
	protected double points[][];
	protected int values[];
	protected int k;

	
	/*
	 * Returns the most common element of an Integer List as an int.
	 */
	public int mostCommon(int[] newclasses){
		int[] classes = newclasses;
		int count = 1;
		int temp = 0;
		int tempCount;
		int common = classes[0];
		for (int i = 0; i < (classes.length-1); i++){
			temp = classes[i];
			//System.out.println(temp);
			tempCount=0;
			for (int j = 0; j < classes.length; j++){
				//System.out.println(tempCount + ", " + classes[j] + ", " + temp);
				if (temp == classes[j]){

					tempCount++;
				}
			}
		if (tempCount>count)
			count=tempCount;
			common=temp;
		}
	return common;	
	}
	
	public double computeEuclidDistance(double[] p1, double[] p2){
		double d = 0;

		for (int i = 0; i < p1.length; i++) {
			double diff = (p1[i] - p2[i]);
			if (!Double.isNaN(diff)) {
				d += diff * diff;
			}
		}
		return d;
	}
	
	public double computeManhattanDistance(double[] p1, double[] p2){
		double d = 0;

		for (int i = 0; i < p1.length; i++) {
			double diff = (p1[i] - p2[i]);
			if (!Double.isNaN(diff)) {
				d += (diff < 0) ? -diff : diff;
			}
		}

		return d;
	}
	
	public int numberTestobjects(){
		return points.length;
	}

	
	public double[][] getPoints() {
	return points;
	}


	public void setPoints(double points[][]) {
	this.points = points;
	}

}
