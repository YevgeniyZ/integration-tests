package bluesky.restapi.lightWeightAccount;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.helpers.DBHelper;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.NodeStatus;
import bluesky.restapi.models.NodeTypes;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.HashMap;

import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;

@Story("BLUESKY-35 LW Account Backend - Add a New IOT Device (New Node)")
public class RegisterNewIotDeviceTest extends BaseApiTest {

  private String responsibleId;

  @BeforeClass
  private void createResponsible() {

    //Add new account
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    this.responsibleId = String.valueOf(getAccountId(responsibleResponse));
  }

  @Test(description = "Verify that New Node is registered successfully through API with required fields and verify "
      + "creation of an object in the DB", groups = "smoke")
  public void verifyNodeCreatedWithRequiredFieldsPositiveTest() throws SQLException {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId);

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Getting attributes from DB
    String NodeCheckQuery = String.format(SELECT_NODE_QUERY, lwa.getExternalId());

    String NodeFromDB = DBHelper.getValueFromDB(NodeCheckQuery, "external_id");

    //Assert json attributes is updated.
    Assert.assertEquals(NodeFromDB, lwa.getExternalId());
  }

  @Test(description = "Verify New Node is registered successfully using the same accountID for both Responsible and "
      + "settlement", groups = "smoke")
  public void verifyNodeCreatedUsingResponsibleIdInSettlementPositiveTest() {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId)
        .setSettlementId(responsibleId);

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);
  }

  @Test(description = "Verify New Node is registered successfully using nm_space/nm_login for Responsible and "
      + "settlement", groups = "smoke")
  public void verifyNodeCreatedUsingNmspaceForSettlementPositivetest() {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId("mt/hanzel")
        .setSettlementId("mt/hanzel");

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);
  }

  @Test(description = "Verify New Node is registered successfully when Node's startDate is equal to Payer's "
      + "startDate", groups = "smoke")
  public void verifyNodeRegisteredStartDateEqualPayerStartDatePositiveTest() {

    //Add new account
    AccountEntity responsibleCurrent = new AccountEntityManager().createAccountEntity()
        .setVtStart((DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START)));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsibleCurrent);
    Integer responsibleId3 = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId3.toString())
        .setStartDate(DateTimeHelper.getCurrentDate((DateTimeHelper.DATE_PATTERN_DEFAULT_START)));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);
  }

  @Test(description = "Verify New Node  is registered successfully when nodeType's is AirSensor and Payer's status is"
      + " Suspended.", groups = "smoke")
  public void verifyNodeAirSensorWithSuspendedPayer() {

    //Add new account
    AccountEntity responsibleCurrent = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsibleCurrent);
    Integer responsibleIdSuspended = getAccountId(responsibleResponse);

    //Update account to Suspended
    AccountEntityManager.updateAccountStatusToSuspended(responsibleIdSuspended);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode()
        .setNodeType(NodeTypes.NODE_TYPE_AIRSENSOR)
        .setAttributes(airSensorAttributes);

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleIdSuspended.toString())
        .setStartDate(DateTimeHelper.getCurrentDate((DateTimeHelper.DATE_PATTERN_DEFAULT_START)));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);
  }

  @Test(description = "Verify New Node is registered successfully when nodeStatus's is SUSPENDED", groups = "smoke")
  public void verifyNodeRegisteredWhenStatusSuspendedPositiveTest() throws SQLException {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId)
        .setNodeStatus(NodeStatus.SUSPENDED);

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Getting attributes from DB
    String NodeCheckQuery = String.format(SELECT_NODE_QUERY, lwa.getExternalId());

    String NodeFromDB = DBHelper.getValueFromDB(NodeCheckQuery, "status");

    //Assert json attributes is updated.
    Assert.assertEquals(NodeFromDB, "Suspended");
  }

  @Test(description = "Verify the API request for Node Creation is rejected if StartDate Null", groups = "smoke")
  public void verifyRequestRejectedWhenMandatoryFieldStartDateMissingNegativeTest() {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId)
        .setStartDate(null);

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(400);
  }

  @Test(description = "Verify the API request for Node Creation is rejected if Responsible Null", groups = "smoke")
  public void verifyRequestRejectedWhenMandatoryFieldResponsibleMissingNegativeTest() {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(null);

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(400);
  }

  @Test(description = "Verify the API request for Node Creation is rejected if NodeType Null (negative Test)", groups =
      "smoke")
  public void verifyRequestRejectedWhenMandatoryFieldNodeTypeMissingNegativeTest() {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode()
        .setNodeType(null);

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId);

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(400);
  }

  @Test(description = "Verify request rejected when Responsible Status is Closed (Negative Test)", groups = "smoke")
  public void requestRejectedWhenResponsibleClosedNegativeTest() {

    //Add new account
    AccountEntity responsibleCurrent = new AccountEntityManager().createAccountEntity()
        .setVtStart((DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START)));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsibleCurrent);
    Integer responsibleIdClosed = getAccountId(responsibleResponse);

    //Update account to Closed
    AccountEntityManager.updateAccountStatusToClosed(responsibleIdClosed);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleIdClosed.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(428);
  }

  @Test(description = "Verify request rejected when Responsible Status is Archived (Negative Test)", groups = "smoke")
  public void requestRejectedWhenResponsibleArchivedNegativeTest() {

    //Add new account
    AccountEntity responsibleCurrent = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsibleCurrent);
    Integer responsibleIdArchived = getAccountId(responsibleResponse);

    //Update account to Archived
    AccountEntityManager.updateAccountStatusToArchived(responsibleIdArchived);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleIdArchived.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(428);
  }

  @Test(description = "Verify the API request for \"Node Creation\" is rejected if externalId is duplicate(negative "
      + "test", groups = "smoke")
  public void verifyNodeCreationIfExternalIdExistsNegativeTest() {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId);

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    LightWeightAccount.Node node2 = createDefaultNode();

    LightWeightAccount lwa2 = createDefaultLightWeightAccount(node2)
        .setResponsibleId(responsibleId)
        .setExternalId(lwa.getExternalId());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa2).then().statusCode(428);
  }

  @Test(description = "Verify the API request for Node Creation is rejected if node's attributes contains imsi when "
      + "nodeType's: AirSensor (Negative Test)", groups = "smoke")
  public void verifyRequestRejectedNodeAirSensorCreatedWithImsiNegativeTest() {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode()
        .setNodeType(NodeTypes.NODE_TYPE_AIRSENSOR)
        .setAttributes(new HashMap<String, Object>() {{
          put("msisdn", "updatedValueMSISDN");
          put("imsi", "updatedIMSI");
        }});

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId);

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(428);
  }

  @Test(description = "Verify the API request for Node Creation is rejected if node's attributes contains ipAddress "
      + "(Negative Test)", groups = "smoke")
  public void verifyRequestRejectedNodeCreatedWithIpAddressNegativeTest() {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode()
        .setAttributes(new HashMap<String, Object>() {{
          put("msisdn", "updatedValueMSISDN");
          put("ipaddress", "123.123.123.123");
    }});

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId);

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(428);
  }

  @Test(description = "Verify the API request for Node Creation is rejected if  nodeStatus's contains invalid "
      + "value ('abc123@/') (Negative Test)", groups = "smoke")
  public void verifyRequestRejectedNodeCreatedWithInvalidStatusNegativeTest() {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId)
        .setNodeStatus("abc123@/");

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(400);
  }

  @Test(description = "Checking that request rejected when start date in invalid format (negative test)", groups =
      "smoke")
  public void verifyRequestRejectedWhenStartDateInvalidNegativeTest() {

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId)
        .setStartDate("2019-13-32T13:12:36.36Z");

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(400);
  }

  @Test(description = "Verify Request Rejected when externalId value equal 50char (Max Length) (negative test)",
      groups = "smoke")
  public void verifyRequestExternalIdMaxLenghtPositiveTest() {

    String maximumExternalId = RandomStringUtils.randomAlphabetic(50);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId)
        .setExternalId(maximumExternalId);

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);
  }

  @Test(description = "Verify Request Rejected when externalId value more then 50char (negative test)", groups = "smoke")
  public void verifyRequestRejectedExternalIdLengthNegativeTest() {

    String longExternalId = RandomStringUtils.randomAlphabetic(51);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId)
        .setExternalId(longExternalId);

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(400);
  }

  @Test(description = "Verify that new node type can be created on the fly (to avoid issues node will be deleted "
      + "after this test (positive test)", groups = "smoke")
  public void verifyNewNodeTypeCreatedPositiveTest() throws SQLException{

    createNewNodeType();

    try{

      //LWA registration
      LightWeightAccount.Node node = createDefaultNode()
          .setNodeType("TESTTYPE")
          .setAttributes(testNodeTypeAttributes);

      LightWeightAccount lwa = createDefaultLightWeightAccount(node)
          .setResponsibleId(responsibleId);

      LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

      //Getting attributes from DB
      String NodeCheckQuery = String.format(SELECT_NODE_QUERY, lwa.getExternalId());

      String NodeFromDB = DBHelper.getValueFromDB(NodeCheckQuery, "status");

      //Assert json attributes is updated.
      Assert.assertEquals(NodeFromDB, "Active" );

      //Deleting node to avoid issues with new node type
      DBHelper.runSQLQuery(String.format("delete from t_lwa_node where external_id = '%s'", lwa.getExternalId()));

    }finally {
      //delete node type after test
      deleteNodeType();
    }
  }

  private HashMap<String, Object> airSensorAttributes = new HashMap<String, Object>() {{
    put("msisdn", "msisdnOld");
    put("oem", "11");
    put("location", "12356");
    put("model", "1234");
  }};

  private HashMap<String, Object> testNodeTypeAttributes = new HashMap<String, Object>() {{
    put("msisdn", "testtypemsisdn");
    put("imsi", "testtypeimsi");
    put("oem", "11");
    put("model", "1234");
  }};

  private static void createNewNodeType() throws SQLException {

    String newNodeTypeQuery = "IF NOT EXISTS (SELECT 1\n"
        + "  FROM [dbo].[t_lwa_node_type]\n"
        + "  WHERE node_type = 'TestType')\n"
        + "BEGIN\n"
        + "  INSERT INTO [dbo].[t_lwa_node_type]\n"
        + "        ([node_type]\n"
        + "        ,[guiding_keys]\n"
        + "        ,[json_schema]\n"
        + "        ,[version]\n"
        + "        )\n"
        + "  VALUES\n"
        + "        ('TestType'\n"
        + "        ,'msisdn,imsi'\n"
        + "        ,'{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"description\":\"TestType type node "
        + "single\",\"type\":\"object\",\"required\":[\"msisdn\",\"imsi\",\"oem\",\"model\"],\"properties\":{\"msisdn\""
        + ":{\"type\":\"string\",\"pattern\":\"^(?!\\\\s).+$\",\"maxLength\":50},\"imsi\":{\"type\":\"string\","
        + "\"maxLength\":50},\"oem\":{\"type\":\"string\",\"maxLength\":50},\"model\":{\"type\":\"string\",\"maxLength\""
        + ":50},\"location\":{\"type\":\"string\",\"maxLength\":50},\"oemPartNr\":{\"type\":\"string\",\"maxLength\":50}},"
        + "\"additionalProperties\":false}'\n"
        + "        ,'1.0'\n"
        + "        )\n"
        + "END\n"
        + "ELSE\n"
        + "BEGIN\n"
        + "  UPDATE [dbo].[t_lwa_node_type]\n"
        + "  SET json_schema = '{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"description\":\"TestType "
        + "type node single\",\"type\":\"object\",\"required\":[\"msisdn\",\"imsi\",\"oem\",\"model\"],\"properties\":"
        + "{\"msisdn\":{\"type\":\"string\",\"pattern\":\"^(?!\\\\s).+$\",\"maxLength\":50},\"imsi\":{\"type\""
        + ":\"string\",\"maxLength\":50},\"oem\":{\"type\":\"string\",\"maxLength\":50},\"model\":{\"type\":\"string\""
        + ",\"maxLength\":50},\"location\":{\"type\":\"string\",\"maxLength\":50},\"oemPartNr\":{\"type\":\"string\","
        + "\"maxLength\":50}},\"additionalProperties\":false}'\n"
        + "  WHERE node_type = 'TestType'\n"
        + "END\n";
    DBHelper.runSQLQuery(newNodeTypeQuery);
  }

  private static void deleteNodeType() throws SQLException{

    String deleteNodeTypeQuery = "delete from t_lwa_node_type where node_type = 'TestType'";
    DBHelper.runSQLQuery(deleteNodeTypeQuery);
  }
}
