package algorithms;

import java.util.Arrays;
import java.util.Random;

public class KMean extends AbstractAlgorithms {
	
	
	  //All Center points of the prototypes
	private double[][] prototypeCenter;
	//The assigned prototypes for every point
	private double[] prototypeAssigned;
	//The classes of all prototypes
	private int[] prototypeClass;

	public KMean(double[][] points, int[] values, int k){
		this.points=points;
		this.values=values;
		this.k=k;
		prototypeCenter = new double[k][points[0].length];
		prototypeAssigned = new double [points.length];
		prototypeClass = new int[k];
	}
	
	public void kmeanAlgorithm(boolean euclid){
		int randomIndex;
		int[] checkRepeat = new int[k];
		Random generator = new Random();
		for (int i = 0; i < k; i++){
			randomIndex = generator.nextInt(points.length);
			while (Arrays.asList(checkRepeat).contains(randomIndex)==true){
				randomIndex = generator.nextInt(points.length);
			}
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
		prototypeCenter[i]=newCloudCenter;
		}		
	}
	
	public void assignPrototypeClass(int prototypeNum, int newclass){
		prototypeClass[prototypeNum]=newclass;
	}
	
	public double[][] getPrototype (int prototypeNum){
		double[][] returnPrototype=null;
		int returncount=0;
		for (int i = 0; i < points.length; i++){
			if (prototypeAssigned[i]==prototypeNum){
				returnPrototype[returncount]=points[prototypeNum];
				returncount++;
			}
		}
		return returnPrototype;
	}
	
	public double[][] checkFalseAssigned(){
		double[][] falseAssigned=new double[points.length][points[0].length];
		int arrayCount=0;
		for (int i = 0; i < points.length; i++){
			if (prototypeAssigned[i]!=values[i]){
				falseAssigned[arrayCount]=points[i];
				arrayCount++;
			}
		}
		return falseAssigned;
	}
	
//	public static void main (String[] args){
//		double[][] points = new double[][]{{1,2,3}, {2,2,4}, {3,1,4}};
//		int[] values = new int[] {5,6,7};
//		int k = 3;
//		KMean testkmean= new KMean(points, values, k);
//		testkmean.kmeanAlgorithm(true);
//		testkmean.assignPrototypeClass(0, 5);
//		testkmean.assignPrototypeClass(1, 5);
//		testkmean.assignPrototypeClass(2, 5);
//		double[][] assigned = testkmean.checkFalseAssigned();
//		System.out.println(assigned[0][0]);
//	}
	

}