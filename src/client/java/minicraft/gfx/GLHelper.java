package minicraft.gfx;

import minicraft.core.Game;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.lwjgl.opengl.GL33.*;

public class GLHelper {
	private GLHelper() {}

	private final static Deque<Integer> framebufferStack = new ArrayDeque<>();

	public static int createAndBindTexture(int width, int height, int type) {
		int texture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, texture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, type, GL_UNSIGNED_BYTE, (ByteBuffer)null);
		return texture;
	}

	public static void bindTextureData(int texture, int width, int height, int type, byte[] bytes) {
		ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
		buffer.put(bytes);
		buffer.flip();
		bindTextureData(texture, width, height, type, buffer);
	}

	public static void bindTextureData(int texture, int width, int height, int type, ByteBuffer bytes) {
		glBindTexture(GL_TEXTURE_2D, texture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, type, GL_UNSIGNED_BYTE, bytes);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	}

	public static int createFramebuffer() {
		int framebuffer = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		Integer current = framebufferStack.peekLast();
		glBindFramebuffer(GL_FRAMEBUFFER, current == null ? 0 : current);
		return framebuffer;
	}

	public static void makeFramebufferDrawTo(int framebuffer, int texture) {
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, texture, GL_TEXTURE_2D, 0);
		glDrawBuffer(GL_COLOR_ATTACHMENT0);
		Integer current = framebufferStack.peekLast();
		glBindFramebuffer(GL_FRAMEBUFFER, current == null ? 0 : current);
	}

	public static void clearDrawnFramebuffers() {
		while(!framebufferStack.isEmpty())
			framebufferStack.pop();
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	public static void startDrawingToFramebuffer(int framebuffer, int width, int height) {
		framebufferStack.push(framebuffer);
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		glViewport(0, 0, width, height);
	}

	public static void stopDrawingToFramebuffer() {
		framebufferStack.pop();
		Integer current = framebufferStack.peekLast();
		glBindFramebuffer(GL_FRAMEBUFFER, current == null ? 0 : current);
	}

	public static void drawSquare() {
		glBindVertexArray(Game.getVao());
		glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
	}
}
