package com.mrsasayo.legacycreaturescorey.util;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.MarkerFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Inicializador del sistema de logging multi-nivel para mutaciones.
 * 
 * Configura programáticamente los appenders de Log4j2 para escribir
 * logs de cada categoría de mutación a archivos separados.
 * 
 * Debe llamarse durante la inicialización del mod (onInitialize).
 */
public final class mutation_logger_initializer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("MutationLoggerInit");
    private static boolean initialized = false;
    
    // Categorías de mutación con sus marcadores
    private static final String[] CATEGORIES = {
        "aura",
        "on_hit", 
        "mob_exclusive",
        "passive",
        "on_being_hit",
        "on_death",
        "synergy",
        "terrain",
        "debug"
    };
    
    private mutation_logger_initializer() {}
    
    /**
     * Inicializa el sistema de logging para mutaciones.
     * Crea los appenders de archivo para cada categoría.
     */
    public static synchronized void initialize() {
        if (initialized) {
            LOGGER.debug("Sistema de logging de mutaciones ya inicializado.");
            return;
        }
        
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path logsDir = gameDir.resolve("logs");
            
            // Asegurar que el directorio existe
            logsDir.toFile().mkdirs();
            
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration config = context.getConfiguration();
            
            // Patrón de formato para los logs de mutación
            PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("[%d{HH:mm:ss}] [%level] %msg%n")
                .withConfiguration(config)
                .build();
            
            // Crear appender para cada categoría
            for (String category : CATEGORIES) {
                createCategoryAppender(config, layout, logsDir, category);
            }
            
            // Crear appender combinado para todas las mutaciones
            createAllMutationsAppender(config, layout, logsDir);
            
            // Configurar el logger MutationLogger
            configureMutationLogger(config);
            
            // Actualizar la configuración
            context.updateLoggers();
            
            initialized = true;
            LOGGER.info("✅ Sistema de logging de mutaciones inicializado correctamente");
            LOGGER.info("📁 Logs de mutaciones en: {}", logsDir);
            
        } catch (Exception e) {
            LOGGER.error("❌ Error inicializando sistema de logging de mutaciones", e);
        }
    }
    
    private static void createCategoryAppender(Configuration config, PatternLayout layout, 
                                                Path logsDir, String category) {
        String fileName = "mutations_" + category + ".log";
        String filePattern = "mutations_" + category + "-%d{yyyy-MM-dd}-%i.log.gz";
        String appenderName = "Mutation" + capitalize(category) + "File";
        String markerName = "MUTATION_" + category.toUpperCase();
        
        try {
            // Crear filtro por marcador
            MarkerFilter filter = MarkerFilter.createFilter(
                markerName,
                org.apache.logging.log4j.core.Filter.Result.ACCEPT,
                org.apache.logging.log4j.core.Filter.Result.DENY
            );
            
            // Políticas de rotación
            TimeBasedTriggeringPolicy timePolicy = TimeBasedTriggeringPolicy.newBuilder()
                .withInterval(1)
                .withModulate(true)
                .build();
            
            SizeBasedTriggeringPolicy sizePolicy = SizeBasedTriggeringPolicy.createPolicy("10MB");
            
            CompositeTriggeringPolicy triggeringPolicy = CompositeTriggeringPolicy.createPolicy(
                timePolicy, sizePolicy
            );
            
            // Estrategia de rollover
            DefaultRolloverStrategy rolloverStrategy = DefaultRolloverStrategy.newBuilder()
                .withMax("5")
                .withConfig(config)
                .build();
            
            // Crear el appender
            RollingFileAppender appender = RollingFileAppender.newBuilder()
                .setName(appenderName)
                .withFileName(logsDir.resolve(fileName).toString())
                .withFilePattern(logsDir.resolve(filePattern).toString())
                .setLayout(layout)
                .withPolicy(triggeringPolicy)
                .withStrategy(rolloverStrategy)
                .setFilter(filter)
                .setConfiguration(config)
                .build();
            
            appender.start();
            config.addAppender(appender);
            
            LOGGER.debug("📝 Appender creado: {} -> {}", appenderName, fileName);
            
        } catch (Exception e) {
            LOGGER.warn("No se pudo crear appender para categoría: {}", category, e);
        }
    }
    
    private static void createAllMutationsAppender(Configuration config, PatternLayout layout, Path logsDir) {
        try {
            String fileName = "mutations_all.log";
            String filePattern = "mutations_all-%d{yyyy-MM-dd}-%i.log.gz";
            String appenderName = "MutationAllFile";
            
            TimeBasedTriggeringPolicy timePolicy = TimeBasedTriggeringPolicy.newBuilder()
                .withInterval(1)
                .withModulate(true)
                .build();
            
            SizeBasedTriggeringPolicy sizePolicy = SizeBasedTriggeringPolicy.createPolicy("20MB");
            
            CompositeTriggeringPolicy triggeringPolicy = CompositeTriggeringPolicy.createPolicy(
                timePolicy, sizePolicy
            );
            
            DefaultRolloverStrategy rolloverStrategy = DefaultRolloverStrategy.newBuilder()
                .withMax("7")
                .withConfig(config)
                .build();
            
            RollingFileAppender appender = RollingFileAppender.newBuilder()
                .setName(appenderName)
                .withFileName(logsDir.resolve(fileName).toString())
                .withFilePattern(logsDir.resolve(filePattern).toString())
                .setLayout(layout)
                .withPolicy(triggeringPolicy)
                .withStrategy(rolloverStrategy)
                .setConfiguration(config)
                .build();
            
            appender.start();
            config.addAppender(appender);
            
            LOGGER.debug("📝 Appender combinado creado: {}", fileName);
            
        } catch (Exception e) {
            LOGGER.warn("No se pudo crear appender combinado", e);
        }
    }
    
    private static void configureMutationLogger(Configuration config) {
        LoggerConfig loggerConfig = new LoggerConfig("MutationLogger", Level.DEBUG, true);
        
        // Agregar todos los appenders al logger
        for (String category : CATEGORIES) {
            String appenderName = "Mutation" + capitalize(category) + "File";
            if (config.getAppender(appenderName) != null) {
                loggerConfig.addAppender(config.getAppender(appenderName), Level.DEBUG, null);
            }
        }
        
        // Agregar appender combinado
        if (config.getAppender("MutationAllFile") != null) {
            loggerConfig.addAppender(config.getAppender("MutationAllFile"), Level.DEBUG, null);
        }
        
        config.addLogger("MutationLogger", loggerConfig);
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        
        // Convertir snake_case a CamelCase
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : str.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Verifica si el sistema está inicializado.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
