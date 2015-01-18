package algorithms;

import java.util.Random;

public class KMean extends AbstractAlgorithms {
	
	private double[][] prototypeCenter;
	private double[] prototypeAssigned;

	public KMean(double[][] points, int k){
		this.points=points;
		this.k=k;
		
	}
	
	public void kmeanAlgorithm(boolean euclid){
		int randomIndex;
		Random generator = new Random();
		for (int i = 0; i < k; i++){
			randomIndex = generator.nextInt(points.length);
			prototypeCenter[i]=points[randomIndex];
		}
		if (euclid == true){
			for (int j = 0; j < 10; j++){
				computeExpectationEuclid(points);
				maximization();
			}
		} else {
			for (int j = 0; j < 10; j++){
				computeExpectationManhattan(points);
				maximization();
			}
		}
	}
	
	private void computeExpectationEuclid(double[][] points){
		int tempCloud=0;
		double tempDist=0;
		double dist;
		for (int i = 0; i < points.length; i++){
			for (int j = 0; j < k; j++){
				dist = computeEuclidDistance(points[i], prototypeCenter[j]);
				if (!(tempDist < dist)){
					tempCloud=i;
					tempDist=dist;
				}
			}
			prototypeAssigned[i]=tempCloud;
		}
	}
	
	private void computeExpectationManhattan(double[][] points){
		int tempCloud=0;
		double tempDist=0;
		double dist;
		for (int i = 0; i < points.length; i++){
			for (int j = 0; j < k; j++){
				dist = computeManhattanDistance(points[i], prototypeCenter[j]);
				if (!(tempDist < dist)){
					tempCloud=i;
					tempDist=dist;
				}
			}
			prototypeAssigned[i]=tempCloud;
		}
	}
	
	private double computeEuclidDistance(double[] p1, double[] p2){
		double d = 0;

		for (int i = 0; i < p1.length; i++) {
			double diff = (p1[i] - p2[i]);
			if (!Double.isNaN(diff)) {
				d += diff * diff;
			}
		}
		return d;
	}
	
	private double computeManhattanDistance(double[] p1, double[] p2){
		double d = 0;

		for (int i = 0; i < p1.length; i++) {
			double diff = (p1[i] - p2[i]);
			if (!Double.isNaN(diff)) {
				d += (diff < 0) ? -diff : diff;
			}
		}

		return d;
	}

	
	private void maximization(){
		double[] newCloudCenter;
		int temp;
		newCloudCenter = new double[points[0].length];
		for (int i = 0; i < k; i++){
			temp=0;
			for (int x = 0; x < points[0].length; x++){
				newCloudCenter[x]=0;
			}
			for (int j = 0; j < points.length; j++){
				if (prototypeAssigned[j]==i){
					for (int h = 0; h < points[0].length; h++){
					newCloudCenter[h]+=points[j][h];
					temp++;
					}
				}
			}
		for (int y = 0; y < newCloudCenter.length; y++){
			newCloudCenter[y]=newCloudCenter[y]/temp;
		}
		prototypeCenter[k]=newCloudCenter;
		}		

	}
	

}