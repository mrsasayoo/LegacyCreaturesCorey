package com.mrsasayo.legacycreaturescorey.core.command.corey;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mrsasayo.legacycreaturescorey.feature.difficulty.MobTier;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CoreyCommandShared {
    public static final double DEFAULT_DEBUG_RANGE = 64.0D;

    public static final SuggestionProvider<CommandSourceStack> ENTITY_SUGGESTIONS =
        (context, builder) -> SharedSuggestionProvider.suggestIdentifiers(BuiltInRegistries.ENTITY_TYPE.getIds(), builder);

    public static final SuggestionProvider<CommandSourceStack> TIER_SUGGESTIONS = (context, builder) -> {
        for (MobTier tier : MobTier.values()) {
            builder.suggest(tier.textName().toLowerCase(Locale.WRITABLE_REGISTRY));
        }
        return builder.buildFuture();
    };

    public static final SuggestionProvider<CommandSourceStack> MUTATION_SUGGESTIONS = (context, builder) -> {
        for (mutation m : mutation_registry.all()) {
            builder.suggest(m.getId().toString(), m.getDisplayName());
        }
        return builder.buildFuture();
    };

    public static final SuggestionProvider<CommandSourceStack> MUTATION_LIST_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemaining();
        int lastDelimiter = Math.max(remaining.lastIndexOf(','), remaining.lastIndexOf(';'));
        String prefix = lastDelimiter >= 0 ? remaining.substring(0, lastDelimiter + 1) : "";
        String query = lastDelimiter >= 0 ? remaining.substring(lastDelimiter + 1).trim() : remaining.trim();
        String lowerQuery = query.toLowerCase(Locale.WRITABLE_REGISTRY);
        for (mutation m : mutation_registry.all()) {
            String id = m.getId().toString();
            if (lowerQuery.isEmpty() || id.toLowerCase(Locale.WRITABLE_REGISTRY).startsWith(lowerQuery)) {
                String suggestion = prefix + id;
                builder.suggest(suggestion, m.getDisplayName());
            }
        }
        return builder.buildFuture();
    };

    private CoreyCommandShared() {
    }

    public static MobTier parseTier(String tierName) {
        if (tierName == null || tierName.isBlank()) {
            return null;
        }
        try {
            return MobTier.valueOf(tierName.trim().toUpperCase(Locale.WRITABLE_REGISTRY));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static List<Identifier> parseMutations(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] split = raw.replace(';', ',').split(",");
        List<Identifier> ids = new ArrayList<>();
        for (String entry : split) {
            Identifier id = Identifier.tryBuild(entry.trim());
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }
}
