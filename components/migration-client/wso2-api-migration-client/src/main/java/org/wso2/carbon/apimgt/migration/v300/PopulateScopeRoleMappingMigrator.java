package org.wso2.carbon.apimgt.migration.v300;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.Migrator;
import org.wso2.carbon.apimgt.migration.Utility;
import org.wso2.carbon.apimgt.migration.dao.SharedDAO;
import org.wso2.carbon.apimgt.migration.dto.UserRoleFromPermissionDTO;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.RegistryServiceImpl;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.IOException;
import java.util.List;

import static org.wso2.carbon.apimgt.migration.client.MigrationClientBase.updateTenantConf;

public class PopulateScopeRoleMappingMigrator extends Migrator {
    private static final Log log = LogFactory.getLog(PopulateScopeRoleMappingMigrator.class);
    List<Tenant> tenants;
    RegistryServiceImpl registryService;

    public PopulateScopeRoleMappingMigrator() throws UserStoreException {
        tenants = loadTenants();
        registryService = new RegistryServiceImpl();
    }

    @Override
    public void migrate() throws APIMigrationException {
      populateRoleMappingWithUserRoles();
    }

    /**
     * This method is used to update the scopes of the user roles which will be retrieved based on the
     * permissions assigned.
     */
    private void populateRoleMappingWithUserRoles() throws APIMigrationException {
        log.info("Updating User Roles based on Permissions started.");
        for (Tenant tenant : tenants) {
            try {
                registryService.startTenantFlow(tenant);
                if (tenant.getId() != MultitenantConstants.SUPER_TENANT_ID) {
                    Utility.loadAndSyncTenantConf(tenant.getId());
                }

                log.info("Updating user roles for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');

                // Retrieve user roles which has create permission
                List<UserRoleFromPermissionDTO> userRolesListWithCreatePermission = SharedDAO.getInstance()
                        .getRoleNamesMatchingPermission(Constants.API_CREATE, tenant.getId());

                // Retrieve user roles which has publish permission
                List<UserRoleFromPermissionDTO> userRolesListWithPublishPermission = SharedDAO.getInstance()
                        .getRoleNamesMatchingPermission(Constants.API_PUBLISH, tenant.getId());

                // Retrieve user roles which has subscribe permission
                List<UserRoleFromPermissionDTO> userRolesListWithSubscribePermission = SharedDAO.getInstance()
                        .getRoleNamesMatchingPermission(Constants.API_SUBSCRIBE, tenant.getId());

                // Retrieve user roles which has manage API permission
                List<UserRoleFromPermissionDTO> userRolesListWithManageAPIPermission = SharedDAO.getInstance()
                        .getRoleNamesMatchingPermission(Constants.API_MANAGE, tenant.getId());
                userRolesListWithCreatePermission.addAll(userRolesListWithManageAPIPermission);
                userRolesListWithPublishPermission.addAll(userRolesListWithManageAPIPermission);
                userRolesListWithSubscribePermission.addAll(userRolesListWithManageAPIPermission);

                // Retrieve user roles which has admin permissions
                List<UserRoleFromPermissionDTO> userRolesListWithAdminPermission = SharedDAO.getInstance()
                        .getRoleNamesMatchingPermissions(Utility.makePermissionsStringByEscapingSlash(
                                Constants.APIM_ADMIN, "/permission"), tenant.getId());

                // Retrieve the tenant-conf.json of the corresponding tenant
                JSONObject tenantConf = Utility.getTenantConfigFromRegistry(tenant.getId());

                // Extract the RoleMappings object (This will be null if this does not exist at the moment)
                JSONObject roleMappings = (JSONObject) tenantConf.get(APIConstants.REST_API_ROLE_MAPPINGS_CONFIG);
                if (roleMappings == null) {
                    // Create RoleMappings field in tenant-conf.json and retrieve the object
                    tenantConf.put(APIConstants.REST_API_ROLE_MAPPINGS_CONFIG, new JSONObject());
                    roleMappings = (JSONObject) tenantConf.get(APIConstants.REST_API_ROLE_MAPPINGS_CONFIG);
                }

                Utility.createOrUpdateRoleMappingsField(roleMappings, userRolesListWithCreatePermission,
                        userRolesListWithPublishPermission, userRolesListWithSubscribePermission,
                        userRolesListWithAdminPermission);

                ObjectMapper mapper = new ObjectMapper();
                String formattedTenantConf = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tenantConf);

                updateTenantConf(formattedTenantConf, tenant.getId());
                log.info("Updated tenant-conf.json for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')'
                        + "\n" + formattedTenantConf);

                log.info("End updating user roles for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            } catch (APIMigrationException e) {
                log.error("Error while retrieving role names based on existing permissions. ", e);
            } catch (JsonProcessingException e) {
                log.error("Error while formatting tenant-conf.json of tenant " + tenant.getId());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                registryService.endTenantFlow();
            }
        }
        log.info("Updating User Roles done for all the tenants.");
    }
}