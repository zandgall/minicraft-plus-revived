package minicraft.core.io;

import minicraft.core.CrashHandler;
import minicraft.core.Renderer;
import minicraft.util.Logging;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.io.*;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL30.*;

public class Shader {

	// TODO: overhaul the types of shaders used

	public static SpriteShader sprite;
	public static RectShader rect;
	public static LineShader line;
	public static LineSpecialShader lineSpecial;
	public static OverlayShader overlay;
	public static LightingShader lighting;
	public static PostprocessShader postprocess;

	public final int shaderProgram;
	protected int vertexShader = 0, fragmentShader = 0;

	public Shader() {
		shaderProgram = glCreateProgram();

		glUseProgram(0);
    }

	public static void initShaders() {
		if(sprite != null) {
			glDeleteProgram(sprite.shaderProgram);
			glDeleteShader(sprite.vertexShader);
			glDeleteShader(sprite.fragmentShader);
			glDeleteProgram(rect.shaderProgram);
			glDeleteShader(rect.vertexShader);
			glDeleteShader(rect.fragmentShader);
			glDeleteProgram(line.shaderProgram);
			glDeleteShader(line.vertexShader);
			glDeleteShader(line.fragmentShader);
			glDeleteProgram(lineSpecial.shaderProgram);
			glDeleteShader(lineSpecial.vertexShader);
			glDeleteShader(lineSpecial.fragmentShader);
			glDeleteProgram(overlay.shaderProgram);
			glDeleteShader(overlay.vertexShader);
			glDeleteShader(overlay.fragmentShader);
			glDeleteProgram(lighting.shaderProgram);
			glDeleteShader(lighting.vertexShader);
			glDeleteShader(lighting.fragmentShader);
			glDeleteProgram(postprocess.shaderProgram);
			glDeleteShader(postprocess.vertexShader);
			glDeleteShader(postprocess.fragmentShader);
		}

		sprite = new SpriteShader();
		rect = new RectShader();
		line = new LineShader();
		lineSpecial = new LineSpecialShader();
		overlay = new OverlayShader();
		lighting = new LightingShader();
		postprocess = new PostprocessShader();
	}

	private void validate() {
		glAttachShader(shaderProgram, vertexShader);
		glAttachShader(shaderProgram, fragmentShader);
		glLinkProgram(shaderProgram);
		glValidateProgram(shaderProgram);
		int[] result = new int[1];
		glGetProgramiv(shaderProgram, GL_VALIDATE_STATUS, result);
		if(result[0] == GL_FALSE) {
			throw new RuntimeException(glGetProgramInfoLog(shaderProgram));
		}
	}

	public void setVertexShader(String vertexCode) throws RuntimeException {
		if(vertexShader!=0) {
			glDeleteShader(vertexShader);
		}
		glUseProgram(shaderProgram);

		// Create and compile vertex shader with given code
		vertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertexShader, vertexCode);
		glCompileShader(vertexShader);
		int[] result = new int[]{0};
		glGetShaderiv(vertexShader, GL_COMPILE_STATUS, result);
		if(result[0] == GL_FALSE) {
			Logging.RESOURCEHANDLER_SHADER.error("Could not compile vertex shader");
			Logging.RESOURCEHANDLER_SHADER.error(glGetShaderInfoLog(vertexShader));
			glDeleteShader(vertexShader);
			throw new RuntimeException("Could not compile vertex shader");
		}

		// If fragment and vertex are both loaded, validate
		if(fragmentShader != 0)
			validate();
	}

	public void setFragmentShader(String fragmentCode) throws RuntimeException {
		if(fragmentShader!=0)
			glDeleteShader(fragmentShader);
		glUseProgram(shaderProgram);

		// Create and compile vertex shader with given code
		fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragmentShader, fragmentCode);
		glCompileShader(fragmentShader);
		int[] result = new int[]{0};
		glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, result);
		if(result[0] == GL_FALSE) {
			Logging.RESOURCEHANDLER_SHADER.error("Could not compile fragment shader");
			Logging.RESOURCEHANDLER_SHADER.error(glGetShaderInfoLog(fragmentShader));
			glDeleteShader(fragmentShader);
			throw new RuntimeException("Could not compile fragment shader");
		}

		// If fragment and vertex are both loaded, validate
		if(vertexShader != 0)
			validate();
	}

	// Get ID of uniform variable in shader
	protected int uniformLocation(String name) {
		return glGetUniformLocation(shaderProgram, name);
	}

	// Give the shader a matrix for the given uniform variable name
	public void setMatrix(Matrix4f matrix, String name) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			FloatBuffer buffer = matrix.get(stack.mallocFloat(16));
			glUniformMatrix4fv(uniformLocation(name), false, buffer);
		}
	}

	public void use() {
		glUseProgram(shaderProgram);
	}

	public static class SpriteShader extends Shader {
		public SpriteShader() { super(); }

		public void setPosition(float x, float y) {glUniform2f(uniformLocation("position"), x/Renderer.WIDTH, y/Renderer.HEIGHT);}
		public void setTexOffset(float x, float y) {glUniform2f(uniformLocation("texOffset"), x, y);}
		public void setTexSize(float x, float y) {
			glUniform2f(uniformLocation("texSize"), x, y);
			glUniform2f(uniformLocation("size"), x / Renderer.WIDTH, y / Renderer.HEIGHT);
		}
		public void setMirror(boolean x, boolean y) {glUniform2i(uniformLocation("mirror"), x ? 1 : 0, y ? 1 : 0);}
		public void setFullBright(boolean fullbright) {glUniform1i(uniformLocation("fullbright"), fullbright ? 1 : 0);}
		public void setWhiteTint(int whiteTint) {
			glUniform1i(uniformLocation("useWhiteTint"), whiteTint == -1 ? 0 : 1);
			glUniform3f(uniformLocation("whiteTint"),
				((whiteTint >> 16) & 0xff) / 255.f, ((whiteTint >> 8) & 0xff) / 255.f, (whiteTint & 0xff) / 255.f);
		}
		public void setColor(int color) {
			glUniform1i(uniformLocation("useColor"), color == 0 ? 0 : 1);
			glUniform3f(uniformLocation("color"),
				((color >> 16) & 0xff)/255.f, ((color >> 8) & 0xff)/255.f, (color & 0xff)/255.f);
		}
		public void setTexture(int texture) {
			glUniform1i(uniformLocation("texture"), 0);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture);
			glUniform1i(uniformLocation("textured"), texture == 0 ? 0 : 1);
		}
	}

	public static class RectShader extends Shader {
		public RectShader() { super(); }

		public void setRectangle(float x, float y, float width, float height) {
			glUniform2f(uniformLocation("position"), x / Renderer.WIDTH, y / Renderer.HEIGHT);
			glUniform2f(uniformLocation("size"), width / Renderer.WIDTH, height / Renderer.HEIGHT);
		}

		public void setColor(int color) {
			glUniform3f(uniformLocation("color"),
				((color >> 16) & 0xff)/255.f, ((color >> 8) & 0xff)/255.f, (color & 0xff)/255.f);
		}
	}

	public static class LineShader extends Shader {
		public LineShader() { super(); }

		public void setPoints(float x0, float y0, float x1, float y1) {
			glUniform2f(uniformLocation("point0"), x0, y0);
			glUniform2f(uniformLocation("point1"), x1, y1);
		}

		public void setColor(int color) {
			glUniform3f(uniformLocation("color"),
				((color >> 16) & 0xff)/255.f, ((color >> 8) & 0xff)/255.f, (color & 0xff)/255.f);
		}
	}

	public static class LineSpecialShader extends Shader {
		public LineSpecialShader() { super(); }

		public void setPoints(float x0, float y0, float x1, float y1) {
			glUniform2f(uniformLocation("point0"), x0, y0);
			glUniform2f(uniformLocation("point1"), x1, y1);
		}

		public void setTexture(int texture) {
			glUniform1i(uniformLocation("texture"), 0);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture);
		}
	}

	public static class OverlayShader extends Shader {

		public OverlayShader() { super(); }

		public void setAdjust(float x, float y) {glUniform2f(uniformLocation("adjust"), x, y);}

		/**
		 * Truncates to single precision float - sorry :(
		 * @param alpha Input double that gets cast to float and passed to GPU
		 */
		public void setAlpha(double alpha) {glUniform1f(uniformLocation("alpha"), (float)alpha);}

		public void setTexture(int texture) {
			glUniform1i(uniformLocation("texture"), 0);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture);
		}

		public void setOverlay(int overlay) {
			glUniform1i(uniformLocation("overlay"), 1);
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, overlay);
		}

		public void setDither(int dither) {
			glUniform1i(uniformLocation("dither"), 2);
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, dither);
		}

	}

	public static class PostprocessShader extends Shader {

		public PostprocessShader() { super(); }

		public void setTexture(int texture) {
			glUniform1i(uniformLocation("texture"), 0);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture);
		}

	}

	public static class LightingShader extends Shader {
		public LightingShader() { super(); }

		public void setCenter(float x, float y) {
			glUniform2f(uniformLocation("center"), x / Renderer.WIDTH, y / Renderer.HEIGHT);
		}

		public void setScreenSize(int x, int y) {glUniform2i(uniformLocation("screenSize"), x, y);}

		public void setRadius(float r) {glUniform1f(uniformLocation("radius"), r);}
	}
}
