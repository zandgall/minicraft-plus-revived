package minicraft.core;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDebugListener;
import com.jogamp.opengl.GLDebugMessage;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import minicraft.core.io.FileHandler;
import minicraft.core.io.Localization;
import minicraft.util.Logging;
import minicraft.util.TinylogLoggingProvider;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.tinylog.provider.ProviderRegistry;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.jogamp.opengl.GL3.*;
import static com.jogamp.opengl.GL4.*;

public class Initializer extends Game {
	private Initializer() {
	}

	/**
	 * Reference to actual frame, also it may be null.
	 */
	static JFrame frame;
	private static GLWindow window;
	private static Animator animator;
	private static GLListener glListener;
	private static GLDebugger glDebugger;
	static int vao, vbo, ebo, shader;
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

		while (running) {
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
		}
	}

	// Creates and displays the JFrame window that the game appears in.
	static void createAndDisplayFrame() {
		GLProfile profile = GLProfile.get(GLProfile.GL4);
		GLCapabilities capabilities = new GLCapabilities(profile);

		window = GLWindow.create(capabilities);

		window.setTitle(NAME);
		window.setSize(Renderer.getWindowSize().width, Renderer.getWindowSize().height);

		glListener = new GLListener();
		glDebugger = new GLDebugger();
		window.addGLEventListener(glListener);
		window.addKeyListener(glListener);

		window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);

		if(window == null) {
			System.out.println("Well fuck");
		}

		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDestroyed(com.jogamp.newt.event.WindowEvent windowEvent) {
				animator.stop();
				running = false;
			}
		});

		Renderer.canvas.setMinimumSize(new java.awt.Dimension(1, 1));
		Renderer.canvas.setPreferredSize(Renderer.getWindowSize());
		Renderer.canvas.setBackground(Color.BLACK);
		JFrame frame = Initializer.frame = new JFrame(NAME);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout()); // Sets the layout of the window
		frame.add(Renderer.canvas, BorderLayout.CENTER); // Adds the game (which is a canvas) to the center of the screen.
		frame.pack(); // Squishes everything into the preferredSize.

		try {
			BufferedImage logo = ImageIO.read(Game.class.getResourceAsStream("/resources/logo.png")); // Load the window logo
			frame.setIconImage(logo);
		} catch (IOException e) {
			CrashHandler.errorHandle(e);
		}

		frame.setLocationRelativeTo(null); // The window will pop up in the middle of the screen when launched.

		frame.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				float w = frame.getWidth() - frame.getInsets().left - frame.getInsets().right;
				float h = frame.getHeight() - frame.getInsets().top - frame.getInsets().bottom;
				Renderer.SCALE = Math.min(w / Renderer.WIDTH, h / Renderer.HEIGHT);
			}
		});

		frame.addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent e) {
			}

			public void windowDeactivated(WindowEvent e) {
			}

			public void windowIconified(WindowEvent e) {
			}

			public void windowDeiconified(WindowEvent e) {
			}

			public void windowOpened(WindowEvent e) {
			}

			public void windowClosed(WindowEvent e) {
				Logging.GAMEHANDLER.debug("Window closed");
			}

			public void windowClosing(WindowEvent e) {
				Logging.GAMEHANDLER.info("Window closing");
				quit();
			}
		});
	}

	/**
	 * Launching the main window.
	 */
	static void launchWindow() {
		frame.setVisible(true);
		frame.requestFocus();
		Renderer.canvas.requestFocus();

		window.setVisible(true);
		animator = new Animator(window);
		animator.start();
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

	static class GLListener implements GLEventListener, KeyListener {

		@Override
		public void init(GLAutoDrawable glAutoDrawable) {
			// Invalid operation somewhere in here
			GL4 gl = glAutoDrawable.getGL().getGL4();

			window.getContext().addGLDebugListener(glDebugger);
			gl.glDebugMessageControl(
				GL_DONT_CARE,
				GL_DONT_CARE,
				GL_DONT_CARE,
				0,
				null,
				false);

			gl.glDebugMessageControl(
				GL_DONT_CARE,
				GL_DONT_CARE,
				GL_DEBUG_SEVERITY_HIGH,
				0,
				null,
				true);

			gl.glDebugMessageControl(
				GL_DONT_CARE,
				GL_DONT_CARE,
				GL_DEBUG_SEVERITY_MEDIUM,
				0,
				null,
				true);

			int ERR = gl.glGetError();

			// Square VAO definitions
			FloatBuffer vertices = GLBuffers.newDirectFloatBuffer(new float[] {
				1.0f,  1.0f, 0.0f, 1, 1, 1, 0, 0, -1, // top right
				1.0f, -1.0f, 0.0f, 1, 1, 0, 0, 0, -1, // bottom right
				-1.0f, -1.0f, 0.0f, 1, 0, 0, 0, 0, -1, // bottom left
				-1.0f,  1.0f, 0.0f, 1, 0, 1, 0, 0, -1  // top left
			});
			IntBuffer indices = GLBuffers.newDirectIntBuffer(new int[]{
				0, 1, 3,   // first triangle
				1, 2, 3    // second triangle
			});

			IntBuffer buff = GLBuffers.newDirectIntBuffer(new int[] {vao});
			vao = -1;
			gl.glGenVertexArrays(1, buff);
			vao = buff.get(0);
			gl.glBindVertexArray(vao);

			ERR = gl.glGetError();

			vbo = -1;
			ebo = -1;
			buff = GLBuffers.newDirectIntBuffer(new int[] {vbo, ebo});
			gl.glGenBuffers(2, buff);
			vbo = buff.get(0);
			ebo = buff.get(1);

			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo);
			gl.glBufferStorage(GL_ARRAY_BUFFER, vertices.capacity()*Float.BYTES, vertices, 0);
			gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
			gl.glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, indices.capacity()*Integer.BYTES, indices, 0);

			ERR = gl.glGetError();

			gl.glEnableVertexAttribArray(0);
			gl.glVertexAttribPointer(0, 4, GL_FLOAT, false, 9*Float.BYTES, 0);
			gl.glEnableVertexAttribArray(1);
			gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 9*Float.BYTES, 4*Float.BYTES);
			gl.glEnableVertexAttribArray(2);
			gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 9*Float.BYTES, 6*Float.BYTES);

			ERR = gl.glGetError();

			shader = gl.glCreateProgram();
			ERR = gl.glGetError();
			int vs = gl.glCreateShader(GL_VERTEX_SHADER);
			ERR = gl.glGetError();
			gl.glShaderSource(vs, 1, new String[] {"""
#version 330
in vec4 in_position;
in vec2 in_uv;
in vec3 in_normal;

out vec2 uv;
uniform mat4 screenspace, transform;

void main() {
	gl_Position = screenspace * transform * in_position;
	//	gl_Position.w = 1;
	//	gl_Position.z = 0;
	uv = in_uv;
}"""}, null);
			gl.glCompileShader(vs);
			ERR = gl.glGetError();
			int[] result = new int[] {0};
			gl.glGetShaderiv(vs, GL_COMPILE_STATUS, result, 0);
			ERR = gl.glGetError();
			if(result[0] == GL_FALSE) {
				gl.glGetShaderiv(vs, GL_INFO_LOG_LENGTH, result, 0);
				byte[] message = new byte[result[0]];
				gl.glGetShaderInfoLog(vs, result[0], result, 0, message, 0);
				System.out.println(new String(message, StandardCharsets.UTF_8));
				gl.glDeleteShader(vs);
			}
			ERR = gl.glGetError();
			int fs = gl.glCreateShader(GL_FRAGMENT_SHADER);
			gl.glShaderSource(fs, 1, new String[] {"""
#version 330
in vec2 uv;
uniform sampler2D texture;
out vec4 out_color;
void main() {
	out_color = texture2D(texture, uv);
}"""}, null);
			gl.glCompileShader(fs);
			ERR = gl.glGetError();
			gl.glGetShaderiv(fs, GL_COMPILE_STATUS, result, 0);
			ERR = gl.glGetError();
			if(result[0] == GL_FALSE) {
				gl.glGetShaderiv(fs, GL_INFO_LOG_LENGTH, result, 0);
				byte[] message = new byte[result[0]];
				gl.glGetShaderInfoLog(fs, result[0], result, 0, message, 0);
				System.out.println(Arrays.toString(message));
				gl.glDeleteShader(fs);
			}
			ERR = gl.glGetError();
			gl.glAttachShader(shader, vs);
			gl.glAttachShader(shader, fs);
			gl.glLinkProgram(shader);
			gl.glValidateProgram(shader);
			ERR = gl.glGetError();

			gl.glDeleteShader(vs);
			gl.glDeleteShader(fs);
			ERR = gl.glGetError();
		}

		@Override
		public void dispose(GLAutoDrawable glAutoDrawable) {

		}

		@Override
		public void display(GLAutoDrawable glAutoDrawable) {
			GL4 gl = glAutoDrawable.getGL().getGL4();

			int w = glAutoDrawable.getSurfaceWidth();
			int h = glAutoDrawable.getSurfaceHeight();
			gl.glViewport(0, 0, w, h);
			gl.glClearColor(0.5f, 0.8f, 1.0f, 1.0f);
			gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			gl.glDisable(GL_DEPTH_TEST);
			int ERR = gl.glGetError();

			gl.glUseProgram(shader);
			ERR = gl.glGetError();

			FloatBuffer buff = Buffers.newDirectFloatBuffer(16);
			new Matrix4f().ortho(0, w, h, 0, -1, 1).get(buff);
			for(int m = 0; m < 16; m++ ) {
				float f = buff.get(m);
				System.nanoTime();
			}
			int i = gl.glGetUniformLocation(shader, "screenspace");
			gl.glUniformMatrix4fv(i, 1, false, buff);
			ERR = gl.glGetError();
			new Matrix4f().identity().get(buff);
			for(int m = 0; m < 16; m++ ) {
				float f = buff.get(m);
				System.nanoTime();
			}
			i = gl.glGetUniformLocation(shader, "transform");
			ERR = gl.glGetError();
			gl.glUniformMatrix4fv(i, 1, false, buff);
			ERR = gl.glGetError();
			gl.glBindVertexArray(vao);
			ERR = gl.glGetError();
			gl.glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
			ERR = gl.glGetError();
		}

		@Override
		public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {

		}

		@Override
		public void keyPressed(KeyEvent keyEvent) {

		}

		@Override
		public void keyReleased(KeyEvent keyEvent) {

		}
	}

	static class GLDebugger implements GLDebugListener {

		@Override
		public void messageSent(GLDebugMessage glDebugMessage) {

		}
	}
}
