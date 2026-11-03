## Legacy Creatures - Corey

## 📖 Descripción
Legacy Creatures - Corey es el módulo base del ecosistema Legacy Creatures. Añade una capa de dificultad progresiva, mutaciones modulares y botín escalado para todos los mobs hostiles de Minecraft 26.3 (Fabric Loader 0.20.1). El proyecto está escrito en Java 25 usando Fabric API 0.165.0+26.3 y sirve como punto de integración para el resto de la suite Legacy.

## 🎯 Características Principales
- **Dificultad dual**: una barra global persistente (`CoreyServerState`) que escala con los días del servidor y un componente individual (`PlayerDifficultyData`) con penalizaciones configurables por muerte.
- **Promoción de mobs**: `TierManager` puede ascender cualquier mob hostil a los tiers Épico, Legendario, Mítico o Definitivo con multiplicadores configurables de vida/daño, partículas dedicadas y nombres personalizados. Los tiers permitidos por mob se controlan vía datapacks (`data/<namespace>/legacycreaturescorey/mob_tier_rules.json`).
- **Motor de mutaciones**: mutaciones activas/pasivas declaradas en JSON se aplican mediante `MutationAssigner` y se procesan por `MutationRuntime` en cada tick o golpe.
- **Botín inteligente**: `TieredLootDataLoader` carga pools condicionales para cada mob/tier y `CoreyLootModifiers` inyecta recompensas únicas incluyendo stacks con componentes 1.21 (encantamientos extendidos, `stored_enchantments`, etc.).
- **Anti-abuso**: `AntiFarmManager` monitoriza muertes por chunk, limpia el calor diariamente, bloquea promociones cuando las granjas detectadas exceden los umbrales, atenúa la dificultad efectiva en zonas calientes y admite exclusiones declaradas en datapacks (`anti_farm_exclusions.json`) además de hooks Fabric para terceros.
- **Detección de sinergias**: `SynergyModule` identifica Armory, Arcaney, Artifactys y futuros módulos y expone su estado para que otros proyectos Legacy activen integraciones cuando estén listas.

## 📦 Instalación
1. Instala [Fabric Loader 0.20.1](https://fabricmc.net/use/) y una versión de Fabric API ≥ 0.165.0 para **Minecraft 26.3**.
2. Compila este repositorio con `./gradlew build` o descarga el artefacto de `build/libs/legacycreaturescorey-<version>.jar`.
3. Copia el `.jar` resultante dentro de la carpeta `mods/` de tu instancia Fabric (cliente o servidor). Para pruebas locales con los proyectos adjuntos, usa la carpeta `run/mods/`.

## 🔧 Configuración
`CoreyConfig.INSTANCE` expone todos los toggles y multiplicadores. En entornos de producción se recomienda mapearlo a un archivo JSON/HOCON externo usando tu sistema de configuración preferido. Campos relevantes:
- `maxGlobalDifficulty`, `playerDifficultyIncreaseChance`.
- HUD opcional: `enableDifficultyHud` (false por defecto; si el servidor lo habilita, cada jugador puede usar `/coreyhud on|off|toggle` para decidir si ver la barra dual).
- Multiplicador por bioma: `biomeDifficultyMultiplier` (default 1.5) se aplica automáticamente en biomas peligrosos como Snowy Slopes, Deep Dark, Crimson Forest, etc.
- Multiplicadores por tier: `epicHealthMultiplier`, `legendaryDamageMultiplier`, etc.
- Probabilidades relativas: `*_ChanceMultiplier` para cada tier.
- Botín escalado: `tieredLootEnabled` y `tieredLootStrictEntityTables` controlan cuándo interviene `CoreyLootModifiers`, mientras que `tieredLoot<Epic|Legendary|Mythic|Definitive>Enabled` permite activar/desactivar cada tier sin tocar datapacks.
- Telemetría de botín: `tieredLootTelemetryEnabled` publica `TieredLootTelemetryEvents.TIER_LOOT_APPLIED` con snapshots antes/después de modificar los drops para dashboards y alertas.
- Anti-granjas: `antiFarmKillThreshold`, `antiFarmWindowTicks`, `antiFarmBlockRadiusChunks`, `antiFarmDailyDecayAmount`, `antiFarmHeatPenaltyEnabled`, `antiFarmHeatPenaltyMinMultiplier`, `antiFarmHeatPenaltyExponent`.
- Exclusiones anti-farm: crea `data/<namespace>/legacycreaturescorey/anti_farm_exclusions.json` con `{"entries": ["minecraft:villager", "#minecraft:bosses"]}` para omitir mobs o tags completos sin depender de command tags.
- Reglas de tiers por mob: `mob_tier_rules.json` permite definir explícitamente qué tiers puede alcanzar cada entidad (además de los tags heredados). El archivo por defecto se incluye en `data/legacycreaturescorey/mob_tier_rules.json` y se puede sobrescribir en datapacks.
- Herramientas de debug: `debugForceExactTier`, `debugForceHighestAllowedTier`, `debugLogProbabilityDetails`.

## 🎮 Uso
### Para Jugadores
- Cuanto más sobrevivas y explores, mayor será la probabilidad de encontrar mobs promovidos con habilidades y botín mejorado.
- Las partículas de color y los sufijos en el nombre indican el tier; los efectos visuales persistentes se reaplican cada 15 ticks por `TierParticleTicker`.
- Morir varias veces en intervalos cortos reduce la dificultad personal (y las recompensas). Mantente vivo para aprovechar premios legendarios.
- Usa `/coreyhud toggle` para activar o desactivar tu HUD personal de dificultad cuando el servidor lo permita (`enableDifficultyHud = true`).
- La dificultad efectiva aumenta +50 % cuando peleas en biomas de alto riesgo (Snowy Slopes, Deep Dark, Crimson Forest, The End, Dark Forest, End Highlands, Jagged Peaks, Swamp, Nether Wastes, Deep Ocean) y disminuye de forma dinámica si peleas en chunks con calor anti-farm elevado (hasta el mínimo configurado).

### Para Administradores/Operadores
- `/corey debug mob current` muestra atributos, mutaciones y calor de chunk del mob apuntado.
- `/corey tier <entidad> <tier>` fuerza promociones y `/corey spawn mob ...` permite generar hordas de prueba con mutaciones personalizadas.
- `/corey debug chunk activity` inspecciona el estado del anti-farming en el chunk actual.
- `MutationCommand` ofrece herramientas para recargar mutaciones desde datapacks sin reiniciar el servidor.
- Telemetría/Integraciones: `AntiFarmEvents` permite anular detecciones o ajustar thresholds, mientras que `AntiFarmDashboardEvents` + `AntiFarmDashboardApi` entregan datos de calor para dashboards y bots.

## 🔌 Sinergias con Otros Mods
| Mod | Estado | Funcionalidad |
| --- | --- | --- |
| Armory | 🚧 | Solo detección de `.jar` (sin equipamiento automático todavía) |
| Arcaney | 🚧 | Solo detección de `.jar` (encantamientos pendientes) |
| Artifactys | 🚧 | Solo detección de `.jar` (drops y partículas pendientes) |
| Worldscapesys / Eventys / Summonys / Progressionys / Enemiesys / Spawny / Homeys | 🚧 | Detectados mediante `SynergyModule`; integraciones planificadas |

Consulta la guía de sinergias en `docs/GUIA_DESARROLLO.md` para conocer los tags y hooks exactos.

## 🛠️ Para Desarrolladores
- La API pública `com.mrsasayo.legacycreaturescorey.core.api.CoreyAPI` permite spawnear mobs categorizados y consultar/adornar mutaciones desde otros mods.
- `docs/GUIA_DESARROLLO.md` documenta la arquitectura, el ciclo de vida y las fases de inicialización (bootstrap, carga de mundo, runtime), además del stack 26.3.
- `docs/API_REFERENCE.md` describe los métodos expuestos, contratos y ejemplos de integración.
- `docs/PORTING_NOTES.md` recoge el procedimiento del port a 26.3 y la tabla de renames para portar el resto de mods de la suite.
- `docs/mutaciones.csv` es el catálogo de mutaciones con la columna `implementada_en_el_mod` (SI/NO) que indica qué está ya operativo.
- Ejecuta `./gradlew build` antes de contribuir. El proyecto usa Java 25 (configurado en `gradle.properties`, no hace falta JAVA_HOME).

## 📄 Licencia
Este proyecto se publica bajo **CC0 1.0 Universal** (ver `LICENSE`). Puedes usar, modificar y redistribuir el código sin restricciones, aunque se agradece atribución hacia el proyecto Legacy Creatures.
