package epicsquid.mysticalworld.world;

import epicsquid.mysticalworld.MysticalWorld;
import epicsquid.mysticalworld.config.ConfigManager;
import epicsquid.mysticalworld.world.tree.WorldGenBurntTree;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorldGeneratorTrees implements IWorldGenerator {
  public static List<BiomeDictionary.Type> invalidTypes = new ArrayList<>();

  @Override
  public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
    // TODO: Move into config

    if (world.getWorldType() == WorldType.FLAT) return;

    IntOpenHashSet blacklist = new IntOpenHashSet(ConfigManager.ConfigMysticalWorldBurntTrees.blacklist);
    IntOpenHashSet whitelist = new IntOpenHashSet(ConfigManager.ConfigMysticalWorldBurntTrees.whitelist);

    int dim = world.provider.getDimension();
    if (blacklist.contains(dim)) {
      return;
    } else if (!whitelist.contains(dim)) {
      return;
    }

    int xPos = chunkX * 16 + 8;
    int zPos = chunkZ * 16 + 8;

    BlockPos chunkPos = new BlockPos(xPos, 0, zPos);

    Biome biome = world.getChunk(chunkPos).getBiome(chunkPos, world.getBiomeProvider());

    if (ConfigManager.burntTrees.chance == -1 || random.nextInt(ConfigManager.safeInt(ConfigManager.burntTrees.chance)) != 0) {
      return;
    }

    if (invalidTypes.isEmpty()) {
      for (String type : ConfigManager.burntTrees.excludedBiomes) {
        invalidTypes.add(BiomeDictionary.Type.getType(type));
      }
    }

    if (invalidTypes.stream().anyMatch(type -> BiomeDictionary.hasType(biome, type))) {
      return;
    }

    for (int i = 0; i < ConfigManager.burntTrees.attempts; i++) {
      int x = chunkPos.getX() + (5 - random.nextInt(11));
      int z = chunkPos.getZ() + (5 - random.nextInt(11));
      int y = world.getHeight(x, z);
      BlockPos pos = new BlockPos(x, y, z);

      if (Blocks.SAPLING.canPlaceBlockAt(world, pos)) {
        WorldGenBurntTree burnTree = new WorldGenBurntTree(false);
        try {
          burnTree.generate(world, random, pos);
        } catch (NullPointerException e) {
          MysticalWorld.logger.debug("Failed to create a charred tree at '" + pos.toString() + "' due to a null-pointer:", e);
        }
        break;
      }
    }
  }
}
