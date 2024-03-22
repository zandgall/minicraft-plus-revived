package minicraft.gfx;

import minicraft.core.Game;
import minicraft.core.Renderer;
import minicraft.core.Updater;
import minicraft.gfx.SpriteLinker.LinkedSprite;
import minicraft.gfx.SpriteLinker.SpriteType;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL30.*;

public class Screen {

	public static final int w = Renderer.WIDTH; // Width of the screen
	public static final int h = Renderer.HEIGHT; // Height of the screen
	public static final Point center = new Point(w / 2, h / 2);

	private static final int MAXDARK = 128;

	/// width and height of screen instance
	private final int width;
	private final int height;

	/// x and y offset of screen:
	private int xOffset;
	private int yOffset;

	// Used for mirroring an image:
	private static final int BIT_MIRROR_X = 0x01; // Written in hexadecimal; binary: 01
	private static final int BIT_MIRROR_Y = 0x02; // Binary: 10

	private final int texture, framebuffer;

	// Outdated Information:
	// Since each sheet is 256x256 pixels, each one has 1024 8x8 "tiles"
	// So 0 is the start of the item sheet 1024 the start of the tile sheet, 2048 the start of the entity sheet,
	// And 3072 the start of the gui sheet

	public Screen(int width, int height) {
		/// Screen width and height are typically the window size (Renderer.WIDTH/HEIGHT), but not always (see QuestDisplay.java)
		this.width = width;
		this.height = height;

		framebuffer = glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		texture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, texture);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		glDrawBuffer(GL_COLOR_ATTACHMENT0);
		glViewport(0, 0, width, height);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glBindFramebuffer(GL_FRAMEBUFFER, 0);

		glBindTexture(GL_TEXTURE_2D, 0);

		if(ditherTexture == 0) {
			ByteBuffer buffer = BufferUtils.createByteBuffer(16);
			buffer.put((byte)0)  .put((byte)80) .put((byte)20) .put((byte)100);
			buffer.put((byte)120).put((byte)40) .put((byte)140).put((byte)60);
			buffer.put((byte)30) .put((byte)110).put((byte)10) .put((byte)90);
			buffer.put((byte)150).put((byte)70) .put((byte)130).put((byte)50);
			buffer.flip();
			ditherTexture = glGenTextures();
			glBindTexture(GL_TEXTURE_2D, ditherTexture);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 4, 4, 0, GL_RED, GL_UNSIGNED_BYTE, buffer);
		}
	}
	public int getTexture() {return texture;}

	/**
	 * Clears all the colors on the screen
	 */
	public void clear(int color) {
		// Turns each pixel into a single color (clearing the screen!)
		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		glViewport(0, 0, width, height);
		glClearColor(((color >> 16) & 0xFF) / 255.f, ((color >> 8) & 0xFF) / 255.f, (color & 0xFF) / 255.f, ((color >> 24) & 0xFF) / 255.f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}

	public void render(int xp, int yp, int xt, int yt, int bits, MinicraftImage sheet) {
		render(xp, yp, xt, yt, bits, sheet, -1);
	}

	public void render(int xp, int yp, int xt, int yt, int bits, MinicraftImage sheet, int whiteTint) {
		render(xp, yp, xt, yt, bits, sheet, whiteTint, false);
	}

	/**
	 * This method takes care of assigning the correct spritesheet to assign to the sheet variable
	 **/
	public void render(int xp, int yp, int xt, int yt, int bits, MinicraftImage sheet, int whiteTint, boolean fullbright) {
		render(xp, yp, xt, yt, bits, sheet, whiteTint, fullbright, 0);
	}

	public void render(int xp, int yp, LinkedSprite sprite) {
		render(xp, yp, sprite.getSprite());
	}

	public void render(int xp, int yp, Sprite sprite) {
		render(xp, yp, sprite, false);
	}

	public void render(int xp, int yp, Sprite sprite, boolean fullbright) {
		render(xp, yp, sprite, 0, fullbright, 0);
	}

	public void render(int xp, int yp, Sprite sprite, int mirror, boolean fullbright) {
		render(xp, yp, sprite, mirror, fullbright, 0);
	}

	public void render(int xp, int yp, Sprite sprite, int mirror, boolean fullbright, int color) {
		for (int r = 0; r < sprite.spritePixels.length; r++) {
			for (int c = 0; c < sprite.spritePixels[r].length; c++) {
				Sprite.Px px = sprite.spritePixels[r][c];
				render(xp + c * 8, yp + r * 8, px, mirror, sprite.color, fullbright, color);
			}
		}
	}

	public void render(int xp, int yp, Sprite.Px pixel) {
		render(xp, yp, pixel, -1);
	}

	public void render(int xp, int yp, Sprite.Px pixel, int whiteTint) {
		render(xp, yp, pixel, 0, whiteTint);
	}

	public void render(int xp, int yp, Sprite.Px pixel, int mirror, int whiteTint) {
		render(xp, yp, pixel, mirror, whiteTint, false);
	}

	public void render(int xp, int yp, Sprite.Px pixel, int mirror, int whiteTint, boolean fullbright) {
		render(xp, yp, pixel, mirror, whiteTint, fullbright, 0);
	}

	public void render(int xp, int yp, Sprite.Px pixel, int mirror, int whiteTint, boolean fullbright, int color) {
		render(xp, yp, pixel.x, pixel.y, pixel.mirror ^ mirror, pixel.sheet, whiteTint, fullbright, color);
	}

	/**
	 * Renders an object from the sprite sheet based on screen coordinates, tile (SpriteSheet location), colors, and bits (for mirroring). I believe that xp and yp refer to the desired position of the upper-left-most pixel.
	 */
	public void render(int xp, int yp, int xt, int yt, int bits, MinicraftImage sheet, int whiteTint, boolean fullbright, int color) {
		if (sheet == null) return; // Verifying that sheet is not null.

		// xp and yp are originally in level coordinates, but offset turns them to screen coordinates.
		xp -= xOffset; //account for screen offset
		yp -= yOffset;

		// Determines if the image should be mirrored...
		boolean mirrorX = (bits & BIT_MIRROR_X) > 0; // Horizontally.
		boolean mirrorY = (bits & BIT_MIRROR_Y) > 0; // Vertically.

		// Validation check
//		if (xt * 8 + yt * 8 * sheet.width + 7 + 7 * sheet.width >= sheet.pixels.length) {
		// OpenGL has no problems when referencing textures out of bounds, so no need to check if within texture data
		// But can check if outside sheet dimensions
		if (xt * 8 >= sheet.width || yt * 8 >= sheet.height) {
			sheet = Renderer.spriteLinker.missingSheet(SpriteType.Item);
			xt = 0;
			yt = 0;
		}

		try(MemoryStack stack = MemoryStack.stackPush()) {
			glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
			glViewport(0, 0, width, height);
			glUseProgram(Game.getDefaultShader());
			FloatBuffer sp = new Matrix4f().ortho(0, width, height, 0,-1, 1)
				.get(stack.mallocFloat(16));
			glUniformMatrix4fv(glGetUniformLocation(Game.getDefaultShader(), "screenspace"), false, sp);
			FloatBuffer tf = new Matrix4f().identity().translate(xp+4, yp+4,0).scale(4).get(stack.mallocFloat(16));
			glUniformMatrix4fv(glGetUniformLocation(Game.getDefaultShader(), "transform"), false, tf);
			glUniform1i(glGetUniformLocation(Game.getDefaultShader(), "textured"), 1);
			glUniform2f(glGetUniformLocation(Game.getDefaultShader(), "texOffset"), xt, yt);
			glUniform2i(glGetUniformLocation(Game.getDefaultShader(), "mirror"), mirrorX ? 1 : 0, mirrorY ? 1 : 0);
			glUniform1i(glGetUniformLocation(Game.getDefaultShader(), "fullbright"), fullbright ? 1 : 0);
			glUniform1i(glGetUniformLocation(Game.getDefaultShader(), "useWhiteTint"), whiteTint == -1 ? 0 : 1);
			glUniform3f(glGetUniformLocation(Game.getDefaultShader(), "whiteTint"),
				((whiteTint >> 16) & 0xff)/255.f, ((whiteTint >> 8) & 0xff)/255.f, (whiteTint & 0xff)/255.f);
			glUniform1i(glGetUniformLocation(Game.getDefaultShader(), "useColor"), color == 0 ? 0 : 1);
			glUniform3f(glGetUniformLocation(Game.getDefaultShader(), "color"),
				((color >> 16) & 0xff)/255.f, ((color >> 8) & 0xff)/255.f, (color & 0xff)/255.f);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, sheet.texture);
			glBindVertexArray(Game.getVao());
			glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
		}
	}

	/**
	 * Just draws {@param color} to the rectangle provided
	 * @param x Screen x-coordinate
	 * @param y Screen y-coordinate
	 */
	public void render(int x, int y, int width, int height, int color) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
			glViewport(0, 0, this.width, this.height);

			glUseProgram(Game.getDefaultShader());
			FloatBuffer sp = new Matrix4f().ortho(0, this.width, this.height, 0, -1, 1)
				.get(stack.mallocFloat(16));
			glUniformMatrix4fv(glGetUniformLocation(Game.getDefaultShader(), "screenspace"), false, sp);
			FloatBuffer tf = new Matrix4f().identity().translate(x+width*0.5f, y+height*0.5f,1).scale(width*0.5f, height*0.5f, 1).get(stack.mallocFloat(16));
			glUniformMatrix4fv(glGetUniformLocation(Game.getDefaultShader(), "transform"), false, tf);
			glUniform1i(glGetUniformLocation(Game.getDefaultShader(), "textured"), 0);
			glUniform1i(glGetUniformLocation(Game.getDefaultShader(), "useColor"), color == 0 ? 0 : 1);
			glUniform3f(glGetUniformLocation(Game.getDefaultShader(), "color"),
				((color >> 16) & 0xff)/255.f, ((color >> 8) & 0xff)/255.f, (color & 0xff)/255.f);

			glBindVertexArray(Game.getVao());
			glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
		}
	}

	/**
	 * Draws a screen on top of another screen, without the overlay effect(s)
	 */
	public void render(int xp, int yp, Screen screen2) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
			glViewport(0, 0, w, h);

			glUseProgram(Game.getPassthroughShader());
			FloatBuffer sp = new Matrix4f().ortho(0, width, height, 0,-1, 1)
				.get(stack.mallocFloat(16));
			glUniformMatrix4fv(glGetUniformLocation(Game.getPassthroughShader(), "screenspace"), false, sp);
			// Usually we use a different ortho perspective, but it's messed up in this case, so we're drawing the screen upside down instead
			FloatBuffer tf = new Matrix4f().identity().translate(xp+screen2.width*0.5f, yp+screen2.height*0.5f,1)
				.scale(screen2.width*0.5f, -screen2.height*0.5f, 1).get(stack.mallocFloat(16));
			glUniformMatrix4fv(glGetUniformLocation(Game.getPassthroughShader(), "transform"), false, tf);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, screen2.getTexture());
			glBindVertexArray(Game.getVao());
			glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
		}
	}

	/**
	 * Sets the offset of the screen
	 */
	public void setOffset(int xOffset, int yOffset) {
		// This is called in few places, one of which is level.renderBackground, right before all the tiles are rendered. The offset is determined by the Game class (this only place renderBackground is called), by using the screen's width and the player's position in the level.
		// In other words, the offset is a conversion factor from level coordinates to screen coordinates. It makes a certain coord in the level the upper left corner of the screen, when subtracted from the tile coord.

		this.xOffset = xOffset;
		this.yOffset = yOffset;
	}

	/* Used for the scattered dots at the edge of the light radius underground.

		These values represent the minimum light level, on a scale from 0 to 25 (255/10), 0 being no light, 25 being full light (which will be portrayed as transparent on the overlay lightScreen pixels) that a pixel must have in order to remain lit (not black).
		each row and column is repeated every 4 pixels in the proper direction, so the pixel lightness minimum varies. It's highly worth note that, as the rows progress and loop, there's two sets or rows (1,4 and 2,3) whose values in the same column add to 15. The exact same is true for columns (sets are also 1,4 and 2,3), execpt the sums of values in the same row and set differ for each row: 10, 18, 12, 20. Which... themselves... are another set... adding to 30... which makes sense, sort of, since each column totals 15+15=30.
		In the end, "every other every row", will need, for example in column 1, 15 light to be lit, then 0 light to be lit, then 12 light to be lit, then 3 light to be lit. So, the pixels of lower light levels will generally be lit every other pixel, while the brighter ones appear more often. The reason for the variance in values is to provide EVERY number between 0 and 15, so that all possible light levels (below 16) are represented fittingly with their own pattern of lit and not lit.
		16 is the minimum pixel lighness required to ensure that the pixel will always remain lit.
	*/

	private static int ditherTexture = 0;

	/**
	 * Overlays the screen with pixels
	 */
	public void overlay(Screen screen2, int currentLevel, int xa, int ya) {
		double tintFactor = getTintFactor(currentLevel);

		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		glViewport(0, 0, width, height);
		try(MemoryStack stack = MemoryStack.stackPush()) {
			glUseProgram(Game.getOverlayShader());
			FloatBuffer sp = new Matrix4f().ortho(0, width, 0, height,-1, 1)
				.get(stack.mallocFloat(16));
			glUniformMatrix4fv(glGetUniformLocation(Game.getOverlayShader(), "screenspace"), false, sp);
			FloatBuffer tf = new Matrix4f().identity().translate(width / 2.f, height / 2.f, 0)
				.scale(width / 2.f, height / 2.f, 1).get(stack.mallocFloat(16));
			glUniformMatrix4fv(glGetUniformLocation(Game.getOverlayShader(), "transform"), false, tf);
			glUniform1f(glGetUniformLocation(Game.getOverlayShader(), "tintFactor"), (float) (tintFactor/255.f));
			glUniform1i(glGetUniformLocation(Game.getOverlayShader(), "currentLevel"), currentLevel);
			glUniform2f(glGetUniformLocation(Game.getOverlayShader(), "adjust"), xa, ya);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture);
			glActiveTexture(GL_TEXTURE1);
			glBindTexture(GL_TEXTURE_2D, screen2.getTexture());
			glActiveTexture(GL_TEXTURE2);
			glBindTexture(GL_TEXTURE_2D, ditherTexture);
			glActiveTexture(GL_TEXTURE0);
			glBindVertexArray(Game.getVao());
			glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
		}
	}

	private static double getTintFactor(int currentLevel) {
		double tintFactor = 0;
		if (currentLevel >= 3 && currentLevel < 5) {
			int transTime = Updater.dayLength / 4;
			double relTime = (Updater.tickCount % transTime) * 1.0 / transTime;

			switch (Updater.getTime()) {
				case Morning:
					tintFactor = Updater.pastDay1 ? (1 - relTime) * MAXDARK : 0;
					break;
				case Day:
					tintFactor = 0;
					break;
				case Evening:
					tintFactor = relTime * MAXDARK;
					break;
				case Night:
					tintFactor = MAXDARK;
					break;
			}

			if (currentLevel > 3) tintFactor -= (tintFactor < 10 ? tintFactor : 10);
			tintFactor *= -1; // All previous operations were assuming this was a darkening factor.
		} else if (currentLevel >= 5)
			tintFactor = -MAXDARK;
		return tintFactor;
	}

	public void renderLight(int x, int y, int r) {
		// Applies offsets:
		x -= xOffset;
		y -= yOffset;

		glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
		glViewport(0, 0, width, height);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		try(MemoryStack stack = MemoryStack.stackPush()) {
			glUseProgram(Game.getLightingShader());
			FloatBuffer sp = new Matrix4f().ortho(0, width, height, 0, -1, 1)
				.get(stack.mallocFloat(16));
			glUniformMatrix4fv(glGetUniformLocation(Game.getLightingShader(), "screenspace"), false, sp);
			FloatBuffer tf = new Matrix4f().identity().translate(x, y, 0)
				.scale(r, r, 1).get(stack.mallocFloat(16));
			glUniformMatrix4fv(glGetUniformLocation(Game.getLightingShader(), "transform"), false, tf);
			glUniform4i(glGetUniformLocation(Game.getLightingShader(), "rectangle"), x-r, y-r, x+r, y+r);
			glUniform2i(glGetUniformLocation(Game.getLightingShader(), "screenSize"), Renderer.WIDTH, Renderer.HEIGHT);
			glUniform1i(glGetUniformLocation(Game.getLightingShader(), "r"), r);
			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture);
			glBindVertexArray(Game.getVao());
			glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
		}
	}
}
