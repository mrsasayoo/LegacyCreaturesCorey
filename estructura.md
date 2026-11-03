.
├── bin
│   ├── client
│   │   ├── com
│   │   │   └── mrsasayo
│   │   │       └── legacycreaturescorey
│   │   │           ├── client
│   │   │           │   ├── ClientDifficultyHud.class
│   │   │           │   ├── ClientEffectHandler$ActiveEffect.class
│   │   │           │   └── ClientEffectHandler.class
│   │   │           ├── LegacycreaturescoreyClient.class
│   │   │           ├── LegacycreaturescoreyDataGenerator.class
│   │   │           └── mixin
│   │   │               └── client
│   │   │                   ├── ClientPlayerEntityMixin.class
│   │   │                   └── KeyboardInputMixin.class
│   │   └── legacycreaturescorey.client.mixins.json
│   └── main
│       ├── assets
│       │   └── legacycreaturescorey
│       ├── com
│       │   └── mrsasayo
│       │       └── legacycreaturescorey
│       │           ├── antifarm
│       │           │   ├── AntiFarmManager.class
│       │           │   ├── ChunkActivityData.class
│       │           │   └── data
│       │           │       └── AntiFarmExclusionDataLoader.class
│       │           ├── api
│       │           │   ├── AntiFarmDashboardApi.class
│       │           │   ├── CoreyAPI.class
│       │           │   ├── event
│       │           │   │   ├── AntiFarmDashboardEvents$ChunkActivityUpdated.class
│       │           │   │   ├── AntiFarmDashboardEvents$UpdateReason.class
│       │           │   │   ├── AntiFarmDashboardEvents.class
│       │           │   │   ├── AntiFarmEvents$ChunkBlocked.class
│       │           │   │   ├── AntiFarmEvents$ShouldIgnore.class
│       │           │   │   ├── AntiFarmEvents$ThresholdModifier.class
│       │           │   │   ├── AntiFarmEvents.class
│       │           │   │   ├── MutationDataEvents$ModifyMutations.class
│       │           │   │   ├── MutationDataEvents$PostApplyMutations.class
│       │           │   │   ├── MutationDataEvents.class
│       │           │   │   ├── TieredLootTableEvents$ModifyTieredLootTables.class
│       │           │   │   ├── TieredLootTableEvents$PostApplyTieredLootTables.class
│       │           │   │   ├── TieredLootTableEvents.class
│       │           │   │   ├── TierEvents$TierApplied.class
│       │           │   │   ├── TierEvents.class
│       │           │   │   ├── TierLootEvents$AfterTierLoot.class
│       │           │   │   ├── TierLootEvents$AllowTierLoot.class
│       │           │   │   ├── TierLootEvents.class
│       │           │   │   ├── TierLootTelemetryEvents$TierLootApplied.class
│       │           │   │   └── TierLootTelemetryEvents.class
│       │           │   └── LegacyCreaturesTierApi.class
│       │           ├── block
│       │           │   ├── FakeLootPileBlock.class
│       │           │   └── ModBlocks.class
│       │           ├── command
│       │           │   ├── corey
│       │           │   │   ├── CoreyCommand.class
│       │           │   │   ├── CoreyCommandPermissions.class
│       │           │   │   ├── CoreyCommandShared.class
│       │           │   │   ├── CoreyCommandUtil.class
│       │           │   │   ├── CoreyDebugCommands.class
│       │           │   │   ├── CoreyMenuCommand.class
│       │           │   │   ├── CoreyMutationCommand.class
│       │           │   │   ├── CoreyReloadCommand.class
│       │           │   │   ├── CoreySpawnCommands.class
│       │           │   │   ├── CoreyStatsCommand$StatsSnapshot$PlayerEntry.class
│       │           │   │   ├── CoreyStatsCommand$StatsSnapshot.class
│       │           │   │   └── CoreyStatsCommand.class
│       │           │   ├── CoreyHudCommand.class
│       │           │   └── MutationCommand.class
│       │           ├── component
│       │           │   ├── ModDataAttachments.class
│       │           │   └── PlayerDifficultyData.class
│       │           ├── config
│       │           │   ├── CoreyConfig$ReloadResult.class
│       │           │   └── CoreyConfig.class
│       │           ├── difficulty
│       │           │   ├── CoreyServerState.class
│       │           │   ├── DifficultyManager.class
│       │           │   ├── DifficultyTickHandler.class
│       │           │   ├── EffectiveDifficultyCalculator.class
│       │           │   ├── MobTier$DoubleSupplier.class
│       │           │   └── MobTier.class
│       │           ├── health
│       │           │   ├── CoreyHealthMonitor$HealthCheckResult.class
│       │           │   └── CoreyHealthMonitor.class
│       │           ├── item
│       │           │   ├── GhostFragmentItem.class
│       │           │   └── ModItems.class
│       │           ├── Legacycreaturescorey.class
│       │           ├── loot
│       │           │   ├── CoreyLootModifiers.class
│       │           │   └── data
│       │           │       ├── IntRange.class
│       │           │       ├── TieredLootDataLoader$TargetSelection.class
│       │           │       ├── TieredLootDataLoader.class
│       │           │       ├── TieredLootManager.class
│       │           │       ├── TieredMobLoot$Drop.class
│       │           │       ├── TieredMobLoot$JsonDefinedStackProvider.class
│       │           │       ├── TieredMobLoot$SimpleStackProvider.class
│       │           │       ├── TieredMobLoot$StackProvider.class
│       │           │       ├── TieredMobLoot$WeightedDrop.class
│       │           │       └── TieredMobLoot.class
│       │           ├── mixin
│       │           │   ├── entity_damage_cooldown_accessor.class
│       │           │   ├── ExampleMixin.class
│       │           │   ├── general
│       │           │   │   ├── flee_entity_goal_mixin.class
│       │           │   │   └── phantom_swoop_movement_goal_mixin.class
│       │           │   ├── GoatRamDamageMixin.class
│       │           │   ├── HungerManagerMixin.class
│       │           │   ├── ItemEntityMixin.class
│       │           │   ├── LivingEntityDamageMixin.class
│       │           │   ├── LivingEntityHealMixin.class
│       │           │   ├── living_entity_hurt_time_accessor.class
│       │           │   ├── LivingEntityKillMixin.class
│       │           │   ├── LivingEntityStatusEffectMixin.class
│       │           │   ├── persistent_projectile_entity_accessor.class
│       │           │   ├── ServerPlayerInteractionManagerMixin.class
│       │           │   ├── SnowballEntityMixin.class
│       │           │   ├── StatusEffectInstanceMixin.class
│       │           │   └── VindicatorDamageMixin.class
│       │           ├── mob
│       │           │   ├── data
│       │           │   │   ├── BiomeTierWeightDataLoader.class
│       │           │   │   ├── MobAttributeDataLoader$AttributeValues.class
│       │           │   │   ├── MobAttributeDataLoader$Entry.class
│       │           │   │   ├── MobAttributeDataLoader.class
│       │           │   │   └── MobTierRuleDataLoader.class
│       │           │   ├── FuryHelper.class
│       │           │   ├── MobLegacyComponent.class
│       │           │   ├── MobLegacyData.class
│       │           │   ├── MobSpawnHandler.class
│       │           │   ├── TierManager.class
│       │           │   ├── TierParticleHelper.class
│       │           │   ├── TierParticleTicker.class
│       │           │   ├── TierProbabilityCalculator$Stage.class
│       │           │   └── TierProbabilityCalculator.class
│       │           ├── mutation
│       │           │   ├── AbstractMutation.class
│       │           │   ├── action
│       │           │   │   ├── ActionContext$HitContext.class
│       │           │   │   ├── ActionContext.class
│       │           │   │   ├── AlchemicalSeepAction$ActiveAura.class
│       │           │   │   ├── AlchemicalSeepAction$Handler.class
│       │           │   │   ├── AlchemicalSeepAction$Mode.class
│       │           │   │   ├── AlchemicalSeepAction.class
│       │           │   │   ├── AllyDeathHealAuraAction$ActiveAura.class
│       │           │   │   ├── AllyDeathHealAuraAction$Handler.class
│       │           │   │   ├── AllyDeathHealAuraAction.class
│       │           │   │   ├── AlphaPresenceAction$ActiveAura.class
│       │           │   │   ├── AlphaPresenceAction$Handler.class
│       │           │   │   ├── AlphaPresenceAction$Mode.class
│       │           │   │   ├── AlphaPresenceAction.class
│       │           │   │   ├── AncientCurseAction$Handler.class
│       │           │   │   ├── AncientCurseAction$Mode.class
│       │           │   │   ├── AncientCurseAction.class
│       │           │   │   ├── ApiarianWarfareAction$Handler$TemporaryBlock.class
│       │           │   │   ├── ApiarianWarfareAction$Handler.class
│       │           │   │   ├── ApiarianWarfareAction$Mode.class
│       │           │   │   ├── ApiarianWarfareAction.class
│       │           │   │   ├── AquaticStalkerAction$Handler.class
│       │           │   │   ├── AquaticStalkerAction$Mode.class
│       │           │   │   ├── AquaticStalkerAction.class
│       │           │   │   ├── ArcticFortitudeAction$Mode.class
│       │           │   │   ├── ArcticFortitudeAction.class
│       │           │   │   ├── AttributeAuraAction$Operation.class
│       │           │   │   ├── AttributeAuraAction$Target.class
│       │           │   │   ├── AttributeAuraAction.class
│       │           │   │   ├── AttributeMutationAction$Mode.class
│       │           │   │   ├── AttributeMutationAction.class
│       │           │   │   ├── auras
│       │           │   │   │   ├── corruption_aura_1_action.class
│       │           │   │   │   ├── corruption_aura_2_action.class
│       │           │   │   │   ├── corruption_aura_3_action.class
│       │           │   │   │   ├── deep_darkness_aura_1_action.class
│       │           │   │   │   ├── deep_darkness_aura_2_action.class
│       │           │   │   │   ├── deep_darkness_aura_3_action.class
│       │           │   │   │   ├── entropy_aura_1_action.class
│       │           │   │   │   ├── entropy_aura_2_action.class
│       │           │   │   │   ├── entropy_aura_3_action$Handler$ActiveAura.class
│       │           │   │   │   ├── entropy_aura_3_action$Handler.class
│       │           │   │   │   ├── entropy_aura_3_action.class
│       │           │   │   │   ├── horde_beacon_aura_1_action.class
│       │           │   │   │   ├── horde_beacon_aura_2_action.class
│       │           │   │   │   ├── horde_beacon_aura_3_action.class
│       │           │   │   │   ├── HordeBeaconHandler$ActiveAura.class
│       │           │   │   │   ├── HordeBeaconHandler$MarkedTarget.class
│       │           │   │   │   ├── HordeBeaconHandler.class
│       │           │   │   │   ├── HordeBeaconSource$Mode.class
│       │           │   │   │   ├── HordeBeaconSource.class
│       │           │   │   │   ├── interference_aura_1_action.class
│       │           │   │   │   ├── interference_aura_2_action.class
│       │           │   │   │   ├── interference_aura_3_action.class
│       │           │   │   │   ├── InterferenceHandler$ActiveAura.class
│       │           │   │   │   ├── InterferenceHandler.class
│       │           │   │   │   ├── InterferenceSource$Mode.class
│       │           │   │   │   ├── InterferenceSource.class
│       │           │   │   │   ├── oppressive_presence_aura_1_action.class
│       │           │   │   │   ├── oppressive_presence_aura_2_action.class
│       │           │   │   │   ├── oppressive_presence_aura_3_action.class
│       │           │   │   │   ├── OppressivePresenceHandler$ActiveAura.class
│       │           │   │   │   ├── OppressivePresenceHandler.class
│       │           │   │   │   ├── OppressivePresenceSource.class
│       │           │   │   │   ├── PhantasmalHandler$ActiveAura.class
│       │           │   │   │   ├── PhantasmalHandler$IllusionRecord.class
│       │           │   │   │   ├── PhantasmalHandler.class
│       │           │   │   │   ├── PhantasmalSource$Mode.class
│       │           │   │   │   ├── PhantasmalSource.class
│       │           │   │   │   ├── phantasmal_veil_aura_1_action.class
│       │           │   │   │   ├── phantasmal_veil_aura_2_action.class
│       │           │   │   │   ├── phantasmal_veil_aura_3_action.class
│       │           │   │   │   ├── projectile_shroud_aura_1_action.class
│       │           │   │   │   ├── projectile_shroud_aura_2_action.class
│       │           │   │   │   ├── projectile_shroud_aura_3_action.class
│       │           │   │   │   ├── ProjectileShroudHandler$ActiveAura.class
│       │           │   │   │   ├── ProjectileShroudHandler.class
│       │           │   │   │   ├── ProjectileShroudSource$Mode.class
│       │           │   │   │   ├── ProjectileShroudSource.class
│       │           │   │   │   ├── psionic_thorns_aura_1_action.class
│       │           │   │   │   ├── psionic_thorns_aura_2_action.class
│       │           │   │   │   ├── psionic_thorns_aura_3_action.class
│       │           │   │   │   ├── PsionicThornsHandler.class
│       │           │   │   │   ├── PsionicThornsSource$ThornsEffect.class
│       │           │   │   │   ├── PsionicThornsSource.class
│       │           │   │   │   ├── stasis_field_aura_1_action.class
│       │           │   │   │   ├── stasis_field_aura_2_action.class
│       │           │   │   │   ├── stasis_field_aura_3_action.class
│       │           │   │   │   ├── StasisHandler$ActiveAura.class
│       │           │   │   │   ├── StasisHandler.class
│       │           │   │   │   ├── StasisSource$Mode.class
│       │           │   │   │   ├── StasisSource.class
│       │           │   │   │   ├── vanguards_bulwark_aura_1_action.class
│       │           │   │   │   ├── vanguards_bulwark_aura_2_action.class
│       │           │   │   │   ├── vanguards_bulwark_aura_3_action.class
│       │           │   │   │   ├── VanguardsBulwarkHandler$ActiveAura.class
│       │           │   │   │   ├── VanguardsBulwarkHandler.class
│       │           │   │   │   ├── VanguardsBulwarkSource$Mode.class
│       │           │   │   │   ├── VanguardsBulwarkSource.class
│       │           │   │   │   ├── virulent_growth_aura_1_action.class
│       │           │   │   │   ├── virulent_growth_aura_2_action.class
│       │           │   │   │   ├── virulent_growth_aura_3_action.class
│       │           │   │   │   ├── VirulentHandler$ActiveAura.class
│       │           │   │   │   ├── VirulentHandler$DecayEntry.class
│       │           │   │   │   ├── VirulentHandler.class
│       │           │   │   │   ├── VirulentSource$Mode.class
│       │           │   │   │   └── VirulentSource.class
│       │           │   │   ├── AxeMasteryAction$Handler$AxeState.class
│       │           │   │   ├── AxeMasteryAction$Handler.class
│       │           │   │   ├── AxeMasteryAction$Mode.class
│       │           │   │   ├── AxeMasteryAction.class
│       │           │   │   ├── BambooEaterAction$Handler.class
│       │           │   │   ├── BambooEaterAction$Mode.class
│       │           │   │   ├── BambooEaterAction.class
│       │           │   │   ├── BastionGuardAction$Handler.class
│       │           │   │   ├── BastionGuardAction$Mode.class
│       │           │   │   ├── BastionGuardAction.class
│       │           │   │   ├── BatteringRamAction$Mode.class
│       │           │   │   ├── BatteringRamAction.class
│       │           │   │   ├── BeamRefractionAction$Mode.class
│       │           │   │   ├── BeamRefractionAction.class
│       │           │   │   ├── BlizzardOrbAction$Mode.class
│       │           │   │   ├── BlizzardOrbAction.class
│       │           │   │   ├── BloodlustAction$Mode.class
│       │           │   │   ├── BloodlustAction.class
│       │           │   │   ├── BoarFrenzyAction$Mode.class
│       │           │   │   ├── BoarFrenzyAction.class
│       │           │   │   ├── DamageAuraAction.class
│       │           │   │   ├── DeepDarknessAuraAction$ActiveAura.class
│       │           │   │   ├── DeepDarknessAuraAction$Handler.class
│       │           │   │   ├── DeepDarknessAuraAction$Mode.class
│       │           │   │   ├── DeepDarknessAuraAction$ScheduledLight.class
│       │           │   │   ├── DeepDarknessAuraAction.class
│       │           │   │   ├── DetonatingRemainsManager$ChainMark.class
│       │           │   │   ├── DetonatingRemainsManager$Remnant.class
│       │           │   │   ├── DetonatingRemainsManager$RemnantConfig.class
│       │           │   │   ├── DetonatingRemainsManager.class
│       │           │   │   ├── DetonatingRemainsOnDeathAction.class
│       │           │   │   ├── EntropyAuraAction$ActiveAura.class
│       │           │   │   ├── EntropyAuraAction$Handler.class
│       │           │   │   ├── EntropyAuraAction$Mode.class
│       │           │   │   ├── EntropyAuraAction.class
│       │           │   │   ├── FakeLootPileManager$LootPile.class
│       │           │   │   ├── FakeLootPileManager.class
│       │           │   │   ├── FakeLootPileOnDeathAction.class
│       │           │   │   ├── FinalBurstOnDeathAction$FireCleanupTask.class
│       │           │   │   ├── FinalBurstOnDeathAction.class
│       │           │   │   ├── FinalGambitOnDeathAction$Mode.class
│       │           │   │   ├── FinalGambitOnDeathAction.class
│       │           │   │   ├── GhostFragmentsOnDeathAction.class
│       │           │   │   ├── GroundHazardManager$Hazard.class
│       │           │   │   ├── GroundHazardManager$HazardConfig.class
│       │           │   │   ├── GroundHazardManager$Target.class
│       │           │   │   ├── GroundHazardManager.class
│       │           │   │   ├── GroundHazardOnDeathAction$1.class
│       │           │   │   ├── GroundHazardOnDeathAction.class
│       │           │   │   ├── HealAction.class
│       │           │   │   ├── HordeBeaconAuraAction$ActiveAura.class
│       │           │   │   ├── HordeBeaconAuraAction$Handler.class
│       │           │   │   ├── HordeBeaconAuraAction$MarkedTarget.class
│       │           │   │   ├── HordeBeaconAuraAction$Mode.class
│       │           │   │   ├── HordeBeaconAuraAction.class
│       │           │   │   ├── InterferenceAuraAction$ActiveAura.class
│       │           │   │   ├── InterferenceAuraAction$Handler.class
│       │           │   │   ├── InterferenceAuraAction$Mode.class
│       │           │   │   ├── InterferenceAuraAction.class
│       │           │   │   ├── LootDropHelper.class
│       │           │   │   ├── LootScatterOnDeathAction.class
│       │           │   │   ├── mob_exclusive
│       │           │   │   │   ├── abyssal_armor_1_action.class
│       │           │   │   │   ├── abyssal_armor_2_action.class
│       │           │   │   │   ├── abyssal_armor_3_action.class
│       │           │   │   │   ├── abyssal_armor_base_action$armor_state.class
│       │           │   │   │   ├── abyssal_armor_base_action.class
│       │           │   │   │   ├── abyssal_maneuvers_1_action.class
│       │           │   │   │   ├── abyssal_maneuvers_2_action$blink_state.class
│       │           │   │   │   ├── abyssal_maneuvers_2_action.class
│       │           │   │   │   ├── abyssal_maneuvers_3_action.class
│       │           │   │   │   ├── acidic_core_1_action.class
│       │           │   │   │   ├── acidic_core_2_action.class
│       │           │   │   │   ├── acidic_core_3_action.class
│       │           │   │   │   ├── aerial_maneuvers_1_action.class
│       │           │   │   │   ├── aerial_maneuvers_2_action.class
│       │           │   │   │   ├── aerial_maneuvers_3_action$burst_state.class
│       │           │   │   │   ├── aerial_maneuvers_3_action.class
│       │           │   │   │   ├── alphas_vengeance_1_action.class
│       │           │   │   │   ├── alphas_vengeance_2_action.class
│       │           │   │   │   ├── alphas_vengeance_3_action.class
│       │           │   │   │   ├── alphas_vengeance_base_action.class
│       │           │   │   │   ├── ambusher_1_action.class
│       │           │   │   │   ├── ambusher_2_action.class
│       │           │   │   │   ├── ambusher_3_action$Handler.class
│       │           │   │   │   ├── ambusher_3_action.class
│       │           │   │   │   ├── ambusher_base_action.class
│       │           │   │   │   ├── amphibious_assault_1_action.class
│       │           │   │   │   ├── amphibious_assault_2_action.class
│       │           │   │   │   ├── amphibious_assault_3_action.class
│       │           │   │   │   ├── amphibious_assault_base_action.class
│       │           │   │   │   └── ancient_curse_1_action.class
│       │           │   │   ├── MutationAction.class
│       │           │   │   ├── MutationTaskScheduler$TimedTask.class
│       │           │   │   ├── MutationTaskScheduler.class
│       │           │   │   ├── on_being_hit
│       │           │   │   ├── on_death
│       │           │   │   ├── on_hit
│       │           │   │   ├── passive
│       │           │   │   ├── PhantasmalVeilAuraAction$ActiveAura.class
│       │           │   │   ├── PhantasmalVeilAuraAction$Handler.class
│       │           │   │   ├── PhantasmalVeilAuraAction$IllusionRecord.class
│       │           │   │   ├── PhantasmalVeilAuraAction$Mode.class
│       │           │   │   ├── PhantasmalVeilAuraAction$ShroudState.class
│       │           │   │   ├── PhantasmalVeilAuraAction.class
│       │           │   │   ├── ProcOnHitAction.class
│       │           │   │   ├── ProjectileShroudAuraAction$Mode.class
│       │           │   │   ├── ProjectileShroudAuraAction.class
│       │           │   │   ├── PsionicThornsAuraAction$Handler.class
│       │           │   │   ├── PsionicThornsAuraAction$ThornsEffect.class
│       │           │   │   ├── PsionicThornsAuraAction.class
│       │           │   │   ├── RealityCollapseManager$Field.class
│       │           │   │   ├── RealityCollapseManager.class
│       │           │   │   ├── StatusEffectAuraAction$Target.class
│       │           │   │   ├── StatusEffectAuraAction.class
│       │           │   │   ├── StatusEffectOnDeathAction$1.class
│       │           │   │   ├── StatusEffectOnDeathAction$Target.class
│       │           │   │   ├── StatusEffectOnDeathAction.class
│       │           │   │   ├── SummonMobAction.class
│       │           │   │   ├── synergy
│       │           │   │   ├── terrain
│       │           │   │   ├── UndeadPotionBurstAction$ActiveAura.class
│       │           │   │   ├── UndeadPotionBurstAction$Handler.class
│       │           │   │   └── UndeadPotionBurstAction.class
│       │           │   ├── ConfiguredMutation.class
│       │           │   ├── data
│       │           │   │   ├── MutationDataLoader$ActionParser$ParseException.class
│       │           │   │   ├── MutationDataLoader$ActionParser.class
│       │           │   │   ├── MutationDataLoader$EntitySelector.class
│       │           │   │   ├── MutationDataLoader.class
│       │           │   │   ├── MutationSchemaValidator$ActionValidator.class
│       │           │   │   ├── MutationSchemaValidator$ValidationCollector.class
│       │           │   │   └── MutationSchemaValidator.class
│       │           │   ├── MutationAssigner.class
│       │           │   ├── Mutation.class
│       │           │   ├── MutationRegistry$1.class
│       │           │   ├── MutationRegistry$2.class
│       │           │   ├── MutationRegistry$3.class
│       │           │   ├── MutationRegistry$4.class
│       │           │   ├── MutationRegistry$5.class
│       │           │   ├── MutationRegistry$6.class
│       │           │   ├── MutationRegistry$7.class
│       │           │   ├── MutationRegistry.class
│       │           │   ├── MutationRestrictions.class
│       │           │   ├── MutationRuntime.class
│       │           │   ├── MutationType.class
│       │           │   └── util
│       │           │       ├── mutation_action_config.class
│       │           │       ├── status_effect_config_parser$status_effect_config_entry.class
│       │           │       └── status_effect_config_parser.class
│       │           ├── network
│       │           │   ├── ClientCapabilitiesPayload$1.class
│       │           │   ├── ClientCapabilitiesPayload.class
│       │           │   ├── ClientEffectPayload$1.class
│       │           │   ├── ClientEffectPayload.class
│       │           │   ├── ClientEffectType.class
│       │           │   ├── ClientFeature.class
│       │           │   ├── DifficultySyncPayload$1.class
│       │           │   ├── DifficultySyncPayload.class
│       │           │   ├── ModNetworking$ClientCapabilities.class
│       │           │   ├── ModNetworking.class
│       │           │   ├── ServerHelloPayload$1.class
│       │           │   └── ServerHelloPayload.class
│       │           ├── status
│       │           │   ├── HealingReductionStatusEffect.class
│       │           │   ├── HeavyGravityStatusEffect.class
│       │           │   ├── ModStatusEffects.class
│       │           │   ├── NotifyingStatusEffect.class
│       │           │   ├── SlipperyFloorStatusEffect.class
│       │           │   ├── StatusEffectSyncHelper.class
│       │           │   ├── StatusEffectTicker$EffectState.class
│       │           │   ├── StatusEffectTicker$TrackedEffect$1.class
│       │           │   ├── StatusEffectTicker$TrackedEffect$2.class
│       │           │   ├── StatusEffectTicker$TrackedEffect.class
│       │           │   ├── StatusEffectTicker$TrackedEntry.class
│       │           │   └── StatusEffectTicker.class
│       │           └── synergy
│       │               ├── CoreySynergyApi.class
│       │               ├── SynergyManager.class
│       │               ├── SynergyModule.class
│       │               ├── SynergyProvider.class
│       │               └── SynergyStatus.class
│       ├── data
│       │   └── legacycreaturescorey
│       │       ├── biome_tier_weights.json
│       │       ├── loot
│       │       │   └── tiered
│       │       │       ├── definitive
│       │       │       │   ├── blaze.json
│       │       │       │   ├── bogged.json
│       │       │       │   ├── breeze.json
│       │       │       │   ├── cave_spider.json
│       │       │       │   ├── creaking.json
│       │       │       │   ├── creeper.json
│       │       │       │   ├── drowned.json
│       │       │       │   ├── elder_guardian.json
│       │       │       │   ├── enderman.json
│       │       │       │   ├── evoker.json
│       │       │       │   ├── ghast.json
│       │       │       │   ├── guardian.json
│       │       │       │   ├── hoglin.json
│       │       │       │   ├── iron_golem.json
│       │       │       │   ├── magma_cube.json
│       │       │       │   ├── phantom.json
│       │       │       │   ├── piglin_brute.json
│       │       │       │   ├── ravager.json
│       │       │       │   ├── shulker.json
│       │       │       │   ├── stray.json
│       │       │       │   ├── vex.json
│       │       │       │   ├── vindicator.json
│       │       │       │   ├── warden.json
│       │       │       │   ├── witch.json
│       │       │       │   ├── wither_skeleton.json
│       │       │       │   ├── zoglin.json
│       │       │       │   └── zombified_piglin.json
│       │       │       ├── epic
│       │       │       │   ├── bee.json
│       │       │       │   ├── blaze.json
│       │       │       │   ├── bogged.json
│       │       │       │   ├── breeze.json
│       │       │       │   ├── cave_spider.json
│       │       │       │   ├── creaking.json
│       │       │       │   ├── creeper.json
│       │       │       │   ├── dolphin.json
│       │       │       │   ├── drowned.json
│       │       │       │   ├── elder_guardian.json
│       │       │       │   ├── enderman.json
│       │       │       │   ├── endermite.json
│       │       │       │   ├── evoker.json
│       │       │       │   ├── fox.json
│       │       │       │   ├── ghast.json
│       │       │       │   ├── goat.json
│       │       │       │   ├── guardian.json
│       │       │       │   ├── hoglin.json
│       │       │       │   ├── husk.json
│       │       │       │   ├── iron_golem.json
│       │       │       │   ├── llama.json
│       │       │       │   ├── magma_cube.json
│       │       │       │   ├── panda.json
│       │       │       │   ├── phantom.json
│       │       │       │   ├── piglin_brute.json
│       │       │       │   ├── pillager.json
│       │       │       │   ├── polar_bear.json
│       │       │       │   ├── ravager.json
│       │       │       │   ├── shulker.json
│       │       │       │   ├── silverfish.json
│       │       │       │   ├── skeleton.json
│       │       │       │   ├── slime.json
│       │       │       │   ├── snow_golem.json
│       │       │       │   ├── spider.json
│       │       │       │   ├── stray.json
│       │       │       │   ├── vex.json
│       │       │       │   ├── vindicator.json
│       │       │       │   ├── warden.json
│       │       │       │   ├── witch.json
│       │       │       │   ├── wither_skeleton.json
│       │       │       │   ├── wolf.json
│       │       │       │   ├── zoglin.json
│       │       │       │   ├── zombie.json
│       │       │       │   ├── zombie_villager.json
│       │       │       │   └── zombified_piglin.json
│       │       │       ├── legendary
│       │       │       │   ├── blaze.json
│       │       │       │   ├── bogged.json
│       │       │       │   ├── breeze.json
│       │       │       │   ├── cave_spider.json
│       │       │       │   ├── creaking.json
│       │       │       │   ├── creeper.json
│       │       │       │   ├── drowned.json
│       │       │       │   ├── elder_guardian.json
│       │       │       │   ├── enderman.json
│       │       │       │   ├── endermite.json
│       │       │       │   ├── evoker.json
│       │       │       │   ├── ghast.json
│       │       │       │   ├── guardian.json
│       │       │       │   ├── hoglin.json
│       │       │       │   ├── husk.json
│       │       │       │   ├── iron_golem.json
│       │       │       │   ├── magma_cube.json
│       │       │       │   ├── phantom.json
│       │       │       │   ├── piglin_brute.json
│       │       │       │   ├── pillager.json
│       │       │       │   ├── ravager.json
│       │       │       │   ├── shulker.json
│       │       │       │   ├── silverfish.json
│       │       │       │   ├── skeleton.json
│       │       │       │   ├── slime.json
│       │       │       │   ├── spider.json
│       │       │       │   ├── stray.json
│       │       │       │   ├── vex.json
│       │       │       │   ├── vindicator.json
│       │       │       │   ├── warden.json
│       │       │       │   ├── witch.json
│       │       │       │   ├── wither_skeleton.json
│       │       │       │   ├── zoglin.json
│       │       │       │   ├── zombie.json
│       │       │       │   ├── zombie_villager.json
│       │       │       │   └── zombified_piglin.json
│       │       │       └── mythic
│       │       │           ├── blaze.json
│       │       │           ├── bogged.json
│       │       │           ├── breeze.json
│       │       │           ├── cave_spider.json
│       │       │           ├── creaking.json
│       │       │           ├── creeper.json
│       │       │           ├── drowned.json
│       │       │           ├── elder_guardian.json
│       │       │           ├── enderman.json
│       │       │           ├── evoker.json
│       │       │           ├── ghast.json
│       │       │           ├── guardian.json
│       │       │           ├── hoglin.json
│       │       │           ├── iron_golem.json
│       │       │           ├── magma_cube.json
│       │       │           ├── phantom.json
│       │       │           ├── piglin_brute.json
│       │       │           ├── ravager.json
│       │       │           ├── shulker.json
│       │       │           ├── stray.json
│       │       │           ├── vex.json
│       │       │           ├── vindicator.json
│       │       │           ├── warden.json
│       │       │           ├── witch.json
│       │       │           ├── wither_skeleton.json
│       │       │           ├── zoglin.json
│       │       │           └── zombified_piglin.json
│       │       ├── mob_attributes.json
│       │       ├── mob_tier_rules.json
│       │       └── mutations
│       │           ├── active
│       │           ├── auras
│       │           │   ├── corruption_aura_1.json
│       │           │   ├── corruption_aura_2.json
│       │           │   ├── corruption_aura_3.json
│       │           │   ├── deep_darkness_aura_1.json
│       │           │   ├── deep_darkness_aura_2.json
│       │           │   ├── deep_darkness_aura_3.json
│       │           │   ├── entropy_aura_1.json
│       │           │   ├── entropy_aura_2.json
│       │           │   ├── entropy_aura_3.json
│       │           │   ├── horde_beacon_aura_1.json
│       │           │   ├── horde_beacon_aura_2.json
│       │           │   ├── horde_beacon_aura_3.json
│       │           │   ├── interference_aura_1.json
│       │           │   ├── interference_aura_2.json
│       │           │   ├── interference_aura_3.json
│       │           │   ├── oppressive_presence_aura_1.json
│       │           │   ├── oppressive_presence_aura_2.json
│       │           │   ├── oppressive_presence_aura_3.json
│       │           │   ├── phantasmal_veil_aura_1.json
│       │           │   ├── phantasmal_veil_aura_2.json
│       │           │   ├── phantasmal_veil_aura_3.json
│       │           │   ├── projectile_shroud_aura_1.json
│       │           │   ├── projectile_shroud_aura_2.json
│       │           │   ├── projectile_shroud_aura_3.json
│       │           │   ├── psionic_thorns_aura_1.json
│       │           │   ├── psionic_thorns_aura_2.json
│       │           │   ├── psionic_thorns_aura_3.json
│       │           │   ├── stasis_field_aura_1.json
│       │           │   ├── stasis_field_aura_2.json
│       │           │   ├── stasis_field_aura_3.json
│       │           │   ├── vanguards_bulwark_aura_1.json
│       │           │   ├── vanguards_bulwark_aura_2.json
│       │           │   ├── vanguards_bulwark_aura_3.json
│       │           │   ├── virulent_growth_aura_1.json
│       │           │   ├── virulent_growth_aura_2.json
│       │           │   └── virulent_growth_aura_3.json
│       │           ├── mob_exclusive
│       │           │   ├── abyssal_armor_1.json
│       │           │   ├── abyssal_armor_2.json
│       │           │   ├── abyssal_armor_3.json
│       │           │   ├── abyssal_maneuvers_1.json
│       │           │   ├── abyssal_maneuvers_2.json
│       │           │   ├── abyssal_maneuvers_3.json
│       │           │   ├── acidic_core_1.json
│       │           │   ├── acidic_core_2.json
│       │           │   ├── acidic_core_3.json
│       │           │   ├── aerial_maneuvers_1.json
│       │           │   ├── aerial_maneuvers_2.json
│       │           │   ├── aerial_maneuvers_3.json
│       │           │   ├── alphas_vengeance_1.json
│       │           │   ├── alphas_vengeance_2.json
│       │           │   ├── alphas_vengeance_3.json
│       │           │   ├── ambusher_1.json
│       │           │   ├── ambusher_2.json
│       │           │   ├── ambusher_3.json
│       │           │   ├── amphibious_assault_1.json
│       │           │   ├── amphibious_assault_2.json
│       │           │   ├── amphibious_assault_3.json
│       │           │   ├── ancient_curse_1.json
│       │           │   ├── ancient_curse_2.json
│       │           │   ├── ancient_curse_3.json
│       │           │   ├── apiarian_warfare_1.json
│       │           │   ├── apiarian_warfare_2.json
│       │           │   ├── apiarian_warfare_3.json
│       │           │   ├── aquatic_stalker_1.json
│       │           │   ├── aquatic_stalker_2.json
│       │           │   ├── aquatic_stalker_3.json
│       │           │   ├── arctic_fortitude_1.json
│       │           │   ├── arctic_fortitude_2.json
│       │           │   ├── arctic_fortitude_3.json
│       │           │   ├── axe_mastery_1.json
│       │           │   ├── axe_mastery_2.json
│       │           │   ├── axe_mastery_3.json
│       │           │   ├── bamboo_eater_1.json
│       │           │   ├── bamboo_eater_2.json
│       │           │   ├── bamboo_eater_3.json
│       │           │   ├── bastion_guard_1.json
│       │           │   ├── bastion_guard_2.json
│       │           │   ├── bastion_guard_3.json
│       │           │   ├── battering_ram_1.json
│       │           │   ├── battering_ram_2.json
│       │           │   ├── battering_ram_3.json
│       │           │   ├── beam_refraction_1.json
│       │           │   ├── beam_refraction_2.json
│       │           │   ├── beam_refraction_3.json
│       │           │   ├── blizzard_orb_1.json
│       │           │   ├── blizzard_orb_2.json
│       │           │   ├── blizzard_orb_3.json
│       │           │   ├── bloodlust_1.json
│       │           │   ├── bloodlust_2.json
│       │           │   ├── bloodlust_3.json
│       │           │   ├── boar_frenzy_1.json
│       │           │   ├── boar_frenzy_2.json
│       │           │   └── boar_frenzy_3.json
│       │           ├── on_hit
│       │           │   ├── anti_armor_1.json
│       │           │   ├── anti_armor_2.json
│       │           │   ├── anti_armor_3.json
│       │           │   ├── anti_shield_1.json
│       │           │   ├── anti_shield_2.json
│       │           │   ├── bleeding_1.json
│       │           │   ├── bleeding_2.json
│       │           │   ├── bleeding_3.json
│       │           │   ├── blindness_1.json
│       │           │   ├── chaos_touch_1.json
│       │           │   ├── chaos_touch_2.json
│       │           │   ├── chaos_touch_3.json
│       │           │   ├── concussive_blow_1.json
│       │           │   ├── concussive_blow_2.json
│       │           │   ├── concussive_blow_3.json
│       │           │   ├── critical_1.json
│       │           │   ├── critical_2.json
│       │           │   ├── critical_3.json
│       │           │   ├── deafening_strike_1.json
│       │           │   ├── deafening_strike_2.json
│       │           │   ├── deafening_strike_3.json
│       │           │   ├── disarm_1.json
│       │           │   ├── essence_siphon_1.json
│       │           │   ├── essence_siphon_2.json
│       │           │   ├── essence_siphon_3.json
│       │           │   ├── experience_theft_1.json
│       │           │   ├── experience_theft_2.json
│       │           │   ├── experience_theft_3.json
│       │           │   ├── fire_1.json
│       │           │   ├── fire_2.json
│       │           │   ├── fire_3.json
│       │           │   ├── freezing_1.json
│       │           │   ├── freezing_2.json
│       │           │   ├── freezing_3.json
│       │           │   ├── frenzy_1.json
│       │           │   ├── frenzy_2.json
│       │           │   ├── frenzy_3.json
│       │           │   ├── glowing_1.json
│       │           │   ├── glowing_2.json
│       │           │   ├── glowing_3.json
│       │           │   ├── hunger_1.json
│       │           │   ├── hunger_2.json
│       │           │   ├── hunger_3.json
│       │           │   ├── levitation_1.json
│       │           │   ├── levitation_2.json
│       │           │   ├── mining_fatigue_1.json
│       │           │   ├── mining_fatigue_2.json
│       │           │   ├── mining_fatigue_3.json
│       │           │   ├── mortal_wound_1.json
│       │           │   ├── mortal_wound_2.json
│       │           │   ├── mortal_wound_3.json
│       │           │   ├── nausea_1.json
│       │           │   ├── pain_link_1.json
│       │           │   ├── pain_link_2.json
│       │           │   ├── pain_link_3.json
│       │           │   ├── poison_1.json
│       │           │   ├── poison_2.json
│       │           │   ├── poison_3.json
│       │           │   ├── push_1.json
│       │           │   ├── push_2.json
│       │           │   ├── push_3.json
│       │           │   ├── shatter_armor_1.json
│       │           │   ├── shatter_armor_2.json
│       │           │   ├── shatter_armor_3.json
│       │           │   ├── slow_falling_1.json
│       │           │   ├── slow_falling_2.json
│       │           │   ├── slow_falling_3.json
│       │           │   ├── slowness_1.json
│       │           │   ├── slowness_2.json
│       │           │   ├── slowness_3.json
│       │           │   ├── teleportation_1.json
│       │           │   ├── teleportation_2.json
│       │           │   ├── teleportation_3.json
│       │           │   ├── theft_of_life_1.json
│       │           │   ├── theft_of_life_2.json
│       │           │   ├── theft_of_life_3.json
│       │           │   ├── true_damage_1.json
│       │           │   ├── true_damage_2.json
│       │           │   ├── unstable_hit_1.json
│       │           │   ├── unstable_hit_2.json
│       │           │   ├── unstable_hit_3.json
│       │           │   ├── vertical_thrust_1.json
│       │           │   ├── vertical_thrust_2.json
│       │           │   ├── vertical_thrust_3.json
│       │           │   ├── weakness_1.json
│       │           │   ├── wither_1.json
│       │           │   ├── wither_2.json
│       │           │   └── wither_3.json
│       │           └── passive
│       │               ├── armor_1.json
│       │               ├── armor_2.json
│       │               ├── armor_3.json
│       │               ├── attack_damage_1.json
│       │               ├── attack_damage_2.json
│       │               ├── attack_speed_1.json
│       │               ├── attack_speed_2.json
│       │               ├── follow_range_1.json
│       │               ├── follow_range_2.json
│       │               ├── knockback_resistance_1.json
│       │               ├── knockback_resistance_2.json
│       │               ├── knockback_resistance_3.json
│       │               ├── max_health_1.json
│       │               ├── max_health_2.json
│       │               ├── max_health_3.json
│       │               └── movement_speed_1.json
│       ├── fabric.mod.json
│       └── legacycreaturescorey.mixins.json
├── build.gradle
├── .cursorrules
├── docs
│   ├── API_REFERENCE.md
│   ├── GUIA_DESARROLLO.md
│   ├── informacion
│   │   ├── actualizacion de atributos de los mobs - Hoja 1.csv
│   │   └── WhatsApp Image 2025-11-13 at 12.54.51 AM.jpeg
│   ├── Legacy Creatures - Corey.md
│   ├── mutaciones.csv
│   ├── planes
│   │   ├── arquitectura_master.md
│   │   ├── plan general boceto.md
│   │   ├── plan_configuracion_externa.md
│   │   └── plan_logging.md
│   ├── scripts_agente
│   └── sobre_corey.md
├── estructura.md
├── .gitattributes
├── .github
│   └── workflows
│       └── build.yml
├── .gitignore
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradle.properties
├── gradlew
├── gradlew.bat
├── LICENSE
├── net
│   └── minecraft
│       ├── enchantment
│       │   └── Enchantment.java
│       └── registry
│           ├── BuiltinRegistries.java
│           ├── Registries.java
│           └── RegistryKeys.java
├── README.md
├── run
│   ├── command_history.txt
│   ├── config
│   │   ├── fabric
│   │   │   └── indigo-renderer.properties
│   │   ├── jade
│   │   │   ├── hide-blocks.json
│   │   │   ├── hide-entities.json
│   │   │   ├── hide-mob-effects.json
│   │   │   ├── jade.json
│   │   │   ├── profiles
│   │   │   │   ├── 1
│   │   │   │   │   └── jade.json
│   │   │   │   ├── 2
│   │   │   │   │   └── jade.json
│   │   │   │   └── 3
│   │   │   │       └── jade.json
│   │   │   ├── server-plugin-overrides.json
│   │   │   └── sort-order.json
│   │   ├── legacycreaturescorey.json
│   │   └── modmenu.json
│   ├── crash-reports
│   │   ├── crash-2025-11-12_19.41.22-client.txt
│   │   ├── crash-2025-11-12_19.53.54-client.txt
│   │   ├── crash-2025-11-12_19.54.24-client.txt
│   │   ├── crash-2025-11-13_16.18.11-server.txt
│   │   ├── crash-2025-11-13_21.53.51-client.txt
│   │   ├── crash-2025-11-13_22.14.14-server.txt
│   │   ├── crash-2025-11-15_15.58.51-server.txt
│   │   ├── crash-2025-11-15_16.31.37-server.txt
│   │   ├── crash-2025-11-15_16.33.49-server.txt
│   │   ├── crash-2025-11-17_03.10.53-client.txt
│   │   ├── crash-2025-11-18_00.39.06-client.txt
│   │   ├── crash-2025-11-21_02.29.53-client.txt
│   │   ├── crash-2025-11-21_02.47.27-client.txt
│   │   ├── crash-2025-11-21_02.50.48-client.txt
│   │   ├── crash-2025-11-23_21.20.15-server.txt
│   │   └── crash-2025-11-23_21.21.57-server.txt
│   ├── data
│   ├── datapacks
│   │   └── legacycreaturescorey_validation
│   │       ├── data
│   │       │   └── legacycreaturescorey
│   │       │       └── mutations
│   │       │           ├── calling_depths.json
│   │       │           ├── healing_spirit.json
│   │       │           ├── passive
│   │       │           │   ├── armor_1.json
│   │       │           │   ├── armor_2.json
│   │       │           │   ├── armor_3.json
│   │       │           │   ├── attack_damage_1.json
│   │       │           │   ├── attack_damage_2.json
│   │       │           │   ├── attack_speed_1.json
│   │       │           │   ├── attack_speed_2.json
│   │       │           │   ├── follow_range_1.json
│   │       │           │   ├── follow_range_2.json
│   │       │           │   ├── knockback_resistance_1.json
│   │       │           │   ├── knockback_resistance_2.json
│   │       │           │   ├── knockback_resistance_3.json
│   │       │           │   ├── max_health_1.json
│   │       │           │   ├── max_health_2.json
│   │       │           │   ├── max_health_3.json
│   │       │           │   └── movement_speed_1.json
│   │       │           └── volatile_aura.json
│   │       └── pack.mcmeta
│   ├── debug
│   │   ├── disconnect-2025-11-13_22.05.18-client.txt
│   │   ├── disconnect-2025-11-13_22.05.55-client.txt
│   │   ├── disconnect-2025-11-13_22.10.47-client.txt
│   │   └── disconnect-2025-11-13_22.10.59-client.txt
│   ├── debug-profile.json
│   ├── downloads
│   │   └── log.json
│   ├── .fabric
│   │   ├── processedMods
│   │   │   ├── jade-20.1.0+fabric-64b5720b4b825f21.jar
│   │   │   ├── legacycreatureshomeys-version0-64b5720b4b825f21.jar
│   │   │   ├── modmenu-16.0.0-rc.1-64b5720b4b825f21.jar
│   │   │   └── placeholder-api-2.8.0+1.21.9-5e2ecf82589d912d.jar
│   │   └── tmp
│   │       └── placeholder-api8626922124061463320.jar
│   ├── legacycreatures
│   │   └── reports
│   │       ├── legacy_creatures_report_2025-11-15_19.18.29.html
│   │       └── legacy_creatures_report_2025-11-15_19.19.37.json
│   ├── logs
│   │   ├── 2025-11-22-1.log.gz
│   │   ├── 2025-11-23-1.log.gz
│   │   ├── 2025-11-23-2.log.gz
│   │   ├── 2025-11-23-3.log.gz
│   │   ├── 2025-11-23-4.log.gz
│   │   ├── 2025-11-23-5.log.gz
│   │   ├── 2025-11-23-6.log.gz
│   │   ├── 2025-11-23-7.log.gz
│   │   ├── 2025-11-24-1.log.gz
│   │   ├── 2025-11-24-2.log.gz
│   │   ├── 2025-11-24-3.log.gz
│   │   ├── 2025-11-24-4.log.gz
│   │   ├── 2025-11-24-5.log.gz
│   │   ├── 2025-11-24-6.log.gz
│   │   ├── 2025-11-25-1.log.gz
│   │   ├── 2025-11-25-2.log.gz
│   │   ├── debug-1.log.gz
│   │   ├── debug-2.log.gz
│   │   ├── debug-3.log.gz
│   │   ├── debug-4.log.gz
│   │   ├── debug-5.log.gz
│   │   ├── debug.log
│   │   ├── latest.log
│   │   └── telemetry
│   ├── mods
│   │   ├── Jade-1.21.9-Fabric-20.1.0.jar
│   │   ├── legacycreatureshomey(version0).jar
│   │   └── modmenu-16.0.0-rc.1.jar
│   ├── options.txt
│   ├── resourcepacks
│   ├── resources
│   ├── saves
│   │   ├── New World
│   │   │   ├── advancements
│   │   │   │   ├── 05a2b883-672c-35b9-82ec-eccc4e0cabc2.json
│   │   │   │   ├── 05d3c308-6531-310d-988c-a6164eaf800d.json
│   │   │   │   ├── 0cb96bf3-4c77-3186-b066-73e68c05fc28.json
│   │   │   │   ├── 0d3f56a3-2b3f-38d6-a849-9d61b8f97a5c.json
│   │   │   │   ├── 0de21b13-73b3-37ba-84f2-92c78b6712c2.json
│   │   │   │   ├── 0ef61a2a-d91b-37b4-a10d-af67b917d543.json
│   │   │   │   ├── 1217a77c-23b0-3185-a8fb-c1d093f65cc0.json
│   │   │   │   ├── 1393f182-8170-3d81-bbcc-663c68900b91.json
│   │   │   │   ├── 171ff2be-e113-3c14-a482-20b6c4712810.json
│   │   │   │   ├── 19b458ba-c952-3dc3-bceb-bb32067d178f.json
│   │   │   │   ├── 1b5803ef-dab8-3673-85ae-9a9160028e1f.json
│   │   │   │   ├── 1ef891cf-32a6-31f4-b463-6277834cb67f.json
│   │   │   │   ├── 2164d6a3-d058-3632-9c13-ff2b4a7698db.json
│   │   │   │   ├── 216c785c-0df1-3594-b574-a2156a14723d.json
│   │   │   │   ├── 218a3571-b707-3992-b46a-db90429dc7f7.json
│   │   │   │   ├── 22dbc6e5-f5c0-33a5-9d21-617eb950afc0.json
│   │   │   │   ├── 2659329e-1c65-3850-9659-d27fc655aa3c.json
│   │   │   │   ├── 27630cc8-9d92-34f7-8c10-85caa979d8b3.json
│   │   │   │   ├── 28401ba7-dc22-34de-936c-d10dd3435a89.json
│   │   │   │   ├── 28de3746-47a4-326a-afac-3983252542f7.json
│   │   │   │   ├── 2ec36810-f0f3-3c4c-b9b5-ef9a4a85598d.json
│   │   │   │   ├── 34c6f761-a0d4-3d62-b5ee-df15e4530215.json
│   │   │   │   ├── 377f1ac0-6050-32e3-9d68-daf21c3caf7c.json
│   │   │   │   ├── 37fb5f1a-f986-3b16-8135-e7bdb177538f.json
│   │   │   │   ├── 38ee4029-e397-3902-8d79-d6c4d51a8fef.json
│   │   │   │   ├── 3c7cef0c-c4d3-3eb2-a04d-c369cce398ef.json
│   │   │   │   ├── 3da1e349-17b0-38ed-a07b-1e78c8ebd956.json
│   │   │   │   ├── 42c8facc-cbd2-36d2-8ac3-7dd20c6ce2a1.json
│   │   │   │   ├── 45c2c1e2-9dd4-3d62-8e53-82e2b4b32b4a.json
│   │   │   │   ├── 4bf8ac1c-047f-3d7f-9946-705bc571459f.json
│   │   │   │   ├── 4ebb02a4-f60f-3bbd-8cd6-a4ac4ed3f510.json
│   │   │   │   ├── 55199b58-d04f-3d22-9ce2-5472509a024b.json
│   │   │   │   ├── 56a259bf-cc44-38c1-8061-dd05953c60e0.json
│   │   │   │   ├── 588419cf-d7d5-3f7b-8fe7-89996b3b7551.json
│   │   │   │   ├── 5a8bc451-c1a7-3b73-97ff-34b045dbb0ff.json
│   │   │   │   ├── 5b6052a7-6bab-33d6-92c2-a3d77755ef06.json
│   │   │   │   ├── 5db36629-721d-3a36-8728-14b3ab112351.json
│   │   │   │   ├── 63415afc-3fc6-3204-b29f-d3e2c160b7a4.json
│   │   │   │   ├── 651bbe0e-980e-38d7-8fbb-83ebfec85347.json
│   │   │   │   ├── 66212eae-12d0-33f7-bd64-547255e16b3f.json
│   │   │   │   ├── 6634a609-43e8-3247-9054-c77a3a707d73.json
│   │   │   │   ├── 68d62d0c-b648-358d-9849-8d64b49bd6dc.json
│   │   │   │   ├── 691b5991-d81d-336c-9128-6e2de0ef0a95.json
│   │   │   │   ├── 79804575-3cdf-34b1-8aa3-c1036031e749.json
│   │   │   │   ├── 7daaf105-640b-34bc-83e6-6c045d1c9591.json
│   │   │   │   ├── 7de0b911-1474-3b51-bbfc-d3e0a184ff98.json
│   │   │   │   ├── 7e352200-5a1b-37ca-9d08-8fe764041962.json
│   │   │   │   ├── 8773eef5-d9ab-33f4-b901-5d723e8af879.json
│   │   │   │   ├── 8b435ab7-751f-377f-8d6e-243e4ae9abf2.json
│   │   │   │   ├── 8c1ef9e6-3632-3ca3-9eee-559f6e0a39df.json
│   │   │   │   ├── 8cfc5391-e37d-328f-a689-9b5f6c4663f5.json
│   │   │   │   ├── 8f2ecf49-60e3-3c84-b5c7-945f8f24636d.json
│   │   │   │   ├── 8fbcb74c-bd8d-3181-a022-3c858a10477d.json
│   │   │   │   ├── 90bb5d97-9dd4-3b7b-a71d-c51bbf8a4878.json
│   │   │   │   ├── 90ee7131-905c-3e5a-b4c0-60424db8bd36.json
│   │   │   │   ├── 94590a34-bd24-3d07-8605-517566cb65fe.json
│   │   │   │   ├── 94cce2b9-df97-3b51-a97b-9838f894b678.json
│   │   │   │   ├── 99e80a89-582d-3fb3-8388-49270da696a6.json
│   │   │   │   ├── 9ba1d9fb-277b-3ec4-9552-50e8dd85d3d4.json
│   │   │   │   ├── 9bfc7d2e-b7d2-36ba-8889-dc94b2f1b428.json
│   │   │   │   ├── a126324d-4d5f-37d1-a7c2-428b903619ef.json
│   │   │   │   ├── a240c974-e4f5-311c-82e7-f92bb39b2584.json
│   │   │   │   ├── a4081500-9e8c-36ae-a97c-f165f9882cba.json
│   │   │   │   ├── b04d78c5-604b-370c-97e9-acff5d35c648.json
│   │   │   │   ├── b6f7a483-95d4-3c1c-a80d-d6147f86003c.json
│   │   │   │   ├── bc3e28b6-7ce2-3598-9156-cbc22efaa5b0.json
│   │   │   │   ├── bd11acc2-8e1a-348b-89a6-a309159aa437.json
│   │   │   │   ├── c17a5542-4699-3d3f-81f1-a187d3ec58fe.json
│   │   │   │   ├── c6c946a7-7334-3c2c-8670-50d9130a2dd5.json
│   │   │   │   ├── d30b1ac0-9841-3b27-8824-151859abeeec.json
│   │   │   │   ├── d8673417-c4e9-3a05-9b69-9e1ec4bb9002.json
│   │   │   │   ├── d978d670-4b07-3a90-bfb6-b4e7c70fe7fc.json
│   │   │   │   ├── dbac8158-8d79-3f57-906a-4939f5c379cd.json
│   │   │   │   ├── dc5bf89f-805c-3c6b-8f75-15e42fa5be8f.json
│   │   │   │   ├── dd69aa73-45c1-3c79-904a-4ef5b22df161.json
│   │   │   │   ├── dec201cc-1fa4-3af7-9261-26208e4b16bf.json
│   │   │   │   ├── dfa57433-6d23-31fd-a282-bdca1175e553.json
│   │   │   │   ├── e93afb92-bc6e-34ce-b382-da5f2dd619b2.json
│   │   │   │   ├── ef82fb01-b598-3553-9c9c-a797489528f6.json
│   │   │   │   ├── f318a6c7-0ff1-368e-9d3e-1b850b84da5e.json
│   │   │   │   ├── f4755d8e-161a-3abe-8338-935e4189097a.json
│   │   │   │   ├── f56bf8d6-603c-3250-9b1f-ef8f865996e1.json
│   │   │   │   ├── fb99d07b-9b6d-3e33-b8da-b8b7c2f42608.json
│   │   │   │   ├── fd6f2cd7-e2c4-325a-9688-114cce28bfc9.json
│   │   │   │   └── fd848f75-c1a0-33b6-9d05-284593849e10.json
│   │   │   ├── data
│   │   │   ├── datapacks
│   │   │   ├── DIM-1
│   │   │   │   └── data
│   │   │   ├── DIM1
│   │   │   │   └── data
│   │   │   ├── entities
│   │   │   ├── playerdata
│   │   │   ├── poi
│   │   │   ├── region
│   │   │   ├── session.lock
│   │   │   └── stats
│   │   │       ├── 05a2b883-672c-35b9-82ec-eccc4e0cabc2.json
│   │   │       ├── 05d3c308-6531-310d-988c-a6164eaf800d.json
│   │   │       ├── 0cb96bf3-4c77-3186-b066-73e68c05fc28.json
│   │   │       ├── 0d3f56a3-2b3f-38d6-a849-9d61b8f97a5c.json
│   │   │       ├── 0de21b13-73b3-37ba-84f2-92c78b6712c2.json
│   │   │       ├── 0ef61a2a-d91b-37b4-a10d-af67b917d543.json
│   │   │       ├── 1217a77c-23b0-3185-a8fb-c1d093f65cc0.json
│   │   │       ├── 1393f182-8170-3d81-bbcc-663c68900b91.json
│   │   │       ├── 171ff2be-e113-3c14-a482-20b6c4712810.json
│   │   │       ├── 19b458ba-c952-3dc3-bceb-bb32067d178f.json
│   │   │       ├── 1b5803ef-dab8-3673-85ae-9a9160028e1f.json
│   │   │       ├── 1ef891cf-32a6-31f4-b463-6277834cb67f.json
│   │   │       ├── 2164d6a3-d058-3632-9c13-ff2b4a7698db.json
│   │   │       ├── 216c785c-0df1-3594-b574-a2156a14723d.json
│   │   │       ├── 218a3571-b707-3992-b46a-db90429dc7f7.json
│   │   │       ├── 22dbc6e5-f5c0-33a5-9d21-617eb950afc0.json
│   │   │       ├── 2659329e-1c65-3850-9659-d27fc655aa3c.json
│   │   │       ├── 27630cc8-9d92-34f7-8c10-85caa979d8b3.json
│   │   │       ├── 28401ba7-dc22-34de-936c-d10dd3435a89.json
│   │   │       ├── 28de3746-47a4-326a-afac-3983252542f7.json
│   │   │       ├── 2ec36810-f0f3-3c4c-b9b5-ef9a4a85598d.json
│   │   │       ├── 34c6f761-a0d4-3d62-b5ee-df15e4530215.json
│   │   │       ├── 377f1ac0-6050-32e3-9d68-daf21c3caf7c.json
│   │   │       ├── 37fb5f1a-f986-3b16-8135-e7bdb177538f.json
│   │   │       ├── 38ee4029-e397-3902-8d79-d6c4d51a8fef.json
│   │   │       ├── 3c7cef0c-c4d3-3eb2-a04d-c369cce398ef.json
│   │   │       ├── 3da1e349-17b0-38ed-a07b-1e78c8ebd956.json
│   │   │       ├── 42c8facc-cbd2-36d2-8ac3-7dd20c6ce2a1.json
│   │   │       ├── 45c2c1e2-9dd4-3d62-8e53-82e2b4b32b4a.json
│   │   │       ├── 4bf8ac1c-047f-3d7f-9946-705bc571459f.json
│   │   │       ├── 4ebb02a4-f60f-3bbd-8cd6-a4ac4ed3f510.json
│   │   │       ├── 55199b58-d04f-3d22-9ce2-5472509a024b.json
│   │   │       ├── 56a259bf-cc44-38c1-8061-dd05953c60e0.json
│   │   │       ├── 588419cf-d7d5-3f7b-8fe7-89996b3b7551.json
│   │   │       ├── 5a8bc451-c1a7-3b73-97ff-34b045dbb0ff.json
│   │   │       ├── 5b6052a7-6bab-33d6-92c2-a3d77755ef06.json
│   │   │       ├── 5db36629-721d-3a36-8728-14b3ab112351.json
│   │   │       ├── 63415afc-3fc6-3204-b29f-d3e2c160b7a4.json
│   │   │       ├── 651bbe0e-980e-38d7-8fbb-83ebfec85347.json
│   │   │       ├── 66212eae-12d0-33f7-bd64-547255e16b3f.json
│   │   │       ├── 6634a609-43e8-3247-9054-c77a3a707d73.json
│   │   │       ├── 68d62d0c-b648-358d-9849-8d64b49bd6dc.json
│   │   │       ├── 691b5991-d81d-336c-9128-6e2de0ef0a95.json
│   │   │       ├── 79804575-3cdf-34b1-8aa3-c1036031e749.json
│   │   │       ├── 7daaf105-640b-34bc-83e6-6c045d1c9591.json
│   │   │       ├── 7de0b911-1474-3b51-bbfc-d3e0a184ff98.json
│   │   │       ├── 7e352200-5a1b-37ca-9d08-8fe764041962.json
│   │   │       ├── 8773eef5-d9ab-33f4-b901-5d723e8af879.json
│   │   │       ├── 8b435ab7-751f-377f-8d6e-243e4ae9abf2.json
│   │   │       ├── 8c1ef9e6-3632-3ca3-9eee-559f6e0a39df.json
│   │   │       ├── 8cfc5391-e37d-328f-a689-9b5f6c4663f5.json
│   │   │       ├── 8f2ecf49-60e3-3c84-b5c7-945f8f24636d.json
│   │   │       ├── 8fbcb74c-bd8d-3181-a022-3c858a10477d.json
│   │   │       ├── 90bb5d97-9dd4-3b7b-a71d-c51bbf8a4878.json
│   │   │       ├── 90ee7131-905c-3e5a-b4c0-60424db8bd36.json
│   │   │       ├── 94590a34-bd24-3d07-8605-517566cb65fe.json
│   │   │       ├── 94cce2b9-df97-3b51-a97b-9838f894b678.json
│   │   │       ├── 99e80a89-582d-3fb3-8388-49270da696a6.json
│   │   │       ├── 9ba1d9fb-277b-3ec4-9552-50e8dd85d3d4.json
│   │   │       ├── 9bfc7d2e-b7d2-36ba-8889-dc94b2f1b428.json
│   │   │       ├── a126324d-4d5f-37d1-a7c2-428b903619ef.json
│   │   │       ├── a240c974-e4f5-311c-82e7-f92bb39b2584.json
│   │   │       ├── a4081500-9e8c-36ae-a97c-f165f9882cba.json
│   │   │       ├── b04d78c5-604b-370c-97e9-acff5d35c648.json
│   │   │       ├── b6f7a483-95d4-3c1c-a80d-d6147f86003c.json
│   │   │       ├── bc3e28b6-7ce2-3598-9156-cbc22efaa5b0.json
│   │   │       ├── bd11acc2-8e1a-348b-89a6-a309159aa437.json
│   │   │       ├── c17a5542-4699-3d3f-81f1-a187d3ec58fe.json
│   │   │       ├── c6c946a7-7334-3c2c-8670-50d9130a2dd5.json
│   │   │       ├── d30b1ac0-9841-3b27-8824-151859abeeec.json
│   │   │       ├── d8673417-c4e9-3a05-9b69-9e1ec4bb9002.json
│   │   │       ├── d978d670-4b07-3a90-bfb6-b4e7c70fe7fc.json
│   │   │       ├── dbac8158-8d79-3f57-906a-4939f5c379cd.json
│   │   │       ├── dc5bf89f-805c-3c6b-8f75-15e42fa5be8f.json
│   │   │       ├── dd69aa73-45c1-3c79-904a-4ef5b22df161.json
│   │   │       ├── dec201cc-1fa4-3af7-9261-26208e4b16bf.json
│   │   │       ├── dfa57433-6d23-31fd-a282-bdca1175e553.json
│   │   │       ├── e93afb92-bc6e-34ce-b382-da5f2dd619b2.json
│   │   │       ├── ef82fb01-b598-3553-9c9c-a797489528f6.json
│   │   │       ├── f318a6c7-0ff1-368e-9d3e-1b850b84da5e.json
│   │   │       ├── f4755d8e-161a-3abe-8338-935e4189097a.json
│   │   │       ├── f56bf8d6-603c-3250-9b1f-ef8f865996e1.json
│   │   │       ├── fb99d07b-9b6d-3e33-b8da-b8b7c2f42608.json
│   │   │       ├── fd6f2cd7-e2c4-325a-9688-114cce28bfc9.json
│   │   │       └── fd848f75-c1a0-33b6-9d05-284593849e10.json
│   │   └── New World (1)
│   │       ├── advancements
│   │       │   ├── 01986ee6-ca3c-3b64-bdcd-02039da5963f.json
│   │       │   ├── 09444461-95e8-3d4c-b4c8-61c0297ca18f.json
│   │       │   ├── 1344ba46-6b08-3a2e-813a-3106d9215e61.json
│   │       │   ├── 1a855883-f969-3f42-8f5f-11e58eba890c.json
│   │       │   ├── 1de0626e-6c80-37c2-856b-2a8735302b74.json
│   │       │   ├── 2164d6a3-d058-3632-9c13-ff2b4a7698db.json
│   │       │   ├── 22fa5c62-1593-3376-ba15-517f68f16a46.json
│   │       │   ├── 23345143-148b-327b-8641-d2091803951a.json
│   │       │   ├── 24078ad8-d7e0-3e5c-a4a9-a3bc169a7177.json
│   │       │   ├── 24a251d3-c89c-39ba-8cae-1f7d4c0691c9.json
│   │       │   ├── 25887e3a-1ce2-3acf-9352-3d844017a709.json
│   │       │   ├── 25d814e7-4101-3c44-9bf7-65f7d6c6dd55.json
│   │       │   ├── 2654e3c3-150d-3857-a426-0b141796a4e0.json
│   │       │   ├── 2814925e-9add-35ba-8fa3-433e445fe43d.json
│   │       │   ├── 3c0b2b93-1042-3a3d-b6b2-a97839f06c80.json
│   │       │   ├── 3c28cca8-db38-324d-ac91-779beed87c8d.json
│   │       │   ├── 48c2e6c7-8875-31cc-9a01-be7eaa5f9386.json
│   │       │   ├── 492dc575-b72e-3d83-b2fd-33ab63727150.json
│   │       │   ├── 50dde602-6a46-34aa-95e3-8463932e70f7.json
│   │       │   ├── 56a259bf-cc44-38c1-8061-dd05953c60e0.json
│   │       │   ├── 5a8bc451-c1a7-3b73-97ff-34b045dbb0ff.json
│   │       │   ├── 5b615aa5-0042-3651-8971-2294273b0cec.json
│   │       │   ├── 5e0a92cf-afd6-30a3-b846-b305e832c04d.json
│   │       │   ├── 5f97b8e7-1c17-34e1-ba31-af3a61d45498.json
│   │       │   ├── 6a12f6e4-e9d6-38e2-bb44-10484bb75cf2.json
│   │       │   ├── 71ee4b7e-330e-3d50-90a4-3d964a618ea8.json
│   │       │   ├── 7a8e1502-fe7e-37fe-bfcf-5a4ebee14073.json
│   │       │   ├── 7daaf105-640b-34bc-83e6-6c045d1c9591.json
│   │       │   ├── 7dc80f62-93ab-31a4-97b1-46f4b80402c9.json
│   │       │   ├── 7e94e5c9-1ae4-352e-9cf6-8305e5b127c7.json
│   │       │   ├── 7fc02cf2-da5a-351c-a700-aaa88bd6d88d.json
│   │       │   ├── 94cce2b9-df97-3b51-a97b-9838f894b678.json
│   │       │   ├── 99963b1e-252d-3ba6-8126-9071dd762e6c.json
│   │       │   ├── 9c6f7109-b578-36c3-a272-028e06c40407.json
│   │       │   ├── a0a7ec06-d96a-3187-943b-79bf7e87b3a2.json
│   │       │   ├── b5b2ff24-9e23-373f-97d2-19884357bdea.json
│   │       │   ├── d0f5fb0c-30f8-3708-84d1-e619aef4d8f4.json
│   │       │   ├── d1587dea-b047-3ee8-b46a-f3d599824159.json
│   │       │   ├── e4358028-bc3b-38cc-b645-4d08d3b4f456.json
│   │       │   ├── e5dd22dc-f598-3b10-8140-672a285a563c.json
│   │       │   ├── e7efa64c-d66a-3088-a9f4-5a1090261573.json
│   │       │   ├── eed453d8-1b3a-3fa3-9fc5-1cf433d73521.json
│   │       │   ├── f07953aa-b16d-3fed-8d8a-d64904ef9606.json
│   │       │   ├── f189da80-d431-3f80-9126-43ccee16f040.json
│   │       │   ├── f4755d8e-161a-3abe-8338-935e4189097a.json
│   │       │   ├── f7621ff1-653f-32ec-8621-00c0392bfbcc.json
│   │       │   └── fd4dff36-f6aa-35f9-8e99-ec749e44651e.json
│   │       ├── data
│   │       ├── datapacks
│   │       ├── DIM-1
│   │       │   ├── data
│   │       │   ├── entities
│   │       │   ├── poi
│   │       │   └── region
│   │       ├── DIM1
│   │       │   └── data
│   │       ├── entities
│   │       ├── playerdata
│   │       ├── poi
│   │       ├── region
│   │       ├── session.lock
│   │       └── stats
│   │           ├── 01986ee6-ca3c-3b64-bdcd-02039da5963f.json
│   │           ├── 09444461-95e8-3d4c-b4c8-61c0297ca18f.json
│   │           ├── 1344ba46-6b08-3a2e-813a-3106d9215e61.json
│   │           ├── 1a855883-f969-3f42-8f5f-11e58eba890c.json
│   │           ├── 1de0626e-6c80-37c2-856b-2a8735302b74.json
│   │           ├── 2164d6a3-d058-3632-9c13-ff2b4a7698db.json
│   │           ├── 22fa5c62-1593-3376-ba15-517f68f16a46.json
│   │           ├── 23345143-148b-327b-8641-d2091803951a.json
│   │           ├── 24078ad8-d7e0-3e5c-a4a9-a3bc169a7177.json
│   │           ├── 24a251d3-c89c-39ba-8cae-1f7d4c0691c9.json
│   │           ├── 25887e3a-1ce2-3acf-9352-3d844017a709.json
│   │           ├── 25d814e7-4101-3c44-9bf7-65f7d6c6dd55.json
│   │           ├── 2654e3c3-150d-3857-a426-0b141796a4e0.json
│   │           ├── 2814925e-9add-35ba-8fa3-433e445fe43d.json
│   │           ├── 3c0b2b93-1042-3a3d-b6b2-a97839f06c80.json
│   │           ├── 3c28cca8-db38-324d-ac91-779beed87c8d.json
│   │           ├── 48c2e6c7-8875-31cc-9a01-be7eaa5f9386.json
│   │           ├── 492dc575-b72e-3d83-b2fd-33ab63727150.json
│   │           ├── 50dde602-6a46-34aa-95e3-8463932e70f7.json
│   │           ├── 56a259bf-cc44-38c1-8061-dd05953c60e0.json
│   │           ├── 5a8bc451-c1a7-3b73-97ff-34b045dbb0ff.json
│   │           ├── 5b615aa5-0042-3651-8971-2294273b0cec.json
│   │           ├── 5e0a92cf-afd6-30a3-b846-b305e832c04d.json
│   │           ├── 5f97b8e7-1c17-34e1-ba31-af3a61d45498.json
│   │           ├── 6a12f6e4-e9d6-38e2-bb44-10484bb75cf2.json
│   │           ├── 71ee4b7e-330e-3d50-90a4-3d964a618ea8.json
│   │           ├── 7a8e1502-fe7e-37fe-bfcf-5a4ebee14073.json
│   │           ├── 7daaf105-640b-34bc-83e6-6c045d1c9591.json
│   │           ├── 7dc80f62-93ab-31a4-97b1-46f4b80402c9.json
│   │           ├── 7e94e5c9-1ae4-352e-9cf6-8305e5b127c7.json
│   │           ├── 7fc02cf2-da5a-351c-a700-aaa88bd6d88d.json
│   │           ├── 94cce2b9-df97-3b51-a97b-9838f894b678.json
│   │           ├── 99963b1e-252d-3ba6-8126-9071dd762e6c.json
│   │           ├── 9c6f7109-b578-36c3-a272-028e06c40407.json
│   │           ├── a0a7ec06-d96a-3187-943b-79bf7e87b3a2.json
│   │           ├── b5b2ff24-9e23-373f-97d2-19884357bdea.json
│   │           ├── d0f5fb0c-30f8-3708-84d1-e619aef4d8f4.json
│   │           ├── d1587dea-b047-3ee8-b46a-f3d599824159.json
│   │           ├── e4358028-bc3b-38cc-b645-4d08d3b4f456.json
│   │           ├── e5dd22dc-f598-3b10-8140-672a285a563c.json
│   │           ├── e7efa64c-d66a-3088-a9f4-5a1090261573.json
│   │           ├── eed453d8-1b3a-3fa3-9fc5-1cf433d73521.json
│   │           ├── f07953aa-b16d-3fed-8d8a-d64904ef9606.json
│   │           ├── f189da80-d431-3f80-9126-43ccee16f040.json
│   │           ├── f4755d8e-161a-3abe-8338-935e4189097a.json
│   │           ├── f7621ff1-653f-32ec-8621-00c0392bfbcc.json
│   │           └── fd4dff36-f6aa-35f9-8e99-ec749e44651e.json
│   ├── screenshots
│   │   └── debug
│   │       ├── minecraft_textures_atlas_armor_trims.png.txt
│   │       ├── minecraft_textures_atlas_banner_patterns.png.txt
│   │       ├── minecraft_textures_atlas_beds.png.txt
│   │       ├── minecraft_textures_atlas_blocks.png.txt
│   │       ├── minecraft_textures_atlas_chest.png.txt
│   │       ├── minecraft_textures_atlas_decorated_pot.png.txt
│   │       ├── minecraft_textures_atlas_gui.png.txt
│   │       ├── minecraft_textures_atlas_map_decorations.png.txt
│   │       ├── minecraft_textures_atlas_paintings.png.txt
│   │       ├── minecraft_textures_atlas_particles.png.txt
│   │       ├── minecraft_textures_atlas_shield_patterns.png.txt
│   │       ├── minecraft_textures_atlas_shulker_boxes.png.txt
│   │       └── minecraft_textures_atlas_signs.png.txt
│   └── usercache.json
├── settings.gradle
├── src
│   ├── client
│   │   ├── java
│   │   │   └── com
│   │   │       └── mrsasayo
│   │   │           └── legacycreaturescorey
│   │   │               ├── client
│   │   │               │   ├── ClientDifficultyHud.java
│   │   │               │   └── ClientEffectHandler.java
│   │   │               ├── LegacycreaturescoreyClient.java
│   │   │               ├── LegacycreaturescoreyDataGenerator.java
│   │   │               └── mixin
│   │   │                   └── client
│   │   │                       ├── ClientPlayerEntityMixin.java
│   │   │                       ├── ExampleClientMixin.java
│   │   │                       └── KeyboardInputMixin.java
│   │   └── resources
│   │       └── legacycreaturescorey.client.mixins.json
│   └── main
│       ├── java
│       │   └── com
│       │       └── mrsasayo
│       │           └── legacycreaturescorey
│       │               ├── content
│       │               │   ├── block
│       │               │   │   ├── FakeLootPileBlock.java
│       │               │   │   └── ModBlocks.java
│       │               │   ├── health
│       │               │   │   └── CoreyHealthMonitor.java
│       │               │   ├── item
│       │               │   │   ├── GhostFragmentItem.java
│       │               │   │   └── ModItems.java
│       │               │   └── status
│       │               │       ├── bleeding_status_effect.java
│       │               │       ├── HealingReductionStatusEffect.java
│       │               │       ├── HeavyGravityStatusEffect.java
│       │               │       ├── ModStatusEffects.java
│       │               │       ├── NotifyingStatusEffect.java
│       │               │       ├── precarious_hunger_status_effect.java
│       │               │       ├── SlipperyFloorStatusEffect.java
│       │               │       ├── StatusEffectSyncHelper.java
│       │               │       └── StatusEffectTicker.java
│       │               ├── core
│       │               │   ├── api
│       │               │   │   ├── AntiFarmDashboardApi.java
│       │               │   │   ├── CoreyAPI.java
│       │               │   │   ├── event
│       │               │   │   │   ├── AntiFarmDashboardEvents.java
│       │               │   │   │   ├── AntiFarmEvents.java
│       │               │   │   │   ├── MutationDataEvents.java
│       │               │   │   │   ├── TieredLootTableEvents.java
│       │               │   │   │   ├── TierEvents.java
│       │               │   │   │   ├── TierLootEvents.java
│       │               │   │   │   └── TierLootTelemetryEvents.java
│       │               │   │   └── LegacyCreaturesTierApi.java
│       │               │   ├── command
│       │               │   │   ├── corey
│       │               │   │   │   ├── CoreyCommand.java
│       │               │   │   │   ├── CoreyCommandPermissions.java
│       │               │   │   │   ├── CoreyCommandShared.java
│       │               │   │   │   ├── CoreyCommandUtil.java
│       │               │   │   │   ├── CoreyDebugCommands.java
│       │               │   │   │   ├── CoreyMenuCommand.java
│       │               │   │   │   ├── CoreyMutationCommand.java
│       │               │   │   │   ├── CoreyReloadCommand.java
│       │               │   │   │   ├── CoreySpawnCommands.java
│       │               │   │   │   └── CoreyStatsCommand.java
│       │               │   │   ├── CoreyHudCommand.java
│       │               │   │   └── MutationCommand.java
│       │               │   ├── component
│       │               │   │   ├── ModDataAttachments.java
│       │               │   │   └── PlayerDifficultyData.java
│       │               │   ├── config
│       │               │   │   └── CoreyConfig.java
│       │               │   └── network
│       │               │       ├── ClientCapabilitiesPayload.java
│       │               │       ├── ClientEffectPayload.java
│       │               │       ├── ClientEffectType.java
│       │               │       ├── ClientFeature.java
│       │               │       ├── DifficultySyncPayload.java
│       │               │       ├── ModNetworking.java
│       │               │       └── ServerHelloPayload.java
│       │               ├── feature
│       │               │   ├── antifarm
│       │               │   │   ├── AntiFarmManager.java
│       │               │   │   ├── ChunkActivityData.java
│       │               │   │   └── data
│       │               │   │       └── AntiFarmExclusionDataLoader.java
│       │               │   ├── difficulty
│       │               │   │   ├── CoreyServerState.java
│       │               │   │   ├── DifficultyManager.java
│       │               │   │   ├── DifficultyTickHandler.java
│       │               │   │   ├── EffectiveDifficultyCalculator.java
│       │               │   │   └── MobTier.java
│       │               │   ├── loot
│       │               │   │   ├── CoreyLootModifiers.java
│       │               │   │   └── data
│       │               │   │       ├── IntRange.java
│       │               │   │       ├── TieredLootDataLoader.java
│       │               │   │       ├── TieredLootManager.java
│       │               │   │       └── TieredMobLoot.java
│       │               │   ├── mob
│       │               │   │   ├── data
│       │               │   │   │   ├── BiomeTierWeightDataLoader.java
│       │               │   │   │   ├── MobAttributeDataLoader.java
│       │               │   │   │   └── MobTierRuleDataLoader.java
│       │               │   │   ├── FuryHelper.java
│       │               │   │   ├── MobLegacyComponent.java
│       │               │   │   ├── MobLegacyData.java
│       │               │   │   ├── MobSpawnHandler.java
│       │               │   │   ├── TierManager.java
│       │               │   │   ├── TierParticleHelper.java
│       │               │   │   ├── TierParticleTicker.java
│       │               │   │   └── TierProbabilityCalculator.java
│       │               │   └── synergy
│       │               │       ├── CoreySynergyApi.java
│       │               │       ├── SynergyManager.java
│       │               │       ├── SynergyModule.java
│       │               │       ├── SynergyProvider.java
│       │               │       └── SynergyStatus.java
│       │               ├── Legacycreaturescorey.java
│       │               ├── mixin
│       │               │   ├── accessor
│       │               │   │   ├── entity_damage_cooldown_accessor.java
│       │               │   │   ├── living_entity_hurt_time_accessor.java
│       │               │   │   ├── mob_entity_goal_accessor.java
│       │               │   │   └── persistent_projectile_entity_accessor.java
│       │               │   ├── general
│       │               │   │   ├── flee_entity_goal_mixin.java
│       │               │   │   └── phantom_swoop_movement_goal_mixin.java
│       │               │   ├── mechanic
│       │               │   │   ├── BeeEntityMixin.java
│       │               │   │   ├── HungerManagerMixin.java
│       │               │   │   ├── ItemEntityMixin.java
│       │               │   │   ├── LivingEntityStatusEffectMixin.java
│       │               │   │   ├── ServerPlayerInteractionManagerMixin.java
│       │               │   │   └── StatusEffectInstanceMixin.java
│       │               │   └── mutation
│       │               │       ├── GoatRamDamageMixin.java
│       │               │       ├── LivingEntityDamageMixin.java
│       │               │       ├── LivingEntityHealMixin.java
│       │               │       ├── LivingEntityKillMixin.java
│       │               │       ├── SnowballEntityMixin.java
│       │               │       └── VindicatorDamageMixin.java
│       │               └── mutation
│       │                   ├── action
│       │                   │   ├── auras
│       │                   │   │   ├── AllyDeathHealAuraAction.java
│       │                   │   │   ├── AttributeAuraAction.java
│       │                   │   │   ├── corruption_aura_1_action.java
│       │                   │   │   ├── corruption_aura_2_action.java
│       │                   │   │   ├── corruption_aura_3_action.java
│       │                   │   │   ├── DamageAuraAction.java
│       │                   │   │   ├── deep_darkness_aura_1_action.java
│       │                   │   │   ├── deep_darkness_aura_2_action.java
│       │                   │   │   ├── deep_darkness_aura_3_action.java
│       │                   │   │   ├── DeepDarknessAuraAction.java
│       │                   │   │   ├── entropy_aura_1_action.java
│       │                   │   │   ├── entropy_aura_2_action.java
│       │                   │   │   ├── entropy_aura_3_action.java
│       │                   │   │   ├── EntropyAuraAction.java
│       │                   │   │   ├── horde_beacon_aura_1_action.java
│       │                   │   │   ├── horde_beacon_aura_2_action.java
│       │                   │   │   ├── horde_beacon_aura_3_action.java
│       │                   │   │   ├── HordeBeaconAuraAction.java
│       │                   │   │   ├── HordeBeaconHandler.java
│       │                   │   │   ├── HordeBeaconSource.java
│       │                   │   │   ├── interference_aura_1_action.java
│       │                   │   │   ├── interference_aura_2_action.java
│       │                   │   │   ├── interference_aura_3_action.java
│       │                   │   │   ├── InterferenceAuraAction.java
│       │                   │   │   ├── InterferenceHandler.java
│       │                   │   │   ├── InterferenceSource.java
│       │                   │   │   ├── oppressive_presence_aura_1_action.java
│       │                   │   │   ├── oppressive_presence_aura_2_action.java
│       │                   │   │   ├── oppressive_presence_aura_3_action.java
│       │                   │   │   ├── OppressivePresenceHandler.java
│       │                   │   │   ├── OppressivePresenceSource.java
│       │                   │   │   ├── PhantasmalHandler.java
│       │                   │   │   ├── PhantasmalSource.java
│       │                   │   │   ├── phantasmal_veil_aura_1_action.java
│       │                   │   │   ├── phantasmal_veil_aura_2_action.java
│       │                   │   │   ├── phantasmal_veil_aura_3_action.java
│       │                   │   │   ├── PhantasmalVeilAuraAction.java
│       │                   │   │   ├── projectile_shroud_aura_1_action.java
│       │                   │   │   ├── projectile_shroud_aura_2_action.java
│       │                   │   │   ├── projectile_shroud_aura_3_action.java
│       │                   │   │   ├── ProjectileShroudAuraAction.java
│       │                   │   │   ├── ProjectileShroudHandler.java
│       │                   │   │   ├── ProjectileShroudSource.java
│       │                   │   │   ├── psionic_thorns_aura_1_action.java
│       │                   │   │   ├── psionic_thorns_aura_2_action.java
│       │                   │   │   ├── psionic_thorns_aura_3_action.java
│       │                   │   │   ├── PsionicThornsAuraAction.java
│       │                   │   │   ├── PsionicThornsHandler.java
│       │                   │   │   ├── PsionicThornsSource.java
│       │                   │   │   ├── stasis_field_aura_1_action.java
│       │                   │   │   ├── stasis_field_aura_2_action.java
│       │                   │   │   ├── stasis_field_aura_3_action.java
│       │                   │   │   ├── StasisHandler.java
│       │                   │   │   ├── StasisSource.java
│       │                   │   │   ├── StatusEffectAuraAction.java
│       │                   │   │   ├── vanguards_bulwark_aura_1_action.java
│       │                   │   │   ├── vanguards_bulwark_aura_2_action.java
│       │                   │   │   ├── vanguards_bulwark_aura_3_action.java
│       │                   │   │   ├── VanguardsBulwarkHandler.java
│       │                   │   │   ├── VanguardsBulwarkSource.java
│       │                   │   │   ├── virulent_growth_aura_1_action.java
│       │                   │   │   ├── virulent_growth_aura_2_action.java
│       │                   │   │   ├── virulent_growth_aura_3_action.java
│       │                   │   │   ├── VirulentHandler.java
│       │                   │   │   └── VirulentSource.java
│       │                   │   ├── helper
│       │                   │   │   ├── DetonatingRemainsManager.java
│       │                   │   │   ├── FakeLootPileManager.java
│       │                   │   │   ├── GroundHazardManager.java
│       │                   │   │   ├── HealAction.java
│       │                   │   │   ├── LootDropHelper.java
│       │                   │   │   ├── RealityCollapseManager.java
│       │                   │   │   └── SummonMobAction.java
│       │                   │   ├── mob_exclusive
│       │                   │   │   ├── abyssal_armor_1_action.java
│       │                   │   │   ├── abyssal_armor_2_action.java
│       │                   │   │   ├── abyssal_armor_3_action.java
│       │                   │   │   ├── abyssal_armor_base_action.java
│       │                   │   │   ├── abyssal_maneuvers_1_action.java
│       │                   │   │   ├── abyssal_maneuvers_2_action.java
│       │                   │   │   ├── abyssal_maneuvers_3_action.java
│       │                   │   │   ├── acidic_core_1_action.java
│       │                   │   │   ├── acidic_core_2_action.java
│       │                   │   │   ├── acidic_core_3_action.java
│       │                   │   │   ├── aerial_maneuvers_1_action.java
│       │                   │   │   ├── aerial_maneuvers_2_action.java
│       │                   │   │   ├── aerial_maneuvers_3_action.java
│       │                   │   │   ├── alphas_vengeance_1_action.java
│       │                   │   │   ├── alphas_vengeance_2_action.java
│       │                   │   │   ├── alphas_vengeance_3_action.java
│       │                   │   │   ├── alphas_vengeance_base_action.java
│       │                   │   │   ├── ambusher_1_action.java
│       │                   │   │   ├── ambusher_2_action.java
│       │                   │   │   ├── ambusher_3_action.java
│       │                   │   │   ├── ambusher_base_action.java
│       │                   │   │   ├── amphibious_assault_1_action.java
│       │                   │   │   ├── amphibious_assault_2_action.java
│       │                   │   │   ├── amphibious_assault_3_action.java
│       │                   │   │   ├── amphibious_assault_base_action.java
│       │                   │   │   ├── ancient_curse_1_action.java
│       │                   │   │   ├── ancient_curse_2_action.java
│       │                   │   │   ├── ancient_curse_3_action.java
│       │                   │   │   ├── ancient_curse_base_action.java
│       │                   │   │   ├── ancient_curse_curse_tracker.java
│       │                   │   │   ├── apiarian_warfare_1_action.java
│       │                   │   │   ├── apiarian_warfare_2_action.java
│       │                   │   │   ├── apiarian_warfare_3_action.java
│       │                   │   │   ├── apiarian_warfare_base_action.java
│       │                   │   │   ├── aquatic_stalker_1_action.java
│       │                   │   │   ├── aquatic_stalker_2_action.java
│       │                   │   │   ├── aquatic_stalker_3_action.java
│       │                   │   │   ├── aquatic_stalker_base_action.java
│       │                   │   │   ├── arctic_fortitude_1_action.java
│       │                   │   │   ├── arctic_fortitude_2_action.java
│       │                   │   │   ├── arctic_fortitude_3_action.java
│       │                   │   │   ├── arctic_fortitude_base_action.java
│       │                   │   │   ├── axe_mastery_1_action.java
│       │                   │   │   ├── axe_mastery_2_action.java
│       │                   │   │   ├── axe_mastery_3_action.java
│       │                   │   │   ├── axe_mastery_base_action.java
│       │                   │   │   ├── bamboo_eater_1_action.java
│       │                   │   │   ├── bamboo_eater_2_action.java
│       │                   │   │   ├── bamboo_eater_3_action.java
│       │                   │   │   ├── bamboo_eater_base_action.java
│       │                   │   │   ├── bastion_guard_1_action.java
│       │                   │   │   ├── bastion_guard_2_action.java
│       │                   │   │   ├── bastion_guard_3_action.java
│       │                   │   │   ├── bastion_guard_base_action.java
│       │                   │   │   ├── battering_ram_1_action.java
│       │                   │   │   ├── battering_ram_2_action.java
│       │                   │   │   ├── battering_ram_3_action.java
│       │                   │   │   ├── battering_ram_base_action.java
│       │                   │   │   ├── beam_refraction_1_action.java
│       │                   │   │   ├── beam_refraction_2_action.java
│       │                   │   │   ├── beam_refraction_3_action.java
│       │                   │   │   ├── beam_refraction_base_action.java
│       │                   │   │   ├── blizzard_orb_1_action.java
│       │                   │   │   ├── blizzard_orb_2_action.java
│       │                   │   │   ├── blizzard_orb_3_action.java
│       │                   │   │   ├── blizzard_orb_base_action.java
│       │                   │   │   ├── bloodlust_1_action.java
│       │                   │   │   ├── bloodlust_2_action.java
│       │                   │   │   ├── bloodlust_3_action.java
│       │                   │   │   ├── bloodlust_base_action.java
│       │                   │   │   ├── boar_frenzy_1_action.java
│       │                   │   │   ├── boar_frenzy_2_action.java
│       │                   │   │   ├── boar_frenzy_3_action.java
│       │                   │   │   ├── boar_frenzy_base_action.java
│       │                   │   │   ├── GhastDiveBombAction.java
│       │                   │   │   └── GhastLateralDashAction.java
│       │                   │   ├── on_death
│       │                   │   │   ├── DetonatingRemainsOnDeathAction.java
│       │                   │   │   ├── FakeLootPileOnDeathAction.java
│       │                   │   │   ├── FinalBurstOnDeathAction.java
│       │                   │   │   ├── FinalGambitOnDeathAction.java
│       │                   │   │   ├── GhostFragmentsOnDeathAction.java
│       │                   │   │   ├── GroundHazardOnDeathAction.java
│       │                   │   │   ├── LootScatterOnDeathAction.java
│       │                   │   │   └── StatusEffectOnDeathAction.java
│       │                   │   ├── on_hit
│       │                   │   │   ├── anti_armor_1_action.java
│       │                   │   │   ├── anti_armor_2_action.java
│       │                   │   │   ├── anti_armor_3_action.java
│       │                   │   │   ├── anti_armor_base_action.java
│       │                   │   │   ├── anti_shield_1_action.java
│       │                   │   │   ├── anti_shield_2_action.java
│       │                   │   │   ├── anti_shield_base_action.java
│       │                   │   │   ├── bleeding_1_action.java
│       │                   │   │   ├── bleeding_2_action.java
│       │                   │   │   ├── bleeding_3_action.java
│       │                   │   │   ├── bleeding_base_action.java
│       │                   │   │   ├── bleeding_effect_helper.java
│       │                   │   │   ├── blindness_1_action.java
│       │                   │   │   ├── chaos_touch_1_action.java
│       │                   │   │   ├── chaos_touch_2_action.java
│       │                   │   │   ├── chaos_touch_3_action.java
│       │                   │   │   ├── chaos_touch_base_action.java
│       │                   │   │   ├── concussive_blow_1_action.java
│       │                   │   │   ├── concussive_blow_2_action.java
│       │                   │   │   ├── concussive_blow_3_action.java
│       │                   │   │   ├── concussive_blow_base_action.java
│       │                   │   │   ├── critical_1_action.java
│       │                   │   │   ├── critical_2_action.java
│       │                   │   │   ├── critical_3_action.java
│       │                   │   │   ├── critical_base_action.java
│       │                   │   │   ├── deafening_strike_1_action.java
│       │                   │   │   ├── deafening_strike_2_action.java
│       │                   │   │   ├── deafening_strike_3_action.java
│       │                   │   │   ├── deafening_strike_base_action.java
│       │                   │   │   ├── disarm_1_action.java
│       │                   │   │   ├── essence_siphon_1_action.java
│       │                   │   │   ├── essence_siphon_2_action.java
│       │                   │   │   ├── essence_siphon_3_action.java
│       │                   │   │   ├── essence_siphon_base_action.java
│       │                   │   │   ├── experience_theft_1_action.java
│       │                   │   │   ├── experience_theft_2_action.java
│       │                   │   │   ├── experience_theft_3_action.java
│       │                   │   │   ├── experience_theft_base_action.java
│       │                   │   │   ├── fire_1_action.java
│       │                   │   │   ├── fire_2_action.java
│       │                   │   │   ├── fire_3_action.java
│       │                   │   │   ├── fire_base_action.java
│       │                   │   │   ├── freezing_1_action.java
│       │                   │   │   ├── freezing_2_action.java
│       │                   │   │   ├── freezing_3_action.java
│       │                   │   │   ├── freezing_base_action.java
│       │                   │   │   ├── frenzy_1_action.java
│       │                   │   │   ├── frenzy_2_action.java
│       │                   │   │   ├── frenzy_3_action.java
│       │                   │   │   ├── frenzy_base_action.java
│       │                   │   │   ├── glowing_1_action.java
│       │                   │   │   ├── glowing_2_action.java
│       │                   │   │   ├── glowing_3_action.java
│       │                   │   │   ├── glowing_base_action.java
│       │                   │   │   ├── hunger_1_action.java
│       │                   │   │   ├── hunger_2_action.java
│       │                   │   │   ├── hunger_3_action.java
│       │                   │   │   ├── hunger_base_action.java
│       │                   │   │   ├── levitation_1_action.java
│       │                   │   │   ├── levitation_2_action.java
│       │                   │   │   ├── levitation_base_action.java
│       │                   │   │   ├── mining_fatigue_1_action.java
│       │                   │   │   ├── mining_fatigue_2_action.java
│       │                   │   │   ├── mining_fatigue_3_action.java
│       │                   │   │   ├── mining_fatigue_base_action.java
│       │                   │   │   ├── mortal_wound_1_action.java
│       │                   │   │   ├── mortal_wound_2_action.java
│       │                   │   │   ├── mortal_wound_3_action.java
│       │                   │   │   ├── mortal_wound_base_action.java
│       │                   │   │   ├── nausea_1_action.java
│       │                   │   │   ├── pain_link_1_action.java
│       │                   │   │   ├── pain_link_2_action.java
│       │                   │   │   ├── pain_link_3_action.java
│       │                   │   │   ├── pain_link_base_action.java
│       │                   │   │   ├── poison_1_action.java
│       │                   │   │   ├── poison_2_action.java
│       │                   │   │   ├── poison_3_action.java
│       │                   │   │   ├── push_1_action.java
│       │                   │   │   ├── push_2_action.java
│       │                   │   │   ├── push_3_action.java
│       │                   │   │   ├── push_base_action.java
│       │                   │   │   ├── shatter_armor_1_action.java
│       │                   │   │   ├── shatter_armor_2_action.java
│       │                   │   │   ├── shatter_armor_3_action.java
│       │                   │   │   ├── shatter_armor_base_action.java
│       │                   │   │   ├── slow_falling_1_action.java
│       │                   │   │   ├── slow_falling_2_action.java
│       │                   │   │   ├── slow_falling_3_action.java
│       │                   │   │   ├── slowness_1_action.java
│       │                   │   │   ├── slowness_2_action.java
│       │                   │   │   ├── slowness_3_action.java
│       │                   │   │   ├── status_effect_single_target_base_action.java
│       │                   │   │   ├── teleportation_1_action.java
│       │                   │   │   ├── teleportation_2_action.java
│       │                   │   │   ├── teleportation_3_action.java
│       │                   │   │   ├── teleportation_base_action.java
│       │                   │   │   ├── theft_of_life_1_action.java
│       │                   │   │   ├── theft_of_life_2_action.java
│       │                   │   │   ├── theft_of_life_3_action.java
│       │                   │   │   ├── theft_of_life_base_action.java
│       │                   │   │   ├── true_damage_1_action.java
│       │                   │   │   ├── true_damage_2_action.java
│       │                   │   │   ├── true_damage_base_action.java
│       │                   │   │   ├── unstable_hit_1_action.java
│       │                   │   │   ├── unstable_hit_2_action.java
│       │                   │   │   ├── unstable_hit_3_action.java
│       │                   │   │   ├── vertical_thrust_1_action.java
│       │                   │   │   ├── vertical_thrust_2_action.java
│       │                   │   │   ├── vertical_thrust_3_action.java
│       │                   │   │   ├── vertical_thrust_base_action.java
│       │                   │   │   ├── weakness_1_action.java
│       │                   │   │   ├── wither_1_action.java
│       │                   │   │   ├── wither_2_action.java
│       │                   │   │   └── wither_3_action.java
│       │                   │   ├── passive
│       │                   │   │   ├── AdaptivePlatingAction.java
│       │                   │   │   ├── AlphaPresenceAction.java
│       │                   │   │   ├── armor_1_action.java
│       │                   │   │   ├── armor_2_action.java
│       │                   │   │   ├── armor_3_action.java
│       │                   │   │   ├── attack_damage_1_action.java
│       │                   │   │   ├── attack_damage_2_action.java
│       │                   │   │   ├── attack_speed_1_action.java
│       │                   │   │   ├── attack_speed_2_action.java
│       │                   │   │   ├── attribute_bonus_passive_action.java
│       │                   │   │   ├── follow_range_1_action.java
│       │                   │   │   ├── follow_range_2_action.java
│       │                   │   │   ├── knockback_resistance_1_action.java
│       │                   │   │   ├── knockback_resistance_2_action.java
│       │                   │   │   ├── knockback_resistance_3_action.java
│       │                   │   │   ├── max_health_1_action.java
│       │                   │   │   ├── max_health_2_action.java
│       │                   │   │   ├── max_health_3_action.java
│       │                   │   │   ├── movement_speed_1_action.java
│       │                   │   │   ├── passive_attribute_ids.java
│       │                   │   │   └── ReactiveRetaliationAction.java
│       │                   │   └── terrain
│       │                   │       ├── AlchemicalSeepAction.java
│       │                   │       ├── BlinkStrikeAction.java
│       │                   │       ├── SoulVortexAction.java
│       │                   │       └── UndeadPotionBurstAction.java
│       │                   ├── a_system
│       │                   │   ├── abstract_mutation.java
│       │                   │   ├── configured_mutation.java
│       │                   │   ├── incompatibility_manager.java
│       │                   │   ├── mutation_assigner.java
│       │                   │   ├── mutation.java
│       │                   │   ├── mutation_registry.java
│       │                   │   ├── mutation_runtime.java
│       │                   │   └── mutation_type.java
│       │                   ├── data
│       │                   │   ├── mutation_data_loader.java
│       │                   │   └── mutation_schema_validator.java
│       │                   └── util
│       │                       ├── action_context.java
│       │                       ├── attribute_mutation_action.java
│       │                       ├── mutation_action_config.java
│       │                       ├── mutation_action.java
│       │                       ├── mutation_task_scheduler.java
│       │                       ├── proc_on_hit_action.java
│       │                       └── status_effect_config_parser.java
│       └── resources
│           ├── assets
│           │   └── legacycreaturescorey
│           ├── data
│           │   └── legacycreaturescorey
│           │       ├── biome_tier_weights.json
│           │       ├── damage_type
│           │       │   └── true_damage.json
│           │       ├── loot
│           │       │   └── tiered
│           │       │       ├── definitive
│           │       │       │   ├── blaze.json
│           │       │       │   ├── bogged.json
│           │       │       │   ├── breeze.json
│           │       │       │   ├── cave_spider.json
│           │       │       │   ├── creaking.json
│           │       │       │   ├── creeper.json
│           │       │       │   ├── drowned.json
│           │       │       │   ├── elder_guardian.json
│           │       │       │   ├── enderman.json
│           │       │       │   ├── evoker.json
│           │       │       │   ├── ghast.json
│           │       │       │   ├── guardian.json
│           │       │       │   ├── hoglin.json
│           │       │       │   ├── iron_golem.json
│           │       │       │   ├── magma_cube.json
│           │       │       │   ├── phantom.json
│           │       │       │   ├── piglin_brute.json
│           │       │       │   ├── ravager.json
│           │       │       │   ├── shulker.json
│           │       │       │   ├── stray.json
│           │       │       │   ├── vex.json
│           │       │       │   ├── vindicator.json
│           │       │       │   ├── warden.json
│           │       │       │   ├── witch.json
│           │       │       │   ├── wither_skeleton.json
│           │       │       │   ├── zoglin.json
│           │       │       │   └── zombified_piglin.json
│           │       │       ├── epic
│           │       │       │   ├── bee.json
│           │       │       │   ├── blaze.json
│           │       │       │   ├── bogged.json
│           │       │       │   ├── breeze.json
│           │       │       │   ├── cave_spider.json
│           │       │       │   ├── creaking.json
│           │       │       │   ├── creeper.json
│           │       │       │   ├── dolphin.json
│           │       │       │   ├── drowned.json
│           │       │       │   ├── elder_guardian.json
│           │       │       │   ├── enderman.json
│           │       │       │   ├── endermite.json
│           │       │       │   ├── evoker.json
│           │       │       │   ├── fox.json
│           │       │       │   ├── ghast.json
│           │       │       │   ├── goat.json
│           │       │       │   ├── guardian.json
│           │       │       │   ├── hoglin.json
│           │       │       │   ├── husk.json
│           │       │       │   ├── iron_golem.json
│           │       │       │   ├── llama.json
│           │       │       │   ├── magma_cube.json
│           │       │       │   ├── panda.json
│           │       │       │   ├── phantom.json
│           │       │       │   ├── piglin_brute.json
│           │       │       │   ├── pillager.json
│           │       │       │   ├── polar_bear.json
│           │       │       │   ├── ravager.json
│           │       │       │   ├── shulker.json
│           │       │       │   ├── silverfish.json
│           │       │       │   ├── skeleton.json
│           │       │       │   ├── slime.json
│           │       │       │   ├── snow_golem.json
│           │       │       │   ├── spider.json
│           │       │       │   ├── stray.json
│           │       │       │   ├── vex.json
│           │       │       │   ├── vindicator.json
│           │       │       │   ├── warden.json
│           │       │       │   ├── witch.json
│           │       │       │   ├── wither_skeleton.json
│           │       │       │   ├── wolf.json
│           │       │       │   ├── zoglin.json
│           │       │       │   ├── zombie.json
│           │       │       │   ├── zombie_villager.json
│           │       │       │   └── zombified_piglin.json
│           │       │       ├── legendary
│           │       │       │   ├── blaze.json
│           │       │       │   ├── bogged.json
│           │       │       │   ├── breeze.json
│           │       │       │   ├── cave_spider.json
│           │       │       │   ├── creaking.json
│           │       │       │   ├── creeper.json
│           │       │       │   ├── drowned.json
│           │       │       │   ├── elder_guardian.json
│           │       │       │   ├── enderman.json
│           │       │       │   ├── endermite.json
│           │       │       │   ├── evoker.json
│           │       │       │   ├── ghast.json
│           │       │       │   ├── guardian.json
│           │       │       │   ├── hoglin.json
│           │       │       │   ├── husk.json
│           │       │       │   ├── iron_golem.json
│           │       │       │   ├── magma_cube.json
│           │       │       │   ├── phantom.json
│           │       │       │   ├── piglin_brute.json
│           │       │       │   ├── pillager.json
│           │       │       │   ├── ravager.json
│           │       │       │   ├── shulker.json
│           │       │       │   ├── silverfish.json
│           │       │       │   ├── skeleton.json
│           │       │       │   ├── slime.json
│           │       │       │   ├── spider.json
│           │       │       │   ├── stray.json
│           │       │       │   ├── vex.json
│           │       │       │   ├── vindicator.json
│           │       │       │   ├── warden.json
│           │       │       │   ├── witch.json
│           │       │       │   ├── wither_skeleton.json
│           │       │       │   ├── zoglin.json
│           │       │       │   ├── zombie.json
│           │       │       │   ├── zombie_villager.json
│           │       │       │   └── zombified_piglin.json
│           │       │       └── mythic
│           │       │           ├── blaze.json
│           │       │           ├── bogged.json
│           │       │           ├── breeze.json
│           │       │           ├── cave_spider.json
│           │       │           ├── creaking.json
│           │       │           ├── creeper.json
│           │       │           ├── drowned.json
│           │       │           ├── elder_guardian.json
│           │       │           ├── enderman.json
│           │       │           ├── evoker.json
│           │       │           ├── ghast.json
│           │       │           ├── guardian.json
│           │       │           ├── hoglin.json
│           │       │           ├── iron_golem.json
│           │       │           ├── magma_cube.json
│           │       │           ├── phantom.json
│           │       │           ├── piglin_brute.json
│           │       │           ├── ravager.json
│           │       │           ├── shulker.json
│           │       │           ├── stray.json
│           │       │           ├── vex.json
│           │       │           ├── vindicator.json
│           │       │           ├── warden.json
│           │       │           ├── witch.json
│           │       │           ├── wither_skeleton.json
│           │       │           ├── zoglin.json
│           │       │           └── zombified_piglin.json
│           │       ├── mob_attributes.json
│           │       ├── mob_tier_rules.json
│           │       ├── mutations
│           │       │   ├── auras
│           │       │   │   ├── corruption_aura_1.json
│           │       │   │   ├── corruption_aura_2.json
│           │       │   │   ├── corruption_aura_3.json
│           │       │   │   ├── deep_darkness_aura_1.json
│           │       │   │   ├── deep_darkness_aura_2.json
│           │       │   │   ├── deep_darkness_aura_3.json
│           │       │   │   ├── entropy_aura_1.json
│           │       │   │   ├── entropy_aura_2.json
│           │       │   │   ├── entropy_aura_3.json
│           │       │   │   ├── horde_beacon_aura_1.json
│           │       │   │   ├── horde_beacon_aura_2.json
│           │       │   │   ├── horde_beacon_aura_3.json
│           │       │   │   ├── interference_aura_1.json
│           │       │   │   ├── interference_aura_2.json
│           │       │   │   ├── interference_aura_3.json
│           │       │   │   ├── oppressive_presence_aura_1.json
│           │       │   │   ├── oppressive_presence_aura_2.json
│           │       │   │   ├── oppressive_presence_aura_3.json
│           │       │   │   ├── phantasmal_veil_aura_1.json
│           │       │   │   ├── phantasmal_veil_aura_2.json
│           │       │   │   ├── phantasmal_veil_aura_3.json
│           │       │   │   ├── projectile_shroud_aura_1.json
│           │       │   │   ├── projectile_shroud_aura_2.json
│           │       │   │   ├── projectile_shroud_aura_3.json
│           │       │   │   ├── psionic_thorns_aura_1.json
│           │       │   │   ├── psionic_thorns_aura_2.json
│           │       │   │   ├── psionic_thorns_aura_3.json
│           │       │   │   ├── stasis_field_aura_1.json
│           │       │   │   ├── stasis_field_aura_2.json
│           │       │   │   ├── stasis_field_aura_3.json
│           │       │   │   ├── vanguards_bulwark_aura_1.json
│           │       │   │   ├── vanguards_bulwark_aura_2.json
│           │       │   │   ├── vanguards_bulwark_aura_3.json
│           │       │   │   ├── virulent_growth_aura_1.json
│           │       │   │   ├── virulent_growth_aura_2.json
│           │       │   │   └── virulent_growth_aura_3.json
│           │       │   ├── mob_exclusive
│           │       │   │   ├── abyssal_armor_1.json
│           │       │   │   ├── abyssal_armor_2.json
│           │       │   │   ├── abyssal_armor_3.json
│           │       │   │   ├── abyssal_maneuvers_1.json
│           │       │   │   ├── abyssal_maneuvers_2.json
│           │       │   │   ├── abyssal_maneuvers_3.json
│           │       │   │   ├── acidic_core_1.json
│           │       │   │   ├── acidic_core_2.json
│           │       │   │   ├── acidic_core_3.json
│           │       │   │   ├── aerial_maneuvers_1.json
│           │       │   │   ├── aerial_maneuvers_2.json
│           │       │   │   ├── aerial_maneuvers_3.json
│           │       │   │   ├── alphas_vengeance_1.json
│           │       │   │   ├── alphas_vengeance_2.json
│           │       │   │   ├── alphas_vengeance_3.json
│           │       │   │   ├── ambusher_1.json
│           │       │   │   ├── ambusher_2.json
│           │       │   │   ├── ambusher_3.json
│           │       │   │   ├── amphibious_assault_1.json
│           │       │   │   ├── amphibious_assault_2.json
│           │       │   │   ├── amphibious_assault_3.json
│           │       │   │   ├── ancient_curse_1.json
│           │       │   │   ├── ancient_curse_2.json
│           │       │   │   ├── ancient_curse_3.json
│           │       │   │   ├── apiarian_warfare_1.json
│           │       │   │   ├── apiarian_warfare_2.json
│           │       │   │   ├── apiarian_warfare_3.json
│           │       │   │   ├── aquatic_stalker_1.json
│           │       │   │   ├── aquatic_stalker_2.json
│           │       │   │   ├── aquatic_stalker_3.json
│           │       │   │   ├── arctic_fortitude_1.json
│           │       │   │   ├── arctic_fortitude_2.json
│           │       │   │   ├── arctic_fortitude_3.json
│           │       │   │   ├── axe_mastery_1.json
│           │       │   │   ├── axe_mastery_2.json
│           │       │   │   ├── axe_mastery_3.json
│           │       │   │   ├── bamboo_eater_1.json
│           │       │   │   ├── bamboo_eater_2.json
│           │       │   │   ├── bamboo_eater_3.json
│           │       │   │   ├── bastion_guard_1.json
│           │       │   │   ├── bastion_guard_2.json
│           │       │   │   ├── bastion_guard_3.json
│           │       │   │   ├── battering_ram_1.json
│           │       │   │   ├── battering_ram_2.json
│           │       │   │   ├── battering_ram_3.json
│           │       │   │   ├── beam_refraction_1.json
│           │       │   │   ├── beam_refraction_2.json
│           │       │   │   ├── beam_refraction_3.json
│           │       │   │   ├── blizzard_orb_1.json
│           │       │   │   ├── blizzard_orb_2.json
│           │       │   │   ├── blizzard_orb_3.json
│           │       │   │   ├── bloodlust_1.json
│           │       │   │   ├── bloodlust_2.json
│           │       │   │   ├── bloodlust_3.json
│           │       │   │   ├── boar_frenzy_1.json
│           │       │   │   ├── boar_frenzy_2.json
│           │       │   │   └── boar_frenzy_3.json
│           │       │   ├── on_hit
│           │       │   │   ├── anti_armor_1.json
│           │       │   │   ├── anti_armor_2.json
│           │       │   │   ├── anti_armor_3.json
│           │       │   │   ├── anti_shield_1.json
│           │       │   │   ├── anti_shield_2.json
│           │       │   │   ├── bleeding_1.json
│           │       │   │   ├── bleeding_2.json
│           │       │   │   ├── bleeding_3.json
│           │       │   │   ├── blindness_1.json
│           │       │   │   ├── chaos_touch_1.json
│           │       │   │   ├── chaos_touch_2.json
│           │       │   │   ├── chaos_touch_3.json
│           │       │   │   ├── concussive_blow_1.json
│           │       │   │   ├── concussive_blow_2.json
│           │       │   │   ├── concussive_blow_3.json
│           │       │   │   ├── critical_1.json
│           │       │   │   ├── critical_2.json
│           │       │   │   ├── critical_3.json
│           │       │   │   ├── deafening_strike_1.json
│           │       │   │   ├── deafening_strike_2.json
│           │       │   │   ├── deafening_strike_3.json
│           │       │   │   ├── disarm_1.json
│           │       │   │   ├── essence_siphon_1.json
│           │       │   │   ├── essence_siphon_2.json
│           │       │   │   ├── essence_siphon_3.json
│           │       │   │   ├── experience_theft_1.json
│           │       │   │   ├── experience_theft_2.json
│           │       │   │   ├── experience_theft_3.json
│           │       │   │   ├── fire_1.json
│           │       │   │   ├── fire_2.json
│           │       │   │   ├── fire_3.json
│           │       │   │   ├── freezing_1.json
│           │       │   │   ├── freezing_2.json
│           │       │   │   ├── freezing_3.json
│           │       │   │   ├── frenzy_1.json
│           │       │   │   ├── frenzy_2.json
│           │       │   │   ├── frenzy_3.json
│           │       │   │   ├── glowing_1.json
│           │       │   │   ├── glowing_2.json
│           │       │   │   ├── glowing_3.json
│           │       │   │   ├── hunger_1.json
│           │       │   │   ├── hunger_2.json
│           │       │   │   ├── hunger_3.json
│           │       │   │   ├── levitation_1.json
│           │       │   │   ├── levitation_2.json
│           │       │   │   ├── mining_fatigue_1.json
│           │       │   │   ├── mining_fatigue_2.json
│           │       │   │   ├── mining_fatigue_3.json
│           │       │   │   ├── mortal_wound_1.json
│           │       │   │   ├── mortal_wound_2.json
│           │       │   │   ├── mortal_wound_3.json
│           │       │   │   ├── nausea_1.json
│           │       │   │   ├── pain_link_1.json
│           │       │   │   ├── pain_link_2.json
│           │       │   │   ├── pain_link_3.json
│           │       │   │   ├── poison_1.json
│           │       │   │   ├── poison_2.json
│           │       │   │   ├── poison_3.json
│           │       │   │   ├── push_1.json
│           │       │   │   ├── push_2.json
│           │       │   │   ├── push_3.json
│           │       │   │   ├── shatter_armor_1.json
│           │       │   │   ├── shatter_armor_2.json
│           │       │   │   ├── shatter_armor_3.json
│           │       │   │   ├── slow_falling_1.json
│           │       │   │   ├── slow_falling_2.json
│           │       │   │   ├── slow_falling_3.json
│           │       │   │   ├── slowness_1.json
│           │       │   │   ├── slowness_2.json
│           │       │   │   ├── slowness_3.json
│           │       │   │   ├── teleportation_1.json
│           │       │   │   ├── teleportation_2.json
│           │       │   │   ├── teleportation_3.json
│           │       │   │   ├── theft_of_life_1.json
│           │       │   │   ├── theft_of_life_2.json
│           │       │   │   ├── theft_of_life_3.json
│           │       │   │   ├── true_damage_1.json
│           │       │   │   ├── true_damage_2.json
│           │       │   │   ├── unstable_hit_1.json
│           │       │   │   ├── unstable_hit_2.json
│           │       │   │   ├── unstable_hit_3.json
│           │       │   │   ├── vertical_thrust_1.json
│           │       │   │   ├── vertical_thrust_2.json
│           │       │   │   ├── vertical_thrust_3.json
│           │       │   │   ├── weakness_1.json
│           │       │   │   ├── wither_1.json
│           │       │   │   ├── wither_2.json
│           │       │   │   └── wither_3.json
│           │       │   └── passive
│           │       │       ├── armor_1.json
│           │       │       ├── armor_2.json
│           │       │       ├── armor_3.json
│           │       │       ├── attack_damage_1.json
│           │       │       ├── attack_damage_2.json
│           │       │       ├── attack_speed_1.json
│           │       │       ├── attack_speed_2.json
│           │       │       ├── follow_range_1.json
│           │       │       ├── follow_range_2.json
│           │       │       ├── knockback_resistance_1.json
│           │       │       ├── knockback_resistance_2.json
│           │       │       ├── knockback_resistance_3.json
│           │       │       ├── max_health_1.json
│           │       │       ├── max_health_2.json
│           │       │       ├── max_health_3.json
│           │       │       └── movement_speed_1.json
│           │       └── tags
│           │           └── damage_type
│           │               ├── bypasses_armor.json
│           │               ├── bypasses_enchantments.json
│           │               └── bypasses_resistance.json
│           ├── fabric.mod.json
│           └── legacycreaturescorey.mixins.json
└── .vscode
    └── launch.json

208 directories, 1874 files
