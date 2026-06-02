package net.kayn.apothic_fishing.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.kayn.apothic_fishing.ApothicFishing;
import net.kayn.apothic_fishing.adventure.affix.ModHelper;
import net.kayn.apothic_fishing.api.ModPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin extends EntityRenderer<FishingHook> {

    @Unique
    private static final ResourceLocation HOVER_TEXTURE =
            new ResourceLocation(ApothicFishing.MOD_ID, "textures/entity/fishing_hook_hover.png");

    @Unique
    private static final RenderType HOVER_LAYER = RenderType.entityCutout(HOVER_TEXTURE);

    protected FishingHookRendererMixin(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/world/entity/projectile/FishingHook;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
                    ordinal = 0
            )
    )
    private VertexConsumer apothic$getHoveredBuffer(VertexConsumer original,
                                                    FishingHook hook, float f, float g,
                                                    com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                    MultiBufferSource bufferSource, int light) {

        Player owner = hook.getPlayerOwner();
        if (owner == null) return original;

        ModPlayer modPlayer = (ModPlayer) owner;
        ItemStack rod = ModHelper.getHeldRod(owner);
        if (rod == null) return original;

        FishingHook looking = ModHelper.getLookingHook(modPlayer, rod);
        if (looking == hook) {
            return bufferSource.getBuffer(HOVER_LAYER);
        }
        return original;
    }
}