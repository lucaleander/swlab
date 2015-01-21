package data;

import org.garret.perst.Persistent;

public class ImageDefinition {
	private int rows;
	private int columns;
	
	public ImageDefinition(int rows, int columns) {
		this.rows = rows;
		this.columns = columns;
	}
	
	public int getRowLength() {
		return this.rows;
	}
	
	public int getColumnLength() {
		return this.columns;
	}
}
