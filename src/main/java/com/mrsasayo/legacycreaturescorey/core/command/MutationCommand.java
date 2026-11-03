package com.mrsasayo.legacycreaturescorey.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.feature.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_type;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.permissionLevel.entity.Entity;
import net.minecraft.permissionLevel.entity.Mob;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class MutationCommand {
    private MutationCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("mutation")
            .requires(src -> src.hasPermissionLevel(2)); // only ops

        // /mutation <target> assign natural <type> <mutation>
        var targetArg =
            Commands.argument("target", EntityArgument.entity())
                .then(Commands.literal("assign")
                    .then(Commands.literal("natural")
                        .then(Commands.argument("type", StringArgumentType.string())
                            .suggests(MutationCommand::suggestTypes)
                            .then(Commands.argument("mutation", StringArgumentType.greedyString())
                                .suggests(MutationCommand::suggestMutationsByType)
                                .executes(ctx -> executeAssign(ctx, false))
                            )
                        )
                    )
                    .then(Commands.literal("force")
                        .then(Commands.argument("type", StringArgumentType.string())
                            .suggests(MutationCommand::suggestTypes)
                            .then(Commands.argument("mutation", StringArgumentType.greedyString())
                                .suggests(MutationCommand::suggestMutationsByType)
                                .executes(ctx -> executeAssign(ctx, true))
                            )
                        )
                    )
                );

        // /mutation <target> list
        targetArg = targetArg.then(Commands.literal("list")
            .executes(ctx -> executeList(ctx)));

        // /mutation <target> remove <mutation>
        targetArg = targetArg.then(Commands.literal("remove")
            .then(Commands.argument("mutation", StringArgumentType.greedyString())
                .suggests(MutationCommand::suggestMutationsByType)
                .executes(ctx -> executeRemove(ctx))
            )
        );

        // /mutation <target> clear
        targetArg = targetArg.then(Commands.literal("clear")
            .executes(ctx -> executeClear(ctx)));

        root.then(targetArg);

        dispatcher.register(root);
    }

    private static CompletableFuture<Suggestions> suggestTypes(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        for (mutation_type t : mutation_type.values()) {
            builder.suggest(t.textName());
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestMutationsByType(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String typeStr;
        try {
            typeStr = StringArgumentType.getString(ctx, "type");
        } catch (Exception e) {
            typeStr = null;
        }

        mutation_type filter = null;
        if (typeStr != null) {
            try {
                filter = mutation_type.valueOf(typeStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        Collection<mutation> all = mutation_registry.all();
        for (mutation m : all) {
            if (filter != null && m.getType() != filter) continue;
            builder.suggest(m.getId().toString());
        }
        return builder.buildFuture();
    }

    private static int executeAssign(CommandContext<CommandSourceStack> ctx, boolean force) {
        CommandSourceStack src = ctx.getSource();
        Entity targetEntity;
        try {
            targetEntity = EntityArgument.getEntity(ctx, "target");
        } catch (Exception e) {
            src.sendFailure(Component.literal("Target entity not found."));
            return 0;
        }

        if (!(targetEntity instanceof Mob mob)) {
            src.sendFailure(Component.literal("Target is not a mob entity."));
            return 0;
        }

        String mutationIdStr = StringArgumentType.getString(ctx, "mutation");
        ResourceLocation mid = ResourceLocation.tryBuild(mutationIdStr);
        if (mid == null) {
            src.sendFailure(Component.literal("Invalid mutation id: " + mutationIdStr));
            return 0;
        }

        mutation mut = mutation_registry.get(mid);
        if (mut == null) {
            src.sendFailure(Component.literal("Unknown mutation: " + mid.toString()));
            return 0;
        }

        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);

        // If not forcing, validate restrictions, compatibility and budget and provide detailed reasons
        if (!force) {
            List<ResourceLocation> existing = new ArrayList<>(data.getMutations());
            String reason = mut.getApplyFailureReason(mob, existing);
            if (reason != null) {
                src.sendFailure(Component.literal("Cannot assign mutation: " + reason));
                return 0;
            }

            // compute remaining PM
            int budget = getBudgetForTier(data.getTier());
            int used = 0;
            for (ResourceLocation id : data.getMutations()) {
                mutation m = mutation_registry.get(id);
                if (m != null) used += m.getCost();
            }
            int remaining = budget - used;
            if (mut.getCost() > remaining) {
                src.sendFailure(Component.literal("Not enough mutation points remaining for this mob (remaining=" + remaining + ")"));
                return 0;
            }
        }

        // Apply
        try {
            mut.onApply(mob);
            data.addMutation(mid);
            src.sendSuccess(() -> Component.literal("Assigned mutation " + mid + " to " + mob.getTextName().getString()), true);
            Legacycreaturescorey.LOGGER.info("Command: {} assigned mutation {} to {} (force={})", src.getTextName(), mid, mob.getType().getTranslationKey(), force);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("Failed to apply mutation: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        Entity targetEntity;
        try {
            targetEntity = EntityArgument.getEntity(ctx, "target");
        } catch (Exception e) {
            src.sendFailure(Component.literal("Target entity not found."));
            return 0;
        }

        if (!(targetEntity instanceof Mob mob)) {
            src.sendFailure(Component.literal("Target is not a mob entity."));
            return 0;
        }

        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        var list = data.getMutations();
        if (list.isEmpty()) {
            src.sendSuccess(() -> Component.literal("No mutations assigned."), true);
            return 1;
        }

        StringBuilder sb = new StringBuilder();
        for (ResourceLocation id : list) {
            mutation m = mutation_registry.get(id);
            sb.append(id.toString());
            if (m != null) {
                try {
                    sb.append(" (").append(m.getDisplayName().getString()).append(", cost=").append(m.getCost()).append(")");
                } catch (Exception ignore) {}
            }
            sb.append(", ");
        }
    String out = sb.toString();
    if (out.endsWith(", ")) out = out.substring(0, out.length() - 2);
    final String outFinal = out;
    src.sendSuccess(() -> Component.literal("Mutations: " + outFinal), true);
        return 1;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        Entity targetEntity;
        try {
            targetEntity = EntityArgument.getEntity(ctx, "target");
        } catch (Exception e) {
            src.sendFailure(Component.literal("Target entity not found."));
            return 0;
        }

        if (!(targetEntity instanceof Mob mob)) {
            src.sendFailure(Component.literal("Target is not a mob entity."));
            return 0;
        }

        String mutationIdStr = StringArgumentType.getString(ctx, "mutation");
        ResourceLocation mid = ResourceLocation.tryBuild(mutationIdStr);
        if (mid == null) {
            src.sendFailure(Component.literal("Invalid mutation id: " + mutationIdStr));
            return 0;
        }

        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        if (!data.getMutations().contains(mid)) {
            src.sendFailure(Component.literal("Entity does not have mutation " + mid));
            return 0;
        }

        mutation m = mutation_registry.get(mid);
        try {
            if (m != null) m.onRemoval(mob);
        } catch (Exception e) {
            // continue with removal anyway
            Legacycreaturescorey.LOGGER.warn("Error while calling onRemove for {}: {}", mid, e.getMessage());
        }

        data.removeMutation(mid);
        src.sendSuccess(() -> Component.literal("Removed mutation " + mid + " from " + mob.getTextName().getString()), true);
        return 1;
    }

    private static int executeClear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        Entity targetEntity;
        try {
            targetEntity = EntityArgument.getEntity(ctx, "target");
        } catch (Exception e) {
            src.sendFailure(Component.literal("Target entity not found."));
            return 0;
        }

        if (!(targetEntity instanceof Mob mob)) {
            src.sendFailure(Component.literal("Target is not a mob entity."));
            return 0;
        }

        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        var list = new ArrayList<>(data.getMutations());
        for (ResourceLocation id : list) {
            mutation m = mutation_registry.get(id);
            try {
                if (m != null) m.onRemoval(mob);
            } catch (Exception e) {
                Legacycreaturescorey.LOGGER.warn("Error while calling onRemove for {}: {}", id, e.getMessage());
            }
        }
        data.clearMutations();
        src.sendSuccess(() -> Component.literal("Cleared " + list.size() + " mutations from " + mob.getTextName().getString()), true);
        return 1;
    }

    // Duplicate of MutationAssigner's budget logic to avoid changing visibility
    private static int getBudgetForTier(MobTier tier) {
        return switch (tier) {
            case EPIC -> 25;
            case LEGENDARY -> 50;
            case MYTHIC -> 75;
            case DEFINITIVE -> 100;
            default -> 0;
        };
    }
}
