package com.mrsasayo.legacycreaturescorey.client;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.network.DifficultySyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Renderiza en pantalla los valores de dificultad sincronizados por el servidor.
 */
public final class ClientDifficultyHud {
    private static final Identifier HUD_ID = Identifier.of(Legacycreaturescorey.MOD_ID, "difficulty_hud");
    private static int globalDifficulty;
    private static int maxGlobalDifficulty;
    private static int personalDifficulty;
    private static boolean playerHudEnabled;

    private ClientDifficultyHud() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(DifficultySyncPayload.ID, (payload, context) ->
            context.client().execute(() -> update(payload))
        );
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, HUD_ID, ClientDifficultyHud::renderHud);
    }

    private static void update(DifficultySyncPayload payload) {
        globalDifficulty = payload.globalDifficulty();
        maxGlobalDifficulty = Math.max(1, payload.maxGlobalDifficulty());
        personalDifficulty = Math.max(0, payload.personalDifficulty());
        playerHudEnabled = payload.hudEnabled();
    }

    private static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (!playerHudEnabled || player == null || client.options.hudHidden) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int x = Math.max(6, width - 146);
        int y = height - 48;

        float globalPct = Math.min(1.0F, (float) globalDifficulty / maxGlobalDifficulty);
        int barWidth = 120;
        int barHeight = 4;

        int barX = x;
        int barY = y - 6;
        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0x66000000);
        context.fill(barX, barY, barX + (int) (barWidth * globalPct), barY + barHeight, 0xFFEBAA5B);

        var textRenderer = client.textRenderer;
        String globalText = String.format("Dificultad Global: %d/%d", globalDifficulty, maxGlobalDifficulty);
        String personalText = String.format("Dificultad Personal: %d", personalDifficulty);
        context.drawText(textRenderer, Text.literal(globalText), x, y, 0xFFF2E3C6, false);
        context.drawText(textRenderer, Text.literal(personalText), x, y + 10, 0xFFB2F2E8, false);
    }
}
