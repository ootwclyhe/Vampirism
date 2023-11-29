package de.teamlapen.vampirism.core;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import de.teamlapen.vampirism.REFERENCE;
import de.teamlapen.vampirism.mixin.LootTableAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * Handles loading mod loot tables as well as injecting pools into vanilla tables
 * Inspired (or almost copied) from @williewillus LootHandler for vazkii's Botania
 * <a href="https://github.com/williewillus/Botania/blob/07f68b37da9ad3a246b95c042cd6c10bd91698d1/src/main/java/vazkii/botania/common/core/loot/LootHandler.java">reference</a>
 */
public class ModLootTables {
    private final static Logger LOGGER = LogManager.getLogger();
    private static final Set<ResourceLocation> LOOT_TABLES = Sets.newHashSet();
    //chests
    public static final ResourceLocation CHEST_HUNTER_TRAINER = register("chests/village/hunter_trainer");
    public static final ResourceLocation CHEST_VAMPIRE_DUNGEON = register("chests/dungeon/vampire_dungeon");
    public static final ResourceLocation CHEST_VAMPIRE_HUT = register("chests/vampire_hut");
    public static final ResourceLocation CHEST_VAMPIRE_ALTAR = register("chests/vampire_altar");
    public static final ResourceLocation CHEST_CRYPT = register("chests/crypt");
    public static final ResourceLocation CHEST_HUNTER_OUTPOST_SMITH = register("chests/hunter_outpost_smith");
    public static final ResourceLocation CHEST_HUNTER_OUTPOST_TENT = register("chests/hunter_outpost_tent");
    public static final ResourceLocation CHEST_HUNTER_OUTPOST_ALCHEMY = register("chests/hunter_outpost_alchemy");
    public static final ResourceLocation CHEST_HUNTER_OUTPOST_TOWER_FOOD = register("chests/hunter_outpost_tower_food");
    public static final ResourceLocation CHEST_HUNTER_OUTPOST_TOWER_BASIC = register("chests/hunter_outpost_tower_basic");
    public static final ResourceLocation CHEST_HUNTER_OUTPOST_TOWER_SPECIAL = register("chests/hunter_outpost_tower_special");

    private static final Map<String, ResourceLocation> INJECTION_TABLES = Maps.newHashMap();
    //inject
    public static final ResourceLocation ABANDONED_MINESHAFT = registerInject("abandoned_mineshaft");
    public static final ResourceLocation JUNGLE_TEMPLE = registerInject("jungle_temple");
    public static final ResourceLocation STRONGHOLD_CORRIDOR = registerInject("stronghold_corridor");
    public static final ResourceLocation DESERT_PYRAMID = registerInject("desert_pyramid");
    public static final ResourceLocation STRONGHOLD_LIBRARY = registerInject("stronghold_library");
    private static int injected = 0;

    static @NotNull ResourceLocation registerInject(String resourceName) {
        ResourceLocation registryName = register("inject/" + resourceName);
        INJECTION_TABLES.put(resourceName, registryName);
        return registryName;
    }

    static @NotNull ResourceLocation register(@NotNull String resourceName) {
        return register(new ResourceLocation(REFERENCE.MODID, resourceName));
    }

    static @NotNull ResourceLocation register(@NotNull ResourceLocation resourceLocation) {
        LOOT_TABLES.add(resourceLocation);
        return resourceLocation;
    }

    public static @NotNull Set<ResourceLocation> getLootTables() {
        return ImmutableSet.copyOf(LOOT_TABLES);
    }


    @SubscribeEvent
    public static void onLootLoad(@NotNull LootTableLoadEvent event) {
        String prefix = "minecraft:chests/";
        String name = event.getName().toString();
        if (name.startsWith(prefix)) {
            String file = name.substring(name.indexOf(prefix) + prefix.length());
            if (INJECTION_TABLES.containsKey(file)) {
                try {
                    ((LootTableAccessor) event.getTable()).getPools().add(getInjectPool(file));
                    injected++;
                } catch (NullPointerException e) {
                    LOGGER.warn("Loottable {} is broken by some other mod. Cannot add Vampirism loot to it.", name);
                }
            }
        }
    }

    private static @NotNull LootPool getInjectPool(String entryName) {
        LootPoolEntryContainer.Builder<?> entryBuilder = LootTableReference.lootTableReference(INJECTION_TABLES.get(entryName)).setWeight(1);
        return LootPool.lootPool().setBonusRolls(UniformGenerator.between(0, 1)).setRolls(ConstantValue.exactly(1)).add(entryBuilder).build();
    }

    /**
     * @return 0 if alright, or the count of not injected loottables
     */
    public static int checkAndResetInsertedAll() {
        int i = injected;
        injected = 0;
        return Math.max(0, INJECTION_TABLES.size() - i); //Sponge loads the loot tables for all worlds at start. Which makes this test not work anyway.
    }
}