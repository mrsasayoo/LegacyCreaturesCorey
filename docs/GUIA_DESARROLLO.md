# GUÍA DE DESARROLLO - Legacy Creatures Corey

## 🏗️ Arquitectura del Proyecto

### Estructura de Paquetes
```
com.mrsasayo.legacycreaturescorey
├── Legacycreaturescorey        → Punto de entrada Fabric
├── config/                     → Valores y validación (`CoreyConfig`)
├── component/                  → Data Attachments (jugador/mobs)
├── difficulty/                 → Estado global, cálculos y tiers (`MobTier`)
├── mob/                        → Gestión de spawns, partículas y datos legacy
├── mutation/                   → Registro, runtime y loaders JSON
├── loot/                       → Modificadores y loaders de botín
├── antifarm/                   → Detección y bloqueo de granjas
├── command/                    → Comandos `/corey` y utilidades
├── network/                    → Payloads S2C (efectos visuales)
├── status/                     → Efectos personalizados y ticker
├── synergy/                    → Detección/ejecución de sinergias
└── api/                        → `CoreyAPI` público para terceros
```

### Ciclo de Vida

#### Fase 1: Inicialización (`Legacycreaturescorey#onInitialize`)
1. Configuración: `CoreyConfig.INSTANCE.loadOrCreate()`, `validate()` y `save()` dejan un snapshot limpio antes de inicializar subsistemas.
2. Componentes básicos: `ModDataAttachments.initialize()`, `MutationRegistry.initialize()` y `MutationRuntime.register()` activan los adjuntos y hooks de mutaciones.
3. Loaders/datapacks: `MutationDataLoader`, `TieredLootDataLoader`, `MobAttributeDataLoader`, `MobTierRuleDataLoader`, `BiomeTierWeightDataLoader` y `AntiFarmExclusionDataLoader` quedan registrados para `/reload`.
4. Sistemas de soporte: `ModStatusEffects.init()` y `CoreyLootModifiers.register()`.
5. Registro de eventos, red y comandos (`DifficultyTickHandler`, `MobSpawnHandler`, `TierParticleTicker`, `StatusEffectTicker`, `ModNetworking`, `MutationCommand`, `CoreyCommand`, `CoreyHudCommand`, `AntiFarmManager`, `SynergyManager.bootstrap`, `CoreyHealthMonitor`). `SynergyModule.initializeDetections()` se ejecuta dentro de `SynergyManager.bootstrap()`.

#### Fase 2: Carga de Mundo
1. `CoreyServerState.get(server)` → crea/lee estado
2. `DifficultyManager.checkDailyIncrease(server)` en cada amanecer
3. `AntiFarmManager` escucha muertes y actualiza `ChunkActivityData`
4. `MutationDataLoader` / `TieredLootDataLoader` responden a `/reload`

#### Fase 3: Runtime
- **Spawn:** `MobSpawnHandler` → `TierManager.tryCategorize` → `MutationAssigner` → `SynergyManager.onMobTiered`
- **Tick:** `MutationRuntime` aplica mutaciones activas; `StatusEffectTicker` procesa efectos; `DifficultyManager` escucha muertes.
- **Loot:** `CoreyLootModifiers` intercepta drops → `TieredLootManager` y `SynergyManager.onLootGenerated`.

## 📚 Sistemas Principales

### 1. Sistema de Dificultad
- `CoreyServerState`: `PersistentState` con `global_difficulty`, `last_day_checked` y calor por chunk.
- `PlayerDifficultyData`: componente adjunto via `AttachmentRegistry`.
- `DifficultyManager`: sincroniza días/penalizaciones y manda snapshots (`DifficultySyncPayload`) a cada jugador.
- **Cálculo actual:** `EffectiveDifficultyCalculator` toma todos los jugadores del mundo, filtra por `effectiveDifficultyRadius`, aplica un promedio ponderado (más peso a los cercanos), suma la dificultad global, luego multiplica por bioma (`biomeDifficultyMultiplier`) y por penalización anti-farm (según `ChunkActivityData`). El resultado alimenta a `TierProbabilityCalculator`, que ya respeta los multiplicadores configurables por tier.
- **Pendiente:** exponer controles adicionales para biomas y calor cuando se agreguen más escenarios.

### 2. Categorización de Mobs
- `TierManager.determineAllowedTiers` usa `MobTierRuleDataLoader` (`data/legacycreaturescorey/mob_tier_rules.json`) para saber qué tiers tiene permitidos cada `EntityType`; si no existe entrada, el mob se queda en `NORMAL`.
- `TierProbabilityCalculator` combina dificultad efectiva + configuraciones para elegir tier.
- `MobLegacyData` almacena tier, mutaciones y flags (`farmed`, `furious`).
- **Consejo:** usar `CoreyAPI.spawnCategorizedMob` para spawns manuales.

### 3. Mutaciones
- JSON en `data/legacycreaturescorey/mutations/*.json`.
- `MutationDataLoader` crea `ConfiguredMutation` con acciones (`mutation.action.*`).
- `MutationRuntime` ejecuta acciones activas (`onTick`) y pasivas (`onHit`).
- **Budgets por tier:** EPIC=25, LEG=50, MYTH=75, DEF=100 (ver `MutationAssigner`).

#### Acciones on-death (Último Aliento)
| Acción | Campos | Notas |
| --- | --- | --- |
| `status_effect_on_death` | `effect`, `duration`, `amplifier`, `target` (`killer`, `players_in_radius`, `all_players`), `radius` (solo para target de radio), `chance`, `delay_ticks`/`delay_seconds`, `damage`, `pull_strength` | Permite aplicar un efecto y opcionalmente arrastrar o dañar a los jugadores cercanos tras la muerte del mob. `chance` ∈ [0,1]. |
| `ground_hazard_on_death` | `radius`, `duration_ticks`/`duration_seconds`, `interval_ticks`/`interval_seconds`, `damage`, `status_effect`, `status_duration_ticks`/`status_duration_seconds`, `status_amplifier`, `target` (`players`, `hostile_mobs`, `all_living`), `particle`, `particle_count`, `chance`, `delay_ticks`/`delay_seconds` | Invoca al `GroundHazardManager` para dejar charcos/sigilos que pulsan daño/efectos. El hazard colapsa al terminar `duration` y aplica pulsos cada `interval`. |

Ejemplo mínimo para dejar un charco tóxico tras la muerte:

```json
{
	"type": "ground_hazard_on_death",
	"radius": 4.0,
	"duration_seconds": 8,
	"interval_ticks": 20,
	"damage": 2.0,
	"status_effect": "minecraft:poison",
	"status_duration_seconds": 4,
	"target": "players",
	"particle": "minecraft:soul",
	"particle_count": 10,
	"chance": 0.75
}
```

La nueva acción comparte `chance` y `delay_*` con el `status_effect_on_death`, por lo que pueden mezclarse en la misma mutación para componer Último Aliento.

### 4. Loot Escalado
- JSON en `data/legacycreaturescorey/loot/tiered/<tier>/*.json`.
- `TieredMobLoot` soporta drops garantizados + ponderados e `IntRange` para rolls.
- `CoreyLootModifiers` solo actúa si la entidad tiene `MobLegacyData` con tier > NORMAL.
- Config: `tieredLootEnabled`, `tieredLootStrictEntityTables` y los toggles por tier permiten granularidad sin tocar datapacks; habilita `tieredLootTelemetryEnabled` para recibir snapshots.
- Telemetría: `TieredLootTelemetryEvents.TierLootApplied` expone el mob, tier, cuenta de drops antes/después y si se generó botín legado para dashboards o alarmas.
- **Integración:** Hooks disponibles en `SynergyManager.onLootGenerated` para mods externos (Corey ya no añade drops por sí mismo).

### 5. Anti-Farm
- `AntiFarmManager` escucha muertes y usa `ChunkActivityData` para contar kills + bloquear chunks (pendiente TTL).
- Config clave: `antiFarmKillThreshold`, `antiFarmWindowTicks`, `antiFarmDailyDecayAmount`.
- Exclusiones por datapack: `data/<pack>/legacycreaturescorey/anti_farm_exclusions.json` → `"entries": ["minecraft:villager", "#minecraft:bosses"]`.
- Hooks: `AntiFarmEvents.SHOULD_IGNORE`, `THRESHOLD_MODIFIER`, `CHUNK_BLOCKED` permiten modificar el pipeline.
- Telemetría: `AntiFarmDashboardEvents.CHUNK_ACTIVITY_UPDATED` + `AntiFarmDashboardApi` entregan snapshots para paneles.

### 6. Sinergias
- `SynergyModule` enumera mods compatibles y almacena su detección via Fabric Loader.
- `SynergyManager.bootstrap()` solo carga el estado de detección; no hay proveedores internos.
- `SynergyProvider` sigue disponible para que otros mods implementen `validate`, `onMobTiered` y `onLootGenerated`.
- `CoreyAPI.registerSynergyProvider` permite que esos mods expongan su integración.

## 🔌 Integraciones Externas
- **Armory:** detección operativa (`legacycreaturesarmory`), integración pendiente de proveedor externo.
- **Arcaney:** detección operativa (`legacycreaturesarcaney`), encantamientos externos pendientes.
- **Artifactys:** detección operativa (`legacycreaturesartifactys`), drops/partículas pendientes.

## 🧪 Testing Recomendado
1. `./gradlew build` (compilación + procesadores de datos)
2. `./gradlew runClient` para pruebas visuales
3. `./gradlew runServer` + `/corey tier` para test de mutaciones
4. Añadir GameTests (Fabric) para spawn y loot (pendiente en repo)

## 🧰 Hooks para Modders
- `CoreyAPI.spawnCategorizedMob(ServerWorld, BlockPos, EntityType, MobTier[, List<Identifier> mutations])`
- `CoreyAPI.registerSynergyProvider(SynergyProvider)`
- `CoreyAPI.getTier(MobEntity)` / `getMutations(MobEntity)`

## ✅ Buenas Prácticas
- Registrar nuevos providers en `onInitialize` y validar tags requeridos.
- Respetar `MobLegacyData` al modificar entidades (usar `getAttachedOrCreate`).
- Para logs detallados, habilitar `CoreyConfig.debugLogProbabilityDetails`.

## 📅 Roadmap Sugerido
1. Persistencia real de configuración + comandos `/corey config reload`.
2. TTL anti-farm + thresholds por tag.
3. Modularización del comando principal y documentación API.
4. Proveedores adicionales (Worldscapesys, Eventys, etc.).
