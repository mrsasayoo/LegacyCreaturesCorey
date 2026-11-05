# GUÍA DE DESARROLLO - Legacy Creatures Corey

> Actualizada para **MC 26.3** (nombres oficiales de Mojang). Úsala como referencia para
> retomar el desarrollo de este mod y como plantilla de estructura para el resto de la
> suite Legacy (Arcaney, Armory, ...). Los renames de la generación 26.x están detallados
> en `docs/PORTING_NOTES.md`.
> Última verificación: 2026-11-03 (auditoría de memoria).

## 🧰 Stack de Desarrollo (26.3)

| Componente | Versión | Nota |
| --- | --- | --- |
| Java | **25** | `org.gradle.java.home` fijo en `gradle.properties` → `./gradlew` funciona a secas |
| Gradle | 9.8.0 (wrapper) | requerido por Loom 1.19 |
| Loom | `net.fabricmc.fabric-loom` 1.19-SNAPSHOT | **sin mappings ni remapping** (juego sin ofuscar) |
| Fabric Loader | 0.20.1 | |
| Fabric API | 0.165.0+26.3 | |
| Ventanas/input | SDL3 | el runClient fuerza `SDL_VIDEO_DRIVER=wayland` nativo |

Reglas de la era 26.x:
- **No hay yarn ni intermediary**: el código se escribe con nombres oficiales de Mojang.
- Para verificar cualquier firma: `javap` contra `~/.gradle/caches/fabric-loom/26.3/minecraft-common.jar`
  (y `minecraft-client-only.jar` para clases client).
- `./gradlew runClient` **no pide cuenta**; el loop verify-patch está documentado en PORTING_NOTES §2.

## 🏗️ Arquitectura del Proyecto

### Estructura de Paquetes (real, post-26.3)
```
com.mrsasayo.legacycreaturescorey
├── Legacycreaturescorey        → Punto de entrada Fabric (raíz)
├── content/
│   ├── block/                  → FakeLootPile (bloque de señuelo)
│   ├── health/                 → CoreyHealthMonitor (avisos de vida)
│   ├── item/                   → GhostFragment (ítem) + ModItems
│   └── status/                 → Efectos personalizados, ticker y sync client
├── core/
│   ├── api/                    → CoreyAPI, LegacyCreaturesTierApi, AntiFarmDashboardApi + event/
│   ├── command/                → /corey (corey/), /mutation, /coreyhud
│   ├── component/              → Data Attachments (jugador/mobs)
│   ├── config/                 → CoreyConfig
│   └── network/                → Payloads S2C/C2S + ModNetworking
├── feature/
│   ├── antifarm/               → AntiFarmManager + data/ + dashboard API impl
│   ├── difficulty/             → CoreyServerState, DifficultyManager, MobTier, calculators
│   ├── loot/                   → TieredLootDataLoader, CoreyLootModifiers, eventos
│   ├── mob/                    → TierManager, spawns, partículas, MobLegacyData
│   └── synergy/                → SynergyModule/SynergyManager/SynergyProvider
├── mutation/
│   ├── action/                 → auras/ on_death/ on_hit/ passive/ terrain/ helper/
│   ├── a_system/               → registry, runtime, assigner, synergy de mutaciones
│   ├── data/                   → mutation_data_loader, schema_validator
│   └── util/                   → helpers de mutaciones
├── mixin/                      → accessor/ general/ mechanic/ mutation/ (16 mixins)
├── util/                       → utilidades transversales
└── (src/client/java)           → client/ HUD de dificultad + ClientEffectHandler + mixin client
```

### Datos (resources)
```
data/legacycreaturescorey/
├── mutations/{auras,mob_exclusive,on_hit,passive}/   → 197 JSON (formato propio del mod)
├── loot/tiered/<tier>/*.json
├── mob_tier_rules.json, anti_farm_exclusions.json
├── damage_type/, tags/damage_type/                   → daño personalizado vanilla-format
legacycreaturescorey.mixins.json (+ client json)      → compatibilityLevel JAVA_25
```
`docs/mutaciones.csv` es la **fuente de verdad** del catálogo (1.146 mutaciones) y ahora lleva
la columna final `implementada_en_el_mod` (SI/NO, verificada contra los JSON + validador runtime).

### Ciclo de Vida

#### Fase 1: Inicialización (`Legacycreaturescorey#onInitialize`)
1. Configuración: `CoreyConfig.INSTANCE.loadOrCreate()`, `validate()` y `save()` dejan un snapshot limpio antes de inicializar subsistemas.
2. Componentes básicos: `ModDataAttachments.initialize()`, `MutationRegistry.initialize()` y `MutationRuntime.register()` activan los attachments y hooks de mutaciones.
3. Loaders/datapacks: `MutationDataLoader`, `TieredLootDataLoader`, `MobAttributeDataLoader`, `MobTierRuleDataLoader`, `BiomeTierWeightDataLoader` y `AntiFarmExclusionDataLoader` quedan registrados para `/reload`.
4. Sistemas de soporte: `ModStatusEffects.init()` y `CoreyLootModifiers.register()`.
5. Registro de eventos, red y comandos (`DifficultyTickHandler`, `MobSpawnHandler`, `TierParticleTicker`, `StatusEffectTicker`, `ModNetworking`, `MutationCommand`, `CoreyCommand`, `CoreyHudCommand`, `AntiFarmManager`, `SynergyManager.bootstrap`, `CoreyHealthMonitor`).

#### Fase 2: Carga de Mundo
1. `CoreyServerState.get(server)` → `SavedDataStorage.computeIfAbsent(TYPE)` (26.3; ya no existe `DimensionDataStorage.getOrCreate`)
2. `DifficultyManager.checkDailyIncrease(server)` en cada amanecer
3. `AntiFarmManager` escucha muertes y actualiza `ChunkActivityData`
4. `MutationDataLoader` / `TieredLootDataLoader` responden a `/reload` — el `mutation_schema_validator` rechaza con log ❌ cualquier JSON con `actions[].type` desconocido

#### Fase 3: Runtime
- **Spawn:** `MobSpawnHandler` → `TierManager.tryCategorize` → `MutationAssigner` → `SynergyManager.onMobTiered`
- **Tick:** `MutationRuntime` aplica mutaciones activas (`ServerTickEvents.END_LEVEL_TICK`); `StatusEffectTicker` procesa efectos; `DifficultyManager` escucha muertes.
- **Loot:** `CoreyLootModifiers` intercepta drops → `TieredLootManager` y `SynergyManager.onLootGenerated`.

## 📚 Sistemas Principales

### 1. Sistema de Dificultad
- `CoreyServerState`: `SavedData` registrada con `SavedDataType` (id `Identifier` + Codec + DataFixTypes) con `global_difficulty`, `last_day_checked` y calor por chunk.
- `PlayerDifficultyData`: attachment via `AttachmentRegistry`.
- `DifficultyManager`: sincroniza días/penalizaciones y manda snapshots (`DifficultySyncPayload`) a cada jugador.
- **Cálculo actual:** `EffectiveDifficultyCalculator` toma todos los jugadores del mundo, filtra por `effectiveDifficultyRadius`, aplica un promedio ponderado (más peso a los cercanos), suma la dificultad global, luego multiplica por bioma (`biomeDifficultyMultiplier`) y por penalización anti-farm (según `ChunkActivityData`). El resultado alimenta a `TierProbabilityCalculator`, que ya respeta los multiplicadores configurables por tier.

### 2. Categorización de Mobs
- `TierManager.determineAllowedTiers` usa `MobTierRuleDataLoader` (`data/legacycreaturescorey/mob_tier_rules.json`) para saber qué tiers tiene permitidos cada `EntityType`; si no existe entrada, el mob se queda en `NORMAL`.
- `TierProbabilityCalculator` combina dificultad efectiva + configuraciones para elegir tier.
- `MobLegacyData` almacena tier, mutaciones y flags (`farmed`, `furious`).
- **Consejo:** usar `CoreyAPI.spawnCategorizedMob` para spawns manuales.

### 3. Mutaciones
- JSON en `data/legacycreaturescorey/mutations/*.json` (4 categorías activas: auras, on_hit, mob_exclusive, passive).
- `MutationDataLoader` crea `ConfiguredMutation` con acciones (`mutation.action.*`); `mutation_schema_validator` valida tipos y campos al cargar.
- `MutationRuntime` ejecuta acciones activas (`onTick`) y pasivas (`onHit`).
- **Budgets por tier:** EPIC=25, LEG=50, MYTH=75, DEF=100 (ver `mutation/a_system/mutation_assigner.java`).
- **Estado del catálogo:** `docs/mutaciones.csv` marca con `implementada_en_el_mod=SI` las 197
  operativas — incluidas las 6 de aquatic_stalker/arctic_fortitude, implementadas el 2026-11-03
  (clases existentes desde `2590b41` + registro de sus tipos en `mutation_schema_validator`,
  verificadas en mundo: 0 `Validación fallida`, asignación en vivo en logs) —, y 949
  del catálogo aún no tienen JSON.

#### Gramática sensorial de on-hit (Lote «Identidad On-Hit», 2026-11-04)
Todo `proc_on_hit_action` admite campos opcionales en `actions` para dar identidad
audiovisual al proc (solo suenan/dibujan cuando el dado de chance pasa):

| Campo | Tipo | Descripción |
| --- | --- | --- |
| `proc_sound` | Identifier | Sonido del proc (resuelto contra `minecraft:sound_event`) |
| `proc_particles` | Identifier | Partícula SimpleParticleType (p. ej. `flame`, `snowflake`) |
| `proc_particle_count` | Integer (def. 12) | Cantidad de partículas |
| `proc_pitch` | Float (def. 1.0) | Tono del sonido |

Efectos custom nuevos de la categoría: `legacycreaturescorey:expuesto` (marcador de
daño recibido +10/15/20%, amplificador 0/1/2, aplicado por critical y true_damage_3;
la amplificación la hace el mixin `expuesto_damage_mixin` sobre `hurtServer`) y
`legacycreaturescorey:agobio` (ver oppressive_presence). Overlays de pantalla:
`tunnel_vision` (Visión de Túnel) y viñeta de escarcha por congelación (freezing).
Nuevas mutaciones de nivel 3: `anti_shield_3` («Juramento Roto»: escudo -50 durabilidad
+ cooldown 4 s + self-downtime 1 s) y `true_damage_3` («Herida Verdadera»: daño
prevenido re-aplicado + Expuesto II + self-downtime 3 s). Rediseños con mecánica
conservada: theft_of_life/experience_theft (línea de partículas jugador→mob),
fire (estela de fuego 1 daño/bloque pisado, `inFire`, respeta fireImmune),
wither (latido grave acelerado según HP), slow_falling («Aire Espeso»: anula
elytra/Riptide; nivel 3 también rompe el sprint). El guard de re-entrada en
`mutation_runtime` impide que el daño generado dentro de un proc dispare más procs.

#### Acciones on-death (Último Aliento)
| Acción | Campos | Notas |
| --- | --- | --- |
| `status_effect_on_death` | `effect`, `duration`, `amplifier`, `target` (`killer`, `players_in_radius`, `all_players`), `radius` (solo para target de radio), `chance`, `delay_ticks`/`delay_seconds`, `damage`, `pull_strength` | Permite aplicar un efecto y opcionalmente arrastrar o dañar a los jugadores cercanos tras la muerte del mob. `chance` ∈ [0,1]. |
| `ground_hazard_on_death` | `radius`, `duration_ticks`/`duration_seconds`, `interval_ticks`/`interval_seconds`, `damage`, `status_effect`, `status_duration_ticks`/`status_duration_seconds`, `status_amplifier`, `target` (`players`, `hostile_mobs`, `all_living`), `particle`, `particle_count`, `chance`, `delay_ticks`/`delay_seconds` | Invoca al `GroundHazardManager` para dejar charcos/sigilos que pulsan daño/efectos. |

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

### 4. Loot Escalado
- JSON en `data/legacycreaturescorey/loot/tiered/<tier>/*.json`.
- `TieredMobLoot` soporta drops garantizados + ponderados e `IntRange` para rolls; serializa con `RegistryOps` via `lookup.createSerializationContext(JsonOps)` (26.3).
- `CoreyLootModifiers` solo actúa si la entidad tiene `MobLegacyData` con tier > NORMAL.
- Config: `tieredLootEnabled`, `tieredLootStrictEntityTables` y los toggles por tier; `tieredLootTelemetryEnabled` publica `TierLootTelemetryEvents.TIER_LOOT_APPLIED`.

### 5. Anti-Farm
- `AntiFarmManager` escucha muertes y usa `ChunkActivityData` para contar kills + bloquear chunks (pendiente TTL).
- Config clave: `antiFarmKillThreshold`, `antiFarmWindowTicks`, `antiFarmDailyDecayAmount`.
- Exclusiones por datapack: `data/<pack>/legacycreaturescorey/anti_farm_exclusions.json` → `"entries": ["minecraft:villager", "#minecraft:bosses"]`.
- Hooks: `AntiFarmEvents.SHOULD_IGNORE`, `THRESHOLD_MODIFIER`, `CHUNK_BLOCKED`.
- Telemetría: `AntiFarmDashboardEvents.CHUNK_ACTIVITY_UPDATED` + `AntiFarmDashboardApi`.

### 6. Sinergias
- `SynergyModule` enumera los mods compatibles (ARMORY, ARCANEY, ARTIFACTYS, WORLDSCAPESYS, EVENTYS, SUMMONYS, PROGRESSIONYS, ENEMIESYS, ...) y detecta su presencia via Fabric Loader.
- `SynergyManager.bootstrap()` solo carga el estado de detección; no hay proveedores internos.
- `SynergyProvider` disponible para mods externos (`validate`, `onMobTiered`, `onLootGenerated`).
- `CoreyAPI.registerSynergyProvider` es la puerta de entrada oficial.

## 🔌 Integraciones Externas
- **Armory:** detección operativa (`legacycreaturesarmory`), integración pendiente de proveedor externo.
- **Arcaney:** detección operativa (`legacycreaturesarcaney`), encantamientos externos pendientes (portar a 26.3 con `docs/PORTING_NOTES.md`).
- **Artifactys:** detección operativa (`legacycreaturesartifactys`), drops/partículas pendientes.

## 🧪 Testing Recomendado
1. `./gradlew build` — compilación main+client y jar en `build/libs/`
2. `./gradlew runClient` — pruebas visuales (a secas; el entorno Wayland ya va configurado). En el log busca `✅ Legacy Creatures - Corey cargado exitosamente`, **0 líneas `Mixin apply ... failed`**, y los `❌ Validación fallida` del validador de mutaciones.
3. `./gradlew runServer` + `/corey tier` para test de mutaciones sin UI
4. En mundo: `/corey debug mob current`, `/mutation assign`, HUD con `/coreyhud on`
5. Añadir GameTests (Fabric) para spawn y loot (pendiente en repo)

## 🧰 Hooks para Modders
- `CoreyAPI.spawnCategorizedMob(ServerLevel, BlockPos, EntityType, MobTier[, List<Identifier> mutations])`
- `CoreyAPI.registerSynergyProvider(SynergyProvider)`
- `CoreyAPI.getTier(Mob)` / `getMutations(Mob)`
- Detalles y contrato completo: `docs/API_REFERENCE.md`

## ✅ Buenas Prácticas
- Registrar nuevos providers en `onInitialize` y validar tags requeridos.
- Respetar `MobLegacyData` al modificar entidades (usar `getAttachedOrCreate`).
- **26.3:** no crear accessors de campos que ya tienen getter público en vanilla (rompen el BOOT con "cannot overwrite method", aunque compile) — ver PORTING_NOTES §5.
- Verificar firmas nuevas con `javap` contra el jar de la cache antes de usarlas.
- Para logs detallados, habilitar `CoreyConfig.debugLogProbabilityDetails`.

## 📅 Roadmap Sugerido
1. ✅ **Completado 2026-11-03:** clases `aquatic_stalker_*`/`arctic_fortitude_*` auditadas y reutilizadas (decisión: auditar y reutilizar, no reescribir), tipos registrados en `mutation_schema_validator`, 6 mutaciones marcadas SI y verificadas en juego (Lote 7 = 57/57).
2. Continuar el catálogo: 949 mutaciones pendientes según `docs/mutaciones.csv` (la columna `implementada_en_el_mod` guía el orden; el plan de lotes está en `docs/planes/plan_mutaciones.md`).
3. TTL anti-farm + thresholds por tag.
4. Persistencia real de configuración + comandos `/corey config reload`.
5. Portar Arcaney a 26.3 reutilizando `docs/PORTING_NOTES.md`.
6. Proveedores adicionales (Worldscapesys, Eventys, etc.).
