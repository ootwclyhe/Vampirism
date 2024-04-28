package de.teamlapen.vampirism.network;

import com.mojang.serialization.Codec;
import de.teamlapen.vampirism.REFERENCE;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;


public record ServerboundUnlockSkillPacket(ResourceLocation skillId) implements CustomPacketPayload {
    public static final Type<ServerboundUnlockSkillPacket> TYPE = new Type<>(new ResourceLocation(REFERENCE.MODID, "unlock_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundUnlockSkillPacket> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ServerboundUnlockSkillPacket::skillId,
            ServerboundUnlockSkillPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
