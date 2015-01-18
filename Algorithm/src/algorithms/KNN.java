package algorithms;


//import java.util.ArrayList;
import java.util.List;

import algorithms.KdTree.Entry;

public class KNN extends AbstractAlgorithms {
	
	
	private KdTree<Integer> manhattanTree;
	private KdTree<Integer> sqrEuclidTree;
	
	public KNN (double[][] points, int[] values){
		this.points=points;
		this.values=values;
		IndexTree(this.points, this.values);
	}

	/*
	 * Creates a Manhattan distance KDTree and a Euclidian distance Tree
	 */
	
	private void IndexTree(double[][] points, int[] values) {
		this.setPoints(points);
		this.values=values;
		if (manhattanTree == null){
		manhattanTree = new KdTree.Manhattan<Integer>(784, 100000);
		}
		if (sqrEuclidTree == null){
			sqrEuclidTree = new KdTree.SqrEuclid<Integer>(784, 100000);
		}
		for (int i = 0; i < this.values.length; i++) {
			sqrEuclidTree.addPoint(points[i], values[i]);
			manhattanTree.addPoint(points[i], values[i]);
		}
	}

	/*
	 * Searches the k nearest neighbors to point in the Euclidian distance Tree
	 */
	
	public int KNNEuclid (double[] point, int k){
	int pointclass=0;
	List<Entry<Integer>> classes;
	classes = sqrEuclidTree.nearestNeighbor(point, k, false);
	pointclass = mostCommon(classes);
	sqrEuclidTree.addPoint(point, pointclass);
	return pointclass;
	}
	
	/*
	 * Searches the k nearest neighbors to point in the Manhattan distance Tree
	 */

	public int KNNManhattan(double[] point, int k){
		int pointclass=0;
		List<Entry<Integer>> classes;
		classes = manhattanTree.nearestNeighbor(point, k, false);
		pointclass = mostCommon(classes);
		manhattanTree.addPoint(point, pointclass);

		return pointclass;
	}
	
	



//	public static void main (String[] args){
//		List<Integer> list = new ArrayList<Integer>();
//		int foo=0;
//		list.add(1);
//		list.add(1);
//		list.add(1);
//		list.add(3);
//		list.add(3);
//		list.add(1);
//		list.add(1);
//		list.add(3);
//		foo = mostCommon(list);
//		System.out.println(foo);
//		return;
//		
//	}

}