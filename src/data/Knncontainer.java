package data;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by luca on 21.01.15.
 */
public class Knncontainer {
    private Example[] example;
    private ArrayList<Example> falses = new ArrayList<Example>();
    private ArrayList<Integer> shouldbe = new ArrayList<Integer>();
    private int[] result, count_of_learn_per_class, count_of_test_per_class;
    private int count_of_learn, count_of_test, error;

    public Knncontainer(Example[] example, int[] result, int[] count_of_learn_per_class, int[] count_of_test_per_class, int error) {
        this.example = example;
        this.result = result;
        this.count_of_learn_per_class = count_of_learn_per_class;
        this.count_of_learn = sumUp(count_of_learn_per_class);
        this.count_of_test_per_class = count_of_test_per_class;
        this.count_of_test = sumUp(count_of_test_per_class);
        this.error = error;

        for (int i=0;i<example.length;i++){
            if (example[i].getTargetValue() != result[i]) {
                falses.add(example[i]);
                shouldbe.add(result[i]);
            }
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

    public int[] getShouldbe() {
            int[] ret = new int[shouldbe.size()];
            Iterator<Integer> iterator = shouldbe.iterator();
            for (int i = 0; i < ret.length; i++)
            {
                ret[i] = iterator.next().intValue();
            }

        return ret;
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

    public Example[] getFalses() {
        return falses.toArray(new Example[falses.size()]);
        //return falses;
    }
}
