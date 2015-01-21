package data;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.garret.perst.Index;
import org.garret.perst.Storage;
import org.garret.perst.StorageFactory;

import converter.MinstConverter;
import converter.ParserException;

@SuppressWarnings("rawtypes")
public class PerstLearningData {

	private Storage storage;
	private static PerstLearningData instance;
	private static String defaultDatabase = "./data/LearningData.dbs";
	private Index root;

	public static PerstLearningData getInstance() {
		if (instance == null)
			instance = new PerstLearningData(defaultDatabase);
		return instance;
	}

	@SuppressWarnings("rawtypes")
	private PerstLearningData(String dbName) {
		storage = StorageFactory.getInstance().createStorage();
		storage.open(dbName, 1024);
		
		root = (Index) storage.getRoot(); // get storage root
		if (root == null) {
			root = storage.createIndex(String.class, true);
		}

		instance = this;
	}

	@SuppressWarnings("unchecked")
	public void addLearningData(String name, LearningData learningData) {
		root.put(name, learningData);
	}
	
	public LearningData getLearningData(String name) {
		return (LearningData) root.get(name);
	}
	
	protected Storage getStorage() {
		return storage;
	}

	protected void closeDB() {
		storage.close();
	}

	public static void main(String[] args) {
//		LearningData learningData = null;
//		try {
//			learningData = MinstConverter.loadMinst(new Schema(new IntTargetDefinition(0, 9), new ImageDefinition(28, 28)), 1, 100, new File("./data/train-labels.idx1-ubyte"), new File("./data/train-images.idx3-ubyte"));
//		} catch (IOException | ParserException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		PerstLearningData db = PerstLearningData.getInstance();
//		db.addLearningData("first", learningData);
//		db.closeDB();
//		System.out.println(db.getLearningData("first"));
		System.out.println((byte) (int) 1);
	}
}
