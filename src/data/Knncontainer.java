package data;

/**
 * Created by luca on 21.01.15.
 */
public class Knncontainer {
    private Example[] example;
    private int[] result, count_of_learn_per_class, count_of_test_per_class;
    private int count_of_learn, count_of_test, error;

    public Knncontainer(Example[] example, int[] result, int[] count_of_learn_per_class, int count_of_learn, int[] count_of_test_per_class, int count_of_test, int error) {
        this.example = example;
        this.result = result;
        this.count_of_learn_per_class = count_of_learn_per_class;
        this.count_of_learn = count_of_learn;
        this.count_of_test_per_class = count_of_test_per_class;
        this.count_of_test = count_of_test;
        this.error = error;
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
}
