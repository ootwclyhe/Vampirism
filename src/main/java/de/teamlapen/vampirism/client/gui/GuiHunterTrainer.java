package de.teamlapen.vampirism.client.gui;

import de.teamlapen.lib.lib.util.UtilLib;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.core.ModItems;
import de.teamlapen.vampirism.inventory.HunterTrainerContainer;
import de.teamlapen.vampirism.network.InputEventPacket;
import de.teamlapen.vampirism.util.REFERENCE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

/**
 * Gui for the Hunter Trainer interaction
 */
public class GuiHunterTrainer extends GuiContainer {
    private static final ResourceLocation altarGuiTextures = new ResourceLocation(REFERENCE.MODID, "textures/gui/hunterTrainer.png");
    private final HunterTrainerContainer container;
    private GuiButton buttonLevelup;

    public GuiHunterTrainer(HunterTrainerContainer container) {
        super(container);
        this.container = container;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(altarGuiTextures);
        int k = (this.width - this.xSize) / 2;
        int l = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize);
    }


    @Override
    public void updateScreen() {
        super.updateScreen();
        if (container.hasChanged() || this.mc.thePlayer.getRNG().nextInt(40) == 6) {
            buttonLevelup.enabled = container.canLevelup();
        }

    }

    @Override
    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        String string = container.getHunterTrainerInventory().hasCustomName() ? this.container.getHunterTrainerInventory().getName() : I18n.format(this.container.getHunterTrainerInventory().getName());
        this.fontRendererObj.drawString(string, 8, 6, 0x404040);
        this.fontRendererObj.drawString(I18n.format("container.inventory"), 8, this.ySize - 94, 0x404040);

        String text = null;
        if (container.getMissingItems() != null) {
            ItemStack missing = container.getMissingItems();
            IChatComponent item = missing.getItem().equals(ModItems.hunterIntel) ? ModItems.hunterIntel.getDisplayName(missing) : new ChatComponentTranslation(missing.getUnlocalizedName() + ".name");
            text = I18n.format("text.vampirism.ritual_missing_items", missing.stackSize, item.getUnformattedText());
        }
        if (text != null) this.fontRendererObj.drawSplitString(text, 8, 50, this.xSize - 10, 0x000000);
    }

    @Override
    public void initGui() {
        super.initGui();
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        String name = I18n.format("text.vampirism.level_up");
        this.buttonList.add(this.buttonLevelup = new GuiButton(1, i + 120, j + 24, fontRendererObj.getStringWidth(name) + 5, 20, name));
        this.buttonLevelup.enabled = false;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            VampirismMod.dispatcher.sendToServer(new InputEventPacket(InputEventPacket.TRAINERLEVELUP, ""));
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            UtilLib.spawnParticles(player.worldObj, EnumParticleTypes.ENCHANTMENT_TABLE, player.posX, player.posY, player.posZ, 1, 1, 1, 100);
            player.worldObj.playSoundEffect(player.posX, player.posY, player.posZ, "note.harp", 4.0F, (1.0F + (player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.2F) * 0.7F);
        } else {
            super.actionPerformed(button);
        }
    }
}