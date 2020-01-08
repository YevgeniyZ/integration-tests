package bluesky.restapi.lightWeightAccount.updateIotDevice;

import bluesky.restapi.helpers.DBHelper;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.AccountState;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.LightWeightAccount.Node;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import static bluesky.restapi.base.BaseAssertions.verifyErrorResponse;
import static bluesky.restapi.base.BaseAssertions.verifyErrorResponseContainsMessage;
import static bluesky.restapi.base.BaseAssertions.verifyLwaFields;
import static bluesky.restapi.base.BaseAssertions.verifyNodeAttributes;
import static bluesky.restapi.base.BaseAssertions.verifyNodeFields;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;

@Story("BLUESKY-23 LW Account Backend - Update IOT Device  Guiding Info")
public class UpdateIotDeviceGuidingInfoTest extends BaseUpdateIotDevice {

  private static String MT_MAX_DATE;

  @BeforeClass
  public void initialize() {

    MT_MAX_DATE = DateTimeHelper.getMaxDateValueDb();
  }

  @Test(description = "Verify node's update guiding info successfully", groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        //.setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert attributes is updated for lwa
    this.verifyResponseFields(response, lwa, lwaUpdated, node);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-670")
  @Test(description = "Verify node's update guiding info rejected after responsible change in this day at the same "
      + "time", groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoAfterResponsibleChangeSameTimestampNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible after update attributes
    LightWeightAccount lwaUpdatedResponsible = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedResponsible).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);
  }

  @Test(description = "Verify node's update guiding info successfully after responsible change in this day",
      groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoAfterResponsibleChangePositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible after update attributes
    LightWeightAccount lwaUpdatedResponsible = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedResponsible).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert attributes is updated for lwa
    verifyNodeAttributes(response, updatedAttributes);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-306")
  @Test(description = "Verify node's update guiding info will be rejected if attributes has NULL value (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNullMsisdnNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", null);
            }}));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-410")
  @Test(description = "Verify node's update guiding info will be rejected if attributes has empty value (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithEmptyMsisdnNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        //.setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "");
            }}));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    verifyErrorResponseContainsMessage(response, 428,
        "Reason: Invalid JSON attributes: #: required key [msisdn] not found");
    //database validation
    List<LightWeightAccount> lightWeightAccounts = DBHelper.getNodes(lwa.getExternalId(), lwa.getNode().getNodeType());
    Assert.assertEquals(lightWeightAccounts.size(), 1, "Expected node size does not match");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-810")
  @Test(description = "Verify node's update optional json attribute will be accepted (PositiveTest)", groups = "smoke")
  public void updateRegisteredNodeWithEmptyImsiPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "value1");
              put("imsi", "");
            }}));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", "11");
      put("location", "12356");
      put("model", "1234");
      put("msisdn", "value1");
    }});
    //database validation
    List<LightWeightAccount> lightWeightAccounts = DBHelper.getNodes(lwa.getExternalId(), lwa.getNode().getNodeType());
    Assert.assertEquals(lightWeightAccounts.size(), 2, "Expected node size does not match");
    for (LightWeightAccount account : lightWeightAccounts) {
      //most recent
      if (account.getEndDate().equals(MT_MAX_DATE)) {
        Assert.assertEquals(account.getNode().getAttributes().keySet().size(), 4, "json attribute "
            + "keys size does not match");
        Assert.assertFalse(account.getNode().getAttributes().containsKey("imsi"), "key: imsi doesnt exist");
      } else { //old node
        Assert.assertEquals(account.getNode().getAttributes().keySet().size(), 5, "json attribute "
            + "keys size does not match");
        Assert.assertTrue(account.getNode().getAttributes().containsKey("imsi"), "key: imsi exist");
      }
    }
  }

  @Test(description = "Verify node's update existing json attribute value will be accepted (PositiveTest)", groups = "smoke")
  public void updateRegisteredNodeWithChangeExistingJsonValuePositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "value1");
              put("imsi", "value3");
            }}));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", "11");
      put("location", "12356");
      put("model", "1234");
      //changed value
      put("imsi", "value3");
      put("msisdn", "value1");
    }});

    //database validation
    List<LightWeightAccount> lightWeightAccounts = DBHelper.getNodes(lwa.getExternalId(), lwa.getNode().getNodeType());
    Assert.assertEquals(lightWeightAccounts.size(), 2, "Expected node size does not match");
    for (LightWeightAccount account : lightWeightAccounts) {
      //most recent
      if (account.getEndDate().equals(MT_MAX_DATE)) {
        Assert.assertEquals(account.getNode().getAttributes().keySet().size(), 5, "json attribute "
            + "keys size does not match");
        Assert.assertTrue(account.getNode().getAttributes().containsKey("imsi"), "key: imsi exist");
        Assert.assertEquals(account.getNode().getAttributes().get("imsi"), "value3", "imsi value doesnt match");
      } else { //old node
        Assert.assertEquals(account.getNode().getAttributes().keySet().size(), 5, "json attribute "
            + "keys size does not match");
        Assert.assertTrue(account.getNode().getAttributes().containsKey("imsi"), "key: imsi exist");
        Assert.assertEquals(account.getNode().getAttributes().get("imsi"), "value2", "imsi value doesnt match");
      }
    }
  }

  @Test(description = "Verify node's update with same json attribute will be accepted (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithSameAttributesNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "value1");
            }}));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    verifyErrorResponseContainsMessage(response, 428, " current json attributes are same");

    //database validation
    List<LightWeightAccount> lightWeightAccounts = DBHelper.getNodes(lwa.getExternalId(), lwa.getNode().getNodeType());
    Assert.assertEquals(lightWeightAccounts.size(), 1, "Expected node size does not match");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-305")
  @Test(description = "Verify node's update guiding info will be rejected if attributes has wrong data type "
      + "(NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithIntegerMsisdnNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", 5);
            }}));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    verifyErrorResponse(response, 428, "Failed to validate the request. Reason: "
        + "Invalid JSON attributes: #/msisdn: expected type: String, found: Integer");
  }

  @Test(description = "Verify node's update guiding info will be rejected if attributes value exeeded allowed length "
      + "(NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithTooLargeAttributeNegativeTest() {

    //Generate string with letters and numbers of certain length
    String tooLongValue = RandomStringUtils.random(855, true, true);

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", tooLongValue);
            }}));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: The request was rejected because the length of the "
            + "attribute/s value should not exceed the 1700 bytes.");
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-295")
  @Test(description = "Validation message for incorrect attribute value is inconsistent (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithIntegerOemNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "value1");
              put("oem", 5);
            }}));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428, "Failed to validate the request. Reason: "
        + "Invalid JSON attributes: #/oem: expected type: String, found: Integer");
  }

  @Test(description = "Verify node's update guiding info is rejected if update non existent guiding attribute for Air "
      + "Sensor (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeAirSensorWithNotExistedGuidingAttributeNegativeTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode()
        .setNodeType("AIRSENSOR")
        .setAttributes(airSensorAttributes);

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);

    //Assert attributes are not deleted from DB
    //Getting attributes from DB

    String newAttributesQuery = String.format("select * from t_lwa_node where external_id = '%s' and end_date = '%s'",
        lwaUpdated.getExternalId(), MT_MAX_DATE);

    String initialNodeAttributesFromDB = DBHelper.getValueFromDB(newAttributesQuery, "json_attributes");

    String initialNodeAttributes = this.getJsonStringFromObject(airSensorAttributes);

    //Assert json attributes is updated.
    Assert.assertEquals(initialNodeAttributesFromDB, initialNodeAttributes);
  }

  @Test(description = "Verify node's update guiding info is rejected if update existent guiding attribute for Air "
      + "Sensor (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeAirSensorWithExistedGuidingAttributeNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode()
        .setNodeType("AIRSENSOR")
        .setAttributes(airSensorAttributes);

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "msisdnNew");
              put("oem", "oemNew");
            }}));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    HashMap<String, Object> updatedAttributes = new HashMap<String, Object>() {{
      put("msisdn", lwaUpdated.getNode().getAttributes().get("msisdn"));
      put("oem", lwaUpdated.getNode().getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
    }};

    //Assert response
    verifyNodeAttributes(response, updatedAttributes);
  }

  @Test(description = "Verify node's update guiding info is rejected if update existent guiding attribute for Air "
      + "Sensor check in db (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeAirSensorWithExistedGuidingAttributeCheckInDBNegativeTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode()
        .setNodeType("AIRSENSOR")
        .setAttributes(airSensorAttributes);

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "msisdnNew");
              put("oem", "oemNew");
            }}));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(200);

    HashMap<String, Object> updatedAttributes = new HashMap<String, Object>() {{
      put("msisdn", lwaUpdated.getNode().getAttributes().get("msisdn"));
      put("oem", lwaUpdated.getNode().getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
    }};

    //Getting attributes from DB
    String newAttributesQuery = String.format("select * from t_lwa_node where external_id = '%s' and end_date = '%s'",
        lwaUpdated.getExternalId(), MT_MAX_DATE);

    String updatedNodeAttributesFromDB = DBHelper.getValueFromDB(newAttributesQuery, "json_attributes");

    String updatedNodeAttributes = this.getJsonStringFromObject(updatedAttributes);

    //Assert json attributes is updated.
    Assert.assertEquals(updatedNodeAttributesFromDB, updatedNodeAttributes);
  }

  @Test(description = "Verify node's update attributes columns are updated", groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingAttributesCheckInDBPositiveTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Getting attributes from DB
    String newAttributesQuery = String.format(
        "select * from t_lwa_node where "
            + "external_id = '%s' and end_date = '%s'", lwaUpdated.getExternalId(), MT_MAX_DATE);

    String vmsisdnNew = DBHelper.getValueFromDB(newAttributesQuery, "vmsisdn");
    String vimsiNew = DBHelper.getValueFromDB(newAttributesQuery, "vimsi");
    String startDate = DBHelper.getValueFromDB(newAttributesQuery, "start_date");

    //Assert new attributes is updated properly
    Assert.assertEquals(vmsisdnNew, updatedAttributes.get("msisdn"));
    Assert.assertEquals(vimsiNew, updatedAttributes.get("imsi"));
    Assert.assertEquals(startDate, DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START_DB, 2));
  }

  @Test(description = "Verify node's update attributes json attributes column is updated", groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingAttributesCheckPositiveTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    HashMap<String, Object> expectedAttributes = new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", updatedAttributes.get("imsi"));
      put("msisdn", updatedAttributes.get("msisdn"));
    }};

    //Getting attributes from DB
    String newAttributesQuery = String.format("select * from t_lwa_node where external_id = '%s' and end_date = '%s'",
        lwaUpdated.getExternalId(), MT_MAX_DATE);

    String updatedJsonAttributesFromDB = DBHelper.getValueFromDB(newAttributesQuery, "json_attributes");

    String updatedJsonAttributesFromRequest = this.getJsonStringFromObject(expectedAttributes);

    //Assert json attributes is updated.
    Assert.assertEquals(updatedJsonAttributesFromDB, updatedJsonAttributesFromRequest);
  }

  private String getJsonStringFromObject(Object object) {

    ObjectMapper mapperObj = new ObjectMapper();
    String jsonString = "";
    try {
      jsonString = mapperObj.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return jsonString;
  }

  @Test(description = "Verify node's update guiding info successfully and old attributes checking in DB", groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoOldInDBPositiveTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Getting attributes from DB
    String oldAttributesQuery = String.format("select * from t_lwa_node where external_id = '%s' and end_date <> '%s'",
        lwaUpdated.getExternalId(), MT_MAX_DATE);

    String endDate = DBHelper.getValueFromDB(oldAttributesQuery, "end_date");
    String vmsisdnOld = DBHelper.getValueFromDB(oldAttributesQuery, "vmsisdn");
    String vimsiOld = DBHelper.getValueFromDB(oldAttributesQuery, "vimsi");

    //Assert old attributes is on the DB
    Assert.assertEquals(vmsisdnOld, node.getAttributes().get("msisdn"));
    Assert.assertEquals(vimsiOld, node.getAttributes().get("imsi"));
    Assert.assertEquals(endDate, DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_END_DB, 1));
  }

  @Test(description = "Verify node's update guiding info successfully and check seconds accuracy for end date", groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoSecondAccuracyPositiveTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(
            DateTimeHelper.getDatePlusDays(DateTimeHelper.setCustomDatePattern("yyyy-MM-dd'T'00:00:02'Z'"), 2))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Getting attributes from DB
    String oldAttributesQuery = String.format("select * from t_lwa_node where external_id = '%s' and end_date <> '%s'",
        lwaUpdated.getExternalId(),
        MT_MAX_DATE);

    String endDate = DBHelper.getValueFromDB(oldAttributesQuery, "end_date");
    String vmsisdnOld = DBHelper.getValueFromDB(oldAttributesQuery, "vmsisdn");
    String vimsiOld = DBHelper.getValueFromDB(oldAttributesQuery, "vimsi");

    //Assert old attributes is on the DB
    Assert.assertEquals(vmsisdnOld, node.getAttributes().get("msisdn"));
    Assert.assertEquals(vimsiOld, node.getAttributes().get("imsi"));
    Assert.assertEquals(endDate,
        DateTimeHelper.getDatePlusDays(DateTimeHelper.setCustomDatePattern("yyyy-MM-dd 00:00:01.0"), 2));
  }

  @Test(description = "BLUESKY-262 Guiding attributes getting lost even if not updated", groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoAllAttributesPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        //.setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert guiding attributes is updated for lwa
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwa.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("orderId", lwa.getOrderId());
      put("startDate", lwaUpdated.getStartDate());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("imsi", "updatedValueIMSI");
      put("msisdn", "updatedValueMSISDN");
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
    }});
  }

  @Test(description = "Verify node's update guiding info request rejected if trying to update before node is created "
      + "(NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoBeforeNodeCreatedNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert update is rejected
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid request: start date must be after last update");
  }

  @Test(description = "Verify node's update guiding info request rejected if trying to update before last update "
      + "(NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoBeforeLastUpdatedNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with any info
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Update lwa with any info
    LightWeightAccount lwaUpdatedBeforeLastUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        //.setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedBeforeLastUpdated);

    //Assert update is rejected
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid request: start date must be after last update");
  }

  @Test(description = "Verify node guiding info updates rejected if the old responsible in archived status (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoOldResponsibleArchivedNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Change Responsible status to Archived
    AccountEntityManager.updateAccountStatusToArchived(responsibleId);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Update request will be rejected
    verifyErrorResponse(response, 428, "Failed to validate the request. Reason: "
        + "responsible is ineffective on the requested date");
  }

  @Test(description = "Verify node guiding info updates rejected if the new responsible in archived status (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoNewResponsibleArchivedNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate((DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START)));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Change Responsible status to Archived
    AccountEntityManager.updateAccountStatusToArchived(newResponsibleId);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Update request will be rejected
    verifyErrorResponse(response, 428, "Failed to validate the request. Reason: "
        + "responsible is ineffective on the requested date");
  }

  @Test(description = "Verify node guiding info updates if the new responsible in archived status", groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoNewResponsibleNotArchivedPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate((DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2)));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3));
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Change Responsible status to Archived
    AccountEntityManager.updateAccountStatusToArchived(newResponsibleId);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert attributes is updated for lwa
    Assert.assertEquals(response.body().jsonPath().get("responsibleId"), lwaUpdated.getResponsibleId());
    Assert.assertEquals(response.body().jsonPath().get("node.attributes.imsi"),
        lwaUpdated.getNode().getAttributes().get("imsi"));
    Assert.assertEquals(response.body().jsonPath().get("node.attributes.msisdn"),
        lwaUpdated.getNode().getAttributes().get("msisdn"));
  }

  @Test(description = "Verify node's update guiding info successfully in the past", groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoInThePastPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        //.setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert attributes is updated for lwa
    verifyResponseFields(response, lwa, lwaUpdated, node);
  }

  @Test(description = "Verify node's update guiding info successfully in the future", groups = "smoke")
  public void updateRegisteredNodeWithNewGuidingInfoInTheFuturePositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        //.setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert attributes is updated for lwa
    verifyResponseFields(response, lwa, lwaUpdated, node);
  }

  @Test(description = "Verify node's update attributes will be rejected if status terminated (NegativeTest)", groups = "smoke")
  public void updateRegisteredTerminatedNodeWithNewGuidingInfoNegativeTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update node status to 'terminated' in DB
    String query =
        String.format("update dbo.t_lwa_node set status = 'Terminated' where external_id = '%s'", lwa.getExternalId());
    DBHelper.runSQLQuery(query);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428, "Failed to validate the request. Reason: "
        + "Node status in Terminated state");
  }

  @Test(description = "BLUESKY-255 <BUG> Request is rejected if optional attribute is missed (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithoutOptionalAttributesNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "updatedValueMSISDN");
            }}));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert attributes is updated for lwa
    Assert.assertEquals(response.getStatusCode(), 200, "lwa is not updated");
    Assert.assertEquals(response.getBody().jsonPath().get("node.attributes.msisdn"), "updatedValueMSISDN",
        "msisdn is not updated");

  }

  private void verifyResponseFields(Response response, LightWeightAccount lwa, LightWeightAccount lwaUpdated,
                                    Node node) {

    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwa.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("orderId", lwa.getOrderId());
      put("startDate", lwaUpdated.getStartDate());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, updatedAttributes);
  }
}
