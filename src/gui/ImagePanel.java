package gui;

/**
 * Created by luca on 19.01.15.
 */

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import data.ImageValue;

public class ImagePanel extends JPanel{

    private BufferedImage image;

    public ImagePanel(ImageValue imageValue) throws IOException {
		byte[] byteArray = new byte[imageValue.getImageData().length];
		for(int i = 0; i < imageValue.getImageData().length; i++) {
			byteArray[i] = (byte) imageValue.getImageData()[i];
		}
		
		InputStream in = new ByteArrayInputStream(byteArray);
		image = ImageIO.read(in);
	}
    
    public ImagePanel(File file) throws IOException {
        image = ImageIO.read(file);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, null); // see javadoc for more info on the parameters
    }

}
