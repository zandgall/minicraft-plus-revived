package minicraft.core;

import minicraft.core.io.FileHandler;
import minicraft.core.io.Localization;
import minicraft.core.io.Shader;
import minicraft.util.Logging;
import minicraft.util.TinylogLoggingProvider;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.tinylog.provider.ProviderRegistry;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import javax.swing.JFrame;

import java.nio.*;

import static minicraft.core.Renderer.WINDOW_SIZE;
import static minicraft.core.Renderer.screen;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Initializer extends Game {
	private Initializer() {
	}

	/**
	 * LWJGL/GLFW Stuff
	 */
	private static GLFWKeyCallback keyCallback;
	private static GLFWCharCallback charCallback;

	/**
	 * Reference to actual frame, also it may be null.
	 */
	static JFrame frame;
	static int fra, tik; // These store the number of frames and ticks in the previous second; used for fps, at least.

	public static JFrame getFrame() {
		return frame;
	}

	public static int getCurFps() {
		return fra;
	}

	static void parseArgs(String[] args) {
		// Parses command line arguments
		@Nullable
		String saveDir = null;
		boolean enableHardwareAcceleration = true;
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

		vao = glGenVertexArrays();
		glBindVertexArray(vao);

		int vbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, new float[] {
			1.0f, 1.0f, 0.0f, 1, 1, 1, 0, 0, -1, // top right
			1.0f, -1.0f, 0.0f, 1, 1, 0, 0, 0, -1, // bottom right
			-1.0f, -1.0f, 0.0f, 1, 0, 0, 0, 0, -1, // bottom left
			-1.0f,  1.0f, 0.0f, 1, 0, 1, 0, 0, -1  // top left
		}, GL_STATIC_DRAW);
		int ebo = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, new int[] {
			0, 1, 3, 1, 2, 3
		}, GL_STATIC_DRAW);

		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 4, GL_FLOAT, false, 9*Float.BYTES, 0);
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 9*Float.BYTES, 4*Float.BYTES);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(2, 3, GL_FLOAT, false, 9*Float.BYTES, 6*Float.BYTES);

		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		glDisable(GL_DEPTH_TEST);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

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

				glBindFramebuffer(GL_FRAMEBUFFER, 0);
				glViewport(0, 0, WINDOW_SIZE.width, WINDOW_SIZE.height);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

				Shader.postprocess.use();
				Shader.postprocess.setTransform(
					new Matrix4f().identity()
						.translate(Renderer.WIDTH/2.f, Renderer.HEIGHT/2.f,0)
						.scale(Renderer.WIDTH/2.f, Renderer.HEIGHT/2.f, 1)
				);
				Shader.postprocess.setTexture(screen.getTexture());
				glBindVertexArray(vao);
				glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

				glfwSwapBuffers(window);
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

			glfwPollEvents();
		}

		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	static void init() {
		GLFWErrorCallback.createPrint(System.err).set();
		if(!glfwInit())
			Logging.GAMEHANDLER.error("Could not initialize GLFW", new IllegalStateException("Unable to initialize GLFW"));

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		window = glfwCreateWindow(Renderer.getWindowSize().width, Renderer.getWindowSize().height, NAME, NULL, NULL);
		if(window == NULL)
			Logging.GAMEHANDLER.error("Could not create window", new RuntimeException("Unable to create window"));

		glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				Game.input.glfwKeyCallback(window, key, scancode, action, mods);
			}
		});

		glfwSetCharCallback(window, charCallback = new GLFWCharCallback() {
			@Override
			public void invoke(long window, int key) {
				Game.input.glfwCharCallback(window, key);
			}
		});

		glfwSetFramebufferSizeCallback(window, (window, width, height) -> WINDOW_SIZE.setSize(width, height));

		glfwSetWindowFocusCallback(window, (window, focus) -> {
			focused = focus;
		});

		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1);
			IntBuffer pHeight = stack.mallocInt(1);
			glfwGetWindowSize(window, pWidth, pHeight);
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
			glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0))/2, (vidmode.height() - pHeight.get(0))/2);
		}

		glfwMakeContextCurrent(window);

//		glfwSwapInterval(1);

		glfwShowWindow(window);

		GL.createCapabilities();
	}

	/**
	 * Launching the main window.
	 */
	static void launchWindow() {
//		frame.setVisible(true);
//		frame.requestFocus();
//		Renderer.canvas.requestFocus();
	}

	/**
	 * Provides a String representation of the provided Throwable's stack trace
	 * that is extracted via PrintStream.
	 *
	 * @param throwable Throwable/Exception from which stack trace is to be
	 *                  extracted.
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
