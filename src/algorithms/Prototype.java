package algorithms;

import java.util.LinkedList;

public class Prototype {
	private double[] prototypeCenter;
	private LinkedList<double[]> prototypeCloud;
	private int prototypeClass;

	public Prototype(double[] prototypeCenter){
		this.prototypeCenter=prototypeCenter;
	}
	
	public void addPoint(double[] point){
		prototypeCloud.add(point);
	}
	
	public void deletePoint(double[] point){
		prototypeCloud.removeFirstOccurrence(point);
	}
	
	public double[] getPrototypeCenter(){
		return prototypeCenter;
	}
	
	public void setPrototypeCenter(double[] prototypeCenter){
		this.prototypeCenter = prototypeCenter;
	}
	
	public int getPrototypeClass(){
		return prototypeClass;
	}
	
	public void setPrototypeClass(int Class){
		this.prototypeClass = Class;
	}
}
