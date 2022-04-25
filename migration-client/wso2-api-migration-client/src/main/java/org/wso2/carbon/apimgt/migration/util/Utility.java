package org.wso2.carbon.apimgt.migration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apimgt.migration.Config;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utility {

    private static final Logger log = LoggerFactory.getLogger(Utility.class);

    public static String getMigrationResourceDirectoryPath() {

        Path path = Paths.get(System.getProperty(Constants.CARBON_HOME), Constants.MIGRATION_RESOURCE_HOME);
        return path.toString();
    }

    public static Config loadMigrationConfig(String configFilePath) throws MigrationClientException {

        Config config = null;
        Path path = Paths.get(configFilePath);
        if (Files.exists(path)) {
            try {
                Reader in = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8);
                Yaml yaml = new Yaml();
                yaml.setBeanAccess(BeanAccess.FIELD);
                config = yaml.loadAs(in, Config.class);
                if (config == null) {
                    throw new MigrationClientException("Provider is not loaded correctly.");
                }
            } catch (IOException e) {
                String errorMessage = "Error occurred while loading the " + Config.class
                        + " yaml file, " +
                        e.getMessage();
                log.error(errorMessage, e);
                throw new MigrationClientException(errorMessage, e);
            }
        } else {
            throw new MigrationClientException(Constants.MIGRATION_CONFIG_FILE_NAME + " file does not exist at: " +
                    configFilePath);
        }
        return config;
    }
}
