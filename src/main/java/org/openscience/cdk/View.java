package org.openscience.cdk;

import org.openscience.cdk.render.AtomContainerIcon;
import org.openscience.cdk.render.Coloring;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Swing front end for input and display of the structure.  
 * 
 * @author John May
 */
final class View {

    private final JFrame       frame;
    private final JLabel       display;
    private final JTextArea    input;
    private final JSplitPane   splitPane;
    private final JFileChooser fileChooser;
    private final JSlider slider = new JSlider(SwingConstants.VERTICAL, -180, 180, 0);

    final Coloring coloring = Coloring.BLACK;
    final Controller controller;

    private View(final Controller controller) {
        this.controller = controller;
        this.controller.addView(this);
        this.frame = new JFrame();
        this.display = new JLabel();
        this.display.setOpaque(true);
        this.fileChooser = new JFileChooser();
        this.input = new JTextArea();
        this.input.setRows(5);
        this.splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        this.splitPane.add(display);
        this.splitPane.add(input);
        this.splitPane.setDividerLocation(0.5);
        this.frame.add(this.splitPane, BorderLayout.CENTER);
        this.fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // input
        this.input.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) {
                controller.loadContent(input.getText());
            }

            @Override public void removeUpdate(DocumentEvent e) {
                controller.loadContent(input.getText());
            }

            @Override public void changedUpdate(DocumentEvent e) {
                controller.loadContent(input.getText());
            }
        });
        
        // menu bar
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new JMenuItem(new AbstractAction("Save As PDF") {
            @Override public void actionPerformed(ActionEvent e) {
                File f = selectFile();
                if (f != null) controller.exportToPDF(f, slider.getValue());
            }
        }));
        menuBar.add(new JMenuItem(new AbstractAction("Save As SVG") {
            @Override public void actionPerformed(ActionEvent e) {
                File f = selectFile();
                if (f != null) controller.exportToSVG(f, slider.getValue());
            }
        }));
        frame.add(menuBar, BorderLayout.NORTH);
        frame.add(slider, BorderLayout.EAST);
        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.addChangeListener(new ChangeListener() {
            @Override public void stateChanged(ChangeEvent e) {
                update();
            }
        });
    }
    
    File selectFile() {
        int ret = fileChooser.showSaveDialog(frame);
        if (ret == JFileChooser.APPROVE_OPTION)
            return fileChooser.getSelectedFile();
        return null;
    }
    
    void update() {
        if (controller.model == null || controller.model.container == null || controller.model.container.getAtomCount() == 0) {
            System.out.println("null");
            display.setIcon(null);
            return;
        }
        final AtomContainerIcon icon = new AtomContainerIcon(controller.model.rotatedByDegree(slider.getValue()),
                                                             coloring);
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                display.setBackground(coloring.bgColor());
                display.setIcon(icon);
                frame.revalidate();                
            }
        });
    }

    static View createAndShow() {
        final View view = new View(new Controller(null));
        view.frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        view.frame.setSize(new Dimension(512, 512));
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                view.frame.setVisible(true);
                view.splitPane.setDividerLocation(0.5);
            }
        });
        return view;
    }
}
