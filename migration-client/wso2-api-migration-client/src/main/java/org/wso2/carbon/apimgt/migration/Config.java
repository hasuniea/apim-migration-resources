package org.wso2.carbon.apimgt.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.Utility;
import org.wso2.carbon.identity.core.migrate.MigrationClientException;

import java.io.File;
import java.util.List;

public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);
    private static Config config = null;

    private Config() {

    }

    /**
     * Loading configs.
     *
     * @return
     */
    public static Config getInstance() {

        if (config == null) {
            String migrationConfigFileName = Utility.getMigrationResourceDirectoryPath() + File.separator +
                    Constants.MIGRATION_CONFIG_FILE_NAME;
            log.info(Constants.MIGRATION_LOG + "Loading Migration Configs, PATH:" + migrationConfigFileName);
            try {
                config = Utility.loadMigrationConfig(migrationConfigFileName);
            } catch (MigrationClientException e) {
                log.error("Error while loading migration configs.", e);
            }
            log.info(Constants.MIGRATION_LOG + "Successfully loaded the config file.");
        }

        return Config.config;
    }

    public static Config getConfig() {

        return config;
    }

    public static void setConfig(Config config) {

        Config.config = config;
    }

    public boolean isMigrationEnable() {

        return migrationEnable;
    }

    public void setMigrationEnable(boolean migrationEnable) {

        this.migrationEnable = migrationEnable;
    }

    public List<Version> getVersions() {

        return versions;
    }

    public void setVersions(List<Version> versions) {

        this.versions = versions;
    }

    public String getCurrentVersion() {

        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {

        this.currentVersion = currentVersion;
    }

    public String getMigrateVersion() {

        return migrateVersion;
    }

    public void setMigrateVersion(String migrateVersion) {

        this.migrateVersion = migrateVersion;
    }

    public Version getMigrateVersion(String version) {

        for (Version migrateVersion : versions) {
            if (migrateVersion.getVersion().equals(version)) {
                return migrateVersion;
            }
        }
        return null;
    }

}
