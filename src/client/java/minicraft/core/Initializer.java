package minicraft.core;

import minicraft.core.io.FileHandler;
import minicraft.core.io.Localization;
import minicraft.core.io.Settings;
import minicraft.util.Logging;
import minicraft.util.TinylogLoggingProvider;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.tinylog.provider.ProviderRegistry;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static minicraft.core.Renderer.WINDOW_SIZE;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Initializer extends Game {
	private Initializer() {
	}

	static int fra, tik; // These store the number of frames and ticks in the previous second; used for fps, at least.

	public static int getCurFps() {
		return fra;
	}

	static void parseArgs(String[] args) {
		// Parses command line arguments
		@Nullable
		String saveDir = null;
		boolean enableHardwareAcceleration = true; // Likely not a useful flag anymore
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("--savedir") && i + 1 < args.length) {
				i++;
				saveDir = args[i];
			} else if (args[i].equalsIgnoreCase("--fullscreen")) {
				Updater.FULLSCREEN = true;
			} else if (args[i].equalsIgnoreCase("--debug-log-time")) {
				Logging.logTime = true;
			} else if (args[i].equalsIgnoreCase("--debug-log-thread")) {
				Logging.logThread = true;
			} else if (args[i].equalsIgnoreCase("--debug-log-trace")) {
				Logging.logTrace = true;
			} else if (args[i].equalsIgnoreCase("--debug-level")) {
				Logging.logLevel = true;
			} else if (args[i].equalsIgnoreCase("--debug-filelog-full")) {
				Logging.fileLogFull = true;
			} else if (args[i].equalsIgnoreCase("--debug-locale")) {
				Localization.isDebugLocaleEnabled = true;
			} else if (args[i].equalsIgnoreCase("--debug-unloc-tracing")) {
				Localization.unlocalizedStringTracing = true;
			} else if (args[i].equalsIgnoreCase("--no-hardware-acceleration")) {
				enableHardwareAcceleration = false;
			}
		}
		((TinylogLoggingProvider) ProviderRegistry.getLoggingProvider()).init();
		// Reference: https://stackoverflow.com/a/13832805
		if (enableHardwareAcceleration) System.setProperty("sun.java2d.opengl", "true");

		FileHandler.determineGameDir(saveDir);
	}

	/**
	 * This is the main loop that runs the game. It:
	 * -keeps track of the amount of time that has passed
	 * -fires the ticks needed to run the game
	 * -fires the command to render out the screen.
	 */
	static void run() {
		long lastTick = System.nanoTime();
		long lastRender = System.nanoTime();
		double unprocessed = 0;
		int frames = 0;
		int ticks = 0;
		long lastTimer1 = System.currentTimeMillis();

		// enable transparency blending and disable depth testing
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL_DEPTH_TEST);


		// Main loop!
		while (!glfwWindowShouldClose(window)) {
			long now = System.nanoTime();
			double nsPerTick = 1E9D / Updater.normSpeed; // Nanosecs per sec divided by ticks per sec = nanosecs per tick
			if (currentDisplay == null) nsPerTick /= Updater.gamespeed;
			unprocessed += (now - lastTick) / nsPerTick; // Figures out the unprocessed time between now and lastTick.
			lastTick = now;
			while (unprocessed >= 1) { // If there is unprocessed time, then tick.
				ticks++;
				Updater.tick(); // Calls the tick method (in which it calls the other tick methods throughout the code.
				unprocessed--;
			}

			now = System.nanoTime();
			if (now >= lastRender + 1E9D / MAX_FPS / 1.01) {
				frames++;
				lastRender = now;
				Renderer.render();
			}

			try {
				long curNano = System.nanoTime();
				long untilNextTick = (long) (lastTick + nsPerTick - curNano);
				long untilNextFrame = (long) (lastRender + 1E9D / MAX_FPS - curNano);
				if (untilNextTick > 1E3 && untilNextFrame > 1E3) {
					double timeToWait = Math.min(untilNextTick, untilNextFrame) / 1.2; // in nanosecond
					//noinspection BusyWait
					Thread.sleep((long) Math.floor(timeToWait / 1E6), (int) ((timeToWait - Math.floor(timeToWait)) % 1E6));
				}
			} catch (InterruptedException ignored) {
			}

			if (System.currentTimeMillis() - lastTimer1 > 1000) { //updates every 1 second
				long interval = System.currentTimeMillis() - lastTimer1;
				lastTimer1 = System.currentTimeMillis(); // Adds a second to the timer

				fra = (int) Math.round(frames * 1000D / interval); // Saves total frames in last second
				tik = (int) Math.round(ticks * 1000D / interval); // Saves total ticks in last second
				frames = 0; // Resets frames
				ticks = 0; // Resets ticks; ie, frames and ticks only are per second
			}

			// Poll for input events
			glfwPollEvents();
		}

		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	static void initSquareVAO() {
		// Create an object to store a square to be used for rendering
		vao = glGenVertexArrays();
		glBindVertexArray(vao);

		// Generate a buffer to store the 4 corners of the square
		// has 4 vertices, in the format: pos(x y z w), texture(u v), normal(x y z)
		int vbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, new float[] {
			1.0f, 1.0f, 0.0f, 1, 1, 1, 0, 0, -1, // top right
			1.0f, -1.0f, 0.0f, 1, 1, 0, 0, 0, -1, // bottom right
			-1.0f, -1.0f, 0.0f, 1, 0, 0, 0, 0, -1, // bottom left
			-1.0f,  1.0f, 0.0f, 1, 0, 1, 0, 0, -1  // top left
		}, GL_STATIC_DRAW);
		// Create a buffer to store the order we draw the square in.
		// Defines two triangles, with a shared diagonal from the top left to the bottom right |\|
		int ebo = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, new int[] {
			0, 1, 3, // top right triangle
			1, 2, 3, // bottom left triangle
		}, GL_STATIC_DRAW);

		// Enable vertice attributes, i.e. tell opengl that our vertices are in pos(x y z w), texture(u v), normal(x y z) format
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 4, GL_FLOAT, false, 9*Float.BYTES, 0);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 9*Float.BYTES, 4*Float.BYTES);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 3, GL_FLOAT, false, 9*Float.BYTES, 6*Float.BYTES);
	}

	// Initializes a GLFW window, hooks up input events,
	static void init() {
		GLFWErrorCallback.createPrint(System.err).set();
		if(!glfwInit())
			Logging.GAMEHANDLER.error("Could not initialize GLFW", new IllegalStateException("Unable to initialize GLFW"));

		glfwDefaultWindowHints();
		// Using OpenGL 3.3 for now
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		window = glfwCreateWindow(Renderer.getWindowSize().width, Renderer.getWindowSize().height, NAME, NULL, NULL);
		if(window == NULL)
			Logging.GAMEHANDLER.error("Could not create window", new RuntimeException("Unable to create window"));

		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> Game.input.glfwKeyCallback(window, key, scancode, action, mods));

		glfwSetCharCallback(window, (window, key) -> Game.input.glfwCharCallback(window, key));

		glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
			WINDOW_SIZE.setSize(width, height);
		});

		glfwSetWindowFocusCallback(window, (window, focus) -> {
			focused = focus;
		});

		// Put window in the center of the screen
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1);
			IntBuffer pHeight = stack.mallocInt(1);
			glfwGetWindowSize(window, pWidth, pHeight);
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
			glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0))/2, (vidmode.height() - pHeight.get(0))/2);
		}

		// Use the window (required to start using GPU resources)
		glfwMakeContextCurrent(window);

		// Load logo, convert to GLFWImage, and set the icon
		try {
			BufferedImage logo = ImageIO.read(Game.class.getResourceAsStream("/resources/logo.png")); // Load the window logo
			byte[] pixels = ((DataBufferByte)logo.getRaster().getDataBuffer()).getData();
			ByteBuffer buffer = BufferUtils.createByteBuffer(pixels.length);
			buffer.order(ByteOrder.nativeOrder());
			// pixels is in ABGR order, when we need RGBA order
			for(int i = 0; i < pixels.length; i+=4) {
				buffer.put(pixels[i + 3]);
				buffer.put(pixels[i + 2]);
				buffer.put(pixels[i + 1]);
				buffer.put(pixels[i + 0]);
			}

			buffer.flip();
			GLFWImage.Buffer gb = GLFWImage.create(1);
			GLFWImage glfwImage = GLFWImage.create().set(logo.getWidth(), logo.getHeight(), buffer);
			gb.put(0, glfwImage);
			glfwSetWindowIcon(window, gb);
		} catch (IOException e) {
			CrashHandler.errorHandle(e);
		}

		glfwSwapInterval((boolean) Settings.get("vsync") ? 1 : 0);

		glfwShowWindow(window);

		GL.createCapabilities();

		initSquareVAO();
	}

	/**
	 * Provides a String representation of the provided Throwable's stack trace
	 * that is extracted via PrintStream.
	 * @param throwable Throwable/Exception from which stack trace is to be
	 * 	extracted.
	 * @return String with provided Throwable's stack trace.
	 */
	public static String getExceptionTrace(final Throwable throwable) {
		final java.io.ByteArrayOutputStream bytestream = new java.io.ByteArrayOutputStream();
		final java.io.PrintStream printStream = new java.io.PrintStream(bytestream);
		throwable.printStackTrace(printStream);
		String exceptionStr;
		try {
			exceptionStr = bytestream.toString("UTF-8");
		} catch (Exception ex) {
			exceptionStr = "Unavailable";
		}
		return exceptionStr;
	}
}
