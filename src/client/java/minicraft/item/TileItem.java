package minicraft.item;

import minicraft.core.Game;
import minicraft.core.io.Localization;
import minicraft.core.io.Sound;
import minicraft.entity.Direction;
import minicraft.entity.mob.Player;
import minicraft.gfx.SpriteLinker;
import minicraft.gfx.SpriteLinker.LinkedSprite;
import minicraft.gfx.SpriteLinker.SpriteType;
import minicraft.level.Level;
import minicraft.level.tile.Tile;
import minicraft.level.tile.Tiles;
import minicraft.screen.AchievementsDisplay;
import minicraft.util.AdvancementElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TileItem extends StackableItem {

	protected static ArrayList<Item> getAllInstances() {
		ArrayList<Item> items = new ArrayList<>();

		/// TileItem sprites all have 1x1 sprites.
		items.add(new TileItem("Acorn", new LinkedSprite(SpriteType.Item, "acorn"), new TileModel("tree Sapling"), "grass"));
		items.add(new TileItem("Dirt", new LinkedSprite(SpriteType.Item, "dirt"), new TileModel("dirt"), "hole", "water", "lava"));
		items.add(new TileItem("Natural Rock", new LinkedSprite(SpriteType.Item, "stone"), new TileModel("rock"), "hole", "dirt", "sand", "grass", "path", "water", "lava"));

		items.add(new TileItem("Plank", new LinkedSprite(SpriteType.Item, "plank"), new TileModel("Wood Planks"), "hole", "water", "cloud"));
		items.add(new TileItem("Plank Wall", new LinkedSprite(SpriteType.Item, "plank_wall"), new TileModel("Wood Wall"), "Wood Planks"));
		items.add(new TileItem("Wood Door", new LinkedSprite(SpriteType.Item, "wood_door"), new TileModel("Wood Door"), "Wood Planks"));
		items.add(new TileItem("Stone", new LinkedSprite(SpriteType.Item, "stone"), new TileModel("Stone"), "hole", "water", "cloud", "lava"));
		items.add(new TileItem("Stone Brick", new LinkedSprite(SpriteType.Item, "stone_brick"), new TileModel("Stone Bricks"), "hole", "water", "cloud", "lava"));
		items.add(new TileItem("Ornate Stone", new LinkedSprite(SpriteType.Item, "stone_brick"), new TileModel("Ornate Stone"), "hole", "water", "cloud", "lava"));
		items.add(new TileItem("Stone Wall", new LinkedSprite(SpriteType.Item, "stone_wall"), new TileModel("Stone Wall"), "Stone Bricks"));
		items.add(new TileItem("Stone Door", new LinkedSprite(SpriteType.Item, "stone_wall"), new TileModel("Stone Door"), "Stone Bricks"));
		items.add(new TileItem("Raw Obsidian", new LinkedSprite(SpriteType.Item, "obsidian"), new TileModel("Raw Obsidian"), "hole", "water", "cloud", "lava"));
		items.add(new TileItem("Obsidian Brick", new LinkedSprite(SpriteType.Item, "obsidian_brick"), new TileModel("Obsidian"), "hole", "water", "cloud", "lava"));
		items.add(new TileItem("Ornate Obsidian", new LinkedSprite(SpriteType.Item, "obsidian_brick"), new TileModel("Ornate Obsidian"),"hole", "water", "cloud", "lava"));
		items.add(new TileItem("Obsidian Wall", new LinkedSprite(SpriteType.Item, "obsidian_wall"), new TileModel("Obsidian Wall"), "Obsidian"));
		items.add(new TileItem("Obsidian Door", new LinkedSprite(SpriteType.Item, "obsidian_door"), new TileModel("Obsidian Door"), "Obsidian"));

		items.add(new TileItem("Sand", new LinkedSprite(SpriteType.Item, "sand"), new TileModel("sand"), "hole", "water", "lava"));
		items.add(new TileItem("Cactus", new LinkedSprite(SpriteType.Item, "cactus"), new TileModel("cactus Sapling"), "sand"));
		items.add(new TileItem("Cloud", new LinkedSprite(SpriteType.Item, "cloud"), new TileModel("cloud"), "Infinite Fall"));

		TileModel.TileDataGetter seedPlanting = (model1, target, level, xt, yt, player, attackDir) -> {
			AchievementsDisplay.setAchievement("minicraft.achievement.plant_seed", true);
			return TileModel.KEEP_DATA.getTileData(model1, target, level, xt, yt, player, attackDir);
		};
		items.add(new TileItem("Wheat Seeds", new LinkedSprite(SpriteType.Item, "seed"), new TileModel("wheat", seedPlanting), "farmland"));
		items.add(new TileItem("Potato", new LinkedSprite(SpriteType.Item, "potato"), new TileModel("potato", TileModel.KEEP_DATA), "farmland"));
		items.add(new TileItem("Carrot", new LinkedSprite(SpriteType.Item, "carrot"), new TileModel("carrot", TileModel.KEEP_DATA), "farmland"));
		items.add(new TileItem("Tomato Seeds", new LinkedSprite(SpriteType.Item, "seed"), new TileModel("tomato", seedPlanting), "farmland"));
		items.add(new TileItem("Heavenly Berries", new LinkedSprite(SpriteType.Item, "heavenly_berries"), new TileModel("heavenly berries", TileModel.KEEP_DATA), "farmland"));
		items.add(new TileItem("Hellish Berries", new LinkedSprite(SpriteType.Item, "hellish_berries"), new TileModel("hellish berries", TileModel.KEEP_DATA), "farmland"));
		items.add(new TileItem("Grass Seeds", new LinkedSprite(SpriteType.Item, "seed"), new TileModel("grass"), "dirt"));

		items.add(new TileItem("Rose", new LinkedSprite(SpriteType.Item, "rose"), new TileModel("rose"), "grass"));
		items.add(new TileItem("Oxeye Daisy", new LinkedSprite(SpriteType.Item, "oxeye_daisy"), new TileModel("oxeye daisy"), "grass"));
		items.add(new TileItem("Sunflower", new LinkedSprite(SpriteType.Item, "sunflower"), new TileModel("sunflower"), "grass"));
		items.add(new TileItem("Allium", new LinkedSprite(SpriteType.Item, "allium"), new TileModel("allium"), "grass"));
		items.add(new TileItem("Blue Orchid", new LinkedSprite(SpriteType.Item, "blue_orchid"), new TileModel("blue orchid"), "grass"));
		items.add(new TileItem("Cornflower", new LinkedSprite(SpriteType.Item, "cornflower"), new TileModel("cornflower"), "grass"));
		items.add(new TileItem("Dandelion", new LinkedSprite(SpriteType.Item, "dandelion"), new TileModel("dandelion"), "grass"));
		items.add(new TileItem("Hydrangea", new LinkedSprite(SpriteType.Item, "hydrangea"), new TileModel("hydrangea"), "grass"));
		items.add(new TileItem("Iris", new LinkedSprite(SpriteType.Item, "iris"), new TileModel("iris"), "grass"));
		items.add(new TileItem("Orange Tulip", new LinkedSprite(SpriteType.Item, "orange_tulip"), new TileModel("orange tulip"), "grass"));
		items.add(new TileItem("Pink Tulip", new LinkedSprite(SpriteType.Item, "pink_tulip"), new TileModel("pink tulip"), "grass"));
		items.add(new TileItem("Red Tulip", new LinkedSprite(SpriteType.Item, "red_tulip"), new TileModel("red tulip"), "grass"));
		items.add(new TileItem("White Tulip", new LinkedSprite(SpriteType.Item, "white_tulip"), new TileModel("white tulip"), "grass"));
		items.add(new TileItem("Peony", new LinkedSprite(SpriteType.Item, "peony"), new TileModel("peony"), "grass"));
		items.add(new TileItem("Periwinkle", new LinkedSprite(SpriteType.Item, "periwinkle"), new TileModel("periwinkle"), "grass"));
		items.add(new TileItem("Pink Lily", new LinkedSprite(SpriteType.Item, "pink_lily"), new TileModel("pink lily"), "grass"));
		items.add(new TileItem("White Lily", new LinkedSprite(SpriteType.Item, "white_lily"), new TileModel("white lily"), "grass"));
		items.add(new TileItem("Poppy", new LinkedSprite(SpriteType.Item, "poppy"), new TileModel("poppy"), "grass"));
		items.add(new TileItem("Violet", new LinkedSprite(SpriteType.Item, "violet"), new TileModel("violet"), "grass"));

		// Creative mode available tiles:
		items.add(new TileItem("Farmland", SpriteLinker.missingTexture(SpriteType.Item), new TileModel("farmland"), "dirt", "grass", "hole"));
		items.add(new TileItem("hole", SpriteLinker.missingTexture(SpriteType.Item), new TileModel("hole"), "dirt", "grass"));
		items.add(new TileItem("lava", SpriteLinker.missingTexture(SpriteType.Item), new TileModel("lava"), "dirt", "grass", "hole"));
		items.add(new TileItem("path", SpriteLinker.missingTexture(SpriteType.Item), new TileModel("path"), "dirt", "grass", "hole"));
		items.add(new TileItem("water", SpriteLinker.missingTexture(SpriteType.Item), new TileModel("water"), "dirt", "grass", "hole"));

		return items;
	}

	public final @Nullable TileModel model;
	public final List<String> validTiles;

	protected TileItem(String name, LinkedSprite sprite, TileModel model, String... validTiles) {
		this(name, sprite, 1, model, Arrays.asList(validTiles));
	}
	protected TileItem(String name, LinkedSprite sprite, int count, TileModel model, String... validTiles) {
		this(name, sprite, count, model, Arrays.asList(validTiles));
	}
	protected TileItem(String name, LinkedSprite sprite, int count, @Nullable TileModel model, List<String> validTiles) {
		super(name, sprite, count);
		this.model = model;
		this.validTiles = new ArrayList<>();
		for (String tile: validTiles)
			 this.validTiles.add(tile.toUpperCase());
	}

	public static class TileModel {
		public static final TileDataGetter DEFAULT_DATA = ((model, target, level, xt, yt, player, attackDir) -> model.getDefaultData());
		public static final TileDataGetter KEEP_DATA = ((model, target, level, xt, yt, player, attackDir) -> level.getData(xt, yt));

		public final @NotNull String tile;
		public final TileDataGetter data;

		@FunctionalInterface
		interface TileDataGetter {
			int getTileData(Tile model, Tile target, Level level, int xt, int yt, Player player, Direction attackDir);
		}

		public TileModel(String tile) { this(tile, DEFAULT_DATA); }
		public TileModel(String tile, TileDataGetter data) {
			this.tile = tile.toUpperCase();
			this.data = data;
		}

		public static Tile getTile(@Nullable TileModel model) {
			return model == null ? Tiles.get(0) : Tiles.get(model.tile);
		}

		public static int getTileData(@Nullable TileModel model, Tile tile, Tile target, Level level, int xt, int yt, Player player, Direction attackDir) {
			if (model == null) return DEFAULT_DATA.getTileData(tile, target, level, xt, yt, player, attackDir);
			return model.data.getTileData(tile, target, level, xt, yt, player, attackDir);
		}
	}

	public boolean interactOn(Tile tile, Level level, int xt, int yt, Player player, Direction attackDir) {
		for (String tilename : validTiles) {
			if (tile.matches(level.getData(xt, yt), tilename)) {
				Tile t = TileModel.getTile(model);
				level.setTile(xt, yt, t, TileModel.getTileData(model, t, tile, level, xt, yt, player, attackDir));
				AdvancementElement.AdvancementTrigger.PlacedTileTrigger.INSTANCE.trigger(
					new AdvancementElement.AdvancementTrigger.PlacedTileTrigger.PlacedTileTriggerConditionHandler.PlacedTileTriggerConditions(
						this, level.getTile(xt, yt), level.getData(xt, yt), xt, yt, level.depth
					));

				Sound.play("craft");
				return super.interactOn(true);
			}
		}

		Logger.tag("TileItem").debug("{} cannot be placed on {}.", model, tile.name);

		if (model != null) {
			String note = "";
			if (model.tile.contains("WALL")) {
				note = Localization.getLocalized("minicraft.notification.invalid_placement", Tiles.getName(validTiles.get(0)));
			}
			else if (model.tile.contains("DOOR")) {
				note = Localization.getLocalized("minicraft.notification.invalid_placement", Tiles.getName(validTiles.get(0)));
			}
			else if ((model.tile.contains("BRICK") || model.tile.contains("PLANK") || model.tile.equals("STONE") || model.tile.contains("ORNATE"))) {
				note = Localization.getLocalized("minicraft.notification.dig_hole");
			}

			if (note.length() > 0) {
				Game.notifications.add(note);
			}
		}

		return super.interactOn(false);
	}

	@Override
	public boolean equals(Item other) {
		return super.equals(other) && (model == null || model.equals(((TileItem)other).model));
	}

	@Override
	public int hashCode() { return super.hashCode() + (model == null ? 0xFF123 : model.hashCode()); }

	public @NotNull TileItem copy() {
		return new TileItem(getName(), sprite, count, model, validTiles);
	}
}
