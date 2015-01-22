package gui;

import converter.ParserException;
import converter.PngConverter;
import data.Example;
import data.ImageDefinition;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Created by luca on 21.01.15.
 */
public class PanelOfWrongs extends JPanel implements ActionListener {
    private Example[] exs;
    private int[] res;
    int i = 0;
    ImagePanel imgp;
    JPanel imgp_frame;
    JTextField tf1,tf2;
    JButton btn_next,btn_skip;
    JTabbedPane parent;
    Boolean test = false;


    public PanelOfWrongs(String title,JTabbedPane parent,Example[] exs,int[] res) throws IOException {
        this.parent = parent;
        this.exs = exs;
        this.res = res;
        setLayout(new GridLayout(0, 1));
        JPanel outer = new JPanel(new GridLayout(0,2));
        tf1 = new JTextField();
        tf2 = new JTextField();
        imgp_frame = new JPanel(new GridLayout(0,1));
        nextPic();
        outer.add(new JLabel("Labeled as"));
        outer.add(new JLabel("Wrongly detected as"));
        outer.add(tf1);
        outer.add(tf2);
        btn_skip = new JButton("Skip");
        btn_next = new JButton("Show next");
        btn_skip.addActionListener(this);
        btn_next.addActionListener(this);
        outer.add(btn_skip);
        outer.add(btn_next);
        //imgp_frame.add(imgp);
        add(imgp_frame);
        add(outer);
        parent.add(this,title);
        parent.setSelectedComponent(this);
    }
    private void nextPic() throws IOException {
        //System.out.println(Arrays.toString(exs[i].getImageValue().getImageData()));
        //System.out.println((exs[i].getImageValue().getImageData()).length+" should be "+(28*28));
        if (null != imgp) imgp_frame.remove(imgp);

        imgp = new ImagePanel(exs[i].getImageValue());
        imgp_frame.add(imgp);
        validate();

        /*try {
            imgp = new ImagePanel(PngConverter.loadImage(new ImageDefinition(28,28),new File("data/2.png")));
        } catch (ParserException e) {
            e.printStackTrace();
        }*/
        //System.out.println("This:"+exs[i].getTargetValue());
        tf1.setText(Integer.toString(exs[i].getTargetValue()));
        System.out.println(res[i]);
        tf2.setText(Integer.toString(res[i]));
        if (i+1 >= exs.length || i+1 >= res.length ) parent.remove(this); else i++;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btn_skip) {
            parent.remove(this);
            }
        else if (e.getSource() == btn_next) {
            try {
                nextPic();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    }
}
