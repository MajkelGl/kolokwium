package zadanienazajeciach;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

class ImageEditor extends JFrame {

    private BufferedImage image;
    private JPanel canvas;
    private boolean drawing = false;
    private Color currentColor = Color.BLACK;
    final int brushSize = 5;
    private Point lastPoint;

    public ImageEditor() {
        setTitle("Image Editor");
        setSize(1100, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setupUI();
    }

    private void setupUI() {
        canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (image != null) {
                    g.drawImage(image, 0, 0, null);
                }
            }
        };

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (image != null) {
                    drawing = true;
                    lastPoint = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                drawing = false;
            }
        });

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (drawing && image != null) {
                    Graphics2D g = (Graphics2D) image.getGraphics();
                    g.setColor(currentColor);
                    g.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                    Point currentPoint = e.getPoint();
                    g.drawLine(lastPoint.x, lastPoint.y, currentPoint.x, currentPoint.y);

                    lastPoint = currentPoint;
                    canvas.repaint();
                }
            }
        });

        add(canvas, BorderLayout.CENTER);

        JButton loadButton = new JButton("Load Image");
        loadButton.addActionListener(e -> loadImage());

        JButton blurButton = new JButton("Apply Blur");
        blurButton.addActionListener(e -> applyBlur());

        JButton colorChangeButton = new JButton("Change Color");
        colorChangeButton.addActionListener(e -> changeColor());

        JButton drawButton = new JButton("Draw");
        drawButton.addActionListener(e -> {
            drawing = true;
            currentColor = JColorChooser.showDialog(this, "Choose Drawing Color", currentColor);
        });

        JButton clearButton = new JButton("Clear Drawing");
        clearButton.addActionListener(e -> clearDrawing());

        JButton saturationButton = new JButton("Change Saturation");
        saturationButton.addActionListener(e -> changeSaturation());

        JButton grayscaleButton = new JButton("Convert to Grayscale");
        grayscaleButton.addActionListener(e -> convertToGrayscale());

        JButton cancelOperationButton = new JButton("Cancel Operation");
        cancelOperationButton.addActionListener(e -> cancelCurrentOperation());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saturationButton);
        buttonPanel.add(grayscaleButton);
        buttonPanel.add(cancelOperationButton);
        buttonPanel.add(loadButton);
        buttonPanel.add(blurButton);
        buttonPanel.add(colorChangeButton);
        buttonPanel.add(drawButton);
        buttonPanel.add(clearButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }
    private volatile boolean isCancelRequested = false;
    private final Object imageLock = new Object();

    private void loadImage() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile != null && selectedFile.exists()) {
                loadImageInBackground(selectedFile);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid file selected", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cancelCurrentOperation() {
        synchronized (imageLock) {
            isCancelRequested = true;
        }
    }

    private void applyEffectInBackground(BackgroundImageWorker worker) {
        synchronized (imageLock) {
            if (image != null && !isCancelRequested) {
                worker.execute();
            }
            else
            {
                isCancelRequested = false;
                worker.execute();
            }
        }
    }

    private void applyEffect(BackgroundImageWorker worker) {
        applyEffectInBackground(worker);
    }

    private class BackgroundImageWorker extends SwingWorker<Void, Void> {
        private final Runnable effect;
        private boolean isCancelled = false; // Dodaj zmienną do śledzenia anulowania

        public BackgroundImageWorker(Runnable effect) {
            this.effect = effect;
        }

        @Override
        protected Void doInBackground() throws Exception {
            try {
                synchronized (imageLock) {
                    if (!isCancelled) { // Sprawdź, czy nie zostało anulowane przed rozpoczęciem operacji
                        effect.run();
                    }
                }
            } catch (Exception e) {
                // Handle exceptions if needed
            }
            return null;
        }

        @Override
        protected void done() {
            try {
                synchronized (imageLock) {
                    get();
                    canvas.repaint();
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } finally {
                synchronized (imageLock) {
                    isCancelRequested = false;
                    isCancelled = false; // Zresetuj flagę anulowania po zakończeniu operacji
                }
            }
        }

        public void cancelOperation() {
            isCancelled = true; // Ustaw flagę anulowania
            cancel(true);
        }
    }

    private class LoadImageWorker extends SwingWorker<BufferedImage, Void> {

        private final File selectedFile;

        public LoadImageWorker(File selectedFile) {
            this.selectedFile = selectedFile;
        }

        @Override
        protected BufferedImage doInBackground() throws Exception {
            synchronized (imageLock) {
                return ImageUtils.loadImage(selectedFile);
            }
        }

        @Override
        protected void done() {
            try {
                synchronized (imageLock) {
                    image = get();
                    canvas.repaint();
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ImageEditor.this, "Error loading image", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void loadImageInBackground(File selectedFile) {
        LoadImageWorker worker = new LoadImageWorker(selectedFile);
        worker.execute();
    }


    private void clearDrawing() {
        if (image != null) {
            synchronized (imageLock) {
                image = ImageUtils.loadImage(new File("C:\\Users\\michg\\semestr 3\\jezykiprogramowania\\lab5\\zdjecie.jpeg"));
                canvas.repaint();
            }
        }
    }

    private void applyBlur() {
        BackgroundImageWorker worker = new BackgroundImageWorker(() -> ImageUtils.applyBlur(image));
        applyEffect(worker);
    }

    private void changeColor() {
        BackgroundImageWorker worker = new BackgroundImageWorker(() -> ImageUtils.changeColor(image));
        applyEffect(worker);
    }

    private void changeSaturation() {
        BackgroundImageWorker worker = new BackgroundImageWorker(() -> ImageUtils.changeSaturation(image));
        applyEffect(worker);
    }

    private void convertToGrayscale() {
        BackgroundImageWorker worker = new BackgroundImageWorker(() -> ImageUtils.convertToGrayscale(image));
        applyEffect(worker);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageEditor().setVisible(true));
    }
}

class ImageUtils {
    static BufferedImage loadImage(File file) {
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static void changeSaturation(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                float[] hsb = Color.RGBtoHSB((pixel >> 16) & 0xFF, (pixel >> 8) & 0xFF, pixel & 0xFF, null);
                hsb[1] *= (float) 1.5; // Modify the saturation

                int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                image.setRGB(x, y, rgb);
            }
        }
    }

    static void convertToGrayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                int gray = (int) (0.299 * ((pixel >> 16) & 0xFF) + 0.587 * ((pixel >> 8) & 0xFF) + 0.114 * (pixel & 0xFF));
                int grayscalePixel = (gray << 16) | (gray << 8) | gray;

                image.setRGB(x, y, grayscalePixel);
            }
        }
    }


    static void applyBlur(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = getBlurredPixel(image, x, y);
                result.setRGB(x, y, pixel);
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, result.getRGB(x, y));
            }
        }
    }

    private static int getBlurredPixel(BufferedImage image, int x, int y) {
        int red = 0;
        int green = 0;
        int blue = 0;

        int count = 0;

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int newX = x + i;
                int newY = y + j;

                if (newX >= 0 && newX < image.getWidth() && newY >= 0 && newY < image.getHeight()) {
                    int pixel = image.getRGB(newX, newY);

                    red += (pixel >> 16) & 0xFF;
                    green += (pixel >> 8) & 0xFF;
                    blue += pixel & 0xFF;

                    count++;
                }
            }
        }

        red /= count;
        green /= count;
        blue /= count;

        return (red << 16) | (green << 8) | blue;
    }

    static void changeColor(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                Color color = new Color(pixel);
                int newRed = (color.getRed() + 50) % 256;
                int newGreen = (color.getGreen() + 30) % 256;
                int newBlue = (color.getBlue() + 20) % 256;

                Color newColor = new Color(newRed, newGreen, newBlue);
                image.setRGB(x, y, newColor.getRGB());
            }
        }
    }
}
