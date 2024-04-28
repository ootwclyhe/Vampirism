package de.teamlapen.vampirism.network;

import com.mojang.serialization.Codec;
import de.teamlapen.vampirism.REFERENCE;
import de.teamlapen.vampirism.api.items.IRefinementItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.NotNull;


public record ServerboundDeleteRefinementPacket(IRefinementItem.AccessorySlotType slot) implements CustomPacketPayload {

    public static final Type<ServerboundDeleteRefinementPacket> TYPE = new Type<>(new ResourceLocation(REFERENCE.MODID, "delete_refinement"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDeleteRefinementPacket> CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(IRefinementItem.AccessorySlotType.class), ServerboundDeleteRefinementPacket::slot,
            ServerboundDeleteRefinementPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
