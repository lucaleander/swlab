package data;

public class ImageValue {
	private ImageDefinition imageDefinition;
	private int[] imageData;
	private boolean loaded = false;
	
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

	public boolean isLoaded() {
		return this.loaded;
	}
	
	public void setIsLoaded(boolean loaded){
		this.loaded=loaded;
	}
}
