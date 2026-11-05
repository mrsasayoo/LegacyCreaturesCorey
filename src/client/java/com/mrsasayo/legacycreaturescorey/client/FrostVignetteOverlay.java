package com.mrsasayo.legacycreaturescorey.client;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Viñeta de escarcha (freezing «Identidad On-Hit»): mientras el jugador está
 * congelado por la mutación, la pantalla se empaña con escarcha en la periferia —
 * el frío se ve en la pantalla, no solo en la barra. Misma receta que
 * TunnelVisionOverlay; se dispara con getTicksFrozen() > 0.
 */
public final class FrostVignetteOverlay {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "frost_vignette_overlay");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "textures/gui/frost_vignette_overlay.png");

    private FrostVignetteOverlay() {}

    public static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, HUD_ID, FrostVignetteOverlay::render);
    }

    private static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.gui.hud.isHidden()) {
            return;
        }
        if (player.getTicksFrozen() <= 0) {
            return;
        }
        int width = context.guiWidth();
        int height = context.guiHeight();
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, width, height, width, height);
    }
}
