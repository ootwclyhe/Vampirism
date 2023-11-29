package de.teamlapen.vampirism.world.gen.structure.hunteroutpost;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import de.teamlapen.vampirism.REFERENCE;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import static de.teamlapen.vampirism.world.gen.structure.PoolExtensions.single;

public class HunterOutpostPools {

    public static final ResourceKey<StructureTemplatePool> HORSES = ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(REFERENCE.MODID, "hunter_outpost/horses"));
    public static final ResourceKey<StructureTemplatePool> TRAINER = ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(REFERENCE.MODID, "hunter_outpost/trainer"));
    public static final ResourceKey<StructureTemplatePool> TENTS = ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(REFERENCE.MODID, "hunter_outpost/tents"));
    public static final ResourceKey<StructureTemplatePool> TRAINING_DUMMIES = ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(REFERENCE.MODID, "hunter_outpost/training_dummies"));
    public static final ResourceKey<StructureTemplatePool> STABLES = ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(REFERENCE.MODID, "hunter_outpost/stables"));
    public static final ResourceKey<StructureTemplatePool> TOOL_SMITH = ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(REFERENCE.MODID, "hunter_outpost/tool_smith"));
    public static final ResourceKey<StructureTemplatePool> TOWER = ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(REFERENCE.MODID, "hunter_outpost/tower"));
    public static final ResourceKey<StructureTemplatePool> ALCHEMY = ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(REFERENCE.MODID, "hunter_outpost/alchemy"));
    public static final ResourceKey<StructureTemplatePool> FLAG = ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(REFERENCE.MODID, "hunter_outpost/flag"));

    public static void bootstrap(BootstapContext<StructureTemplatePool> context) {
        PlainsHunterOutpostPools.bootstrap(context);
        DesertHunterOutpostPools.bootstrap(context);
        VampireForestHunterOutpostPools.bootstrap(context);
        BadlandsHunterOutpostPools.bootstrap(context);

        HolderGetter<StructureTemplatePool> templatePools = context.lookup(Registries.TEMPLATE_POOL);
        Holder<StructureTemplatePool> empty = templatePools.getOrThrow(Pools.EMPTY);

        context.register(HORSES, new StructureTemplatePool(empty, ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/common/animals/horses_1"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/horses_2"), 1), Pair.of(StructurePoolElement.legacy("village/common/animals/horses_3"), 1)), StructureTemplatePool.Projection.RIGID));
        context.register(TRAINER, new StructureTemplatePool(empty, ImmutableList.of(Pair.of(single("village/entities/hunter_trainer"), 1), Pair.of(EmptyPoolElement.empty(), 1)), StructureTemplatePool.Projection.RIGID));
        context.register(TENTS, new StructureTemplatePool(empty, ImmutableList.of(Pair.of(single("hunter_outpost/common/tent_1"), 1), Pair.of(single("hunter_outpost/common/tent_2"), 1), Pair.of(single("hunter_outpost/common/tent_3"), 1), Pair.of(single("hunter_outpost/common/tent_4"), 1)), StructureTemplatePool.Projection.RIGID));
        context.register(TRAINING_DUMMIES, new StructureTemplatePool(empty, ImmutableList.of(Pair.of(single("hunter_outpost/common/training_dummies"), 1)), StructureTemplatePool.Projection.RIGID));
        context.register(STABLES, new StructureTemplatePool(empty, ImmutableList.of(Pair.of(single("hunter_outpost/common/stables"), 1)), StructureTemplatePool.Projection.RIGID));
        context.register(TOOL_SMITH, new StructureTemplatePool(empty, ImmutableList.of(Pair.of(single("hunter_outpost/common/tool_smith"), 1)), StructureTemplatePool.Projection.RIGID));
        context.register(TOWER, new StructureTemplatePool(empty, ImmutableList.of(Pair.of(single("hunter_outpost/common/tower"), 1)), StructureTemplatePool.Projection.RIGID));
        context.register(ALCHEMY, new StructureTemplatePool(empty, ImmutableList.of(Pair.of(single("hunter_outpost/common/alchemy"), 1)), StructureTemplatePool.Projection.RIGID));
        context.register(FLAG, new StructureTemplatePool(empty, ImmutableList.of(Pair.of(single("hunter_outpost/common/flag"), 1)), StructureTemplatePool.Projection.RIGID));
    }
}
