package minicraft.core.io;

import minicraft.core.CrashHandler;
import minicraft.util.Logging;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Sound {
	// Creates sounds from their respective files
	private static final HashMap<String, Sound> sounds = new HashMap<>();

	private static long device, context;

	private Clip clip; // Creates a audio clip to be played
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
			Logging.RESOURCEHANDLER_SOUND.error("Could not initialize sound device");
		}
		context = alcCreateContext(device, (IntBuffer) null);
		if(context == NULL) {
			Logging.RESOURCEHANDLER_SOUND.error("Could not initialize sound device context");
		}
		alcMakeContextCurrent(context);
		ALCCapabilities capabilities = ALC.createCapabilities(device);
		AL.createCapabilities(capabilities);
	}

	public static void loadSound(String key, InputStream in, String pack) {
		try {
			AudioFormat format = AudioSystem.getAudioFileFormat(in).getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);

			if (!AudioSystem.isLineSupported(info)) {
				Logging.RESOURCEHANDLER_SOUND.error("ERROR: Audio format of file \"{}\" in pack \"\" is not supported: {}", key, pack, AudioSystem.getAudioFileFormat(in));

				Logging.RESOURCEHANDLER_SOUND.error("Supported audio formats:");
				Logging.RESOURCEHANDLER_SOUND.error("-source:");
				Line.Info[] sinfo = AudioSystem.getSourceLineInfo(info);
				Line.Info[] tinfo = AudioSystem.getTargetLineInfo(info);
				for (Line.Info value : sinfo) {
					if (value instanceof DataLine.Info dataLineInfo) {
                        AudioFormat[] supportedFormats = dataLineInfo.getFormats();
						for (AudioFormat af : supportedFormats)
							Logging.RESOURCEHANDLER_SOUND.error(af);
					}
				}
				Logging.RESOURCEHANDLER_SOUND.error("-target:");
                for (Line.Info value : tinfo) {
                    if (value instanceof DataLine.Info dataLineInfo) {
                        AudioFormat[] supportedFormats = dataLineInfo.getFormats();
                        for (AudioFormat af : supportedFormats)
                            Logging.RESOURCEHANDLER_SOUND.error(af);
                    }
                }

				return;
			}

			/*Clip clip = (Clip) AudioSystem.getLine(info);
			clip.open(AudioSystem.getAudioInputStream(in));

			clip.addLineListener(e -> {
				if (e.getType() == LineEvent.Type.STOP) {
					clip.flush();
					clip.setFramePosition(0);
				}
			});*/

//			int source = alGenSources();
//			int buffer = alGenBuffers();
//			AudioInputStream stream = AudioSystem.getAudioInputStream(in);
//			ByteBuffer bytes = ByteBuffer.wrap(stream.readAllBytes());
//			alBufferData(buffer, format.getChannels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, bytes, (int)format.getSampleRate());
//			alSourcei(source, AL_BUFFER, buffer);
//			sounds.put(key, new Sound(source));

		} catch (/*LineUnavailableException | */UnsupportedAudioFileException | IOException e) {
			CrashHandler.errorHandle(e, new CrashHandler.ErrorInfo("Audio Could not Load", CrashHandler.ErrorInfo.ErrorType.REPORT,
				String.format("Could not load audio: %s in pack: %s", key, pack)));
		}
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

//		if (clip.isRunning() || clip.isActive())
//			clip.stop();
//
//		clip.start();
		alSourcePlay(source);
	}

	public void loop(boolean start) {
		if (!(boolean) Settings.get("sound") || source == NULL) return;

		alSourcei(source, AL_LOOPING, start ? 1 : 0);
		if (start)
			alSourcePlay(source);
//			clip.loop(Clip.LOOP_CONTINUOUSLY);
		else
			alSourceStop(source);
//			clip.stop();
	}
}
