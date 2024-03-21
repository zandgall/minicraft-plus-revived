package minicraft.core;

import minicraft.core.io.FileHandler;
import minicraft.core.io.Localization;
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

		defaultShader = glCreateProgram();
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, """
#version 330 core

layout (location = 0) in vec4 in_position;
layout (location = 1) in vec2 in_uv;
layout (location = 2) in vec3 in_normal;

// Only using UV
out vec2 uv;

uniform mat4 screenspace, view, transform;

void main() {
	gl_Position = screenspace * view * transform * in_position;
	uv = in_uv;
}""");
		glCompileShader(vs);
		int[] result = new int[]{0};
		glGetShaderiv(vs, GL_COMPILE_STATUS, result);
		if(result[0] == GL_FALSE) {
			System.out.println(glGetShaderInfoLog(vs));
			glDeleteShader(vs);
		}
		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, """
#version 330 core

in vec2 uv;

uniform sampler2D texture;
uniform vec2 texOffset;
uniform bvec2 mirror;
uniform bool fullbright;
uniform bool useWhiteTint;
uniform vec3 whiteTint;
uniform bool useColor;
uniform vec3 color;

out vec4 out_color;

void main() {
	vec2 nUV = uv;
	if(mirror.x)
		nUV.x = 1 - nUV.x;
	if(mirror.y)
		nUV.y = 1 - nUV.y;
	nUV += texOffset;
	nUV *= 8;
	nUV /= textureSize(texture, 0);
	out_color = texture2D(texture, nUV);
	if(useWhiteTint && out_color.xyz == vec3(1))
		out_color.xyz = whiteTint;
	else if(fullbright)
		out_color.xyz = vec3(1);
	else if(useColor)
		out_color.xyz = color;
}""");
		glCompileShader(fs);
		glGetShaderiv(fs, GL_COMPILE_STATUS, result);
		if(result[0] == GL_FALSE) {
			System.out.println(glGetShaderInfoLog(fs));
			glDeleteShader(fs);
		}

		glAttachShader(defaultShader, vs);
		glAttachShader(defaultShader, fs);
		glLinkProgram(defaultShader);
		glValidateProgram(defaultShader);

		glDeleteShader(fs);

		overlayShader = glCreateProgram();
		fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, """
#version 330 core

in vec2 uv;

uniform sampler2D texture;
uniform sampler2D overlay;
uniform sampler2D dither;
uniform float tintFactor;
uniform int currentLevel;
uniform ivec2 adjust;

out vec4 out_color;

void main() {
//	out_color = vec4(1,0,0,0.5);
	vec2 nUV = uv * textureSize(texture, 0) / textureSize(dither, 0); // make pixel sizes consistent
	nUV += adjust / textureSize(dither, 0);
	out_color = texture2D(texture, uv);
	float overlayAmount = texture2D(overlay, uv).x;
	float ditherSample = texture2D(dither, nUV).x;
	if (overlayAmount <= ditherSample) {
		if (currentLevel < 3) {
			out_color = vec4(0,0,0,1);
		} else {
			out_color.xyz += vec3(tintFactor);
		}
	}
	out_color.xyz += vec3(20.f/256.f);
	//	out_color.w = 0;
}""");
		glCompileShader(fs);
		glGetShaderiv(fs, GL_COMPILE_STATUS, result);
		if(result[0] == GL_FALSE) {
			System.out.println(glGetShaderInfoLog(fs));
			glDeleteShader(fs);
		}

		glAttachShader(overlayShader, vs);
		glAttachShader(overlayShader, fs);
		glLinkProgram(overlayShader);
		glValidateProgram(overlayShader);

		glDeleteShader(fs);

		postprocessShader = glCreateProgram();
		fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, """
#version 330 core

in vec2 uv;

uniform sampler2D texture;

out vec4 out_color;

void main() {
	out_color = texture2D(texture, uv);
}""");
		glCompileShader(fs);
		glGetShaderiv(fs, GL_COMPILE_STATUS, result);
		if(result[0] == GL_FALSE) {
			System.out.println(glGetShaderInfoLog(fs));
			glDeleteShader(fs);
		}

		glAttachShader(postprocessShader, vs);
		glAttachShader(postprocessShader, fs);
		glLinkProgram(postprocessShader);
		glValidateProgram(postprocessShader);

		glDeleteShader(fs);

		lightingShader = glCreateProgram();
		fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, """
#version 330 core

in vec2 uv;

uniform sampler2D texture;
uniform ivec4 rectangle;
uniform ivec2 screenSize;
uniform int r;

out vec4 out_color;

void main() {
	vec2 p = (uv-vec2(0.5))*(rectangle.zw-rectangle.xy);
	float dist = p.x*p.x + p.y*p.y;
	float br = 255 - dist * 255 / (r * r);
	vec2 pUV = (uv * (rectangle.zw-rectangle.xy) + rectangle.xy) / screenSize;
	if(texture2D(texture, pUV).x >= br)
		discard;
	out_color = vec4(br/255.f, 0, 0, 1);
}""");
		glCompileShader(fs);
		glGetShaderiv(fs, GL_COMPILE_STATUS, result);
		if(result[0] == GL_FALSE) {
			System.out.println(glGetShaderInfoLog(fs));
			glDeleteShader(fs);
		}

		glAttachShader(lightingShader, vs);
		glAttachShader(lightingShader, fs);
		glLinkProgram(lightingShader);
		glValidateProgram(lightingShader);

		glDeleteShader(fs);

		glDeleteShader(vs);

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
				try(MemoryStack stack = MemoryStack.stackPush()) {
					glUseProgram(defaultShader);
					FloatBuffer sp = new Matrix4f().ortho(0, Renderer.WIDTH, Renderer.HEIGHT, 0, -1, 1)
						.get(stack.mallocFloat(16));
					glUniformMatrix4fv(glGetUniformLocation(defaultShader, "screenspace"), false, sp);
					FloatBuffer vt = new Matrix4f().identity().get(stack.mallocFloat(16));
					glUniformMatrix4fv(glGetUniformLocation(defaultShader, "view"), false, vt);
					glUniform1i(glGetUniformLocation(defaultShader, "texture"), 0);
					glActiveTexture(GL_TEXTURE0);
					glBindVertexArray(vao);
				}
				Renderer.render();
				try(MemoryStack stack = MemoryStack.stackPush()) {
					glBindFramebuffer(GL_FRAMEBUFFER, 0);
					glViewport(0, 0, WINDOW_SIZE.width, WINDOW_SIZE.height);
					glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
					glUseProgram(postprocessShader);
					FloatBuffer sp = new Matrix4f().ortho(0, Renderer.WIDTH, 0, Renderer.HEIGHT,-1, 1)
						.get(stack.mallocFloat(16));
					glUniformMatrix4fv(glGetUniformLocation(defaultShader, "screenspace"), false, sp);
					FloatBuffer vt = new Matrix4f().identity().get(stack.mallocFloat(16));
					glUniformMatrix4fv(glGetUniformLocation(defaultShader, "view"), false, vt);
					FloatBuffer tf = new Matrix4f().identity().translate(Renderer.WIDTH/2.f, Renderer.HEIGHT/2.f,0).scale(Renderer.WIDTH/2.f, Renderer.HEIGHT/2.f, 1).get(stack.mallocFloat(16));
					glUniformMatrix4fv(glGetUniformLocation(defaultShader, "transform"), false, tf);
					glUniform1i(glGetUniformLocation(defaultShader, "texture"), 0);
					glActiveTexture(GL_TEXTURE0);
					glBindVertexArray(vao);
					glBindTexture(GL_TEXTURE_2D, screen.getTexture());
					glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
				}
//				glBindFramebuffer(GL_READ_FRAMEBUFFER, screen.getFramebuffer());
//				glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
//				glBlitFramebuffer(0, 0, Renderer.WIDTH, Renderer.HEIGHT, 0, 0, WINDOW_SIZE.width, WINDOW_SIZE.height, GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT, GL_NEAREST);
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

//			try(MemoryStack stack = MemoryStack.stackPush()) {
//				glUseProgram(shader);
//				FloatBuffer sp = new Matrix4f().ortho(0, Renderer.getWindowSize().width, Renderer.getWindowSize().height, 0,
//					-1, 1).get(stack.mallocFloat(16));
//				glUniformMatrix4fv(glGetUniformLocation(shader, "screenspace"), false, sp);
//				FloatBuffer tf = new Matrix4f().identity().translate(100, 100, 0).scale(100).get(stack.mallocFloat(16));
//				glUniformMatrix4fv(glGetUniformLocation(shader, "transform"), false, tf);
//			}
//
//			glBindVertexArray(vao);
//			glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
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

		glfwSwapInterval(1);

		glfwShowWindow(window);

		GL.createCapabilities();
	}

	// Creates and displays the JFrame window that the game appears in.
	static void createAndDisplayFrame() {
//		Renderer.canvas.setMinimumSize(new java.awt.Dimension(1, 1));
//		Renderer.canvas.setPreferredSize(Renderer.getWindowSize());
//		Renderer.canvas.setBackground(Color.BLACK);
//		JFrame frame = Initializer.frame = new JFrame(NAME);
//		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//		frame.setLayout(new BorderLayout()); // Sets the layout of the window
//		frame.add(Renderer.canvas, BorderLayout.CENTER); // Adds the game (which is a canvas) to the center of the screen.
//		frame.pack(); // Squishes everything into the preferredSize.

//		try {
//			BufferedImage logo = ImageIO.read(Game.class.getResourceAsStream("/resources/logo.png")); // Load the window logo
//			frame.setIconImage(logo);
//		} catch (IOException e) {
//			CrashHandler.errorHandle(e);
//		}

//		frame.setLocationRelativeTo(null); // The window will pop up in the middle of the screen when launched.

//		frame.addComponentListener(new ComponentAdapter() {
//			public void componentResized(ComponentEvent e) {
//				float w = frame.getWidth() - frame.getInsets().left - frame.getInsets().right;
//				float h = frame.getHeight() - frame.getInsets().top - frame.getInsets().bottom;
//				Renderer.SCALE = Math.min(w / Renderer.WIDTH, h / Renderer.HEIGHT);
//			}
//		});

//		frame.addWindowListener(new WindowListener() {
//			public void windowActivated(WindowEvent e) {
//			}
//
//			public void windowDeactivated(WindowEvent e) {
//			}
//
//			public void windowIconified(WindowEvent e) {
//			}
//
//			public void windowDeiconified(WindowEvent e) {
//			}
//
//			public void windowOpened(WindowEvent e) {
//			}
//
//			public void windowClosed(WindowEvent e) {
//				Logging.GAMEHANDLER.debug("Window closed");
//			}
//
//			public void windowClosing(WindowEvent e) {
//				Logging.GAMEHANDLER.info("Window closing");
//				quit();
//			}
//		});
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
