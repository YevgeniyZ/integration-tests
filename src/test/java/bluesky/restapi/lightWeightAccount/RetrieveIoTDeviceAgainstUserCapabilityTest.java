package bluesky.restapi.lightWeightAccount;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.base.BaseAssertions;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.helpers.TokenAccessHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.managers.AccountHierarchyEntityManager;
import bluesky.restapi.managers.BaseManager;
import bluesky.restapi.managers.UserCapabilityManager;
import bluesky.restapi.methods.UserCapabilityServiceMethods;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.LightWeightAccount.Node;
import bluesky.restapi.models.UserCapabilities;
import io.restassured.response.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static java.lang.Integer.parseInt;

public class RetrieveIoTDeviceAgainstUserCapabilityTest extends BaseApiTest {

  //Before running the tests, hierarchy account needs to be added to the DB. Please refer to https://jira.metratech
  // .com/browse/QA-297 for more details

  private String[] AccountHierarchyEntityArray =
      { "MassachusettsTestEC", "BostonTestEC", "WalthamTestEC", "GETestEC", "Dep1GETestEC", "Dep2GETestEC" };
  private final String softWareId = "JavaTest";
  private final String defaultPassword = "123";

  private AccountHierarchyEntityManager MainGroup = new AccountHierarchyEntityManager();
  private AccountHierarchyEntityManager MainSubGroup1 = new AccountHierarchyEntityManager();
  private AccountHierarchyEntityManager MainSubGroup2 = new AccountHierarchyEntityManager();
  private AccountHierarchyEntityManager PartnerGroup = new AccountHierarchyEntityManager();
  private AccountHierarchyEntityManager PartnerSubGroup1 = new AccountHierarchyEntityManager();
  private AccountHierarchyEntityManager PartnerSubGroup2 = new AccountHierarchyEntityManager();

  private AccountEntity csr;
  private Integer csrId;
  private String customToken;
  private List<UserCapabilities.ManageAccounts> manageAccountsList;
  private UserCapabilities userCapability;
  private String requestDate;
  private UserCapabilityServiceMethods userCapabilityServiceMethods;
  private RetrieveIotDeviceTest retrieveIotDeviceTest;

  @BeforeClass
  private Integer createCSRUser() {

    csr = new AccountEntityManager().createAccountEntity()
        .setUserName("jcsr" + BaseManager.incrementIndex() + RandomStringUtils.randomAlphabetic(5))
        .setPassword("123")
        .setConfirmPassword("123")
        .setNameSpace("system_user")
        .setAccountType("SystemAccount");
    Response csrResponse = AccountEntityServiceMethods.createAccountEntityRequest(csr);
    csrId = AccountEntityServiceMethods.getAccountId(csrResponse);
    return csrId;
  }

  @BeforeClass
  private void createPreConditionVariables() {

    customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, String.format("%s/%s", csr.getNameSpace(), csr.getUserName()),
            defaultPassword);
    requestDate = DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1);
    retrieveIotDeviceTest = new RetrieveIotDeviceTest();
  }

  @BeforeClass
  private void setDefaultUserCapability() {

    userCapabilityServiceMethods = new UserCapabilityServiceMethods();
    userCapability = new UserCapabilityManager().addUserCapability();
    userCapability.setUserId(csrId.toString());
    userCapability.setCapabilityTypeId(1);
  }

  @BeforeClass
  public void methodSetAccountIdForHierarchy() {
    //Getting accountId from DB and set to AccountHierarchyEntity
    retrieveIotDeviceTest.getAccountIdFromDB(MainGroup, AccountHierarchyEntityArray[0]);
    retrieveIotDeviceTest.getAccountIdFromDB(MainSubGroup1, AccountHierarchyEntityArray[1]);
    retrieveIotDeviceTest.getAccountIdFromDB(MainSubGroup2, AccountHierarchyEntityArray[2]);
    retrieveIotDeviceTest.getAccountIdFromDB(PartnerGroup, AccountHierarchyEntityArray[3]);
    retrieveIotDeviceTest.getAccountIdFromDB(PartnerSubGroup1, AccountHierarchyEntityArray[4]);
    retrieveIotDeviceTest.getAccountIdFromDB(PartnerSubGroup2, AccountHierarchyEntityArray[5]);
  }

  @Test(description = "Verify that user is able to retrieve any node with the parent account as responsible")
  public void retrieveSingleNodeWhenUserCanManageOnlyParentAccountPositiveTest() {

    //Add User capability
    userCapabilityServiceMethods.deleteUserCapabilityRequest(userCapability).then().statusCode(200);

    UserCapabilities.ManageAccounts manageParent = new UserCapabilities.ManageAccounts()
        .setAccessHierarchy(MainGroup.getAccountHierarchyId())
        .setAccessLevel("WRITE")
        .setAccessType("CURRENT_NODE");
    manageAccountsList = new ArrayList<>();
    manageAccountsList.add(manageParent);
    userCapability.setManageAccounts(manageAccountsList);
    userCapability.setUserId(csrId.toString());
    userCapabilityServiceMethods.addUserCapabilityRequest(userCapability).then().statusCode(200);

    //Create node
    Node node = createDefaultNode();

    //Create lwa
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(MainGroup.getAccountHierarchyId().toString())
        .setSettlementId(MainGroup.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);

    Assert.assertEquals(response.getStatusCode(), 200, "Response code is incorrect");

    //Compare got Node with created Node
    String lwaPath = String.format("find { it.externalId == '%s'}.", lwa.getExternalId());
    retrieveIotDeviceTest.verifyLwaFields(response, lwa, node, lwaPath);
  }

  @Test(description = "Verify that user is able to retrieve any node with the descendant account as responsible")
  public void retrieveSingleNodeWhenUserCanManageOnlyDescendantAccountPositiveTest() {

    //Add user capability
    userCapabilityServiceMethods.deleteUserCapabilityRequest(userCapability).then().statusCode(200);

    UserCapabilities.ManageAccounts manageDescendant = new UserCapabilities.ManageAccounts()
        .setAccessHierarchy(MainSubGroup1.getAccountHierarchyId())
        .setAccessLevel("WRITE")
        .setAccessType("CURRENT_NODE");
    manageAccountsList = new ArrayList<>();
    manageAccountsList.add(manageDescendant);
    userCapability.setManageAccounts(manageAccountsList);
    userCapability.setUserId(csrId.toString());
    userCapabilityServiceMethods.addUserCapabilityRequest(userCapability).then().statusCode(200);

    //Create node
    Node node = createDefaultNode();

    //Create lwa
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);

    Assert.assertEquals(response.getStatusCode(), 200, "Response code is incorrect");

    //Compare retrieved Node with created Node
    String lwaPath = String.format("find { it.externalId == '%s'}.", lwa.getExternalId());
    retrieveIotDeviceTest.verifyLwaFields(response, lwa, node, lwaPath);
  }

  @Test(description = "Verify that user is unable to retrieve any node when it can not manage either parent or "
      + "descendant account as responsible")
  public void retrieveSingleNodeWhenUserCanNotManageEitherParentOrDescendantsAccountNegativeTest() {

    //Create node
    Node node1 = createDefaultNode();
    Node node2 = createDefaultNode();

    //Create lwa
    LightWeightAccount lwa1 = createDefaultLightWeightAccount(node1)
        .setResponsibleId(MainGroup.getAccountHierarchyId().toString())
        .setSettlementId(MainGroup.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa1).then().statusCode(200);

    //Create lwa
    LightWeightAccount lwa2 = createDefaultLightWeightAccount(node2)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa2).then().statusCode(200);

    //Get Node information
    Response response1 = LightWeightAccountServiceMethods.retrieveIotDevice(lwa1, customToken);
    Response response2 = LightWeightAccountServiceMethods.retrieveIotDevice(lwa2, customToken);

    BaseAssertions.verifyErrorResponse(response1, 403,
        "Access denied");
    BaseAssertions.verifyErrorResponse(response2, 403,
        "Access denied");
  }

  @Test(description = "Verify that user is able to retrieve any node with both parent and descendant account as responsible")
  public void retrieveSingleNodeWhenUserCanManageBothParentAndDescendantsAccountPositiveTest() {

    //Add Capability
    userCapabilityServiceMethods.deleteUserCapabilityRequest(userCapability).then().statusCode(200);

    UserCapabilities.ManageAccounts manageParentAndDescendants = new UserCapabilities.ManageAccounts()
        .setAccessHierarchy(MainGroup.getAccountHierarchyId())
        .setAccessLevel("WRITE")
        .setAccessType("ALL_DESCENDANTS");
    manageAccountsList = new ArrayList<>();
    manageAccountsList.add(manageParentAndDescendants);
    userCapability.setManageAccounts(manageAccountsList);
    userCapability.setUserId(csrId.toString());
    userCapabilityServiceMethods.addUserCapabilityRequest(userCapability).then().statusCode(200);

    //Create node
    Node node1 = createDefaultNode();
    Node node2 = createDefaultNode();

    //Create lwa
    LightWeightAccount lwa1 = createDefaultLightWeightAccount(node1)
        .setResponsibleId(MainGroup.getAccountHierarchyId().toString())
        .setSettlementId(MainGroup.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    LightWeightAccountServiceMethods.newNodeRegistration(lwa1).then().statusCode(200);

    //Create lwa
    LightWeightAccount lwa2 = createDefaultLightWeightAccount(node2)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    LightWeightAccountServiceMethods.newNodeRegistration(lwa2).then().statusCode(200);

    //Get Node information
    Response response1 = LightWeightAccountServiceMethods.retrieveIotDevice(lwa1, customToken);
    Response response2 = LightWeightAccountServiceMethods.retrieveIotDevice(lwa2, customToken);

    //Verify API response
    Assert.assertEquals(response1.getStatusCode(), 200, "Response code is incorrect");
    Assert.assertEquals(response2.getStatusCode(), 200, "Response code is incorrect");

    //Compare retrieved Node with created Node
    String lwaPath1 = String.format("find { it.externalId == '%s'}.", lwa1.getExternalId());
    String lwaPath2 = String.format("find { it.externalId == '%s'}.", lwa2.getExternalId());
    retrieveIotDeviceTest.verifyLwaFields(response1, lwa1, node1, lwaPath1);
    retrieveIotDeviceTest.verifyLwaFields(response2, lwa2, node2, lwaPath2);
  }

  @Test(description = "Verify that a responsible user can retrieve any node registered to it")
  public void retrieveSingleNodeWhenUserIsTheResponsiblePositiveTest() {

    //Get token for created account
    String customTokenForResponsible =
        TokenAccessHelper.getCustomAuthToken(softWareId, "mt/" + MainGroup.getAccountHierarchyName(), defaultPassword);

    //Create node
    Node node = createDefaultNode();

    //Create lwa
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(MainGroup.getAccountHierarchyId().toString())
        .setSettlementId(MainGroup.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customTokenForResponsible);

    //Compare retrieved node with created node
    String lwaPath = String.format("find { it.externalId == '%s'}.", lwa.getExternalId());
    retrieveIotDeviceTest.verifyLwaFields(response, lwa, node, lwaPath);
  }

  @Test(description = "Verify that a Super user can retrieve any node")
  public void retrieveSingleNodeWhenUserIsASuperUserPositiveTest() {

    //Create node
    Node node = createDefaultNode();

    //Create lwa
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(MainGroup.getAccountHierarchyId().toString())
        .setSettlementId(MainGroup.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa);

    //Verify API response
    Assert.assertEquals(response.getStatusCode(), 200, "Response code is incorrect");

    //Compare got Node with created Node
    String lwaPath = String.format("find { it.externalId == '%s'}.", lwa.getExternalId());
    retrieveIotDeviceTest.verifyLwaFields(response, lwa, node, lwaPath);
  }

  @Test(description = "Verify that user is unable to retrieve all the nodes when it cannot manage either the parent or "
      + "the descendant account as responsible")
  public void retrieveAllNodesWhenUserCanNotManageEitherParentOrDescendantAccountNegativeTest() {

    //Create node
    Node node1 = createDefaultNode();
    Node node2 = createDefaultNode();
    Node node3 = createDefaultNode();

    //Create lwa
    LightWeightAccount lwa1 = createDefaultLightWeightAccount(node1)
        .setResponsibleId(MainGroup.getAccountHierarchyId().toString())
        .setSettlementId(MainGroup.getAccountHierarchyId().toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa1).then().statusCode(200);

    LightWeightAccount lwa2 = createDefaultLightWeightAccount(node2)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa2).then().statusCode(200);

    LightWeightAccount lwa3 = createDefaultLightWeightAccount(node3)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa3).then().statusCode(200);

    //Get Node information
    Response response1 =
        LightWeightAccountServiceMethods
            .bulkRetrieveIotDeviceWithoutParams(MainGroup.getAccountHierarchyId().toString(), requestDate,
                customToken);
    Response response2 =
        LightWeightAccountServiceMethods
            .bulkRetrieveIotDeviceWithoutParams(MainSubGroup1.getAccountHierarchyId().toString(), requestDate,
                customToken);

    //Verify API Response
    Assert.assertEquals(response1.getStatusCode(), 200, "Response code is incorrect");
    Assert.assertEquals(response2.getStatusCode(), 200, "Response code is incorrect");
    verifyAmountRecordsPerPage(response1, 0);
    verifyAmountRecordsPerPage(response2, 0);
  }

  @Test(description = "Verify that user is able to retrieve all the nodes with both the parent and the descendant "
      + "account as responsible")
  public void retrieveAllNodesWhenUserCanManageBothParentAndDescendantAccountPositiveTest() {

    //Add Capability
    userCapabilityServiceMethods.deleteUserCapabilityRequest(userCapability).then().statusCode(200);

    UserCapabilities.ManageAccounts manageParentAndDescendant = new UserCapabilities.ManageAccounts()
        .setAccessHierarchy(MainGroup.getAccountHierarchyId())
        .setAccessLevel("WRITE")
        .setAccessType("ALL_DESCENDANTS");
    manageAccountsList = new ArrayList<>();
    manageAccountsList.add(manageParentAndDescendant);
    userCapability.setManageAccounts(manageAccountsList);
    userCapability.setUserId(csrId.toString());
    userCapabilityServiceMethods.addUserCapabilityRequest(userCapability).then().statusCode(200);

    //Create node
    Node node1 = createDefaultNode();
    Node node2 = createDefaultNode();
    Node node3 = createDefaultNode();

    //Create lwa
    LightWeightAccount lwa1 = createDefaultLightWeightAccount(node1)
        .setResponsibleId(MainGroup.getAccountHierarchyId().toString())
        .setSettlementId(MainGroup.getAccountHierarchyId().toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa1).then().statusCode(200);

    LightWeightAccount lwa2 = createDefaultLightWeightAccount(node2)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa2).then().statusCode(200);

    LightWeightAccount lwa3 = createDefaultLightWeightAccount(node3)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa3).then().statusCode(200);

    //Get Node information
    Response response1 =
        LightWeightAccountServiceMethods
            .bulkRetrieveIotDeviceWithoutParams(MainGroup.getAccountHierarchyId().toString(), requestDate, customToken);
    Response response2 =
        LightWeightAccountServiceMethods
            .bulkRetrieveIotDeviceWithoutParams(MainSubGroup1.getAccountHierarchyId().toString(), requestDate,
                customToken);

    //Verify API response
    Assert.assertEquals(response1.getStatusCode(), 200, "Response code is incorrect");
    Assert.assertEquals(response2.getStatusCode(), 200, "Response code is incorrect");
  }

  @Test(description = "Verify that user is able to retrieve all the nodes with the descendant account as responsible")
  public void retrieveAllNodesWhenUserCanManageOnlyDescendantAccountPositiveTest() {

    //Add Capability
    userCapabilityServiceMethods.deleteUserCapabilityRequest(userCapability).then().statusCode(200);

    UserCapabilities.ManageAccounts manageDescendant = new UserCapabilities.ManageAccounts()
        .setAccessHierarchy(MainSubGroup1.getAccountHierarchyId())
        .setAccessLevel("WRITE")
        .setAccessType("CURRENT_NODE");
    manageAccountsList = new ArrayList<>();
    manageAccountsList.add(manageDescendant);
    userCapability.setManageAccounts(manageAccountsList);
    userCapability.setUserId(csrId.toString());
    userCapabilityServiceMethods.addUserCapabilityRequest(userCapability).then().statusCode(200);

    //Create node
    Node node1 = createDefaultNode();
    Node node2 = createDefaultNode();
    Node node3 = createDefaultNode();

    //Create lwa
    LightWeightAccount lwa1 = createDefaultLightWeightAccount(node1)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa1).then().statusCode(200);

    LightWeightAccount lwa2 = createDefaultLightWeightAccount(node2)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa2).then().statusCode(200);

    LightWeightAccount lwa3 = createDefaultLightWeightAccount(node3)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa3).then().statusCode(200);

    //Get Node information
    Response response =
        LightWeightAccountServiceMethods
            .bulkRetrieveIotDeviceWithoutParams(MainSubGroup1.getAccountHierarchyId().toString(), requestDate,
                customToken);

    //Verify API response
    Assert.assertEquals(response.getStatusCode(), 200, "Response code is incorrect");
  }

  @Test(description = "Verify that user is able to retrieve all the nodes with the parent account as responsible")
  public void retrieveAllNodesWhenUserCanManageOnlyParentAccountPositiveTest() {

    //Add Capability
    userCapabilityServiceMethods.deleteUserCapabilityRequest(userCapability).then().statusCode(200);

    UserCapabilities.ManageAccounts manageParent = new UserCapabilities.ManageAccounts()
        .setAccessHierarchy(MainGroup.getAccountHierarchyId())
        .setAccessLevel("WRITE")
        .setAccessType("CURRENT_NODE");
    manageAccountsList = new ArrayList<>();
    manageAccountsList.add(manageParent);
    userCapability.setManageAccounts(manageAccountsList);
    userCapability.setUserId(csrId.toString());
    userCapabilityServiceMethods.addUserCapabilityRequest(userCapability).then().statusCode(200);

    //Create node
    Node node1 = createDefaultNode();
    Node node2 = createDefaultNode();
    Node node3 = createDefaultNode();

    //Create lwa
    LightWeightAccount lwa1 = createDefaultLightWeightAccount(node1)
        .setResponsibleId(MainGroup.getAccountHierarchyId().toString())
        .setSettlementId(MainGroup.getAccountHierarchyId().toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa1).then().statusCode(200);

    LightWeightAccount lwa2 = createDefaultLightWeightAccount(node2)
        .setResponsibleId(MainGroup.getAccountHierarchyId().toString())
        .setSettlementId(MainGroup.getAccountHierarchyId().toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa2).then().statusCode(200);

    LightWeightAccount lwa3 = createDefaultLightWeightAccount(node3)
        .setResponsibleId(MainGroup.getAccountHierarchyId().toString())
        .setSettlementId(MainGroup.getAccountHierarchyId().toString());
    LightWeightAccountServiceMethods.newNodeRegistration(lwa3).then().statusCode(200);

    //Get Node information
    Response response = LightWeightAccountServiceMethods
        .bulkRetrieveIotDeviceWithoutParams(MainGroup.getAccountHierarchyId().toString(), requestDate,
            customToken);

    //Verify API response
    Assert.assertEquals(response.getStatusCode(), 200, "Response code is incorrect");
  }

  @Test(description = "Verify that a super user can retrieve all the nodes based on the search criteria")
  public void retrieveAllNodesWhenUserIsSuperUserPositiveTest() {

    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setAccountStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = AccountEntityServiceMethods.getAccountId(responsibleResponse);

    //Create node
    Node node1 = createDefaultNode();
    Node node2 = createDefaultNode();
    Node node3 = createDefaultNode();

    //Create lwa
    LightWeightAccount lwa1 = createDefaultLightWeightAccount(node1)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    LightWeightAccountServiceMethods.newNodeRegistration(lwa1).then().statusCode(200);

    //Create lwa
    LightWeightAccount lwa2 = createDefaultLightWeightAccount(node2)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    LightWeightAccountServiceMethods.newNodeRegistration(lwa2).then().statusCode(200);

    //Create lwa
    LightWeightAccount lwa3 = createDefaultLightWeightAccount(node3)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    LightWeightAccountServiceMethods.newNodeRegistration(lwa3).then().statusCode(200);

    //Get Node information
    Response response = LightWeightAccountServiceMethods
        .bulkRetrieveIotDeviceWithoutParams(responsibleId.toString(), requestDate);

    //Verify API response
    Assert.assertEquals(response.getStatusCode(), 200, "Response code is incorrect");
    verifyAmountRecordsPerPage(response, 3);
  }

  @Test(description = "Verify that a responsible can retrieve all the nodes registered to it")
  public void retrieveAllNodesWhenUserIsTheResponsiblePositiveTest() {

    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setAccountStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = AccountEntityServiceMethods.getAccountId(responsibleResponse);

    //Get token for created account
    String customTokenForResponsible = TokenAccessHelper
        .getCustomAuthToken(softWareId, String.format("%s/%s", responsible.getNameSpace(), responsible.getUserName()),
            defaultPassword);

    //Create node
    Node node1 = createDefaultNode();
    Node node2 = createDefaultNode();
    Node node3 = createDefaultNode();

    //Create lwa
    LightWeightAccount lwa1 = createDefaultLightWeightAccount(node1)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    LightWeightAccountServiceMethods.newNodeRegistration(lwa1).then().statusCode(200);

    //Create lwa
    LightWeightAccount lwa2 = createDefaultLightWeightAccount(node2)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    LightWeightAccountServiceMethods.newNodeRegistration(lwa2).then().statusCode(200);

    //Create lwa
    LightWeightAccount lwa3 = createDefaultLightWeightAccount(node3)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    LightWeightAccountServiceMethods.newNodeRegistration(lwa3).then().statusCode(200);

    //Get Node information
    Response response =
        LightWeightAccountServiceMethods
            .bulkRetrieveIotDeviceWithoutParams(responsibleId.toString(), requestDate, customTokenForResponsible);

    //Verify API response
    Assert.assertEquals(response.getStatusCode(), 200, "Response code is incorrect");
    verifyAmountRecordsPerPage(response, 3);
  }

  private void verifyAmountRecordsPerPage(Response response, int expectedAmount) {

    String result = response.getBody().jsonPath().getString("records.size()");
    int actualAmount = parseInt(result);
    Assert.assertEquals(actualAmount, expectedAmount,
        "The expected and actual number of records doesn't match");
  }
}