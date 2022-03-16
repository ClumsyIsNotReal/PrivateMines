package me.untouchedodin0.privatemines.iterator;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockType;
import me.untouchedodin0.privatemines.storage.SchematicStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SchematicIterator {

    SchematicStorage schematicStorage;
    BlockVector3 spawn;
    BlockVector3 corner1;
    BlockVector3 corner2;

    public SchematicIterator(SchematicStorage storage) {
        this.schematicStorage = storage;
    }

    public MineBlocks findRelativePoints(File file) {
        MineBlocks mineBlocks = new MineBlocks();
        mineBlocks.corners = new BlockVector3[2];

        Clipboard clipboard;
        ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(file);

        BlockVector3 zero = BlockVector3.ZERO;

        if (clipboardFormat != null) {
            try (ClipboardReader clipboardReader = clipboardFormat.getReader(new FileInputStream(file))) {
                clipboard = clipboardReader.read();
                Bukkit.getLogger().info("Clipboard: " + clipboard);
                Bukkit.getLogger().info("Clipboard Region: " + clipboard.getRegion());

                Material cornerMaterial = Material.POWERED_RAIL;
                Material spawnMaterial = Material.SPONGE;
                BlockType cornerType = BlockType.REGISTRY.get(cornerMaterial.getKey().getKey());
                BlockType spawnType = BlockType.REGISTRY.get(spawnMaterial.getKey().getKey());

                Bukkit.getLogger().info("spawn type: " + spawnType);
                Bukkit.getLogger().info("corner type: " + cornerType);

                clipboard.getRegion().forEach(blockVector3 -> {
                    BlockType blockType = clipboard.getBlock(blockVector3).getBlockType();

                    if (blockType.equals(cornerType)) {
                        if (corner1 == null) {
                            Bukkit.getLogger().info("powered rail " + blockVector3.toParserString());
                            corner1 = zero.add(blockVector3.getX(), blockVector3.getY(), blockVector3.getZ()); //BlockVector3.at(blockVector3.getX(), blockVector3.getY(), blockVector3.getZ());
                        } else if (corner2 == null) {
                            Bukkit.getLogger().info("powered rail " + blockVector3.toParserString());
                            corner2 = zero.add(blockVector3.getX(), blockVector3.getY(), blockVector3.getZ()); //BlockVector3.at(blockVector3.getX(), blockVector3.getY(), blockVector3.getZ());
                        }
                    } else if (blockType.equals(spawnType)) {
                        if (spawn == null) {
                            Bukkit.getLogger().info("Sponge: " + blockVector3.toParserString());
                            spawn = zero.add(blockVector3.getX(), blockVector3.getY(), blockVector3.getZ()); //spawn = BlockVector3.at(blockVector3.getX(), blockVector3.getY(), blockVector3.getZ());
                        }
                    }
                });

                Bukkit.getLogger().info("corner1: " + corner1);
                Bukkit.getLogger().info("corner2: " + corner2);
                Bukkit.getLogger().info("spawn: " + spawn);
                mineBlocks.spawnLocation = spawn;
                mineBlocks.corners[0] = corner1;

                //ignore for now
                mineBlocks.corners[1] = corner2;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mineBlocks;
    }

    public static class MineBlocks {
        BlockVector3 spawnLocation;
        BlockVector3[] corners;

        public BlockVector3 getSpawnLocation() {
            return spawnLocation;
        }

        public BlockVector3[] getCorners() {
            return corners;
        }

        public BlockVector3 getCorner1() {
            return corners[0];
        }

        public BlockVector3 getCorner2() {
            return corners[1];
        }
    }
}
