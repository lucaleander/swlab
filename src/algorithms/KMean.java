package algorithms;

public class KMean extends AbstractAlgorithms {

	public KMean(double[][] points){
		this.points=points;
		
	}
	
	private void Prototype() {
		double[] prototypeCenter;
		double[][] prototypeCloud;
	}
	
	private double[] Maximization(double[][] prototypeCloud){
		double[] newCloudCenter;
		double temp;
		
		newCloudCenter=null;
		for (int i=0; i<prototypeCloud[0].length; i++){
			temp=0;
			for (int j=0; j<prototypeCloud.length; j++){
				temp+=prototypeCloud[i][j];
			}
			temp = temp/prototypeCloud[0].length;
			newCloudCenter[i]=temp;
		}
		return newCloudCenter;
	}
}
