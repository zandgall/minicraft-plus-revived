package minicraft.core.io;

import minicraft.core.CrashHandler;
import minicraft.util.Logging;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Sound {
	// Creates sounds from their respective files
	private static final HashMap<String, Sound> sounds = new HashMap<>();

	private static long device, context;
	private final int source;

	private Sound(int source) {
		this.source = source;
	}

	public static void resetSounds() {
		sounds.clear();
	}

	static {
		device = alcOpenDevice((ByteBuffer)null);
		if(device == NULL) {
			CrashHandler.crashHandle(new RuntimeException("Could not initialize sound device"), new CrashHandler.ErrorInfo("Could not initialize sound device", CrashHandler.ErrorInfo.ErrorType.SERIOUS));
		}
		context = alcCreateContext(device, (IntBuffer) null);
		if(context == NULL) {
			CrashHandler.crashHandle(new RuntimeException("Could not initialize sound device context"), new CrashHandler.ErrorInfo("Could not initialize sound device context", CrashHandler.ErrorInfo.ErrorType.SERIOUS));
		}
		alcMakeContextCurrent(context);
		ALCCapabilities capabilities = ALC.createCapabilities(device);
		AL.createCapabilities(capabilities);
	}

	public static void loadSound(String key, InputStream in, String pack) {
		try {
			AudioFormat format = AudioSystem.getAudioFileFormat(in).getFormat();
			DataLine.Info info = new DataLine.Info(null, format);

			if (!AudioSystem.isLineSupported(info)) {
				Logging.RESOURCEHANDLER_SOUND.error("ERROR: Audio format of file \"{}\" in pack \"\" is not supported: {}", key, pack, AudioSystem.getAudioFileFormat(in));

				Logging.RESOURCEHANDLER_SOUND.error("Supported audio formats:");
				Logging.RESOURCEHANDLER_SOUND.error("-source:");
				Line.Info[] sinfo = AudioSystem.getSourceLineInfo(info);
				Line.Info[] tinfo = AudioSystem.getTargetLineInfo(info);
				for (Line.Info value : sinfo) {
					if (value instanceof DataLine.Info) {
						DataLine.Info dataLineInfo = (DataLine.Info) value;
                        AudioFormat[] supportedFormats = dataLineInfo.getFormats();
						for (AudioFormat af : supportedFormats)
							Logging.RESOURCEHANDLER_SOUND.error(af);
					}
				}
				Logging.RESOURCEHANDLER_SOUND.error("-target:");
                for (Line.Info value : tinfo) {
                    if (value instanceof DataLine.Info) {
						DataLine.Info dataLineInfo = (DataLine.Info) value;
                        AudioFormat[] supportedFormats = dataLineInfo.getFormats();
                        for (AudioFormat af : supportedFormats)
                            Logging.RESOURCEHANDLER_SOUND.error(af);
                    }
                }

				return;
			}

			int source = alGenSources();
			int buffer = alGenBuffers();
			AudioInputStream stream = AudioSystem.getAudioInputStream(in);

			ByteBuffer bytes = getAudioBytes(stream, format);
			int alFormat;
			if(format.getChannels() == 1) {
				if (format.getSampleSizeInBits() == 8) {
					alFormat = AL_FORMAT_MONO8;
				} else alFormat = AL_FORMAT_MONO16;
			} else if(format.getSampleSizeInBits() == 8) {
				alFormat = AL_FORMAT_STEREO8;
			} else alFormat = AL_FORMAT_STEREO16;

			alBufferData(buffer, alFormat, bytes, (int)format.getSampleRate());
			alSourcei(source, AL_BUFFER, buffer);
			sounds.put(key, new Sound(source));

		} catch (UnsupportedAudioFileException | IOException e) {
			CrashHandler.errorHandle(e, new CrashHandler.ErrorInfo("Audio Could not Load", CrashHandler.ErrorInfo.ErrorType.REPORT,
				String.format("Could not load audio: %s in pack: %s", key, pack)));
		}
	}

	/**
	 * Asserts that audio bytes appear in correct order and format
	 * References <a href="https://github.com/LWJGL/lwjgl/blob/master/src/java/org/lwjgl/util/WaveData.java">lwjgl v2, WaveData.java</a>
	 */
	private static ByteBuffer convertAudioBytes(byte[] audio_bytes, boolean two_bytes_data, ByteOrder order) {
		ByteBuffer dest = ByteBuffer.allocateDirect(audio_bytes.length);
		dest.order(ByteOrder.nativeOrder());
		ByteBuffer src = ByteBuffer.wrap(audio_bytes);
		src.order(order);
		if (two_bytes_data) {
			ShortBuffer dest_short = dest.asShortBuffer();
			ShortBuffer src_short = src.asShortBuffer();
			while (src_short.hasRemaining())
				dest_short.put(src_short.get());
		} else {
			while (src.hasRemaining())
				dest.put(src.get());
		}
		dest.rewind();
		return dest;
	}

	/**
     * Creates and returns a ByteBuffer with OpenAL compatible sound buffer data, usable in alBufferData
     * References <a href="https://github.com/LWJGL/lwjgl/blob/master/src/java/org/lwjgl/util/WaveData.java">lwjgl v2, WaveData.java</a>
     * @throws IOException
     */
	private static ByteBuffer getAudioBytes(AudioInputStream stream, AudioFormat format) throws IOException {
		int available = stream.available();
		if(available <= 0) {
			available = stream.getFormat().getChannels() * (int) stream.getFrameLength() * stream.getFormat().getSampleSizeInBits() / 8;
		}
		byte[] buf = new byte[available];
		int read = 0, total = 0;
		while ((read = stream.read(buf, total, buf.length - total)) != -1
			&& total < buf.length) {
			total += read;
		}
		return convertAudioBytes(buf, format.getSampleSizeInBits() == 16, format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Recommend {@link #play(String)} and {@link #loop(String, boolean)}.
	 */
	@Nullable
	public static Sound getSound(String key) {
		return sounds.get(key);
	}

	/**
	 * This method does safe check for {@link #play()}.
	 */
	public static void play(String key) {
		Sound sound = sounds.get(key);
		if (sound != null) sound.play();
	}

	/**
	 * This method does safe check for {@link #loop(boolean)}.
	 */
	public static void loop(String key, boolean start) {
		Sound sound = sounds.get(key);
		if (sound != null) sound.loop(start);
	}

	public void play() {
		if (!(boolean) Settings.get("sound") || source == NULL) return;

		alSourcePlay(source);
	}

	public void loop(boolean start) {
		if (!(boolean) Settings.get("sound") || source == NULL) return;

		alSourcei(source, AL_LOOPING, start ? 1 : 0);
		if (start)
			alSourcePlay(source);
		else
			alSourceStop(source);
	}
}
