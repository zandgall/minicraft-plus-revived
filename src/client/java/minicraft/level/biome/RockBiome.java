package minicraft.level.biome;

import minicraft.level.ChunkManager;
import minicraft.level.noise.LevelNoise;
import minicraft.level.tile.Tiles;
import minicraft.level.structure.Structures;

public class RockBiome extends Biome {
	public RockBiome() {
		super(0, 1.0f, 0, 0.6f);
		structures.add(Structures.skyStairsUp);
		structures.add(Structures.surfaceStairsDown);
	}

	public void generate(ChunkManager map, int x, int y) {
		LevelNoise noise = map.getTileNoise(x, y);
		double dirtStreak = noise.getScale32Noise(x, y, 0) - noise.getScale32Noise(x, y, 1);
		dirtStreak = Math.abs(Math.max(dirtStreak, noise.getScale16Noise(x, y, 0)));
		double dirtCrevace = Math.abs(noise.getScale16Noise(x, y, 1));
		if(noise.getHeight(x, y) > 0.4f && dirtStreak > 0.3 && dirtCrevace > 0.15)
			map.setTile(x, y, Tiles.get("rock"), 0);
		else
			map.setTile(x, y, Tiles.get("dirt"), 0);
	}
}
