package minicraft.core.io;

import minicraft.core.CrashHandler;
import minicraft.util.Logging;
import org.jetbrains.annotations.Nullable;

import com.jogamp.openal.*;
import com.jogamp.openal.util.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class Sound {
	// Creates sounds from their respective files
	private static final HashMap<String, Sound> sounds = new HashMap<>();

	private static final AL al = ALFactory.getAL();
	private final int source;

	static {
		ALut.alutInit();
		al.alGetError();
	}

	private Sound(int source) {
		this.source = source;
	}

	public static void resetSounds() {
		sounds.clear();
	}

	public static void loadSound(String key, InputStream in, String pack) {
		int[] buffer = new int[1];
		int[] source = new int[1];

		int[] format = new int[1];
		int[] size = new int[1];
		ByteBuffer[] data = new ByteBuffer[1];
		int[] freq = new int[1];
		int[] loop = new int[1];
		al.alGenBuffers(1, buffer, 0);
		ALut.alutLoadWAVFile(in, format, data, size, freq, loop);
		al.alBufferData(buffer[0], format[0], data[0], size[0], freq[0]);

		al.alGenSources(1, source, 0);
		al.alSourcei(source[0], AL.AL_BUFFER, buffer[0]);
		sounds.put(key, new Sound(source[0]));
	}

	/**
	 * Recommend {@link #play(String)}.
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

	// Need separate way to detect how many loops a sound has made before we can implement loo

	///**
	// * This method does safe check for {@link #loop(boolean)}.
	// */
//	public static void loop(String key, boolean start) {
//		Sound sound = sounds.get(key);
//		if (sound != null) sound.loop(count);
//	}

	public void play() {
		if (!(boolean) Settings.get("sound")) return;
		al.alSourcePlay(source);
	}

	// Need separate way to detect how many loops a sound has made
//	public void loop(int count) {
//		if (!(boolean) Settings.get("sound")) return;
//	}
}
