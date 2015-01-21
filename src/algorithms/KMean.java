package algorithms;


import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;

import data.Example;
import data.ImageValue;


public class KMean extends AbstractAlgorithms {
	
	
	 //All Center points of the prototypes
	private double[][] prototypeCenter;
	//The classes of all prototypes
	private int[] prototypeClass;
	private ArrayList<Example> temp;
	private ArrayList<Example>[] cluster;
	public KMean(ArrayList<Example> data, int k){
		temp = data;
		points = new int[temp.size()][temp.get(0).getImageValue().getImageData().length];
		for (int i = 0; i < temp.size(); i++){
			points[i] = temp.get(i).getImageValue().getImageData();
			values[i] = temp.get(i).getTargetValue();
		}
		this.k=k;
		prototypeCenter = new double[k][points[0].length];
		prototypeClass = new int[k];
	}
	
	public void kmeanAlgorithm(boolean euclid){
		int randomIndex;
		int[] checkRepeat = new int[k];
		double[] tempCenter = new double[points[0].length];
		Random generator = new Random();
		for (int i = 0; i < k; i++){
			randomIndex = generator.nextInt(points.length);
			while (Arrays.asList(checkRepeat).contains(randomIndex)==true){
				randomIndex = generator.nextInt(points.length);
			}
			for (int j = 0; j < points[0].length; j++){
				tempCenter[j] = (double) points[randomIndex][j];
			}
			prototypeCenter[i]=tempCenter;
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
	
	private void computeExpectationEuclid(int[][] points){
		int tempCloud=0;
		double tempDist=0;
		double dist;
		for (int i = 0; i < points.length; i++){
			for (int j = 0; j < k; j++){
				dist = computeEuclidDistance(points[i], prototypeCenter[j]);
				if (!(tempDist < dist)){
					tempCloud=j;
					tempDist=dist;
				}
			}
			cluster[tempCloud].add(temp.get(i));
		}
	}
	
	private void computeExpectationManhattan(int[][] points){
		int tempCloud=0;
		double tempDist=0;
		double dist;
		for (int x = 0; x < cluster.length; x++){
			cluster[x].clear();
		}
		for (int i = 0; i < points.length; i++){
			for (int j = 0; j < k; j++){
				dist = computeManhattanDistance(points[i], prototypeCenter[j]);
				if (!(tempDist < dist)){
					tempCloud=j;
					tempDist=dist;
				}
			}
			cluster[tempCloud].add(temp.get(i));
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
			for (int j = 0; j < cluster[i].size(); j++){
					for (int h = 0; h < points[0].length; h++){
						newCloudCenter[h]+=cluster[i].get(j).getImageValue().getImageData()[h];
						temp++;
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
	
	public int addPoint(Example newEx, boolean euclid){
		ImageValue newValue = newEx.getImageValue();
		int tempCloud;
		if (euclid == true){
			tempCloud=0;
			double tempDist=0;
			double dist;
				for (int j = 0; j < k; j++){
					dist = computeEuclidDistance(newValue.getImageData(), prototypeCenter[j]);
					if (!(tempDist < dist)){
						tempCloud=j;
						tempDist=dist;
					}
				}
			cluster[tempCloud].add(newEx);
		}else{
			tempCloud=0;
			double tempDist=0;
			double dist;
				for (int j = 0; j < k; j++){
					dist = computeManhattanDistance(newValue.getImageData(), prototypeCenter[j]);
					if (!(tempDist < dist)){
						tempCloud=j;
						tempDist=dist;
					}
				}
			cluster[tempCloud].add(newEx);
			}
		return prototypeClass[tempCloud];
	}
	
	public ArrayList<Example>[] getCluster (){
//		ArrayList<Example>[] returnPrototype=null;
//		int returncount=0;
//		for (int i = 0; i < points.length; i++){
//			if (prototypeAssigned[i]==prototypeNum){
//				returnPrototype[returncount]=points[prototypeNum];
//				returncount++;
//			}
//		}
		return cluster;
	}
	
	public ArrayList<FalseAssigned> checkFalseAssigned(){
		ArrayList<FalseAssigned> falseList = new ArrayList<FalseAssigned>();
		for (int i = 0; i < k; i++){
			for (int j = 0; j < cluster[i].size(); j++){
				if (cluster[i].get(j).getTargetValue() != prototypeClass[i]){
					falseList.add(new FalseAssigned(cluster[i].get(j), prototypeClass[i]));
				}
			}
		}
		return falseList;
	}
	
//	public static void main (String[] args){
//		double[][] points = new double[][] {{1,2},{2,4},{1,5},{3,1},{5,4},{4,3},{1,0},{4,5},{2,5}};
//		int[] values = new int[] {1,2,2,1,3,3,1,3,2};
//		int k = 5;
//		KMean testkmean= new KMean(points, values, k);
//		testkmean.kmeanAlgorithm(true);
//		testkmean.assignPrototypeClass(0, 5);
//		testkmean.assignPrototypeClass(1, 5);
//		testkmean.assignPrototypeClass(2, 5);
//		double[][] assigned = testkmean.checkFalseAssigned();
//		System.out.println(assigned[0][0]);
//	}
	

}