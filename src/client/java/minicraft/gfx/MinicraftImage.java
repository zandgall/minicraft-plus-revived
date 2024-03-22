package minicraft.gfx;

import minicraft.core.CrashHandler;
import minicraft.gfx.SpriteLinker.LinkedSprite;
import org.lwjgl.BufferUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30.*;

/**
 * Although we have SpriteLinker, we still need SpriteSheet for buffering.
 * As BufferedImage is heavy. Our current rendering system still depends on this array.
 */
public class MinicraftImage {
	/**
	 * Each sprite tile size.
	 */
	public static final int boxWidth = 8;

	public final int width, height; // Width and height of the sprite sheet
	public final int texture; // OpenGL texture handle the sprite sheet

	/**
	 * Default with maximum size of image.
	 *
	 * @param image The image to be added.
	 * @throws IOException if I/O exception occurs.
	 */
	public MinicraftImage(BufferedImage image) throws IOException {
		this(image, image.getWidth(), image.getHeight());
	}

	/**
	 * Custom size.
	 *
	 * @param image  The image to be added.
	 * @param width  The width of the {@link MinicraftImage} to be applied to the {@link LinkedSprite}.
	 * @param height The height of the {@link MinicraftImage} to be applied to the {@link LinkedSprite}.
	 * @throws IOException
	 */
	public MinicraftImage(BufferedImage image, int width, int height) throws IOException {
		if (width % 8 != 0)
			CrashHandler.errorHandle(new IllegalArgumentException("Invalid width of SpriteSheet."), new CrashHandler.ErrorInfo(
				"Invalid SpriteSheet argument.", CrashHandler.ErrorInfo.ErrorType.HANDLED,
				String.format("Invalid width: {}, SpriteSheet width should be a multiple of 8.")
			));
		if (height % 8 != 0)
			CrashHandler.errorHandle(new IllegalArgumentException("Invalid height of SpriteSheet."), new CrashHandler.ErrorInfo(
				"Invalid SpriteSheet argument.", CrashHandler.ErrorInfo.ErrorType.HANDLED,
				String.format("Invalid height: {}, SpriteSheet height should be a multiple of 8.")
			));

		// Sets width and height to that of the image
		this.width = width - width % 8;
		this.height = height - height % 8;

		// If size is bigger than image source, throw error.
		if (this.width > image.getWidth() || this.height > image.getHeight()) {
			throw new IOException(new IndexOutOfBoundsException(String.format("Requested size %s*%s out of source size %s*%s",
				this.width, this.height, image.getWidth(), image.getHeight())));
		}

		int[] pixels = image.getRGB(0, 0, width, height, null, 0, width); // Gets the color array of the image pixels

		ByteBuffer buffer = BufferUtils.createByteBuffer(width*height*4);

		// Applying the RGB array into Minicraft rendering system 25 bits RBG array.
		for (int i = 0; i < pixels.length; i++) { // Loops through all the pixels

			// An integer written in hex looks like 0xII II II II, where each two 'I' is a byte
			// An integer can represent a color where TT=transparent, RR=red, GG=green, and BB=blue
			// 0xTT RR GG BB
			// In order to grab the transparency byte (TT), we need to move, or "shift" the integer over 24 bits, or 3 bytes
			// 0xTT RR GG BB >> 24  =  0x00 00 00 TT
			// Now we have an integer between 0 and 255 that holds the transparency value

			// This stuff is to figure out if the pixel is transparent or not
			int transparent = 1;
			// A value of 0 means transparent, a value of 1 means opaque
			if (pixels[i] >> 24 == 0x00) {
				transparent = 0;
			}

			// Actually put the data in the array
			// Grab the Red, Green, and Blue values from image pixels, and put them back in RGBA order
			buffer.put((byte)((pixels[i]>>16)&0xFF));
			buffer.put((byte)((pixels[i]>>8)&0xFF));
			buffer.put((byte)((pixels[i])&0xFF));
			buffer.put((byte)(transparent * 0xFF));
		}
		buffer.flip();

		texture = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, texture);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
		glBindTexture(GL_TEXTURE_2D, 0);
	}
}
