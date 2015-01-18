/**
 * Created by luca on 17.01.15.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class workbench {

    public static void main(String[] args) {
        JFrame mainFrame = new JFrame("Workbench of DOOM");
        mainFrame.setSize(800, 500);
        mainFrame.setVisible(true);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new FlowLayout());

        JButton but_openMINST = new JButton("Import MINST");
        mainFrame.add(but_openMINST);

        JPanel pan_import = new JPanel();
        pan_import.setLayout(new FlowLayout());
        pan_import.add(new JLabel("Label data"));
        pan_import.add(new JButton("Open..."));
        pan_import.add(new JLabel("Image data"));
        pan_import.add(new JButton("Open..."));
        pan_import.add(new JButton("Import"));
        pan_import.add(new JButton("Cancel"));
        JDialog dialog_import = new JDialog();
        //dialog_import.setLayout(new FlowLayout());
        dialog_import.setTitle("Import MINST...");
        dialog_import.setSize(250,150);
        dialog_import.setVisible(true);
        dialog_import.add(pan_import);

        /*but_openMINST.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
            }
        });*/


        //JOptionPane.showMessageDialog(mainFrame, "You suck!");
        JButton but_openImage = new JButton("Import images");
        JButton but_exportCSV = new JButton("Export CSV");
        mainFrame.add(but_openImage);
        mainFrame.add(but_exportCSV);


        mainFrame.validate();
    }
}


