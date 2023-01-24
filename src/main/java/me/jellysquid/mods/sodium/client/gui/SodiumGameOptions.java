package me.jellysquid.mods.sodium.client.gui;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jellysquid.mods.sodium.client.gui.options.TextProvider;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.text.Text;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SodiumGameOptions {
    private static final String DEFAULT_FILE_NAME = "sodium-options.json";

    public final QualitySettings quality = new QualitySettings();
    public final AdvancedSettings advanced = new AdvancedSettings();
    public final CustomizationSettings customization = new CustomizationSettings();
    public final PerformanceSettings performance = new PerformanceSettings();
    public final NotificationSettings notifications = new NotificationSettings();

    private boolean readOnly;

    private Path configPath;

    public static SodiumGameOptions defaults() {
        var options = new SodiumGameOptions();
        options.configPath = getConfigPath(DEFAULT_FILE_NAME);
        options.sanitize();

        return options;
    }

    public static class PerformanceSettings {
        public int chunkBuilderThreads = 0;
        public boolean alwaysDeferChunkUpdates = false;

        public boolean animateOnlyVisibleTextures = true;
        public boolean useEntityCulling = true;
        public boolean useParticleCulling = true;
        public boolean useFogOcclusion = true;
        public boolean useBlockFaceCulling = true;
        public boolean useRasterOcclusionCulling = true;
    }

    public static class AdvancedSettings {
        public ArenaMemoryAllocator arenaMemoryAllocator = null;

        public boolean allowDirectMemoryAccess = true;
        public boolean enableMemoryTracing = false;
        public boolean useAdvancedStagingBuffers = true;

        public int cpuRenderAheadLimit = 3;
    }

    public static class CustomizationSettings {
        public ColorTheme colorTheme = ColorTheme.STANDARD;
    }

    public static class QualitySettings {
        public GraphicsQuality weatherQuality = GraphicsQuality.DEFAULT;
        public GraphicsQuality leavesQuality = GraphicsQuality.DEFAULT;

        public boolean enableVignette = true;
    }

    public static class NotificationSettings {
        public boolean hideDonationButton = false;
    }

    public enum ArenaMemoryAllocator implements TextProvider {
        ASYNC("sodium.options.chunk_memory_allocator.async"),
        SWAP("sodium.options.chunk_memory_allocator.swap");

        private final Text name;

        ArenaMemoryAllocator(String name) {
            this.name = Text.translatable(name);
        }

        @Override
        public Text getLocalizedName() {
            return this.name;
        }
    }

    public enum GraphicsQuality implements TextProvider {
        DEFAULT("options.gamma.default"),
        FANCY("options.clouds.fancy"),
        FAST("options.clouds.fast");

        private final Text name;

        GraphicsQuality(String name) {
            this.name = Text.translatable(name);
        }

        @Override
        public Text getLocalizedName() {
            return this.name;
        }

        public boolean isFancy(GraphicsMode graphicsMode) {
            return (this == FANCY) || (this == DEFAULT && (graphicsMode == GraphicsMode.FANCY || graphicsMode == GraphicsMode.FABULOUS));
        }
    }

    public enum ColorTheme implements TextProvider {
        STANDARD("sodium.options.color_theme.standard", 0x90000000, 0xE0000000, 0xFFFFFFFF, 0x60000000, 0x90FFFFFF),
        LIGHT("sodium.options.color_theme.light", 0x90FFFFFF, 0xE0FFFFFF, 0xFFb9b9b9, 0x60000000, 0x90FFFFFF),
        PURPLE("sodium.options.color_theme.purple", 0x90680e97, 0xE09426c5, 0xFFFFFFFF, 0x60000000, 0x90FFFFFF),
        MAGENTA("sodium.options.color_theme.magenta", 0x90680e97, 0xE09426c5, 0xFFFFFFFF, 0x60000000, 0x90FFFFFF),
        RED("sodium.options.color_theme.red", 0x90970e1d, 0xE0c52638, 0xFFFFFFFF, 0x60000000, 0x90FFFFFF),
        GREEN("sodium.options.color_theme.green", 0x900e9713, 0xE02cc526, 0xFFFFFFFF, 0x60000000, 0x90FFFFFF),
        LIGHT_BLUE("sodium.options.color_theme.light_blue", 0x900e7797, 0xE0269ac5, 0xFFFFFFFF, 0x60000000, 0x90FFFFFF),
        BLUE("sodium.options.color_theme.blue", 0x900e3197, 0xE02637c5, 0xFFFFFFFF, 0x60000000, 0x90FFFFFF),
        RAINBOW("sodium.options.color_theme.rainbow", 0x90000000, 0xE0000000, 0xFFFFFFFF, 0x60000000, 0x90FFFFFF),
        VANILLA("sodium.options.color_theme.vanilla", 0x90000000, 0xE0000000, 0xFFFFFFFF, 0x60000000, 0x90FFFFFF);

        private final Text name;
        public final int enabledColor, enabledHoverColor, enabledTextColor, disabledColor, disabledTextColor;

        ColorTheme(String name, int enabledColor, int enabledHoverColor, int enabledTextColor, int disabledColor, int disabledTextColor) {
            this.name = Text.translatable(name);
            this.enabledColor = enabledColor;
            this.enabledHoverColor = enabledHoverColor;
            this.enabledTextColor = enabledTextColor;
            this.disabledColor = disabledColor;
            this.disabledTextColor = disabledTextColor;
        }

        @Override
        public Text getLocalizedName() {
            return this.name;
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static SodiumGameOptions load() {
        return load(DEFAULT_FILE_NAME);
    }

    public static SodiumGameOptions load(String name) {
        Path path = getConfigPath(name);
        SodiumGameOptions config;

        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                config = GSON.fromJson(reader, SodiumGameOptions.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse config", e);
            }
        } else {
            config = new SodiumGameOptions();
        }

        config.configPath = path;
        config.sanitize();

        try {
            config.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't update config file", e);
        }

        return config;
    }

    private void sanitize() {
        if (this.advanced.arenaMemoryAllocator == null) {
            this.advanced.arenaMemoryAllocator = ArenaMemoryAllocator.ASYNC;
        }
    }

    private static Path getConfigPath(String name) {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(name);
    }

    public void writeChanges() throws IOException {
        if (this.isReadOnly()) {
            throw new IllegalStateException("Config file is read-only");
        }

        Path dir = this.configPath.getParent();

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        } else if (!Files.isDirectory(dir)) {
            throw new IOException("Not a directory: " + dir);
        }

        // Use a temporary location next to the config's final destination
        Path tempPath = this.configPath.resolveSibling(this.configPath.getFileName() + ".tmp");

        // Write the file to our temporary location
        Files.writeString(tempPath, GSON.toJson(this));

        // Atomically replace the old config file (if it exists) with the temporary file
        Files.move(tempPath, this.configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public void setReadOnly() {
        this.readOnly = true;
    }

    public String getFileName() {
        return this.configPath.getFileName().toString();
    }
}
