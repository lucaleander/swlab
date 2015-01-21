package algorithms;


//import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;

import data.Example;
import data.ImageValue;
import algorithms.KdTree.Entry;

public class KNN extends AbstractAlgorithms {
	
	
	private KdTree<Integer> manhattanTree;
	private KdTree<Integer> sqrEuclidTree;
	private int pointDimension;
	
	public KNN (ArrayList<Example> data, int k){
		ArrayList<Example> temp = data;
		points = new int[temp.size()][temp.get(0).getImageValue().getImageData().length];
		values = new int[temp.size()];
		for (int i = 0; i < temp.size(); i++){
			points[i] = temp.get(i).getImageValue().getImageData();
			values[i] = temp.get(i).getTargetValue();
		}
		this.k=k;
		this.pointDimension = points[0].length;
		IndexTree();
	}

	/*
	 * Creates a Manhattan distance KDTree and a Euclidian distance Tree
	 */
	
	private void IndexTree() {
		double[][] tempPoints = new double[points.length][points[0].length];
		if (manhattanTree == null){
		manhattanTree = new KdTree.Manhattan<Integer>(pointDimension, 100000);
		}
		if (sqrEuclidTree == null){
			sqrEuclidTree = new KdTree.SqrEuclid<Integer>(pointDimension, 100000);
		}
		for (int j = 0; j < points.length; j++){
			for (int l = 0; l < points[0].length; l++)
			tempPoints[j][k] = (double) points[j][k];
		}
		for (int i = 0; i < this.values.length; i++) {
			sqrEuclidTree.addPoint(tempPoints[i], values[i]);
			manhattanTree.addPoint(tempPoints[i], values[i]);
		}
	}

	/*
	 * Searches the k nearest neighbors to point in the Euclidian distance Tree
	 */
	
	public int KNNEuclid (ImageValue newValue){
	int pointclass=0;
	double[] point = new double[newValue.getImageData().length];
	for (int i = 0; i < newValue.getImageData().length; i++){
		point[i]=(double) newValue.getImageData()[i];
	}
	//List <Integer> classes = new ArrayList<Integer>();
	int[] classes;
	List<Entry<Integer>> classesEntry;
	classesEntry = sqrEuclidTree.nearestNeighbor(point, k, false);
	classes = new int[classesEntry.size()];
	for (int i= 0; i < classesEntry.size(); i++){
		classes[i] = (classesEntry.get(i).value);
	}
	pointclass = mostCommon(classes);
	sqrEuclidTree.addPoint(point, pointclass);
	return pointclass;
	}
	
	/*
	 * Searches the k nearest neighbors to point in the Manhattan distance Tree
	 */

	public int KNNManhattan(ImageValue newValue){
		int pointclass=0;
		double[] point = new double[newValue.getImageData().length];
		for (int i = 0; i < newValue.getImageData().length; i++){
			point[i]=(double) newValue.getImageData()[i];
		}
		int[] classes;
		List<Entry<Integer>> classesEntry;
		classesEntry = manhattanTree.nearestNeighbor(point, k, false);
		classes = new int[classesEntry.size()];
		for (int i= 0; i < classesEntry.size(); i++){
			classes[i] = (classesEntry.get(i).value);
		}
		pointclass = mostCommon(classes);
		manhattanTree.addPoint(point, pointclass);

		return pointclass;
	}
	
//	public int checkFalseAssigned(){
//		return 0;
//	}
	
	



//	public static void main (String[] args){
//		KNN testKNN;
//		double[][] testpoints;
//		testpoints = new double[][] {{1,2},{2,4},{1,5},{3,1},{5,4},{4,3},{1,0},{4,5},{2,5}};
//		int[] testvalues = new int[] {1,2,2,1,3,3,1,3,2};
//		int k = 3;
////		List<Integer> list = new ArrayList<Integer>();
////		int foo=0;
////		list.add(1);
////		list.add(1);
////		list.add(1);
////		list.add(3);
////		list.add(3);
////		list.add(1);
////		list.add(1);
////		list.add(3);
////		foo = mostCommon(list);
//		testKNN = new KNN(testpoints, testvalues, k);
//		double[] newpoint = new double[] {0,0};
//		double[] newpoint2 = new double[] {5,5};
//		int pointclass = testKNN.KNNEuclid(newpoint);
//		int pointclass2 = testKNN.KNNManhattan(newpoint2);
//		System.out.println(pointclass + " " + pointclass2);
//		return;
//		
//	}

}