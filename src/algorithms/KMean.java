package algorithms;


import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;

import data.Example;
import data.ImageValue;


public class KMean extends AbstractAlgorithms {
	
	
	 //All Center points of the prototypes
	private double[][] clusterCenter;
	//The classes of all prototypes
	private int[] clusterClass;
	private ArrayList<Example> temp;
	private ArrayList<ArrayList<Example>> cluster;
	private boolean euclid;
	public KMean(ArrayList<Example> data, int k){
		temp = data;
		points = new int[temp.size()][temp.get(0).getImageValue().getImageData().length];
		values = new int[temp.size()];
		for (int i = 0; i < temp.size(); i++){
			points[i] = temp.get(i).getImageValue().getImageData();
			values[i] = temp.get(i).getTargetValue();
		}
		this.k=k;
		cluster = new ArrayList<ArrayList<Example>>(k);
		for (int i = 0; i < k; i++){
			cluster.add(new ArrayList<Example>());
		}
		clusterCenter = new double[k][points[0].length];
		clusterClass = new int[k];
	}
	
	public void kmeanAlgorithm(boolean euclid){
		int randomIndex;
		this.euclid = euclid;
		ArrayList<Integer> checkRepeat = new ArrayList<Integer>();
		Random generator = new Random();
		for (int i = 0; i < k; i++){
			double[] tempCenter = new double[points[0].length];
			randomIndex = generator.nextInt(points.length);
			while (checkRepeat.contains(randomIndex)==true){
				randomIndex = generator.nextInt(points.length);
			}
			checkRepeat.add(randomIndex);
			for (int j = 0; j < points[i].length; j++){
				tempCenter[j] =  points[randomIndex][j];
			}

			clusterCenter[i]=tempCenter;
		}
//		for (int y = 0; y < clusterCenter.length-1; y++){
//			System.out.println(clusterCenter[y]==clusterCenter[y+1]);
//		}
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
		double dist = 0;
		for (int x = 0; x < cluster.size(); x++){
			cluster.get(x).clear();
		}
		for (int i = 0; i < points.length; i++){
			double tempDist= -1;
			for (int j = 0; j < k; j++){
				dist = computeEuclidDistance(points[i], clusterCenter[j]);
				if (tempDist < 0 || dist < tempDist){
					tempCloud=j;
					tempDist=dist;
				}
				dist = 0;
			}
			cluster.get(tempCloud).add(temp.get(i));
		}
	}
	
	private void computeExpectationManhattan(int[][] points){
		int tempCloud=0;
		double dist = 0;
		for (int x = 0; x < cluster.size(); x++){
			cluster.get(x).clear();
		}
		for (int i = 0; i < points.length; i++){
			double tempDist= -1;
			for (int j = 0; j < k; j++){
				dist = computeManhattanDistance(points[i], clusterCenter[j]);
				if (tempDist < 0 || dist < tempDist){
					tempCloud=j;
					tempDist=dist;
				}
				dist = 0;
			}
			cluster.get(tempCloud).add(temp.get(i));
		}
	}

	
	private void maximization(){
		double[] newCloudCenter;
		int temp = 0;
		for (int i = 0; i < k; i++){
			newCloudCenter = new double[points[0].length];
			for (int j = 0; j < cluster.get(i).size(); j++){
					for (int h = 0; h < points[0].length; h++){
						newCloudCenter[h]+=cluster.get(i).get(j).getImageValue().getImageData()[h];
					}
			}
		for (int y = 0; y < newCloudCenter.length; y++){
			if (cluster.get(i).size() == 0){
				temp = 1;
			} else {
				temp = cluster.get(i).size();
			}
			newCloudCenter[y]=newCloudCenter[y]/temp;
		}
		clusterCenter[i]=newCloudCenter;
		}		
	}
	
	public void assignPrototypeClass(int prototypeNum, int newclass){
		clusterClass[prototypeNum]=newclass;
	}
	
	public int addPoint(Example newEx){
		ImageValue newValue = newEx.getImageValue();
		int tempCloud;
		if (euclid == true){
			tempCloud=0;
			double tempDist=0;
			double dist;
				for (int j = 0; j < k; j++){
					dist = computeEuclidDistance(newValue.getImageData(), clusterCenter[j]);
					if (!(tempDist < dist)){
						tempCloud=j;
						tempDist=dist;
					}
				}
			cluster.get(tempCloud).add(newEx);
		}else{
			tempCloud=0;
			double tempDist=0;
			double dist;
				for (int j = 0; j < k; j++){
					dist = computeManhattanDistance(newValue.getImageData(), clusterCenter[j]);
					if (!(tempDist < dist)){
						tempCloud=j;
						tempDist=dist;
					}
				}
			cluster.get(tempCloud).add(newEx);
			}
		return clusterClass[tempCloud];
	}
	
	public ArrayList<ArrayList<Example>> getCluster (){
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
			for (int j = 0; j < cluster.get(i).size(); j++){
				if (cluster.get(i).get(j).getTargetValue() != clusterClass[i]){
					falseList.add(new FalseAssigned(cluster.get(i).get(j), clusterClass[i]));
				}
			}
		}
		return falseList;
	}
	
	public double computeMeanSquaredError(){
		double mse=0;
		int errorCount = 0;
		for (int i = 0; i < k; i++){
			for (int j = 0; j < cluster.get(i).size(); j++){
				for (int h = 0; h < cluster.get(i).get(j).getImageValue().getImageData().length; h++){
					mse+=Math.pow((clusterCenter[i][h]-cluster.get(i).get(j).getImageValue().getImageData()[h]),2);
					errorCount++;
				}
			}
		}
		mse=mse/errorCount;
		return mse;
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