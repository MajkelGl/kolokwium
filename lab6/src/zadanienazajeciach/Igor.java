package zadanienazajeciach;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

interface ImageEffect {
    BufferedImage applyEffect(BufferedImage image);
}

class BlurEffect implements ImageEffect {

    private final BufferedImage image;

    public BlurEffect(BufferedImage image) {
        this.image = image;
    }

    @Override
    public BufferedImage applyEffect(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage blurredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int avgColor = calculateAverageColor(x, y);
                blurredImage.setRGB(x, y, avgColor);
            }
        }

        return blurredImage;
    }

    private synchronized int calculateAverageColor(int x, int y) {
        int sumRed = 0, sumGreen = 0, sumBlue = 0;
        int count = 0;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;

                if (nx >= 0 && nx < image.getWidth() && ny >= 0 && ny < image.getHeight()) {
                    Color color = new Color(image.getRGB(nx, ny));
                    sumRed += color.getRed();
                    sumGreen += color.getGreen();
                    sumBlue += color.getBlue();
                    count++;
                }
            }
        }

        int avgRed = sumRed / count;
        int avgGreen = sumGreen / count;
        int avgBlue = sumBlue / count;

        return new Color(avgRed, avgGreen, avgBlue).getRGB();
    }
}

class InvertColorEffect implements ImageEffect {
    @Override
    public synchronized BufferedImage applyEffect(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage invertedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                int alpha = (rgb >> 24) & 0xFF;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                red = 255 - red;
                green = 255 - green;
                blue = 255 - blue;

                int newRGB = (alpha << 24) | (red << 16) | (green << 8) | blue;
                invertedImage.setRGB(x, y, newRGB);
            }
        }

        return invertedImage;
    }
}

class GrayscaleEffect implements ImageEffect {
    @Override
    public synchronized BufferedImage applyEffect(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage grayscaleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                int alpha = (rgb >> 24) & 0xFF;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                int gray = (int) (0.299 * red + 0.587 * green + 0.114 * blue);

                int newRGB = (alpha << 24) | (gray << 16) | (gray << 8) | gray;

                grayscaleImage.setRGB(x, y, newRGB);
            }
        }

        return grayscaleImage;
    }
}

public class Igor    {

    private JFrame frame;
    private JLabel imageLabel;
    private JButton loadButton;
    private JButton blurButton;
    private JButton invertColorButton;
    private JButton grayscaleButton;
    private JButton cancelButton;

    private volatile boolean isProcessingCancelled = false;
    private AtomicReference<BufferedImage> imageReference = new AtomicReference<>();
    private SwingWorker<Void, Void> currentWorker;

    public Igor() {
        frame = new JFrame("Image Processing App");
        frame.setLayout(new BorderLayout());
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel topPanel = new JPanel(new FlowLayout());
        frame.add(topPanel, BorderLayout.NORTH);

        imageLabel = new JLabel();
        frame.add(imageLabel, BorderLayout.CENTER);

        loadButton = new JButton("Load Image");
        loadButton.addActionListener(e -> loadImageInBackground());
        topPanel.add(loadButton);

        blurButton = new JButton("Blur");
        blurButton.addActionListener(e -> applyEffectInBackground(new BlurEffect(imageReference.get())));
        topPanel.add(blurButton);

        invertColorButton = new JButton("Invert Color");
        invertColorButton.addActionListener(e -> applyEffectInBackground(new InvertColorEffect()));
        topPanel.add(invertColorButton);

        grayscaleButton = new JButton("Grayscale");
        grayscaleButton.addActionListener(e -> applyEffectInBackground(new GrayscaleEffect()));
        topPanel.add(grayscaleButton);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancelProcessing());
        topPanel.add(cancelButton);

        frame.setVisible(true);
    }

    private synchronized void loadImageInBackground() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(frame);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    try {
                        BufferedImage image = ImageIO.read(selectedFile);
                        imageReference.set(image);
                        displayImage(image);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };

        worker.execute();
    }

    private void applyEffectInBackground(final ImageEffect effect) {
        isProcessingCancelled = false;

        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        currentWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                BufferedImage currentImage = imageReference.get();
                if (currentImage != null) {
                    synchronized (this) {
                        BufferedImage newImage = effect.applyEffect(currentImage);
                        if (!isProcessingCancelled) {
                            imageReference.set(newImage);
                            displayImage(newImage);
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    isProcessingCancelled = false;
                }
            }
        };

        currentWorker.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                if (currentWorker.isCancelled()) {
                    isProcessingCancelled = false;
                }
            }
        });

        currentWorker.execute();
    }

    private void cancelProcessing() {
        isProcessingCancelled = true;
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
    }

    private void displayImage(BufferedImage image) {
        imageLabel.setIcon(new ImageIcon(image));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Igor());
    }
}
