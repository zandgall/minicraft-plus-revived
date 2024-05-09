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

	public static TileShader tile;
	public static OverlayShader overlay;
	public static PassthroughShader passthrough;
	public static PostprocessShader postprocess;
	public static LightingShader lighting;

	public final int shaderProgram;

	public Shader(String vertexCode, String fragmentCode) throws RuntimeException {
		shaderProgram = glCreateProgram();

		// Create and compile vertex shader with given code
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, vertexCode);
		glCompileShader(vs);
		int[] result = new int[]{0};
		glGetShaderiv(vs, GL_COMPILE_STATUS, result);
		if(result[0] == GL_FALSE) {
			Logging.RESOURCEHANDLER_SHADER.error("Could not compile vertex shader");
			Logging.RESOURCEHANDLER_SHADER.error(glGetShaderInfoLog(vs));
			glDeleteShader(vs);
			CrashHandler.crashHandle(new RuntimeException("Could not compile vertex shader"));
		}
		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, fragmentCode);
		glCompileShader(fs);
		glGetShaderiv(fs, GL_COMPILE_STATUS, result);
		if(result[0] == GL_FALSE) {
			Logging.RESOURCEHANDLER_SHADER.error("Could not compile vertex shader");
			Logging.RESOURCEHANDLER_SHADER.error(glGetShaderInfoLog(fs));
			glDeleteShader(fs);
			CrashHandler.crashHandle(new RuntimeException("Could not compile fragment shader"));
		}
		glAttachShader(shaderProgram, vs);
		glAttachShader(shaderProgram, fs);
		glLinkProgram(shaderProgram);
		glValidateProgram(shaderProgram);
		glGetProgramiv(shaderProgram, GL_VALIDATE_STATUS, result);
		if(result[0] == GL_FALSE) {
			throw new RuntimeException(glGetProgramInfoLog(shaderProgram));
		}
		glDeleteShader(vs);
		glDeleteShader(fs);

		glUseProgram(0);
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

	public static class TileShader extends Shader {
		public TileShader(String vertexCode, String fragmentCode) throws RuntimeException {
			super(vertexCode, fragmentCode);
		}

		public void setTexOffset(float x, float y) {glUniform2f(uniformLocation("texOffset"), x, y);}
		public void setMirror(boolean x, boolean y) {glUniform2i(uniformLocation("mirror"), x ? 1 : 0, y ? 1 : 0);}
		public void setFullBright(boolean fullbright) {glUniform1i(uniformLocation("fullbright"), fullbright ? 1 : 0);}
		public void setWhiteTint(int whiteTint) {
			glUniform1i(uniformLocation("useWhiteTint"), whiteTint == -1 ? 0 : 1);
			glUniform3f(uniformLocation("whiteTint"),
				((whiteTint >> 16) & 0xff)/255.f, ((whiteTint >> 8) & 0xff)/255.f, (whiteTint & 0xff)/255.f);
		}
		public void setColor(int color) {
			glUniform1i(uniformLocation("useColor"), color == 0 ? 0 : 1);
			glUniform3f(uniformLocation("color"),
				((color >> 16) & 0xff)/255.f, ((color >> 8) & 0xff)/255.f, (color & 0xff)/255.f);
		}
		public void setTexture(int texture) {
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture);
			glUniform1i(uniformLocation("textured"), texture == 0 ? 0 : 1);
		}

		public void setProjection(Matrix4f projection) {setMatrix(projection, "projection");}
		public void setView(Matrix4f view) {setMatrix(view, "view");}
		public void setTransform(Matrix4f transform) {setMatrix(transform, "transform");}
	}

	public static class OverlayShader extends Shader {

		public OverlayShader(String vertexCode, String fragmentCode) throws RuntimeException {
			super(vertexCode, fragmentCode);
			use();
			glUniform1i(uniformLocation("texture"), 0);
			glUniform1i(uniformLocation("overlay"), 1);
			glUniform1i(uniformLocation("dither"), 2);
			glUseProgram(0);
		}

		public void setTintFactor(float tintFactor) {glUniform1f(uniformLocation("tintFactor"), tintFactor);}

		public void setCurrentLevel(int currentLevel) {glUniform1i(uniformLocation("currentLevel"), currentLevel);}

		public void setAdjust(float x, float y) {glUniform2f(uniformLocation("adjust"), x, y);}

		public void setTexture(int texture) {
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture);
		}

		public void setOverlay(int overlay) {
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, overlay);
		}

		public void setDither(int dither) {
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, dither);
		}

	}

	public static class PassthroughShader extends Shader {
		public PassthroughShader(String vertexCode, String fragmentCode) throws RuntimeException {
			super(vertexCode, fragmentCode);
		}

		public void setTexture(int texture) {
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture);
		}

	}

	public static class PostprocessShader extends Shader {

		public PostprocessShader(String vertexCode, String fragmentCode) throws RuntimeException {
			super(vertexCode, fragmentCode);
		}

		public void setTexture(int texture) {
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture);
		}

	}

	public static class LightingShader extends Shader {
		public LightingShader(String shaderCode) throws RuntimeException {super(shaderCode);}

		public void setRectangle(int x, int y, int width, int height) {
			glUniform4i(uniformLocation("rectangle"), x, y, width, height);
		}

		public void setScreenSize(int x, int y) {glUniform2i(uniformLocation("screenSize"), x, y);}

		public void setRadius(int r) {glUniform1i(uniformLocation("r"), r);}

		public void setTexture(int texture) {
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture);
		}
	}
}
