package io.github.enerccio.rensu.app.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class ConfigContainer implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(ConfigContainer.class);
    private static final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT)
            .serializeNulls().setPrettyPrinting().create();

    private final Map<String, RensuProfile> profiles = new TreeMap<>();
    private RensuProfile baseProfile = new RensuProfile();
    private File configFolder;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Initializing config with config folder {}", getConfigFolder().getAbsolutePath());

        if (!getConfigFolder().exists())
            getConfigFolder().mkdirs();
        if (!getUserConfigFolder().exists())
            getUserConfigFolder().mkdirs();

        File baseProfileFile = getBaseProfileFile();
        if (baseProfileFile.exists()) {
            baseProfile = gson.fromJson(FileUtils.readFileToString(baseProfileFile, StandardCharsets.UTF_8),
                    RensuProfile.class);
        }
    }

    @Override
    public void destroy() throws Exception {
        save();
    }

    public void save() throws Exception {
        FileUtils.writeStringToFile(getBaseProfileFile(), gson.toJson(baseProfile), StandardCharsets.UTF_8);
    }

    public File getConfigFolder() {
        return configFolder;
    }

    public void setConfigFolder(File configFolder) {
        this.configFolder = configFolder;
    }

    public File getUserConfigFolder() {
        return new File(getConfigFolder(), "userConfigs");
    }

    public File getBaseProfileFile() {
        return new File(getConfigFolder(), "baseProfile.json");
    }

    public RensuProfile getGlobalProfile() {
        return baseProfile;
    }
}
