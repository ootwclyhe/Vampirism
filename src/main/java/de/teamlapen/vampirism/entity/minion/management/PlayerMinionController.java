package de.teamlapen.vampirism.entity.minion.management;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.minion.IMinionTask;
import de.teamlapen.vampirism.api.entity.player.ILordPlayer;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.entity.minion.MinionEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * Minions are represented by a {@link MinionData}. All important information except for position and similar should be stored in there.
 * {@link MinionEntity} are merely shells for this.
 * <p>
 * When the player has a free minion slot {@link PlayerMinionController#createNewMinionSlot(MinionData)} can be used to reserve one.
 * The minion slots are represented by their id (0-x). The minion slot holds the minion data and is not directly related to an entity.
 * <p>
 * A unclaimed minion slot (either a freshly reserved one or of a dead minion) can be claimed by a {@link MinionEntity} via {@link PlayerMinionController#claimMinionSlot(int)}.
 * If successful, it returns a token that grants the entity access to the minion data. It should be saved with the slot id in the minion entity.
 * Once the entity joins the world it checkout the minion data via {@link PlayerMinionController#checkoutMinion(int, int, MinionEntity)}.
 * While the minion is checked out, the minion controller knows about the entity id and dimension and can access it when necessary. Furthermore, no other minion can access the data (noone else should have the token anyway).
 * When the minion is unloaded it must checkin the data and save the token, so it can checkout the data on load again.
 * <p>
 * If the player calls the minions to them, the checkout (loaded) minions  are forced to checkin their data and are removed from the world. Then the tokens are invalidated and a fresh set of tokens is created for the new minion entities that access the same minion slots.
 * If a minion entity is stored in an unloaded chunk, it will try to checkout the minion data/slot again once loaded. However, if the player has recalled it in the meantime (which means a new shell entity has been created, the minion entity will fail to checkout the data and remove itself from the world.
 * <p>
 * <p>
 * - Recruit a new minion{@link PlayerMinionController#createNewMinionSlot(MinionData)}
 * - Associate a entity representation (real entity, nbt saved entity, ...) with the minion slot {@link PlayerMinionController#claimMinionSlot(int)}
 * - Checkout minion slot if entity is added to world. Can "fail" if minion has been reclaimed in the meantime. {@link PlayerMinionController#checkoutMinion(int, int, MinionEntity)}
 * - Checkin minion slot if entity is removed from world {@link PlayerMinionController#checkInMinion(int, int)}
 * - Release minion slot if minion dies {@link PlayerMinionController#markDeadAndReleaseMinionSlot(int, int)}
 */
public class PlayerMinionController implements INBTSerializable<CompoundNBT> {

    private final static Logger LOGGER = LogManager.getLogger();
    private final Random rng = new Random();
    @Nonnull
    private final MinecraftServer server;
    @Nonnull
    private final UUID lordID;
    private int maxMinions;
    @Nullable
    private IPlayableFaction<?> faction;

    @Nonnull
    private MinionInfo[] minions = new MinionInfo[0];
    @SuppressWarnings("unchecked")
    @Nonnull
    private Optional<Integer>[] minionTokens = new Optional[0];

    public PlayerMinionController(@Nonnull MinecraftServer server, @Nonnull UUID lordID) {
        this.server = server;
        this.lordID = lordID;
    }

    public void activateTask(int minionID, IMinionTask<?> task) {
        if (minionID >= minions.length) {
            LOGGER.warn("Trying to activate a task for a non-existent minion {}", minionID);
        } else {
            @Nullable
            IMinionTask.IMinionTaskDesc desc = task.activateTask(getLordPlayer().orElse(null), getMinionEntity(minions[minionID]).orElse(null), minions[minionID].data.getInventory());
            if (desc == null) {
                getLordPlayer().ifPresent(player -> player.sendStatusMessage(new TranslationTextComponent("text.vampirism.minion.could_not_activate"), false));
            } else {
                MinionData d = this.minions[minionID].data;
                d.switchTask(d.getCurrentTaskDesc().getTask(), d.getCurrentTaskDesc(), desc);
                this.contactMinion(minionID, MinionEntity::onTaskChanged);
            }
        }
    }

    /**
     * Contact the minion entity for the given slot if loaded
     */
    public void contactMinion(int slot, Consumer<MinionEntity<?>> entityConsumer) {
        if (slot < minions.length) {
            getMinionEntity(minions[slot]).ifPresent(entityConsumer);
        }
    }


    /**
     * Mark a minion as inactive
     * Don't use associated MinionData afterwards
     *
     * @param id    minion slot
     * @param token Previously received token
     */
    public void checkInMinion(int id, int token) {
        MinionInfo i = getMinionInfo(id, token);
        if (i != null) {
            i.checkin();
        }
    }

    /**
     * Request minion data for a previously claimed slot. Marks the respective minion slot as active
     * Returns null if
     * a) Minion already active
     * b) Minion dead
     * c) Token invalid
     *
     * @param id minion slot
     * @param token  Previously received token
     * @param entity wrapper entity
     */
    @Nullable
    public <T extends MinionData> T checkoutMinion(int id, int token, MinionEntity<T> entity) {
        MinionInfo i = getMinionInfo(id, token);
        if (i != null) {
            int entityId = entity.getEntityId();
            RegistryKey<World> dimension = entity.world.getDimensionKey();
            if (i.checkout(entityId, dimension)) {
                return (T) i.data;
            }
        }
        return null;
    }

    /**
     * Claim a minion slot.
     *
     * @param id slot id
     * @return the granted token or empty if slot either in use or not present
     */
    public Optional<Integer> claimMinionSlot(int id) {
        if (id < minionTokens.length) {
            if (!minionTokens[id].isPresent()) {
                int t = rng.nextInt();
                minionTokens[id] = Optional.of(t);
                return minionTokens[id];
            }
        }
        return Optional.empty();
    }

    /**
     * Contact all currently loaded (checked out) minions
     */
    public void contactMinions(Consumer<MinionEntity<?>> entityConsumer) {
        for (MinionInfo m : minions) {
            getMinionEntity(m).ifPresent(entityConsumer);
        }
    }

    public void createMinionEntityAtPlayer(int id, PlayerEntity p) {
        EntityType<? extends MinionEntity<?>> type = minions[id].minionType;
        if (type == null) {
            LOGGER.warn("Cannot create minion because type does not exist");
        } else {
            MinionEntity<?> m = type.create(p.getEntityWorld());
            if (faction == null || faction.isEntityOfFaction(m)) {
                LOGGER.warn("Specified minion entity is of wrong faction. This: {} Minion: {}", faction, m.getFaction());
                m.remove();
            } else {
                m.claimMinionSlot(id, this);
                m.copyLocationAndAnglesFrom(p);
                p.world.addEntity(m);
                activateTask(id, MinionTasks.stay);
            }

        }
    }

    /**
     * Check {@link PlayerMinionController#hasFreeMinionSlot()}
     *
     * @return minion slot id or -1 if no free minion slot
     */
    public int createNewMinionSlot(MinionData data, EntityType<? extends MinionEntity<?>> minionType) {
        int i = minions.length;
        if (i < maxMinions) {
            MinionInfo[] n = Arrays.copyOf(minions, i + 1);
            Optional<Integer>[] t = Arrays.copyOf(minionTokens, i + 1);
            n[i] = new MinionInfo(i, data, minionType);
            t[i] = Optional.empty();
            minions = n;
            minionTokens = t;
            return i;
        }
        return -1;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        LOGGER.info("Deserializing");//TODO
        IFaction<?> f = VampirismAPI.factionRegistry().getFactionByID(new ResourceLocation(nbt.getString("faction")));
        if (!(f instanceof IPlayableFaction)) {
            this.maxMinions = 0;
            return;
        }
        this.faction = (IPlayableFaction<?>) f;
        this.maxMinions = nbt.getInt("max_minions");
        ListNBT data = nbt.getList("data", 10);
        MinionInfo[] infos = new MinionInfo[data.size()];
        //noinspection unchecked
        Optional<Integer>[] tokens = new Optional[data.size()];
        for (INBT n : data) {
            CompoundNBT tag = (CompoundNBT) n;
            int id = tag.getInt("id");
            MinionData d = MinionData.fromNBT(tag);
            ResourceLocation entityTypeID = new ResourceLocation(tag.getString("entity_type"));
            if (!ForgeRegistries.ENTITIES.containsKey(entityTypeID)) {
                LOGGER.warn("Cannot find saved minion type {}. Aborting controller load", entityTypeID);
                this.minions = new MinionInfo[0];
                //noinspection unchecked
                this.minionTokens = new Optional[0];
                return;
            }

            EntityType type = ForgeRegistries.ENTITIES.getValue(entityTypeID);

            MinionInfo i = new MinionInfo(id, d, type);
            i.deathCooldown = tag.getInt("death_timer");
            infos[id] = i;
            if (tag.contains("token", 99)) {
                tokens[id] = Optional.of(tag.getInt("token"));
            } else {
                tokens[id] = Optional.empty();
            }

        }
        this.minions = infos;
        this.minionTokens = tokens;
    }

    /**
     * @return A collection of currently unclaimed and non dead minion slots
     */
    public Collection<Integer> getUnclaimedMinions() {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < minionTokens.length; i++) {
            if (!minionTokens[i].isPresent()) {
                if (!minions[i].isDead()) {
                    ids.add(i);
                }
            }
        }
        return ids;

    }

    public UUID getUUID() {
        return this.lordID;
    }

    /**
     * @return Whether a new minion can be created via {@link PlayerMinionController#createNewMinionSlot(MinionData, EntityType)}
     */
    public boolean hasFreeMinionSlot() {
        return minions.length < maxMinions;
    }

    /**
     * Mark a minion as dead and as inactive.
     * The minion slot is released and the token is invalidated
     * Don't use associated MinionData afterwards
     *
     * @param id Minion slot
     * @param token Previously received token
     */
    public void markDeadAndReleaseMinionSlot(int id, int token) {
        MinionInfo i = getMinionInfo(id, token);
        if (i != null) {
            i.checkin();
            i.deathCooldown = 20;//* 60 * 5; TODO
            if (id < minionTokens.length) {
                minionTokens[id] = Optional.empty();
            }
        }
    }

    /**
     * The controller is only saved if it has minions
     *
     * @return Whether the minion controller has minions.
     */
    public boolean hasMinions() {
        return this.minions.length > 0;
    }

    /**
     * Recalls all minions.
     * Corresponding entities are removed if present, tokens are invalidated  and slots are released
     *
     * @return A list of minions ids that can be reclaimed
     */
    public Collection<Integer> recallMinions() {
        contactMinions(MinionEntity::recallMinion);
        for (MinionInfo i : minions) { //TODO remove
            if (i.isActive()) {
                LOGGER.warn("Minion still active after recall");
            }
        }
        //noinspection unchecked
        minionTokens = new Optional[minions.length];
        Arrays.fill(minionTokens, Optional.empty());
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < minions.length; i++) {
            if (!minions[i].isDead()) {
                ids.add(i);
            }
        }
        return ids;
    }

    @Override
    public CompoundNBT serializeNBT() {
        LOGGER.info("Serializing");//TODO
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("max_minions", maxMinions);
        if (faction != null) {
            nbt.putString("faction", faction.getID().toString());
        }
        ListNBT data = new ListNBT();
        for (MinionInfo i : minions) {
            CompoundNBT d = i.data.serializeNBT();
            d.putInt("death_timer", i.deathCooldown);
            d.putInt("id", i.minionID);
            if (i.minionType != null) d.putString("entity_type", i.minionType.getRegistryName().toString());
            minionTokens[i.minionID].ifPresent(t -> d.putInt("token", t));
            data.add(d);
        }
        nbt.put("data", data);
        return nbt;
    }

    public void setMaxMinions(@Nullable IPlayableFaction<?> faction, int newCount) {
        assert newCount >= 0;
        if (this.faction != null && faction != this.faction) {
            LOGGER.warn("Changing player minion controller faction");
            contactMinions(MinionEntity::recallMinion);
            minions = new MinionInfo[0];
            //noinspection unchecked
            minionTokens = new Optional[0];
            this.faction = faction;
            this.maxMinions = this.faction == null ? 0 : newCount;
        } else {
            this.faction = faction;
            if (newCount >= maxMinions) {
                this.maxMinions = newCount;
            } else {
                LOGGER.debug("Reducing minion count from {} to {}", this.maxMinions, newCount);
                //Iteratively remove minion entities and minion slots, starting with the last claimed one
                while (this.minions.length > newCount) {
                    int nL = this.minions.length - 1;
                    contactMinion(nL, MinionEntity::recallMinion);
                    MinionInfo[] n = Arrays.copyOf(minions, nL);
                    Optional<Integer>[] t = Arrays.copyOf(minionTokens, nL);
                    minions = n;
                    minionTokens = t;
                }
            }
        }
    }

    public void tick() {
        for (MinionInfo i : minions) {
            if (i.deathCooldown > 0) {
                i.deathCooldown--;
                if (i.deathCooldown == 0) {
                    i.data.setHealth(i.data.getMaxHealth());
                    getLordPlayer().ifPresent(player -> player.sendStatusMessage(new TranslationTextComponent("text.vampirism.minion.can_respawn", i.data.getName()), true));
                }
            } else {
                IMinionTask.IMinionTaskDesc taskDesc = i.data.getCurrentTaskDesc();
                tickTask(taskDesc.getTask(), taskDesc, i);
            }
        }
    }

    private Optional<MinionEntity<?>> getMinionEntity(MinionInfo info) {
        if (info.isActive()) {
            assert info.dimension != null;
            World w = server.getWorld(info.dimension);
            if (w != null) {
                Entity e = w.getEntityByID(info.entityId);
                if (e instanceof MinionEntity) {
                    return Optional.of((MinionEntity<?>) e);
                } else {
                    LOGGER.warn("Retrieved entity is not a minion entity {}", e); //TODO check and remove or adjust
                }
            }
        }
        return Optional.empty();
    }

    private <Q extends IMinionTask.IMinionTaskDesc, T extends IMinionTask<Q>> void tickTask(T task, IMinionTask.IMinionTaskDesc desc, MinionInfo info) {
        if (info.isActive()) {
            task.tickActive((Q) desc, () -> getMinionEntity(info).map(m -> m), info.data.getInventory());
        } else {
            task.tickBackground((Q) desc, info.data.getInventory());

        }
    }

    @Nullable
    private MinionInfo getMinionInfo(int id, int token) {
        assert minions.length == minionTokens.length;
        if (id < minions.length) {
            if (minionTokens[id].map(t -> t == token).orElse(false))
                return minions[id];
        }
        return null;
    }

    private Optional<ILordPlayer> getLord() {
        return getLordPlayer().map(FactionPlayerHandler::get);
    }

    private Optional<PlayerEntity> getLordPlayer() {
        return Optional.ofNullable(server.getPlayerList().getPlayerByUUID(lordID));
    }


    private class MinionInfo {
        final int minionID;
        @Nonnull
        final MinionData data;
        @Nullable
        final EntityType<? extends MinionEntity<?>> minionType;
        int entityId = -1;
        int deathCooldown = 0;
        @Nullable
        RegistryKey<World> dimension;

        private MinionInfo(int id, @Nonnull MinionData data, @Nullable EntityType<? extends MinionEntity<?>> minionType) {
            this.minionID = id;
            this.data = data;
            this.minionType = minionType;
        }

        void checkin() {
            if (this.entityId == -1) {
                LOGGER.warn("Closing minion data for inactive minion"); //TODO check and remove
            }
            this.entityId = -1;
            this.dimension = null;
        }

        boolean checkout(int entityId, RegistryKey<World> dim) {
            if (this.entityId != -1 || isDead()) {
                return false;
            }
            this.entityId = entityId;
            this.dimension = dim;
            return true;
        }

        boolean isActive() {
            return entityId != -1;
        }

        boolean isDead() {
            return deathCooldown > 0;
        }
    }
}
