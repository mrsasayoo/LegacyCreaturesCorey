# AGENTS.md — Reglas para agentes de código en este repo

## Regla de oro: primero Minecraft, después el mod

1. **Antes de implementar cualquier mecánica, verifica qué Minecraft vanilla ya
   ofrece de forma nativa**: atributos (`Attributes`), efectos de estado,
   entidades, tags, items, mecánicas del motor y APIs internas del juego.
   El oráculo oficial de este proyecto es `javap` contra el jar de la cache:
   - `~/.gradle/caches/fabric-loom/26.3/minecraft-common.jar`
   - `~/.gradle/caches/fabric-loom/26.3/minecraft-client-only.jar` (clases client)

2. **NO duplicar funciones que vanilla ya provee, ni construir sistemas propios
   sobre lo que ya existe y funciona**. Si Minecraft tiene el atributo/mechanismo
   correcto, se usa y se configura (datapack/JSON del mod) antes que escribir
   lógica propia.

3. **Si la petición necesita crear cosas nuevas FUERA de lo que Minecraft provee
   (efectos de poción custom, overlays de pantalla, mixins, managers propios,
   contenido inexistente en el juego), está PERMITIDO — es un mod**. Ahí sí se
   crea contenido nuevo del mod, siguiendo las recetas del proyecto (§8 del
   snapshot del workstream: efecto nuevo = clase marker + registro + sprite +
   lang; overlay = HudElementRegistry; handler = patrón INSTANCE + WeakHashMap).

## Lecciones de este proyecto que motivan la regla

- `gravity_1` con el atributo vanilla directo rompía el salto del mob (gravedad
  doble al despegar) → se re-implementó como gravedad solo en caída vía onTick.
- `water_movement_efficiency` vanilla existe pero su cableado en mobs añade una
  aceleración imperceptible → se complementó con asistencia de nado propia.
- `attack_knockback` es inherentemente melee: documentar esa limitación en el CSV
  en vez de inventar una mecánica de empuje para proyectiles.

## Otras reglas vigentes del proyecto

- Verificar firmas con `javap` ANTES de usar cualquier API nueva (26.3 sin mappings).
- `docs/mutaciones.csv` es la fuente de verdad del catálogo; el JSON real manda
  sobre el CSV al documentar divergencias, y la palabra del usuario sobre ambos.
- Avisar SIEMPRE antes de ejecutar `./gradlew runClient`.
- Push permitido (token en `~/.git-credentials`), pero cada lote se commitea solo
  tras verificación en juego del usuario.
