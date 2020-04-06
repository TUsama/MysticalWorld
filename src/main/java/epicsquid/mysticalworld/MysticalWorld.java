package epicsquid.mysticalworld;

import com.tterrag.registrate.util.nullness.NonNullSupplier;
import epicsquid.mysticallib.data.RecipeProvider;
import epicsquid.mysticalworld.config.ConfigManager;
import epicsquid.mysticalworld.events.TooltipHandler;
import epicsquid.mysticalworld.events.mappings.Remaps;
import epicsquid.mysticalworld.init.*;
import epicsquid.mysticalworld.loot.conditions.HasHorns;
import epicsquid.mysticalworld.loot.conditions.IsColor;
import epicsquid.mysticalworld.loot.conditions.IsLava;
import epicsquid.mysticalworld.loot.conditions.IsObsidian;
import epicsquid.mysticalworld.registrate.CustomRegistrate;
import epicsquid.mysticalworld.setup.ClientSetup;
import epicsquid.mysticalworld.setup.ModSetup;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("mysticalworld")
public class MysticalWorld {
  public static final Logger LOG = LogManager.getLogger();
  public static final String MODID = "mysticalworld";

  public static CustomRegistrate REGISTRATE;
  public static RecipeProvider RECIPES = new RecipeProvider(MODID);

  public static final ItemGroup ITEM_GROUP = new ItemGroup("mysticalworld") {
    @Override
    public ItemStack createIcon() {
      return new ItemStack(ModItems.CARAPACE.get());
    }
  };

  public static ModSetup setup = new ModSetup();

  public MysticalWorld() {
    REGISTRATE = CustomRegistrate.create(MODID);
    REGISTRATE.itemGroup(() -> ITEM_GROUP);
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigManager.COMMON_CONFIG);
    ConfigManager.loadConfig(ConfigManager.COMMON_CONFIG, FMLPaths.CONFIGDIR.get().resolve(MODID + "-common.toml"));

    IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

    LootConditionManager.registerCondition(new HasHorns.Serializer());
    LootConditionManager.registerCondition(new IsColor.Serializer());
    LootConditionManager.registerCondition(new IsLava.Serializer());
    LootConditionManager.registerCondition(new IsObsidian.Serializer());

    // This is literally to ensure that they static declarations are loaded
    // before we attempt to actually register stuff.
    ModBlocks.load();
    ModItems.load();
    ModEntities.load();
    ModRecipes.load();
    ModModifiers.load();
    ModSounds.load();
    ModEffects.load();
    ModLang.load();
    ModTags.load();

    DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
      modBus.addListener(ClientSetup::init);
      MinecraftForge.EVENT_BUS.addListener(TooltipHandler::onTooltip);
    });

    REGISTRATE = CustomRegistrate.create(MODID);
    REGISTRATE.itemGroup(NonNullSupplier.of(() -> ITEM_GROUP));

    modBus.addListener(setup::init);

    MinecraftForge.EVENT_BUS.addListener(setup::serverStarting);
    MinecraftForge.EVENT_BUS.addListener(setup::serverAboutToStart);

    MinecraftForge.EVENT_BUS.addGenericListener(Block.class, Remaps::remapBlockEvent);
    MinecraftForge.EVENT_BUS.addGenericListener(Item.class, Remaps::remapItemEvent);
    MinecraftForge.EVENT_BUS.addGenericListener(EntityType.class, Remaps::remapEntityEvent);

    setup.registerListeners();
  }
}
