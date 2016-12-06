package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.rest.model.RestProcessDefinitionModelsCollection;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 12/6/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.CORE })
public class GetProcessDefinitionStartFormModelCoreTests extends RestTest
{
    private UserModel adminUser, adminTenantUser;
    private RestProcessDefinitionModel randomProcessDefinition, returnedProcessDefinition;
    private RestProcessDefinitionModelsCollection allProcessDefinitions;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        allProcessDefinitions = restClient.authenticateUser(adminUser).withWorkflowAPI().getAllProcessDefinitions();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify any user gets a model of the start form type definition for non-network deployments using REST API and status code is OK (200)")
    public void nonNetworkUserGetsStartFormModel() throws Exception
    {
        UserModel nonNetworkUser = dataUser.createRandomTestUser();

        randomProcessDefinition = allProcessDefinitions.getOneRandomEntry();
        restClient.authenticateUser(nonNetworkUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionStartFormModel()
                .assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Bug(id = "ALF-20187")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify if get request returns status code 404 when invalid processDefinitionId is used")
    public void getStartFormModelUsingInvalidProcessDefinitionId() throws Exception
    {
        randomProcessDefinition = allProcessDefinitions.getOneRandomEntry();
        randomProcessDefinition.onModel().setId("invalidID");

        restClient.authenticateUser(adminUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionStartFormModel();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidID"));
    }

    @Bug(id = "ALF-20187")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify if get request returns status code 404 when empty processDefinitionId is used")
    public void getStartFormModelUsingEmptyProcessDefinitionId() throws Exception
    {
        randomProcessDefinition = allProcessDefinitions.getOneRandomEntry();
        randomProcessDefinition.onModel().setId("");

        restClient.authenticateUser(adminUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionStartFormModel();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify Tenant User gets a model of the start form type definition for network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.NETWORKS })
    public void networkUserGetsStartFormModel() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser)
                .usingTenant().createTenant(adminTenantUser);

        UserModel tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");

        randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .getAllProcessDefinitions().getOneRandomEntry();
        restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionStartFormModel()
                .assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
