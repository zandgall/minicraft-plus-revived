package minicraft.level;

import minicraft.core.io.Settings;
import minicraft.gfx.Point;

import minicraft.level.biome.Biome;
import minicraft.level.biome.Biomes;
import minicraft.level.noise.LevelNoise;

import minicraft.core.Game;
import minicraft.core.World;

import minicraft.level.tile.Tile;
import minicraft.level.tile.Tiles;
import minicraft.level.structure.Structures;
import minicraft.util.Logging;
import minicraft.util.Simplex;
import org.tinylog.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class LevelPreview {

	static void generate(Level level, int w, int h, boolean structures) {
        for (int i = 0; i < w / ChunkManager.CHUNK_SIZE; i++)
            for (int j = 0; j < h / ChunkManager.CHUNK_SIZE; j++) {
				int stage;
				do {
	                level.getChunkGenerator().advanceChunk(i, j);
					stage = level.chunkManager.getChunkStage(i, j);
				}while(stage < ChunkManager.CHUNK_STAGE_ASSIGNED_TILES ||
					(level.chunkManager.getChunkStage(i, j) < ChunkManager.CHUNK_STAGE_DONE && structures));
			}
    }

	private static final HashMap<Tile, Integer> colorMap = new HashMap<>();
	static void initColorMap() {
		colorMap.put(Tiles.get("water"), 0x000080);
		colorMap.put(Tiles.get("iron Ore"), 0x000080);
        colorMap.put(Tiles.get("gold Ore"), 0x000080);
        colorMap.put(Tiles.get("gem Ore"), 0x000080);
        colorMap.put(Tiles.get("grass"), 0x208020);
        colorMap.put(Tiles.get("rock"), 0xa0a0a0);
        colorMap.put(Tiles.get("dirt"), 0x604040);
        colorMap.put(Tiles.get("sand"), 0xa0a040);
        colorMap.put(Tiles.get("Stone Bricks"), 0xa0a040);
        colorMap.put(Tiles.get("tree"), 0x003000);
        colorMap.put(Tiles.get("flower"), 0xa050ff);
        colorMap.put(Tiles.get("cactus"), 0x105000);
        colorMap.put(Tiles.get("Obsidian Wall"), 0x0aa0a0);
        colorMap.put(Tiles.get("Obsidian"), 0x000000);
        colorMap.put(Tiles.get("lava"), 0xffff2020);
        colorMap.put(Tiles.get("cloud"), 0xa0a0a0);
        colorMap.put(Tiles.get("Stairs Down"), 0xff0000ff);
        colorMap.put(Tiles.get("Stairs Up"), 0xff00ffff);
        colorMap.put(Tiles.get("Cloud Cactus"), 0xffff00ff);
        colorMap.put(Tiles.get("Ornate Obsidian"), 0x000f0a);
        colorMap.put(Tiles.get("Raw Obsidian"), 0x0a0080);
        colorMap.put(Tiles.get("Lava Stone"), 0xffa500);
        colorMap.put(Tiles.get("Lava Stone Bricks"), 0xffff2020);
        colorMap.put(Tiles.get("Ashed Dirt"), 0xffa500);
	}

    public static void main(String[] args) {
        long worldSeed = 0x100;
        Biomes.initBiomeList();

        Random rand = new Random();

        // Fixes to get this method to work
        // AirWizard needs this in constructor
        Game.gameDir = "";

        Tiles.initTileList();
		Structures.initStructureList();
		Biomes.initBiomeList();
		initColorMap();
        // End of fixes

        int idx = -2;

        int[] maplvls = new int[args.length];
        boolean valid = true;
        if (maplvls.length > 0) {
            for (int i = 0; i < args.length; i++) {
                try {
                    int lvlnum = Integer.parseInt(args[i]);
                    maplvls[i] = lvlnum;
                } catch (Exception ex) {
                    valid = false;
                    break;
                }
            }
        } else {
            valid = false;
        }

        if (!valid) {
            maplvls = new int[1];
        }

        int lvl = maplvls[idx++ % maplvls.length];

		boolean structures = false;

		Logging.WORLD.info("Assigning levels...");
		for(int i = 0; i < Game.levels.length; i++)
			Game.levels[i] = new Level(1024, 1024, worldSeed, World.idxToDepth[i]);

        //noinspection InfiniteLoopStatement
        while (true) {
            int w = 1024;
            int h = 1024;

            //noinspection ConstantConditions
            if (lvl > 1 || lvl < -4) {
                continue;
            }

			Logging.WORLD.info("Generating chunks...");
			Level l = Game.levels[World.lvlIdx(lvl)];
	        LevelPreview.generate(l, w, h, structures);
			ChunkManager map = l.chunkManager;
			structures = false;

            if (map == null)
                continue;

			Logging.WORLD.info("Assigning colors...");
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = x + y * w;
					Tile t = map.getTile(x, y);
					if(colorMap.containsKey(t))
						pixels[i] = colorMap.get(t);
                }
            }
            img.setRGB(0, 0, w, h, pixels, 0, w);

            try {
                ImageIO.write(img, "png", new File("./seed" + worldSeed + ".png"));
            } catch (IOException ignored) {}

            int op = JOptionPane.showOptionDialog(null, null, "LevelViewer " + Game.VERSION + " | Seed: " + worldSeed + " | Level: " + lvl, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    new ImageIcon(img.getScaledInstance(512, 512, Image.SCALE_AREA_AVERAGING)),
                    new String[]{"Next Seed", "Prev Seed", "Next Level", "Prev Level", "Min (0x100)", "Max (0xAAFF20)", "Random Seed", "Gen Structures", "Close"}, "Next");

			long seedChangeCheck = worldSeed;
            if (op == 0) {
                worldSeed++;
            } else if (op == 1) {
                worldSeed--;
            } else if (op == 2) {
                lvl--;
                if (lvl < -4) {
                    lvl = 1;
                }
            } else if (op == 3) {
                lvl++;
                if (lvl > 1) {
                    lvl = -4;
                }
            } else if (op == 4) {
                worldSeed = 0x100;
            } else if (op == 5) {
                worldSeed = 0xAAFF20;
            } else if (op == 6) {
                worldSeed = rand.nextLong();
			} else if (op == 7) {
				structures = true;
            } else if (op == 8) {
                return;
            } else {
                worldSeed++;
            }

			// Different seed means to reassign levels
			if(worldSeed != seedChangeCheck) {
				Logging.WORLD.info("Assigning levels...");
				for(int i = 0; i < Game.levels.length; i++)
					Game.levels[i] = new Level(1024, 1024, worldSeed, World.idxToDepth[i]);
			}
        }
    }

    // Used to easily interface with a list of chunks
}
