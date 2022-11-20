package minicraft.level.tile;

import minicraft.core.io.Sound;
import minicraft.entity.Direction;
import minicraft.entity.Entity;
import minicraft.entity.mob.Player;
import minicraft.gfx.Sprite;
import minicraft.gfx.SpriteAnimation;
import minicraft.gfx.SpriteLinker;
import minicraft.item.Item;
import minicraft.item.Items;
import minicraft.item.ToolItem;
import minicraft.level.Level;

public class BossFloorTile extends Tile {
	protected Material type;
	private SpriteAnimation sprite;

	protected BossFloorTile(Material type) {
		super((type == Material.Obsidian ? "Obsidian" : type.name()) + " Boss Floor", null);
		this.type = type;
		maySpawn = true;
		if (type == Material.Obsidian) {
			sprite = new SpriteAnimation(SpriteLinker.SpriteType.Tile, "ornate_obsidian");;
		}
		super.sprite = sprite;
	}

	public boolean interact(Level level, int xt, int yt, Player player, Item item, Direction attackDir) {
		if (item instanceof ToolItem) {
			ToolItem tool = (ToolItem) item;
			if (tool.type == type.getRequiredTool()) {
				if (player.payStamina(4 - tool.level) && tool.payDurability()) {
					if (level.depth == 1) {
						level.setTile(xt, yt, Tiles.get("Cloud"));
					} else {
						level.setTile(xt, yt, Tiles.get("Hole"));
					}
					Sound.play("monsterhurt");
					return true;
				}
			}
		}
		return false;
	}

	public boolean mayPass(Level level, int x, int y, Entity e) {
		return true;
	}
}
