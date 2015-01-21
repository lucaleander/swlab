package client;
/**
 * Created by luca on 17.01.15.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import algorithms.FalseAssigned;
import algorithms.KMean;
import algorithms.KNN;
import converter.CsvConverter;
import converter.MinstConverter;
import converter.ParserException;
import converter.PngConverter;
import data.*;

public class Workbench {
    private LearningData learningData;
    private KMean kmean;

    public Workbench() {
    }

    public void importMinst(File labelFile, File imageFile, int start, int size) throws IOException, ParserException {
        this.learningData = MinstConverter.loadMinst(new Schema(new IntTargetDefinition(0, 9), new ImageDefinition(28, 28)), start, start + size, labelFile, imageFile);
    }

    public void importPng(File file) throws IOException, ParserException {
        learningData.addExample(new Example(PngConverter.loadImage(this.learningData.getSchema().getImageDefinition(), file)));
    }

    public void importPng(File file, int label) throws IOException, ParserException {
        learningData.addExample(new Example(new IntTargetValue(this.learningData.getSchema().getIntTargetDefinition(), label), PngConverter.loadImage(this.learningData.getSchema().getImageDefinition(), file)));
    }

    public void exportCsv(File file) throws IOException {
        CsvConverter.dumpFile(file, learningData);
    }

    public Knncontainer executeknn(int k, int n, boolean dist) {
        KNN knn = new KNN(new ArrayList<Example>(learningData.getExamples().subList(n+1, learningData.getExamples().size())), k);
        int[] resi = new int[n];
        Example[] rese = new Example[n];
        for (int i=0; i < n; i++){
        	
            Example example = learningData.getExamples().subList(0, n).get(i);
            int ret;
            if (dist) ret = knn.KNNEuclid(example.getImageValue());
            else ret = knn.KNNManhattan(example.getImageValue());
            rese[i] = example;
            resi[i] = ret;
        }
        return new Knncontainer(rese,resi,Example.getClassesByCount(learningData.getExamples().subList(n+1, learningData.getExamples().size())),Example.getClassesByCount(new ArrayList<Example>(learningData.getExamples().subList(0, n))),0);
    }
    public File getPng (ImageValue img) {
        return new File("/home/luca/test.png");
    }

    //KMean: 1. learn 2. assign cluster 3. addTestdata 4. show wrongs
    public ArrayList<ArrayList<Example>> kmeanlearn(int k, int n, boolean dist) {
        kmean = new KMean(new ArrayList<Example>(learningData.getExamples().subList(n+1, learningData.getExamples().size())), k);
        kmean.kmeanAlgorithm(dist);
        return kmean.getCluster();
    }
    public Kmeancontainer kmeantest(int[] clusterlabels, int n) {
       
        int[] resi = new int[n];
        Example[] rese = new Example[n];
        for (int i=0; i < n; i++){
            Example example = learningData.getExamples().subList(0,n).get(i);
            rese[i] = example;
            resi[i] = kmean.addPoint(example);
        }
        return new Kmeancontainer(kmean.checkFalseAssigned(),rese,resi,Example.getClassesByCount(learningData.getExamples().subList(n+1, learningData.getExamples().size())),Example.getClassesByCount(new ArrayList<Example>(learningData.getExamples().subList(0,n))),0);
        }
    public void importPerst(String name) {
        PerstLearningData db = PerstLearningData.getInstance();
        learningData = db.getLearningData(name);
    }
    
    public static void main(String[] args) {
    	Workbench wb = new Workbench();
    	try {
    		wb.importMinst(new File("./data/train-labels.idx1-ubyte"), new File("./data/train-images.idx3-ubyte"), 0, 200);
    		wb.kmeanlearn(20, 10, true);
		} catch (IOException | ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}


