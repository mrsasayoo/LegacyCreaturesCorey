# PORTING NOTES — Corey: MC 1.21.10 (Yarn) → MC 26.3 (nombres oficiales)

Port completado el 2026-11-03. Este documento sirve como **guía reutilizable** para portar el
segundo mod del pack (Arcaney) y como referencia de lo que cambió en esta generación de
Minecraft, para cuando salgan versiones futuras (26.4+).

---

## 1. El cambio de fondo (por qué ningún mod viejo funciona)

Desde **MC 26.1** el juego se distribuye **sin ofuscar, con los nombres oficiales de Mojang**.
Yarn murió con la 1.21.11 (última con Yarn). Consecuencias:

- Los mods escritos con Yarn **no cargan** en 26.x: sus mixins y llamadas apuntan a nombres que ya no existen.
- Ya no hay `mappings`/`yarn`/`intermediary` en el build. El jar del juego se usa tal cual.
- La conversión correcta es **renombrar Yarn → nombres oficiales de Mojang** y luego aplicar
  la **deriva de API de 26.3** (cosas que Mojang renombró entre 1.21.10 y 26.3).

### Stack oficial 26.3 (probado en este port)

| Componente | Versión | Nota |
|---|---|---|
| Gradle | **9.8.0** | 9.6.0 NO alcanza para Loom 1.19 |
| Loom | **1.19-SNAPSHOT** (resuelve 1.19.3) | plugin id nuevo: `net.fabricmc.fabric-loom` |
| Fabric Loader | **0.20.1** | |
| Fabric API | **0.165.0+26.3** | hay 0.165.2 parche |
| Java | **25** (Temurin 25.0.5+8) | obligatorio, no opcional |
| Mixin | sponge-mixin 0.17.5, MixinExtras 0.5.5 | compatibilidad JAVA_25 en los .json |

### build.gradle 26.3 (esqueleto)

```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
}
dependencies {
    implementation "com.mojang:minecraft:${minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${fabric_version}"
}
// SIN línea mappings. SIN remapJar: el task es `jar` directo.
java { toolchain ... } ; tasks.withType(JavaCompile) { options.release = 25 }
```

`gradle.properties`: `minecraft_version=26.3`, `loader_version=0.20.1`,
`fabric_version=0.165.0+26.3`, **sin** `yarn_mappings`.
`fabric.mod.json`: depends `minecraft ~26.3`, `fabricloader >=0.20.1`, `java >=25`.

> ⚠️ Dev sin cuenta de Minecraft: `./gradlew runClient` **no pide login**. Loom baja el jar
> de los servidores públicos de Mojang. Se juega como `Player396` offline.

---

## 2. Procedimiento para portar Arcaney (resumen ejecutable)

1. **Red de seguridad**: `git init` + commit del baseline, backup `.tar.gz` fuera del proyecto.
2. **Stack**: copiar el `build.gradle`, `gradle.properties`, wrapper y `fabric.mod.json` de Corey
   (ya en 26.3). Ajustar `mod_version`/ids.
3. **Conversión masiva Yarn→Mojmap**: la fuente autoritativa que usamos (Linkie resultó ruidosa):
   - `yarn mergedv2` tiny para 1.21.10 (columnas official/intermediary/named) + `client.txt`
     ProGuard de Mojang 1.21.10, unidos **por nombre oficial** → tabla yarn→mojmap completa.
   - Aplicar renombrado con protección de: strings (tokens), segmentos de paquete
     (`world`/`entity` aparecen dentro de FQNs — no renombrar dentro de `net.minecraft.world.entity`),
     y scoping por import para métodos/campos.
   - **Los mixins se reescriben a mano**, nunca por renombrado textual.
4. **Verificación con javap**: TODO rename dudoso se valida contra el jar real:
   `~/.gradle/caches/fabric-loom/26.3/minecraft-common.jar` (clases common) y
   `minecraft-client-only.jar` (clases client: `Minecraft`, `Options`, `Hud`, `GuiGraphicsExtractor`).
   `javap -cp <jar> <fqcn>` es el oráculo — ni Linkie ni memoria sirven para 26.3.
5. **Loop de compilación**: `JAVA_HOME=~/jdks/jdk-25.0.5+8 ./gradlew compileJava compileClientJava`,
   agrupar errores, parchear en rondas. Con el temporal `-Xmaxerrs` alto se ven todos de una vez.
6. **Boot**: `./gradlew runClient` — los errores de mixin aparecen al boot, no al compilar.
   Quitar de `run/mods/` cualquier jar pre-26.x y mundos viejos (incompatibles).
7. Los JSON de datos propios (mutaciones, tiers, loot) **no se tocan**.

---

## 3. Tabla de renames Yarn → Mojang (base del port)

Clases/paquetes (yarn → 26.3 real):

| Yarn | 26.3 |
|---|---|
| `Text` | `net.minecraft.network.chat.Component` |
| `Identifier` | `net.minecraft.resources.Identifier` (¡Mojang lo renombró! era ResourceLocation) |
| `MinecraftServer.getWorlds()` | `getAllLevels()` |
| `LivingEntity.damage(world, src, f)` | `hurtServer(ServerLevel, DamageSource, float)` |
| `LivingEntity.onDeath` | `die` |
| `HungerManager` | `FoodData` (`tick(ServerPlayer)`, `eat(int,float)`) |
| `StatusEffect`/`StatusEffectInstance` | `MobEffect`/`MobEffectInstance` |
| `MobEffects.CONFUSION` | `MobEffects.NAUSEA` |
| `BeeEntity` | `net.minecraft.world.entity.animal.bee.Bee` |
| `FleeEntityGoal` | `AvoidEntityGoal` (`canStart→canUse`, `targetEntity→toAvoid`) |
| `PathAwareEntity` | `PathfinderMob` |
| `PhantomEntity$SwoopMovementGoal` | `Phantom$PhantomSweepAttackGoal` |
| `Vec2f` | `net.minecraft.world.phys.Vec2` |
| `ClientPlayerEntity` | `LocalPlayer` (`tickMovement→aiStep`, `getYaw/getPitch→getYRot/getXRot`) |
| `EntityType.CONSTANTE` | **eliminadas** → `BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("x"))` |
| `CustomPayload.Id` | `CustomPacketPayload.Type` |
| `PayloadTypeRegistry.playS2C/C2S` | `clientboundPlay`/`serverboundPlay` |
| `FriendlyByteBuf` | `ByteBufOrder...` → usar `RegistryFriendlyByteBuf.readUtf/writeUtf`, `writeEnum` |
| `GLFW` | **SDL** (input/window moved; solo afecta si se usaba directo) |

Métodos/campos de mayor uso (yarn → 26.3): `getWorld()→getLevel()`, `random→getRandom()`,
`addVelocity→push`, `multiply→scale`, `setVelocity/getVelocity→setDeltaMovement/getDeltaMovement`,
`spawnEntity→addFreshEntity`, `isAir→isEmptyBlock`, `up/down→above/below`,
`getRotationVector→getViewVector(1.0F)`, `getCameraPosVec→getEyePosition(1.0F)`,
`getRegistryManager→registryAccess()`, `sendMessage(msg)→sendSystemMessage(msg)`,
`sendMessage(msg,bool)→sendSystemMessage(msg,bool)` (¡solo ServerPlayer; en Player no existe!),
`hasPermissionLevel(n)` → ver §4 permisos, `getRawId→getId`, `getIds→keySet`,
`suggestIdentifiers→suggestResource`, `unwrapKey/getKey (Optional)→unwrapKey`,
`ResourceKey.getValue→identifier`, `TagKey.id→location`, `ChunkPos` ver §4.

Fabric API 26.3: `SimpleSynchronousResourceReloadListener→ResourceManagerReloadListener`,
`ServerTickEvents.END_WORLD_TICK→END_LEVEL_TICK`, `ItemGroupEvents→CreativeModeTabEvents`,
`HudElementRegistry.attachElementAfter` sigue, pero `HudElement` cambió (ver §4).

---

## 4. Renames EXCLUSIVOS de 26.3 (drift después de mojmap) — los que más duelen

### Permisos: sistema nuevo con PermissionSet
- Ya NO existe `hasPermissionLevel(int)` ni `hasPermission(int)` en `CommandSourceStack`/`Player`.
- Nuevo: `source.permissions()` → `PermissionSet`; para niveles clásicos:
  `source.permissions() instanceof LevelBasedPermissionSet lbps && lbps.level().id() >= 2`.
- Niveles: `PermissionLevel` = ALL(0), MODERATORS(1), GAMEMASTERS(2), ADMINS(3), OWNERS(4).
- `Commands` expone checks listos: `LEVEL_ALL`, `LEVEL_MODERATORS`, `LEVEL_GAMEMASTERS`...

### Datos guardados (SavedData)
- `DimensionDataStorage` → **`SavedDataStorage`** (`ServerLevel.getDataStorage()`).
- `getOrCreate(TYPE)` → **`computeIfAbsent(TYPE)`**.
- `SavedDataType` ahora: `(Identifier id, Supplier<T>, Codec<T>, DataFixTypes)` — el id es
  `Identifier`, no String. Para mods sin datafixer vanilla se usó
  `DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES` (aproximación, ver §6).

### ChunkPos es record con campos privados
- `new ChunkPos(BlockPos)` → `ChunkPos.containing(BlockPos)`.
- `chunkPos.toLong()` → `chunkPos.pack()`; `ChunkPos.toLong(x,z)` → `ChunkPos.pack(x,z)`.
- `chunkPos.x / .z` → **`chunkPos.x() / chunkPos.z()`** (accesores).
- `Entity.getChunkPos()` → `Entity.chunkPosition()`.
- Cuidado: `Vec3.x/.y/.z` SIGUEN siendo campos públicos — no confundir.

### HUD / render (client)
- `GuiGraphics` → **`GuiGraphicsExtractor`** (nueva arquitectura de render states).
  - `getScaledWindowWidth/Height()` → `guiWidth()/guiHeight()`
  - `drawText(font, Component, x, y, color, shadow)` → `text(...)`, `drawString` → `text(String)`
  - `fill` sigue igual.
- Fabric `HudElement`: `render(GuiGraphics, DeltaTracker)` → **`extractRenderState(GuiGraphicsExtractor, DeltaTracker)`**.
- `Minecraft.options.hideGui` → **`Minecraft.getInstance().gui.hud.isHidden()`**
  (nueva clase `net.minecraft.client.gui.Hud`, con `toggle()`).
- `Minecraft.world` → `Minecraft.level`; `textRenderer` → `font`.
- `Options.getSoundVolume(SoundSource)` → `getSoundSourceVolume(...)`;
  `getSoundVolumeOption(...)` → `getSoundSourceOptionInstance(...)`; `OptionInstance.setValue→set`.
- `Level.playSound(entity, ddd, sound, source, vol, pitch, seed)` — **el overload con seed (9 args) ya no existe**, usar 8 args.

### Ira de mobs (Angerable → NeutralMob)
- La interfaz yarn `Angerable` es `net.minecraft.world.entity.NeutralMob`.
- `setAngerTime(int)` → **no existe**. El sistema ahora es por tiempo absoluto:
  `setTimeToRemainAngry(long ticks)` (default que hace `setPersistentAngerEndTime(gameTime + ticks)`).
- `setAngryAt(UUID)` → `setPersistentAngerTarget(EntityReference.of(uuid))`
  (ahora usa `EntityReference<LivingEntity>`, no UUID).

### Otros
- `BlockPos.Mutable` → `BlockPos.MutableBlockPos`; `toLong()→asLong()`, `ofFloored→containing`.
- `AABB.of(Vec3, w, h, d)` → `AABB.ofSize(Vec3, w, h, d)`.
- `getHeightmapPos(Types, x, z)` → `(Types, BlockPos)` y **devuelve BlockPos** (¡antes int!): añadir `.getY()`.
- `Level.isClientSide` (campo) → **método** `isClientSide()`.
- `GsonHelper`: los overloads con `JsonElement` se eliminaron — solo aceptan `JsonObject`;
  para JsonElement usar `convertToJsonObject(element, key)` primero. `asInt→getAsInt`.
- `Item.Properties.registryKey(key)` → `setId(key)`; `maxCount(n)` → `stacksTo(n)`.
- `MobEffects`, `MobEffectInstance.getEffect()` ok, pero `MobEffect.getEffectType()` del mod propio
  no confundir con `MobEffectInstance.getEffectType→getEffect`.
- `Mob.getGoalSelector()` ahora es **público** — los accessors viejos de ese campo rompen el boot
  (colisión "cannot overwrite method"). Eliminarlos y llamar directo.
- `Entity.age` → `tickCount`; `Entity.isSneaking()` → `isShiftKeyDown()`.
- `ProjectileUtil.clip(...)` → `getEntityHitResult(Entity, Vec3, Vec3, AABB, Predicate, double)` (misma forma).
- `LootContext.get(ContextKey)` → `getOptional(ContextKey)`.
- `Mth.approximatelyEquals` → `Mth.equal`.
- `Files.writeUtf` no existe → `Files.writeString`.
- `server.getTicks()→getTickCount()`, `getRunDirectory()→getServerDirectory()`,
  `getOverworld()→overworld()`, `getDataPackManager().scanPacks()→getPackRepository().reload()`.
- `CommandSourceStack.getPlayerOrThrow()` → `getPlayerOrException()`;
  `getName()` → `getTextName()`.
- `SharedSuggestionProvider.suggestIdentifiers` → `suggestResource`.
- `BuiltInRegistries.createWorldLookup()` → `RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)`.
- `RegistryOps.of(JsonOps, provider)` → `provider.createSerializationContext(JsonOps)`.
- `PowerParticleOption.of(type, power)` → `create(type, power)`.
- `AreaEffectCloud.setParticle→setCustomParticle`, `setRadiusGrowth→setRadiusPerTick`.
- `Level.ExplosionSourceType` → `ExplosionInteraction`.
- `GoalSelector.add` → `addGoal`.
- `Entity.getCommandTags()/addCommandTag` → `getTags()/addTag`.
- `ItemStack.getDamage/setDamage` → `getDamageValue/setDamageValue`;
  `stack.damage(n, player, slot)` → `stack.hurtAndBreak(n, player.serverLevel(), player, consumer)`.
- `MobEffect.canApplyUpdateEffect` → `shouldApplyEffectTickThisTick`;
  `LivingEntity.onStatusEffectApplied/Upgraded/Removed` → `onEffectAdded/onEffectUpdated/onEffectsRemoved`;
  `hasStatusEffect→hasEffect`, `getStatusEffect→getEffect`, `getStatusEffects→getActiveEffects`,
  `removeStatusEffect→removeEffect`.
- `ItemEntity.onPlayerCollision` → `playerTouch`.
- `Entity.velocityModified` → `syncVelocity`; `DamageSource.getAttacker()→getEntity()`;
  `knockback(d,d,d,src,f)`; `onEntityHit→onHitEntity`; `Projectile.setVelocity(5 args)→shoot(...)`.

---

## 5. Mixins: checklist de re-target

1. `refmap` ya no existe: los targets van con **FQN literal** en el json y en `@Mixin`.
2. Los métodos de target se escriben con nombre oficial `"hurtServer"`, `"die"`, etc.
3. Campos privados de record (`ChunkPos.x`) requieren accesores `x()` en código normal;
   en mixins usar `@Shadow` del campo o el accessor.
4. Clases internas con `this$0`: declarar `@Shadow @Final private Phantom this$0;`
   (el nombre sintético se respeta).
5. Si un accessor declara un método que en 26.3 ya existe público en la clase target,
   el **boot** falla con "cannot overwrite method ... @Overwrite is required" — no lo
   detecta la compilación. Eliminar el accessor y llamar directo.
6. Tras editar mixins, SIEMPRE validar con `javap` contra `minecraft-common.jar`/`minecraft-client-only.jar`.

---

## 6. Aproximaciones hechas (a revisar en juego)

| Sitio | Aproximación | Riesgo |
|---|---|---|
| knockbacks de mutaciones | factor fijo `1.0F` donde el signature cambió | sensación de empuje distinta |
| `stack.broadcastBreakEvent(slot)` | se pasa consumer no-op / lambda vacía | sin efecto visual de rotura |
| líneas con `setInvulnerable` | comentadas (el método se eliminó) | mobs ya no invulnerables transitorios |
| `FuryHelper.setTimeToRemainAngry(600L)` | unidades ticks (verificado en bytecode: gameTime + n) | ok, pero revisar duración esperada |
| `SavedDataType` con `DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES` | no hay NONE para datos de mod | si Mojang cambia el schema de ese tipo, la versión de datafix puede no cuadrar; los datos del mod viven bajo su propio id, en la práctica seguro |
| `getRawBrightness(pos, 0)` | firma cambió de mundo/luz | iluminación aproximada |
| `MOVEMENT_SPEED_WHEN_FIGHTING` | → `MOVEMENT_SPEED` | velocidad al pelear usa la base |

---

## 7. Notas para versiones futuras (26.4+)

- **El ritmo de renames sigue alto**: entre 1.21.10 y 26.3 cambió ~30% de la API tocada.
  La nueva era post-ofuscación hace que Mojang ya no preserve nombres "para mods":
  cualquier snapshot puede renombrar. Esperar drift en cada release mayor.
- **Verificación rápida de drift al subir versión**: borrar `~/.gradle/caches/fabric-loom/<nueva>/`,
  compilar, y usar `javap` contra el jar nuevo de la cache. El loop compile→javap→parchear
  de este port es EL método; no confiar en tablas de terceros (Linkie quedó congelado en la era Yarn).
- **Arquitectura de render en transición**: 26.3 movió GUI a render-states (`GuiGraphicsExtractor`,
  `Hud.extractRenderState`, `GuiRenderState`, `CameraRenderState.hideGui`). Es probable que
  26.4+ siga moviendo piezas client aquí.
- **Permisos**: el sistema `PermissionSet/PermissionLevel` parece diseñado para crecer hacia
  permisos granulares por nodo; los checks por nivel int pueden volver a cambiar.
- **Ira (NeutralMob)**: pasó de int a timestamps absolutos + `EntityReference`; vigilar si
  `EntityReference` se extiende a más APIs de entidades.
- **SDL reemplaza GLFW** en windowing/input: mods con input nativo deben migrar (Corey no usaba).
- **Jar sin ofuscar**: desde 26.1 se puede javap directo contra `minecraft-common.jar`
  de la cache de Loom para verificar cualquier firma en segundos.
- El kernel de conversión (scripts Python en `/tmp/port-corey-scripts/` — **se borran con reboot**;
  copiar a un lugar durable si se van a reutilizar pronto) + la tabla §3/§4 cubren ~95% de un mod
  tipo Arcaney (tamaño medio, mixins moderados).

---

## 8. Estado final del port de Corey (2026-11-03)

- `compileJava` + `compileClientJava`: **verde** (de 1.416 errores iniciales a 0, ~17 rondas).
- `runClient`: boot limpio en 26.3 — mod cargado exitosamente, **0 errores de mixin**,
  Jade 26.3.5 y ModMenu 21.0.0 cargados desde `run/mods/`.
- Único ERROR del log: `Failed to create backend OpenGL` → fallback de render del entorno
  XWayland de este PC (probar `SDL_VIDEO_DRIVER=wayland` si la ventana se ve rara). No es del mod.
- Datos: `docs/mutaciones.csv` (1.147 líneas) y los 197 JSON de mutaciones intactos.
- Git: baseline 1.21.10 en `097b91c`, checkpoints del port hasta el final; backup
  `Corey_backup_yarn-1.21.10.tar.gz` en la carpeta del modpack.
- Pendiente humano: prueba in-game de lo que ya existía (HUD de dificultad, `/corey`,
  `/mutation`, efectos, mutaciones, anti-farm) y los puntos de §6.
