package data;

public class ImageValue {
	private ImageDefinition imageDefinition;
	private int[] imageData;
	
	public ImageValue(ImageDefinition imageDefinition, int[] imageData) {
		this.imageDefinition = imageDefinition;
		this.imageData = imageData;
	}
	
	public ImageDefinition getDefinition() {
		return this.imageDefinition;
	}
	
	public int[] getImageData() {
		return imageData;
	}
}
