import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Main {
	private static final int WINDOW_WIDTH = 370;
	private static final int WINDOW_HEIGHT = 350;
	private static final int SWITCH_DELAY_MS = 250;
	private static final int MOVE_DELAY_MS = 16;
	private static final int MOVE_STEP = 3;
	private static final int TICKER_SPEED_PIXELS = 5;
	private static final int TICKER_WIDTH = 2000;
	private static final int TICKER_HEIGHT = 50;
	private static final String APP_ICON = "Zlogo.ico";
	private static final String FIRST_IMAGE = "MainPic1.png";
	private static final String SECOND_IMAGE = "MainPic2.png";
	private static final String TOP_TICKER_IMAGE = "ZtheBunTopTiker.png";
	private static final String BACKGROUND_AUDIO = "Background Audio.wav";
	private static final Random RANDOM = new Random();
	private static final Set<Integer> PRESSED_KEYS = new HashSet<>();
	private static Clip audioClip;
	private static int closeCount;
	private static boolean tickerShown;

	public static void main(String[] args) {
		installExitShortcut();
		SwingUtilities.invokeLater(Main::createWindow);
	}

	private static void installExitShortcut() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
			if (event.getID() == KeyEvent.KEY_PRESSED) {
				PRESSED_KEYS.add(event.getKeyCode());
			}
			if (event.getID() == KeyEvent.KEY_RELEASED) {
				PRESSED_KEYS.remove(event.getKeyCode());
			}

			boolean exitShortcutPressed = PRESSED_KEYS.contains(KeyEvent.VK_CONTROL)
					&& PRESSED_KEYS.contains(KeyEvent.VK_ALT)
					&& PRESSED_KEYS.contains(KeyEvent.VK_Z)
					&& PRESSED_KEYS.contains(KeyEvent.VK_T)
					&& PRESSED_KEYS.contains(KeyEvent.VK_B);
			if (exitShortcutPressed) {
				if (audioClip != null) {
					audioClip.close();
				}
				System.exit(0);
			}
			return false;
		});
	}

	private static void createWindow() {
		createWindow(false);
	}

	private static void createWindow(boolean randomLocation) {
		Image firstImage = loadImage(FIRST_IMAGE);
		Image secondImage = loadImage(SECOND_IMAGE);
		ImagePanel imagePanel = new ImagePanel(firstImage, secondImage);

		JDialog window = new JDialog((java.awt.Frame) null, "YOU ARE AN IDIOT!", false);
		window.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		window.setAlwaysOnTop(true);
		window.setIconImage(loadIcon(APP_ICON));
		window.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		window.setResizable(false);
		window.add(imagePanel);
		if (randomLocation) {
			setRandomLocation(window);
		} else {
			window.setLocationRelativeTo(null);
		}
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent event) {
				closeCount++;
				window.dispose();
				if (closeCount == 2) {
					showTicker();
				}
				createWindow(true);
				createWindow(true);
			}
		});
		window.setVisible(true);
		playBackgroundAudio();

		new Timer(SWITCH_DELAY_MS, event -> {
			imagePanel.showSecondImage = !imagePanel.showSecondImage;
			imagePanel.repaint();
		}).start();

		Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int[] velocity = { MOVE_STEP, MOVE_STEP };
		new Timer(MOVE_DELAY_MS, event -> {
			int nextX = window.getX() + velocity[0];
			int nextY = window.getY() + velocity[1];
			int minX = screenBounds.x;
			int minY = screenBounds.y;
			int maxX = screenBounds.x + screenBounds.width - window.getWidth();
			int maxY = screenBounds.y + screenBounds.height - window.getHeight();

			if (nextX <= minX || nextX >= maxX) {
				velocity[0] = -velocity[0];
				nextX = Math.max(minX, Math.min(nextX, maxX));
			}
			if (nextY <= minY || nextY >= maxY) {
				velocity[1] = -velocity[1];
				nextY = Math.max(minY, Math.min(nextY, maxY));
			}
			window.setLocation(nextX, nextY);
		}).start();
	}

	private static void showTicker() {
		if (tickerShown) {
			return;
		}
		tickerShown = true;

		JWindow tickerWindow = new JWindow();
		tickerWindow.setAlwaysOnTop(true);
		tickerWindow.setSize(TICKER_WIDTH, TICKER_HEIGHT);
		Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int x = screenBounds.x + Math.max(0, (screenBounds.width - TICKER_WIDTH) / 2);
		int y = screenBounds.y + screenBounds.height - TICKER_HEIGHT;
		tickerWindow.setLocation(x, y);
		tickerWindow.add(new TickerPanel());
		tickerWindow.setVisible(true);

		JWindow topTickerWindow = new JWindow();
		topTickerWindow.setAlwaysOnTop(true);
		topTickerWindow.setSize(TICKER_WIDTH, TICKER_HEIGHT);
		topTickerWindow.setLocation(x, screenBounds.y);
		topTickerWindow.add(new ImageTickerPanel(loadImage(TOP_TICKER_IMAGE)));
		topTickerWindow.setVisible(true);
	}

	private static void setRandomLocation(JDialog window) {
		Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int maxX = screenBounds.x + screenBounds.width - window.getWidth();
		int maxY = screenBounds.y + screenBounds.height - window.getHeight();
		int x = screenBounds.x + RANDOM.nextInt(Math.max(1, maxX - screenBounds.x + 1));
		int y = screenBounds.y + RANDOM.nextInt(Math.max(1, maxY - screenBounds.y + 1));
		window.setLocation(x, y);
	}

	private static void playBackgroundAudio() {
		if (audioClip != null && audioClip.isOpen()) {
			return;
		}
		try (AudioInputStream audioStream = getAudioInputStream(BACKGROUND_AUDIO)) {
			audioClip = AudioSystem.getClip();
			audioClip.open(audioStream);
			audioClip.loop(Clip.LOOP_CONTINUOUSLY);
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to play audio: " + BACKGROUND_AUDIO, exception);
		}
	}

	private static AudioInputStream getAudioInputStream(String filename) throws Exception {
		URL resource = resourceUrl(filename);
		return resource != null
				? AudioSystem.getAudioInputStream(resource)
				: AudioSystem.getAudioInputStream(new File(filename));
	}

	private static Image loadImage(String filename) {
		URL resource = resourceUrl(filename);
		if (resource != null) {
			return new ImageIcon(resource).getImage();
		}
		File imageFile = new File(filename);
		if (!imageFile.isFile()) {
			throw new IllegalStateException("Image not found: " + imageFile.getAbsolutePath());
		}
		return new ImageIcon(imageFile.getAbsolutePath()).getImage();
	}

	private static Image loadIcon(String filename) {
		try {
			byte[] bytes = loadResourceBytes(filename);
			ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
			int width = Byte.toUnsignedInt(bytes[6]);
			int height = Byte.toUnsignedInt(bytes[7]);
			int bitsPerPixel = Short.toUnsignedInt(buffer.getShort(12));
			int imageOffset = buffer.getInt(18);
			if (width == 0 || height == 0 || bitsPerPixel != 32) {
				throw new IllegalStateException("Unsupported ICO format: " + filename);
			}

			BufferedImage icon = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			int rowSize = width * 4;
			for (int y = 0; y < height; y++) {
				int sourceRow = height - 1 - y;
				for (int x = 0; x < width; x++) {
					int pixelOffset = imageOffset + sourceRow * rowSize + x * 4;
					int blue = Byte.toUnsignedInt(bytes[pixelOffset]);
					int green = Byte.toUnsignedInt(bytes[pixelOffset + 1]);
					int red = Byte.toUnsignedInt(bytes[pixelOffset + 2]);
					int alpha = Byte.toUnsignedInt(bytes[pixelOffset + 3]);
					icon.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
				}
			}
			return icon;
		} catch (IOException | IndexOutOfBoundsException exception) {
			throw new IllegalStateException("Icon not found or invalid: " + filename, exception);
		}
	}

	private static byte[] loadResourceBytes(String filename) throws IOException {
		URL resource = resourceUrl(filename);
		if (resource != null) {
			try (InputStream input = resource.openStream()) {
				return input.readAllBytes();
			}
		}
		return Files.readAllBytes(Path.of(filename));
	}

	private static URL resourceUrl(String filename) {
		return Main.class.getResource("/" + filename);
	}

	private static class ImagePanel extends JPanel {
		private final Image firstImage;
		private final Image secondImage;
		private boolean showSecondImage;

		private ImagePanel(Image firstImage, Image secondImage) {
			this.firstImage = firstImage;
			this.secondImage = secondImage;
			setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Image image = showSecondImage ? secondImage : firstImage;
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.drawImage(image, 0, 0, getWidth(), getHeight(), this);
			graphics2D.dispose();
		}
	}

	private static class TickerPanel extends JPanel {
		private static final String TICKER_TEXT = "Fuck you!     ";
		private static final Font TICKER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 26);
		private int textOffset;

		private TickerPanel() {
			setBackground(Color.BLACK);
			setForeground(Color.WHITE);
			new Timer(20, event -> {
				int textWidth = getFontMetrics(TICKER_FONT).stringWidth(TICKER_TEXT);
				if (textWidth == 0) {
					return;
				}
				textOffset = (textOffset + TICKER_SPEED_PIXELS) % textWidth;
				repaint();
			}).start();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			graphics.setColor(Color.WHITE);
			graphics.setFont(TICKER_FONT);
			int textWidth = graphics.getFontMetrics().stringWidth(TICKER_TEXT);
			int x = textOffset - textWidth;
			while (x < getWidth()) {
				graphics.drawString(TICKER_TEXT, x, 35);
				x += textWidth;
			}
		}
	}

	private static class ImageTickerPanel extends JPanel {
		private final Image tickerImage;
		private int imageOffset;

		private ImageTickerPanel(Image tickerImage) {
			this.tickerImage = tickerImage;
			setBackground(Color.BLACK);
			new Timer(20, event -> {
				int imageWidth = scaledImageWidth();
				if (imageWidth == 0) {
					return;
				}
				imageOffset = (imageOffset + TICKER_SPEED_PIXELS) % imageWidth;
				repaint();
			}).start();
		}

		private int scaledImageWidth() {
			if (tickerImage.getHeight(this) <= 0) {
				return 0;
			}
			return Math.max(1, tickerImage.getWidth(this) * getHeight() / tickerImage.getHeight(this));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			int imageWidth = scaledImageWidth();
			if (imageWidth == 0) {
				return;
			}
			int x = imageOffset - imageWidth;
			while (x < getWidth()) {
				graphics.drawImage(tickerImage, x, 0, imageWidth, getHeight(), this);
				x += imageWidth;
			}
		}
	}
}
