package client;
/**
 * Created by luca on 17.01.15.
 */

import java.io.File;
import java.io.IOException;

import algorithms.KMean;
import algorithms.KNN;
import com.sun.xml.internal.bind.v2.TODO;
import converter.CsvConverter;
import converter.MinstConverter;
import converter.ParserException;
import converter.PngConverter;
import data.*;
import gui.ImagePanel;

public class Workbench {
    private LearningData learningData;

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

    public Knncontainer executeknn(int k, int nperclass, boolean dist) {
        KNN knn = new KNN(learningData, k);
        final int x = 10;
        int[] resi = new int[x];
        Example[] rese = new Example[x];
        for (int i=0; i < x; i++){
            Example example = learningData.getExamples().get(i);
            int ret;
            if (dist) ret = knn.KNNEuclid(example.getImageValue());
            else ret = knn.KNNManhattan(example.getImageValue());
            rese[i] = example;
            resi[i] = ret;
        }
        int[] temp = {0};
        return new Knncontainer(rese,resi,temp,0,temp,0,0);
    }

    public int[][] executekmean(int k, int nperclass, boolean dist) {
        KMean kmean = new KMean(learningData, k);
        kmean.kmeanAlgorithm(dist);
        return kmean.checkFalseAssigned();
    }
}


