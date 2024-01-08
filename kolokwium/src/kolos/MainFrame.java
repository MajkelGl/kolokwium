package kolos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;

class CircleThread extends Thread{

    private static final int STEP = 5;
    private volatile boolean isMoving = true;

    private final JPanel panel;
    private final int radius;
    private int x,y;

    public CircleThread(JPanel panel, int radius, int x, int y){
        this.panel = panel;
        this.radius = radius;
        this.x = x;
        this.y = y;
    }
}

class DrawingPanel extends JPanel{

    private final List<CircleThread> circles = new ArrayList<>();

    public void addCircle() {
        Random random = new Random();
        int radius = 30;
        int x = random.nextInt(getHeight() - 2 * radius) + radius;
        int y = random.nextInt(getHeight() - 2 * radius) + radius;
    }

}

public class MainFrame extends JFrame {
    private final DrawingPanel drawingPanel;
    public MainFrame(){
        setTitle("Drawing and Moving Circles");
        setSize(800,600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        drawingPanel = new DrawingPanel();
        add(drawingPanel,BorderLayout.CENTER);

        JButton addButton = new JButton("Dodaj koło");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                drawingPanel.addCircle();
            }
        });

        add(addButton,BorderLayout.SOUTH);

        drawingPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {

            }
        });

        setFocusable(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            }
        });
    }


}
