package gui;

/**
 * Created by luca on 19.01.15.
 */

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import client.Workbench;
import converter.ParserException;
import data.Example;
import data.Kmeancontainer;
import data.Knncontainer;

public class MainFrame extends JFrame implements ActionListener{
    static final String path_images = "/home/luca/train-images.idx3-ubyte";
    static final String path_labels = "/home/luca/train-labels.idx1-ubyte";

    static private final String newline = "\n";
    JButton openLabels,openImages,importbtn,exportCSV,btn_import,btn_execute,btn_import_png,btn_skip_png,btn_cluster_more,btn_cluster;
    JFileChooser fc,fcPNG;
    Workbench wbench = new Workbench();
    JTabbedPane tabbedPane;
    File file_labels = new File(localsettings.path_labels);
    File file_images = new File(localsettings.path_images);
    File importedPNG;
    JTextField tf_k, tf_n, tf_size, tf_start;
    JComboBox cb_algo, cb_dist,cb_pngimport, cb_cluster;
    JPanel setuppanel, pngpanel, clusterpanel,importpanel,exportpanel,statspanel;
    JTextArea ta_stats;
    ArrayList<Example>[] cluster_list;
    int[] cluster_labels;
    int cluster_i, cluster_j;
    ImagePanel clusterimgp;



    public MainFrame() {
        super("Workbench of DOOM");
        setVisible(true);
        tabbedPane = new JTabbedPane();

        /* IMPORT */
        fc = new JFileChooser();
        importpanel = new JPanel(false);
        importpanel.setLayout(new GridLayout(0, 1));
        importpanel.setPreferredSize(new Dimension(410, 150));
        openLabels = new JButton("Select labels file...");
        openLabels.addActionListener(this);
        openImages = new JButton("Select images file...");
        openImages.addActionListener(this);
        importbtn = new JButton("Begin import");
        importbtn.addActionListener(this);
        tf_size = new JTextField("420");
        tf_start = new JTextField("0");
        importpanel.add(openLabels);
        importpanel.add(openImages);
        JComponent range = new JPanel();
        range.setLayout(new GridLayout(1,0));
        range.add(new JLabel("Samplesize"));
        range.add(tf_size);
        range.add(new JLabel("Begin at"));
        range.add(tf_start);
        importpanel.add(range);
        importpanel.add(importbtn);
        tabbedPane.addTab("Import", importpanel);
        /* END OF IMPORT */
        /* EXPORT */
        exportpanel = new JPanel(false);
        exportpanel.setLayout(new GridLayout(0, 1));
        exportCSV = new JButton("Export CSV");
        exportCSV.addActionListener(this);
        exportpanel.add(exportCSV);
        /* END OF EXPORT */

        createSetupPanel();
        createStatsPanel();

        add(tabbedPane);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 1));
        pack();
    }

    private void dialogPNG (File file) throws IOException {
        tabbedPane.remove(pngpanel);

        JPanel png_import_outer = new JPanel();
        png_import_outer.setLayout(new GridLayout(0,1));
        JPanel png_import = new JPanel();
        png_import.setLayout(new GridLayout(0,2));
        ImagePanel imgp = new ImagePanel(file);
        //imgp.setSize(20,20);
        png_import.add(new JLabel("What number is  this?"));
        String[] classes = {"0","1","2","3","4","5","6","7","8","9"};
        cb_pngimport = new JComboBox(classes);
        png_import.add(cb_pngimport);
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

    private void createClusterPanel (ArrayList<Example>[] cluster_list) throws IOException {
        /* WIP */
        tabbedPane.remove(clusterpanel);

        JPanel cluster_outer = new JPanel();
        cluster_outer.setLayout(new GridLayout(0,1));
        JPanel cluster = new JPanel();
        cluster.setLayout(new GridLayout(0,2));
        cluster_i = 0;
        cluster_j = 0;
        clusterimgp = new ImagePanel(cluster_list[cluster_i].get(cluster_j).getImageValue());
        cluster.add(new JLabel("Assign class to this cluster"));
        String[] classes = {"0","1","2","3","4","5","6","7","8","9"};
        cb_cluster = new JComboBox(classes);
        cluster.add(cb_cluster);
        btn_cluster_more = new JButton("Show me more");
        btn_cluster_more.addActionListener(this);
        btn_cluster = new JButton("That's it!");
        btn_cluster.addActionListener(this);
        cluster.add(btn_cluster_more);
        cluster.add(btn_cluster);

        //cluster_outer.add(imgp);
        cluster_outer.add(cluster);
        clusterpanel = new JPanel();
        clusterpanel.add(cluster_outer);
    }

    private void createStatsPanel(){
        statspanel = new JPanel(false);
        statspanel.setLayout(new GridLayout(0,1));

        ta_stats = new JTextArea();
        statspanel.add(ta_stats);
    }

    private void createSetupPanel(){
        setuppanel = new JPanel(false);
        setuppanel.setLayout(new GridLayout(0,2));

        String[] cb_algo_strings = {"KNN","KMean"};
        cb_algo = new JComboBox(cb_algo_strings);
        setuppanel.add(new JLabel("Algorithm"));
        setuppanel.add(cb_algo);

        tf_k = new JTextField("20");
        setuppanel.add(new JLabel("k"));
        setuppanel.add(tf_k);

        String[] cb_dist_strings = {"Manhattan","Euclidean"};
        cb_dist = new JComboBox(cb_dist_strings);
        setuppanel.add(new JLabel("Distance measuring"));
        setuppanel.add(cb_dist);

        tf_n = new JTextField("10");
        setuppanel.add(new JLabel("How many test datas?"));
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
        } else if (e.getSource() == exportCSV) {
            int returnVal = fc.showOpenDialog(MainFrame.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    wbench.exportCsv(fc.getSelectedFile());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        } else if (e.getSource() == btn_import) {
            int returnVal = fcPNG.showOpenDialog(MainFrame.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                importedPNG = fcPNG.getSelectedFile();
                try {
                    dialogPNG(importedPNG);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                tabbedPane.add("Import PNG", pngpanel);
                tabbedPane.setSelectedComponent(pngpanel);
            }
        } else if (e.getSource() == btn_execute) {
            boolean dist;
            String diststr;
            if (cb_dist.getSelectedIndex() == 1) {dist = true; diststr = "euclidean";} else {dist = false; diststr = "Manhattan";}
            if (cb_algo.getSelectedIndex() == 0) {
                Knncontainer result = wbench.executeknn(Integer.parseInt(tf_k.getText()), Integer.parseInt(tf_n.getText()), dist);
                try {
                    new PanelOfWrongs(tabbedPane, result.getExample(), result.getResult());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                ta_stats.append(
                        "Test finished!\n" +
                        "KNN learned with "+result.getCount_of_learn()+" "+result.getCount_of_learn_per_class()+"\n"+
                        "and classified "+result.getFalses().size()+" wrong with a mean squared error of: "+result.getError()+"\n"+
                        result.getCount_of_test()+" "+result.getCount_of_test_per_class()+" objects were used in the test.\n"+
                        "Distance was measured the "+diststr+" way.");
                tabbedPane.addTab("Stats", statspanel);

            } else if (cb_algo.getSelectedIndex() == 0) {
                ArrayList<Example>[] ret;
                ret = wbench.kmeanlearn(Integer.parseInt(tf_k.getText()), Integer.parseInt(tf_n.getText()), dist);
                try {
                    createClusterPanel(ret);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                tabbedPane.add("Cluster", clusterpanel);
                tabbedPane.setSelectedComponent(clusterpanel);
            }
        } else if (e.getSource() == importbtn) {
            try {
                wbench.importMinst( file_labels,file_images,Integer.parseInt(tf_start.getText()),Integer.parseInt(tf_size.getText()) );
                tabbedPane.remove(importpanel);
                tabbedPane.addTab("Export", exportpanel);
                tabbedPane.addTab("Setup", setuppanel);
                tabbedPane.setSelectedComponent(setuppanel);
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ParserException e1) {
                e1.printStackTrace();
            }
        } else if (e.getSource() == btn_import_png) {
            tabbedPane.remove(pngpanel);
            try {
                System.out.println(cb_pngimport.getSelectedItem().toString());
                wbench.importPng(importedPNG,Integer.getInteger(cb_pngimport.getSelectedItem().toString()));
                System.out.println(cb_pngimport.getSelectedItem().toString());
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ParserException e1) {
                e1.printStackTrace();
            }
        } else if (e.getSource() == btn_skip_png) {
            tabbedPane.remove(pngpanel);
            try {
                wbench.importPng(fcPNG.getSelectedFile());
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ParserException e1) {
                e1.printStackTrace();
            }
        } else if (e.getSource() == btn_cluster) {
            cluster_labels[cluster_i] = Integer.getInteger(cb_cluster.getSelectedItem().toString());
            if(cluster_i+1 >= cluster_list.length){
                tabbedPane.remove(clusterpanel);
                Kmeancontainer result = wbench.kmeantest(cluster_labels,Integer.parseInt(tf_n.getText()));
                try {
                    new PanelOfWrongs(tabbedPane, result.getExample(), result.getResult());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                ta_stats.append(
                        "Test finished!\n" +
                                "KNN learned with "+result.getCount_of_learn()+" "+result.getCount_of_learn_per_class()+"\n"+
                                "and classified "+result+" wrong with a mean squared error of: "+result.getError()+"\n"+
                                result.getCount_of_test()+" "+result.getCount_of_test_per_class()+" objects were used in the test.\n"+
                                "Distance was measured the "+" way.");
            } else if (cluster_j+1 <= cluster_list[cluster_i].size()){
                cluster_i++;
                cluster_j = 0;
                try {
                    clusterimgp = new ImagePanel(cluster_list[cluster_i].get(cluster_j).getImageValue());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }



        } else if (e.getSource() == btn_cluster_more) {
            cluster_labels[cluster_i] = Integer.getInteger(cb_cluster.getSelectedItem().toString());
            if (cluster_j+1 <= cluster_list[cluster_i].size()){
                cluster_j++;
                try {
                    clusterimgp = new ImagePanel(cluster_list[cluster_i].get(cluster_j).getImageValue());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }



        }
    }
}