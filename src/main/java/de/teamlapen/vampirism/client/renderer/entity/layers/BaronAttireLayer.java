package de.teamlapen.vampirism.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import de.teamlapen.vampirism.REFERENCE;
import de.teamlapen.vampirism.api.util.VResourceLocation;
import de.teamlapen.vampirism.client.core.ModEntitiesRender;
import de.teamlapen.vampirism.client.model.BaronAttireModel;
import de.teamlapen.vampirism.client.model.BaronWrapperModel;
import de.teamlapen.vampirism.client.model.BaronessAttireModel;
import de.teamlapen.vampirism.entity.vampire.VampireBaronEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;


/**
 * Render attire for baron. Includes Male and female version
 */
public class BaronAttireLayer extends RenderLayer<VampireBaronEntity, BaronWrapperModel> {
    private final @NotNull BaronessAttireModel baroness;
    private final @NotNull BaronAttireModel baron;
    private final ResourceLocation textureBaroness = VResourceLocation.mod("textures/entity/baroness_attire.png");
    private final ResourceLocation textureBaron = VResourceLocation.mod("textures/entity/baron_attire.png");
    private final Predicate<VampireBaronEntity> predicateFemale;

    /**
     * @param predicateFemale used to choose between baron and baroness attire
     */
    public BaronAttireLayer(@NotNull RenderLayerParent<VampireBaronEntity, BaronWrapperModel> entityRendererIn, EntityRendererProvider.@NotNull Context context, Predicate<VampireBaronEntity> predicateFemale) {
        super(entityRendererIn);
        this.baroness = new BaronessAttireModel(context.bakeLayer(ModEntitiesRender.BARONESS_ATTIRE));
        this.baron = new BaronAttireModel(context.bakeLayer(ModEntitiesRender.BARON_ATTIRE));
        this.predicateFemale = predicateFemale;
    }


    @Override
    public void render(@NotNull PoseStack matrixStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn, @NotNull VampireBaronEntity entityIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entityIn.isInvisible()) {
            boolean female = predicateFemale.test(entityIn);
            EntityModel<VampireBaronEntity> model = female ? baroness : baron;
            coloredCutoutModelCopyLayerRender(this.getParentModel(), model, female ? textureBaroness : textureBaron, matrixStackIn, bufferIn, packedLightIn, entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTicks, -1);
        }
    }


}