package minicraft.gfx;

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
	 * Initializes a {@code MinicraftImage} instance from the provided size.
	 * All values are filled with zero after construction.
	 * @param width the final width of this image
	 * @param height the final height of this image
	 * @throws IllegalArgumentException if either {@code width} or {@code height} is zero or negative
	 */
	public MinicraftImage(int width, int height) {
		if (width < 1) throw new IllegalArgumentException("width cannot be zero or negative");
		if (height < 1) throw new IllegalArgumentException("height cannot be zero or negative");

		this.width = width;
		this.height = height;
		pixels = new int[width * height];
	}

	/**
	 * Constructs a {@code MinicraftImage} with the maximum size of dimension supplied from the source {@code BufferedImage}.
	 * @param image the {@code BufferedImage} to be constructed from
	 * @see LinkedSprite
	 */
	public MinicraftImage(@NotNull BufferedImage image) {
		this(image, image.getWidth(), image.getHeight());
	}

	/**
	 * Constructs a {@code MinicraftImage} with the given size from the source {@code BufferedImage}.
	 * If the requested size is out of the source's dimension, the remaining values will be left {@code 0}.
	 * @param image the {@code BufferedImage} to be constructed from
	 * @param width  the requested width for this image, must be a non-zero natural number
	 * @param height the requested height for this image, must be a non-zero natural number
	 * @throws IllegalArgumentException if either {@code width} or {@code height} is zero or negative
	 * @see LinkedSprite
	 */
	public MinicraftImage(@NotNull BufferedImage image, int width, int height) {
		Objects.requireNonNull(image, "image");
		if (width < 1) throw new IllegalArgumentException("width cannot be zero or negative");
		if (height < 1) throw new IllegalArgumentException("height cannot be zero or negative");

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

	/**
	 * Creates a {@link MinicraftImage} from the provided image with default dimension validation.
	 * @param image the {@code BufferedImage} to be constructed from
	 * @return the constructed {@code Minicraft}
	 * @throws MinicraftImageDimensionIncompatibleException if the image's dimension is not a multiple of 8
	 */
	public static MinicraftImage createDefaultCompatible(BufferedImage image) throws MinicraftImageDimensionIncompatibleException {
		validateImageDimension(image);
		return new MinicraftImage(image);
	}

	/**
	 * Validates if the provided image is compatible with the game's general sprite rendering system.
	 * @param image The image to be validated
	 * @throws MinicraftImageDimensionIncompatibleException if the image's dimension is not a multiple of 8
	 */
	public static void validateImageDimension(BufferedImage image) throws MinicraftImageDimensionIncompatibleException {
		if (image.getHeight() % 8 != 0 || image.getWidth() % 8 != 0)
			throw new MinicraftImageDimensionIncompatibleException(image.getWidth(), image.getHeight());
	}

	/**
	 * Validates if the provided image is respective to the required size.
	 * @param image The image to be validated
	 * @param width The requested width
	 * @param height The requested height
	 * @throws MinicraftImageRequestOutOfBoundsException if the requested size is out of the image's dimension
	 */
	public static void validateImageDimension(BufferedImage image, int width, int height)
		throws MinicraftImageRequestOutOfBoundsException {
		if (image.getWidth() < width || image.getHeight() < height)
			throw new MinicraftImageRequestOutOfBoundsException(image.getWidth(), image.getHeight(), width, height);
	}

	public static class MinicraftImageDimensionIncompatibleException extends Exception {
		private final int width, height;

		public MinicraftImageDimensionIncompatibleException(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public final int getWidth() { return width; }
		public final int getHeight() { return height; }
	}

	public static class MinicraftImageRequestOutOfBoundsException extends Exception {
		private final int srcW, srcH, rqtW, rqtH;
		public MinicraftImageRequestOutOfBoundsException(int srcW, int srcH, int rqtW, int rqtH) {
			this.srcW = srcW;
			this.srcH = srcH;
			this.rqtW = rqtW;
			this.rqtH = rqtH;
		}

		public final int getSourceWidth() { return srcW; }
		public final int getSourceHeight() { return srcH; }
		public final int getRequestedWidth() { return rqtW; }
		public final int getRequestedHeight() { return rqtH; }
	}
}
