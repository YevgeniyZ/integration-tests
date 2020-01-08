package bluesky.restapi.lightWeightAccount;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.base.BaseAssertions;
import bluesky.restapi.helpers.DBHelper;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.helpers.TokenAccessHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.managers.AccountHierarchyEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.NodeStatus;
import bluesky.restapi.models.OwnershipModes;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.HashMap;

import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;

@Story("BLUESKY-37 LW Account Backend - Retrieve IOT Device")
public class RetrieveIotDeviceTest extends BaseApiTest {
  //before run tests, hierarchy account will need add to DB. Please refer to https://jira.metratech.com/browse/QA-297 for more details

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

  @BeforeClass
  void methodSetAccountIdForHierarchy() {
    //Getting accountId from DB and set to AccountHierarchyEntity
    getAccountIdFromDB(MainGroup, AccountHierarchyEntityArray[0]);
    getAccountIdFromDB(MainSubGroup1, AccountHierarchyEntityArray[1]);
    getAccountIdFromDB(MainSubGroup2, AccountHierarchyEntityArray[2]);
    getAccountIdFromDB(PartnerGroup, AccountHierarchyEntityArray[3]);
    getAccountIdFromDB(PartnerSubGroup1, AccountHierarchyEntityArray[4]);
    getAccountIdFromDB(PartnerSubGroup2, AccountHierarchyEntityArray[5]);
  }

  @Test(description =
      "Verify that only NodretrieveIOTDeviceWithAccountHierarchyForTokensChildAccountWhenNodeHasSeveralResponsiblesPositiveTeste information is returned for the time period the User making the request "
          + "has access", groups = "smoke")
  public void retrieveIOTDeviceWithAccountHierarchyForTokenChildAccountPositiveTest() {
    //Get token for first child account
    String customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, "mt/" + MainSubGroup1.getAccountHierarchyName(), defaultPassword);

    //Create New Node when Node's Responsible is first child account and token's first child
    LightWeightAccount.Node node = createDefaultNode();
    node.setNodeType("CAMERA");
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setOwnershipMode(OwnershipModes.OWNER)
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa, customToken).then().statusCode(200);

    //Update Node's status to Suspended with token's first child account
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus, customToken).then()
        .statusCode(200);

    //Update Node's Responsible to second child account with token's first child account
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(MainSubGroup2.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated, customToken).then().statusCode(200);

    //Get Node information with token's first child account
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);

    //check that Node's response contains all Node's changed for first child account (only 2 changes)
    for (int i = 0; i < 2; i++) {
      String lwaPath = String.format("find { it.startDate == '%s'}.",
          DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, i));
      verifyGetLwaFields(response, lwaPath, getExpectedLwaFieldsWithoutNodeStatus(lwa));
    }
  }

  @Test(description =
      "Verify that only Node information is returned for the time period the User making the request has access when a "
          + "node has several responsibles (send request when token's child account)", groups = "smoke")
  public void retrieveIOTDeviceWithAccountHierarchyForTokensChildAccountWhenNodeHasSeveralResponsiblesPositiveTest() {

    //Get token for first child account
    String customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, "mt/" + MainSubGroup1.getAccountHierarchyName(), defaultPassword);

    // Create New Node when Node's Responsible is first child account and token's first child account
    LightWeightAccount.Node node = createDefaultNode();
    node.setNodeType("CAMERA");
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setOwnershipMode(OwnershipModes.OWNER)
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa, customToken).then().statusCode(200);

    //Update Node's status to Suspended with token's first child account
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus, customToken).then()
        .statusCode(200);

    //Update Node's Responsible to second child account with token's first child account
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(MainSubGroup2.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated, customToken).then().statusCode(200);

    //Get token for second child account
    customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, "mt/" + MainSubGroup2.getAccountHierarchyName(), defaultPassword);

    //Update Node's status to Active with token's second child account
    LightWeightAccount lwaUpdatedStatusWithTokenMainSubGroup2 = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedStatusWithTokenMainSubGroup2, customToken).then()
        .statusCode(200);

    //Get Node information with token's second child account
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);

    //check that Node's response contains all Node's changed for second child account (2 changes)
    for (int i = 0; i < 2; i++) {
      lwa.setResponsibleId(MainSubGroup2.getAccountHierarchyId().toString());
      String lwaPath = String.format("find { it.startDate == '%s'}.",
          DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, i + 2));
      verifyGetLwaFields(response, lwaPath, getExpectedLwaFieldsWithoutNodeStatus(lwa));
    }
  }

  @Test(description =
      "Verify that only Node information is returned for the time period the User making the request has access when "
          + "a node has several responsibles from Partner group (send request when token's Main group child account)",
      groups = "smoke")
  public void retrieveIOTDeviceWithAccountHierarchyWhenTokensMainChildAccountAndNodeHasResponsiblesFromPartnerPositiveTest() {

    //Get token for child account of Partner
    String customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, "mt/" + PartnerSubGroup1.getAccountHierarchyName(), defaultPassword);

    // Create New Node when Node's Responsible is child account and token's child account of Partner
    LightWeightAccount.Node node = createDefaultNode();
    node.setNodeType("CAMERA");
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(PartnerSubGroup1.getAccountHierarchyId().toString())
        .setOwnershipMode(OwnershipModes.OWNER)
        .setSettlementId(PartnerSubGroup1.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa, customToken).then().statusCode(200);

    //Update Node's status to Suspended with token's child account of Partner
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus, customToken).then()
        .statusCode(200);

    //Update Node's Responsible to Main child account with token's child account of Partner
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated, customToken).then().statusCode(200);

    //Get token for child account of Main group
    customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, "mt/" + MainSubGroup1.getAccountHierarchyName(), defaultPassword);

    //Update Node's status to Active with token's child account of Main group
    LightWeightAccount lwaUpdatedStatusWithTokenMainSubGroup2 = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedStatusWithTokenMainSubGroup2, customToken).then()
        .statusCode(200);

    //Get Node information with token's child account of Main group
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);

    //check that Node's response contains only Node's changed for child account of Main group (2 changes)
    for (int i = 0; i < 2; i++) {
      lwa.setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString());
      String lwaPath = String.format("find { it.startDate == '%s'}.",
          DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, i + 2));
      verifyGetLwaFields(response, lwaPath, getExpectedLwaFieldsWithoutNodeStatus(lwa));
    }

    //Get token for child account of Partner
    customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, "mt/" + PartnerSubGroup1.getAccountHierarchyName(), defaultPassword);

    //Get Node information with token's child account of Partner
    response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);

    //check that Node's response contains only Node's changed for child account of Partner (2 changes)
    for (int i = 0; i < 2; i++) {
      lwa.setResponsibleId(PartnerSubGroup1.getAccountHierarchyId().toString());
      String lwaPath = String.format("find { it.startDate == '%s'}.",
          DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, i));
      verifyGetLwaFields(response, lwaPath, getExpectedLwaFieldsWithoutNodeStatus(lwa));
    }
  }

  @Test(description = "Verify that all Node information is returned when used token's parent account", groups = "smoke")
  public void retrieveIOTDeviceWithAccountHierarchyWhenTokensParentAccountPositiveTest() {
    //Get token for first child account
    String customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, "mt/" + MainSubGroup1.getAccountHierarchyName(), defaultPassword);

    //Create New Node when Node's Responsible is first child account and token's first child
    LightWeightAccount.Node node = createDefaultNode();
    node.setNodeType("CAMERA");
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString())
        .setOwnershipMode(OwnershipModes.OWNER)
        .setSettlementId(MainSubGroup1.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa, customToken).then().statusCode(200);

    //Update Node's status to Suspended with token's first child account
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus, customToken).then()
        .statusCode(200);

    //Update Node's Responsible to second child account with token's first child account
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(MainSubGroup2.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated, customToken).then().statusCode(200);

    //Get token for second child account
    customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, "mt/" + MainSubGroup2.getAccountHierarchyName(), defaultPassword);

    //Update Node's status to Active with token's second child account
    LightWeightAccount lwaUpdatedStatusWithTokenMainSubGroup2 = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedStatusWithTokenMainSubGroup2, customToken).then()
        .statusCode(200);

    //Get token for parent account
    customToken =
        TokenAccessHelper.getCustomAuthToken(softWareId, "mt/" + MainGroup.getAccountHierarchyName(), defaultPassword);

    //Get Node information with token's parent account
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);

    //check that Node's response contains all Node's changed (4 changes)
    for (int i = 0; i < 3; i++) {
      String lwaPath = String.format("find { it.startDate == '%s'}.",
          DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, i));
      if (i < 2)
        lwa.setResponsibleId(MainSubGroup1.getAccountHierarchyId().toString());
      else
        lwa.setResponsibleId(MainSubGroup2.getAccountHierarchyId().toString());
      verifyGetLwaFields(response, lwaPath, getExpectedLwaFieldsWithoutNodeStatus(lwa));
    }
  }

  @Test(description = "Verify that the API returns all Node information for the matching requested External Node "
      + "ID/Node_Type combination when nodeType is CAMERA and OwnershipMode is OwnershipModes.OWNER", groups = "smoke")
  public void retrieveIOTDeviceWhenNodeTypeCameraPositiveTest() {

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Get token for created account
    String customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, String.format("%s/%s", responsible.getNameSpace(), responsible.getUserName()),
            defaultPassword);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();
    node.setNodeType("CAMERA");
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOwnershipMode(OwnershipModes.OWNER)
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa, customToken).then().statusCode(200);

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);

    //Assert created Node compare got Node
    String lwaPath = String.format("find { it.externalId == '%s'}.", lwa.getExternalId());
    verifyLwaFields(response, lwa, node, lwaPath);
  }

  @Test(description =
      "Verify that the API returns all Node information for the matching requested External Node ID/Node_Type  "
          + "combination when nodeType is AIRSENSOR and OwnershipMode is RENT", groups = "smoke")
  public void retrieveIOTDeviceWhenNodeTypeAirSensorPositiveTest() {

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Get token for created account
    String customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, String.format("%s/%s", responsible.getNameSpace(), responsible.getUserName()),
            defaultPassword);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode()
        .setAttributes(new HashMap<String, Object>() {{
          put("msisdn", "value11");
          put("oem", "ABC-12345");
          put("location", "US-123");
          put("model", "ABC-123456");
        }})
        .setNodeType("AIRSENSOR");
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOwnershipMode(OwnershipModes.RENT)
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa, customToken).then().statusCode(200);

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);

    //Compare got Node with created Node
    String lwaPath = String.format("find { it.externalId == '%s'}.", lwa.getExternalId());
    verifyLwaFields(response, lwa, node, lwaPath);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-396")
  @Test(enabled = false, description = "Verify that the API returns all Node information for the matching requested "
      + "External Node ID/Node_Type when token's from system user name space", groups = "smoke")
  public void retrieveIOTDeviceWithTokensSystemUserPositiveTest() {

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode()
        .setAttributes(new HashMap<String, Object>() {{
          put("msisdn", "value11");
          put("oem", "ABC-12345");
          put("location", "US-123");
          put("model", "ABC-123456");
        }})
        .setNodeType("AIRSENSOR");
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOwnershipMode(OwnershipModes.RENT)
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa);

    //Compare got Node with created Node
    String lwaPath = String.format("find { it.externalId == '%s'}.", lwa.getExternalId());
    verifyLwaFields(response, lwa, node, lwaPath);
  }

  @Test(enabled = false, description = "Verify that the API returns all Node information for the matching requested "
      + "External Node ID/Node_Type when NodeType has several version", groups = "smoke")
  //for future release
  public void retrieveIOTDeviceWithTokensSystemUserAndNodeTypeHasSeveralVersionPositiveTest() throws SQLException {

    //Getting nodeType Camera version from DB
    String newAttributesQuery = "select * " +
        "from t_lwa_node_type " +
        "where version = '1.0'";

    String nodeType = null;

    try {
      nodeType = DBHelper.getValueFromDB(newAttributesQuery, "node_type");
    } catch (SQLException e) {
      e.printStackTrace();
    }

    //verify that nodeType Camera version 3.5 doesn't exist
    assert nodeType != null;
    if (!nodeType.equals("Camera")) {
      //Add NodeType: Camera with version 3.5 in DB
      String query = "insert into t_lwa_node_type (node_type, guiding_keys, json_schema, version) values ('Camera', "
          + "'msisdn,imsi', '{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"description\":\"Camera type "
          + "node single\",\"type\":\"object\",\"required\":[\"msisdn\"],\"properties\":{\"msisdn\":"
          + "{\"type\":\"string\"},\"imsi\":{\"type\":\"string\"},\"oem\":{\"type\":\"string\"},\"make\":"
          + "{\"type\":\"string\"},\"model\":{\"type\":\"string\"},\"location\":{\"type\":\"string\"}},"
          + "\"additionalProperties\":false}', '3.5')";
      DBHelper.runSQLQuery(query);
    }

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOwnershipMode(OwnershipModes.RENT)
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa);

    //Assert created Node compare got Node
    verifyLwaFields(response, lwa, node, "externalId");
  }

  @Test(description = "Verify that after updating the node, the returned information about the node contains changes",
      groups = "smoke")
  public void retrieveIOTDeviceWhenNodeUpdateStatusPositiveTest() {

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Get token for created account
    String customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, String.format("%s/%s", responsible.getNameSpace(), responsible.getUserName()),
            defaultPassword);

    //LWA registration with Method Of Sale is OwnershipModes.RENT
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setOwnershipMode(OwnershipModes.RENT)
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa, customToken).then().statusCode(200);

    //Update Node's status to Suspended
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus, customToken).then()
        .statusCode(200);

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);

    //compare got Node with created Node
    verifyLwaFields(response, lwa, node, "find { it.nodeStatus == 'ACTIVE'}.");
    lwa.setNodeStatus(lwaUpdatedwithTerminatedStatus.getNodeStatus());
    lwa.setStartDate(lwaUpdatedwithTerminatedStatus.getStartDate());
    verifyLwaFields(response, lwa, node, "find { it.nodeStatus == 'SUSPENDED'}.");
  }

  @Test(description = "Verify that after several updating (8 changes) the node, the returned information about the "
      + "node contains changes", groups = "smoke")
  public void retrieveIOTDeviceWhenNodeHasSeveralUpdatePositiveTest() {
    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Get token for created account
    String customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, String.format("%s/%s", responsible.getNameSpace(), responsible.getUserName()),
            defaultPassword);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa, customToken).then().statusCode(200);

    //Update Node's Status to Suspended
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus, customToken).then()
        .statusCode(200);

    //Update Node's Attributes 5 times
    for (int i = 1; i < 6; i++) {
      String msisdn = "updatedValueMSISDN" + i;
      String imsi = "updatedValueIMSI" + i;

      LightWeightAccount lwaUpdated = new LightWeightAccount()
          .setExternalId(lwa.getExternalId())
          .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, i + 1))
          .setNode(new LightWeightAccount.Node()
              .setNodeType(node.getNodeType())
              .setAttributes(new HashMap<String, Object>() {{
                put("msisdn", msisdn);
                put("imsi", imsi);
              }}));

      LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated, customToken);
    }

    //Update Node's Status to Suspended
    lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 7))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.ACTIVE);
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus, customToken).then()
        .statusCode(200);

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);

    //compare Node's response with all Node's changed
    String lwaPath;
    for (int i = 0; i < 7; i++) {
      lwaPath = String.format("find { it.startDate == '%s'}.",
          DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, i));
      verifyGetLwaFields(response, lwaPath, getExpectedLwaFieldsWithoutNodeStatus(lwa));
    }
  }

  @Test(description = "Verify that the node information is not provided to the user who does not have access to this "
      + "period (NegativeTest)", groups = "smoke")
  public void retrieveIOTDeviceWithAccountHierarchyWhenTokenIsNotAccessToNodesInformationNegativeTest() {

    //Get token for child account of Partner
    String customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, "mt/" + PartnerSubGroup1.getAccountHierarchyName(), defaultPassword);

    // Create New Node when Node's Responsible is child account of Partner and token's child account of Partner
    LightWeightAccount.Node node = createDefaultNode();
    node.setNodeType("CAMERA");
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(PartnerSubGroup1.getAccountHierarchyId().toString())
        .setOwnershipMode(OwnershipModes.OWNER)
        .setSettlementId(PartnerSubGroup1.getAccountHierarchyId().toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa, customToken).then().statusCode(200);

    //Get token for child account of Main group
    customToken = TokenAccessHelper
        .getCustomAuthToken(softWareId, "mt/" + MainSubGroup1.getAccountHierarchyName(), defaultPassword);

    //Get Node information with token's child account of Main group
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa, customToken);

    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: No accessible nodes exist");
  }

  @Test(description = "Verify that when External Node ID doesn't exists in DB then API return validation message "
      + "(NegativeTest)", groups = "smoke")
  public void retrieveIOTDeviceWhenNodeIncorrectExternalIdNegativeTest() {

    //LWA creating
    LightWeightAccount.Node node = createDefaultNode();
    LightWeightAccount lwa = createDefaultLightWeightAccount(node);
    lwa.setExternalId("-0-");

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa);

    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: No nodes exists for external id: -0- node type: Camera");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-362")
  @Test(enabled = false, description = "Verify that API response contains validation message if Node's ExternalId is "
      + "space character (NegativeTest)", groups = "smoke")
  public void retrieveIOTDeviceWhenNodesExternalIdIsSpaceCharacterNegativeTest() {

    //LWA creating
    LightWeightAccount.Node node = createDefaultNode();
    LightWeightAccount lwa = createDefaultLightWeightAccount(node);
    lwa.setExternalId(" ");

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa);

    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: No nodes exists for external id:  node type: Camera");
  }

  @Test(description = "Verify that when Node Type doesn't exists in DB then API return validation message (NegativeTest)", groups = "smoke")
  public void retrieveIOTDeviceWhenNodeIncorrectNodeTypeNegativeTest() {

    //LWA creating
    LightWeightAccount.Node node = createDefaultNode();
    node.setNodeType("Inccorect@1");
    LightWeightAccount lwa = createDefaultLightWeightAccount(node);

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa);

    BaseAssertions.verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid Node type");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-362")
  @Test(enabled = false, description = "Verify that API response contains validation message if value NodeType is space character (NegativeTest)", groups = "smoke")
  public void retrieveIOTDeviceWhenNodeNodeTypeIsSpaceCharacterNegativeTest() {

    //LWA creating
    LightWeightAccount.Node node = createDefaultNode();
    node.setNodeType(" ");
    LightWeightAccount lwa = createDefaultLightWeightAccount(node);

    //Get Node information
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa);

    BaseAssertions.verifyErrorResponse(response, 404, "Not Found");
  }

  @Test(description = "Verify that when Node Type in request doesn't equal Node's Type in DB for the specified External ID then API return validation message (NegativeTest)", groups = "smoke")
  public void retrieveIOTDeviceWithNodeTypeIsNotEqualNodesExternalIdNegativeTest() {

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();
    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Get Node information
    node.setNodeType("AIRSENSOR");
    Response response = LightWeightAccountServiceMethods.retrieveIotDevice(lwa);

    BaseAssertions.verifyErrorResponse(response, 428,
        String.format(
            "Failed to validate the request. Reason: No nodes exists for external id: %s node type: AirSensor",
            lwa.getExternalId()));
  }

  void verifyLwaFields(Response response, LightWeightAccount lwa,
                       LightWeightAccount.Node node, String lwaPath) {

    verifyGetLwaFields(response, lwaPath, getExpectedLwaFields(lwa));

    String nodePath = lwaPath + "node.";
    verifyGetLwaFields(response, nodePath, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    String nodeAttributesPath = nodePath + "attributes.";
    verifyGetNodeAttributes(response, nodeAttributesPath, getExpectedNodeFields(node));
  }

  private static HashMap<String, Object> getExpectedLwaFields(LightWeightAccount lwa) {

    return new HashMap<String, Object>() {{
      put("responsibleId", lwa.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("startDate", lwa.getStartDate());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
    }};
  }

  private static HashMap<String, Object> getExpectedLwaFieldsWithoutNodeStatus(LightWeightAccount lwa) {

    return new HashMap<String, Object>() {{
      put("responsibleId", lwa.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("ownershipMode", lwa.getOwnershipMode());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
    }};
  }

  private static HashMap<String, Object> getExpectedNodeFields(LightWeightAccount.Node node) {

    return new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }};
  }

  void getAccountIdFromDB(AccountHierarchyEntityManager currentAccountHierarchyEntity,
                          String currentAccountName) {

    String accountIdQuery = "select * from t_account_mapper " +
        "where nm_login = '" + currentAccountName + "'";

    int currentId = 0;
    try {
      String strCurrentId = DBHelper.getValueFromDB(accountIdQuery, "id_acc");
      currentId = Integer.parseInt(strCurrentId);
    } catch (SQLException e) {
      e.printStackTrace();
    }

    //Set Id to account hierarchy
    currentAccountHierarchyEntity.setAccountHierarchyName(currentAccountName);
    currentAccountHierarchyEntity.setAccountHierarchyId(currentId);
  }

  @Step("Verify node attributes {1} are correct ")
  private void verifyGetLwaFields(Response response, String request, HashMap<String, Object> nodeFields) {

    nodeFields.forEach((key, value) -> Assert
        .assertEquals(response.getBody().jsonPath().getString(request + key), value,
            "Field " + key + " is incorrect or not found"));
  }

  @Step("Verify lwa fields {1} are correct")
  private void verifyGetNodeAttributes(Response response, String request, HashMap<String, Object> lwaFields) {

    lwaFields.forEach((key, value) -> Assert
        .assertEquals(response.getBody().jsonPath().getString(request + key), value,
            "Field " + key + " is incorrect or not found"));
  }
}
