package algorithms;

public abstract class AbstractAlgorithms {
	protected int points[][];
	protected int values[];
	protected int k;

	
	/*
	 * Returns the most common element of an Integer List as an int.
	 */
	public int mostCommon(int[] a)
	{
	  int count = 1, tempCount;
	  int popular = a[0];
	  int temp = 0;
	  for (int i = 0; i < (a.length - 1); i++)
	  {
	    temp = a[i];
	    tempCount = 0;
	    for (int j = 1; j < a.length; j++)
	    {
	      if (temp == a[j])
	        tempCount++;
	    }
	    if (tempCount > count)
	    {
	      popular = temp;
	      count = tempCount;
	    }
	  }
	  return popular;
	}
	
	public double computeEuclidDistance(int[] p1, double[] p2){
		double d = 0;
		
		for (int i = 0; i < p1.length; i++) {
			double diff = (p1[i] - p2[i]);
			if (!Double.isNaN(diff)) {
				d += diff * diff;
			}
		}
		return d;
	}
	
	public double computeManhattanDistance(int[] p1, double[] p2){
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

	
	public int[][] getPoints() {
	return points;
	}


	public void setPoints(int[][] points) {
	this.points = points;
	}
	


}
