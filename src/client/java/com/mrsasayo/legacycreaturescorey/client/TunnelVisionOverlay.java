package com.mrsasayo.legacycreaturescorey.client;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.content.status.ModStatusEffects;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * "Visión de Túnel" (concussive_blow_2): overlay completamente nuevo del mod.
 * Mientras el efecto legacycreaturescorey:tunnel_vision esté activo, dibuja una
 * máscara radial (periferia cegada, pequeño túnel de visión al centro) sobre la
 * capa de overlays del HUD, igual que hace vanilla con la calabaza tallada.
 */
public final class TunnelVisionOverlay {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "tunnel_vision_overlay");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "textures/gui/tunnel_vision_overlay.png");
    private static final Holder<MobEffect> TUNNEL_VISION = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModStatusEffects.TUNNEL_VISION);

    private TunnelVisionOverlay() {}

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, HUD_ID, TunnelVisionOverlay::render);
    }

    private static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.gui.hud.isHidden()) {
            return;
        }
        MobEffectInstance instance = player.getEffect(TUNNEL_VISION);
        if (instance == null) {
            return;
        }
        int width = context.guiWidth();
        int height = context.guiHeight();
        // Estira la textura 256x256 a pantalla completa (UV normalizados al tamaño de pantalla)
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, width, height, width, height);
    }
}
