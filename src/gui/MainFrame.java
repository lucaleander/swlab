package gui;

/**
 * Created by luca on 19.01.15.
 */

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;

import client.Workbench;

public class MainFrame extends JFrame implements ActionListener{

    static private final String newline = "\n";
    JButton openLabels,openImages,importbtn,btn_import,btn_execute,btn_import_png,btn_skip_png;
    JFileChooser fc,fcPNG;
    Workbench wbench = new Workbench();
    JTabbedPane tabbedPane;
    File file_labels, file_images;
    JTextField tf_k, tf_n;
    JComboBox cb_algo, cb_dist;
    JPanel setuppanel, pngpanel;


    public MainFrame() {
        super("Workbench of DOOM");
        setVisible(true);
        tabbedPane = new JTabbedPane();

        /* IMPORT */
        fc = new JFileChooser();
        JPanel importpanel = new JPanel(false);
        importpanel.setLayout(new GridLayout(0, 1));
        importpanel.setPreferredSize(new Dimension(410, 150));
        Dimension dim = new Dimension(10,10);
        Box.Filler fill = new Box.Filler(dim,dim,dim);
        openLabels = new JButton("Select labels file...");
        openLabels.addActionListener(this);
        openImages = new JButton("Select images file...");
        openImages.addActionListener(this);
        importbtn = new JButton("Begin import");
        importbtn.addActionListener(this);
        importpanel.add(openLabels);
        importpanel.add(openImages);
        importpanel.add(fill);
        importpanel.add(importbtn);
        tabbedPane.addTab("Import", importpanel);
        /* END OF IMPORT */

        createSetupPanel();

        add(tabbedPane);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 1));
        pack();
    }

    private void dialogPNG (File file) {
        tabbedPane.remove(pngpanel);

        JPanel png_import_outer = new JPanel();
        png_import_outer.setLayout(new GridLayout(0,1));
        JPanel png_import = new JPanel();
        png_import.setLayout(new GridLayout(0,2));
        ImagePanel imgp = new ImagePanel(file);
        //imgp.setSize(20,20);
        png_import.add(new JLabel("What number is  this?"));
        String[] classes = {"0","1","2","3","4","5","6","7","8","9"};
        png_import.add(new JComboBox(classes));
        btn_skip_png = new JButton("Skip");
        btn_skip_png.addActionListener(this);
        btn_import_png = new JButton("That's it!");
        btn_import_png.addActionListener(this);
        png_import.add(btn_skip_png);
        png_import.add(btn_import_png);

        png_import_outer.add(imgp);
        png_import_outer.add(png_import);
        pngpanel = new JPanel();
        pngpanel.add(png_import_outer);
    }

    private void ClusterPanel (File file) {
        /* WIP */
/*        tabbedPane.remove(pngpanel);

        JPanel png_import_outer = new JPanel();
        png_import_outer.setLayout(new GridLayout(0,1));
        JPanel png_import = new JPanel();
        png_import.setLayout(new GridLayout(0,2));
        ImagePanel imgp = new ImagePanel(file);
        //imgp.setSize(20,20);
        png_import.add(new JLabel("Assign a number to this cluster"));
        String[] classes = {"0","1","2","3","4","5","6","7","8","9"};
        png_import.add(new JComboBox(classes));
        btn_skip_png = new JButton("Show me more");
        btn_skip_png.addActionListener(this);
        btn_import_png = new JButton("That's it!");
        btn_import_png.addActionListener(this);
        png_import.add(btn_skip_png);
        png_import.add(btn_import_png);

        png_import_outer.add(imgp);
        png_import_outer.add(png_import);
        pngpanel = new JPanel();
        pngpanel.add(png_import_outer);*/
    }

    private void createSetupPanel(){
        setuppanel = new JPanel(false);
        setuppanel.setLayout(new GridLayout(0,2));

        String[] cb_algo_strings = {"KNN","KMean"};
        cb_algo = new JComboBox(cb_algo_strings);
        setuppanel.add(new JLabel("Algorithm"));
        setuppanel.add(cb_algo);

        tf_k = new JTextField();
        setuppanel.add(new JLabel("k"));
        setuppanel.add(tf_k);

        String[] cb_dist_strings = {"Manhatten","Euclidean"};
        cb_dist = new JComboBox(cb_dist_strings);
        setuppanel.add(new JLabel("Distance measuring"));
        setuppanel.add(cb_dist);

        tf_n = new JTextField();
        setuppanel.add(new JLabel("Set size per class"));
        setuppanel.add(tf_n);

        fcPNG = new JFileChooser();
        FileFilter filter = new FileNameExtensionFilter(".PNG","png");
        fcPNG.setFileFilter(filter);
        btn_import = new JButton("Import extra PNGs");
        btn_import.addActionListener(this);
        setuppanel.add(btn_import);

        btn_execute = new JButton("Execute algorithm");
        btn_execute.addActionListener(this);

        setuppanel.add(btn_execute);
    }

    public void actionPerformed(ActionEvent e) {
        //Handle open button action.
        if (e.getSource() == openLabels) {
            int returnVal = fc.showOpenDialog(MainFrame.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                file_labels = fc.getSelectedFile();
            }
        } else if (e.getSource() == openImages) {
            int returnVal = fc.showOpenDialog(MainFrame.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                file_images = fc.getSelectedFile();
            }
        } else if (e.getSource() == btn_import) {
            int returnVal = fcPNG.showOpenDialog(MainFrame.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                 dialogPNG(fcPNG.getSelectedFile());
                 tabbedPane.add("Import PNG",pngpanel);
                tabbedPane.setSelectedComponent(pngpanel);
            }
        } else if (e.getSource() == importbtn) {
            if (wbench.import_MINST(file_labels,file_images) == 0){
                tabbedPane.addTab("Setup", setuppanel);
                tabbedPane.setSelectedComponent(setuppanel);
            }
        } else if (e.getSource() == btn_import_png || e.getSource() == btn_skip_png) {
            tabbedPane.remove(pngpanel);
            wbench.import_PNG(fcPNG.getSelectedFile());
        }
    }
}