package data;

import algorithms.FalseAssigned;

import java.util.ArrayList;

/**
 * Created by luca on 21.01.15.
 */
public class Kmeancontainer {
    private Example[] example;
    private ArrayList<Example> falses;
    private int[] result, count_of_learn_per_class, count_of_test_per_class;
    private int count_of_learn, count_of_test, error;

    public Kmeancontainer(ArrayList<FalseAssigned> falseAssigneds,Example[] example, int[] result, int[] count_of_learn_per_class, int[] count_of_test_per_class, int error) {
        this.example = example;
        this.result = result;
        this.count_of_learn_per_class = count_of_learn_per_class;
        this.count_of_learn = sumUp(count_of_learn_per_class);
        this.count_of_test_per_class = count_of_test_per_class;
        this.count_of_test = sumUp(count_of_test_per_class);
        this.error = error;
        falses = new ArrayList<Example>();

        for (int i=0;i<example.length;i++){
            if (example[i].getTargetValue() == result[i]) falses.add(example[i]);
        }
        for (FalseAssigned i:falseAssigneds){
            falses.add(i.getExample());
        }
    }

    private int sumUp (int[] input){
        int sum = 0;
        for (int i=0; i<input.length;i++){
            sum += input[i];
        }
        return sum;
    }

    public int getError() {
        return error;
    }

    public int[] getCount_of_test_per_class() {
        return count_of_test_per_class;
    }

    public int getCount_of_learn() {
        return count_of_learn;
    }

    public int getCount_of_test() {
        return count_of_test;
    }

    public int[] getCount_of_learn_per_class() {
        return count_of_learn_per_class;
    }

    public Example[] getExample(){
        return example;
    }
    public int[] getResult(){
        return result;
    }

    public ArrayList<Example> getFalses() {
        return falses;
    }
}
