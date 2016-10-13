package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.requests.RestTasksApi;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.GroupModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "workflow", "tasks", "sanity" })
public class GetTaskSanityTests extends RestWorkflowTest
{
    @Autowired
    RestTasksApi tasksApi;

    UserModel userModel;
    SiteModel siteModel;
    UserModel candidateUser;
    FileModel fileModel;
    UserModel assigneeUser;
    TaskModel taskModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);

        tasksApi.useRestClient(restClient);
    }

    @TestRail(section = { "rest-api", "workflow", "tasks" }, executionType = ExecutionType.SANITY, description = "Verify admin user gets any existing task with Rest API and response is successfull (200)")
    public void adminUserGetsAnyTaskWithSuccess() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        tasksApi.getTask(taskModel);
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "workflow", "tasks" }, executionType = ExecutionType.SANITY, description = "Verify assignee user gets its assigned task with Rest API and response is successfull (200)")
    public void assigneeUserGetsItsTaskWithSuccess() throws Exception
    {
        restClient.authenticateUser(assigneeUser);
        tasksApi.getTask(taskModel);
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "workflow", "tasks" }, executionType = ExecutionType.SANITY, description = "Verify user that started the task gets the started task with Rest API and response is successfull (200)")
    public void starterUserGetsItsTaskWithSuccess() throws Exception
    {
        restClient.authenticateUser(userModel);
        tasksApi.getTask(taskModel);
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "workflow", "tasks" }, executionType = ExecutionType.SANITY, description = "Verify any user with no relation to task id forbidden to get other task with Rest API (403)")
    public void anyUserIsForbiddenToGetOtherTask() throws Exception
    {
        UserModel anyUser= dataUser.createRandomTestUser();
        
        restClient.authenticateUser(anyUser);
        tasksApi.getTask(taskModel);
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }
    
    @TestRail(section = { "rest-api", "workflow", "tasks" }, executionType = ExecutionType.SANITY, description = "Verify candidate user gets its specific task and no other user claimed the task with Rest API and response is successfull (200)")
    public void candidateUserGetsItsTasks() throws Exception
    {
        UserModel userModel1 = dataUser.createRandomTestUser();
        UserModel userModel2 = dataUser.createRandomTestUser();
        GroupModel group = dataGroup.createRandomGroup();
        dataGroup.addListOfUsersToGroup(group, userModel1, userModel2);
        TaskModel taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createPooledReviewTaskAndAssignTo(group);
        
        restClient.authenticateUser(userModel1);
        tasksApi.getTask(taskModel);
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
