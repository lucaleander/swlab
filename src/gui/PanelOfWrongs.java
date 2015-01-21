package gui;

import converter.PngConverter;
import data.Example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by luca on 21.01.15.
 */
public class PanelOfWrongs extends JPanel implements ActionListener {
    private Example[] exs;
    private int[] res;
    int i = 0;
    ImagePanel imgp;
    JTextField tf1,tf2;
    JButton btn_next,btn_skip;
    JTabbedPane parent;


    public PanelOfWrongs(JTabbedPane parent,Example[] exs,int[] res){
        this.parent = parent;
        this.exs = exs.clone();
        this.res = res.clone();
        setLayout(new GridLayout(0, 1));
        JPanel outer = new JPanel(new GridLayout(0,2));
        nextPic();
        tf1 = new JTextField();
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
        add(imgp);
        add(outer);
        parent.add(this);
        parent.setSelectedComponent(this);
    }
    private void nextPic(){
        imgp = new ImagePanel(PngConverter.getPng(exs[i].getImageValue().getImageData()));
        tf1.setText(new Integer(exs[i].getTargetValue()).toString());
        tf2.setText(new Integer(res[i]).toString());
        if (i+1 >= exs.length || i+1 >= res.length ) parent.remove(this); else i++;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btn_skip) {
            parent.remove(this);
            }
        else if (e.getSource() == btn_next) {
            nextPic();
        }

    }
}
