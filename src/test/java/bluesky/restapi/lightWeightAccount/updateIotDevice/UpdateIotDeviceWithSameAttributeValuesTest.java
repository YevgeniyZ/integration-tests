/*
 * COPYRIGHT (c) Ericsson AB 2019. The copyright to the computer program(s) herein is the property
 * of Ericsson Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 */

package bluesky.restapi.lightWeightAccount.updateIotDevice;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.base.BaseAssertions;
import bluesky.restapi.helpers.DBHelper;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.OwnershipModes;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;

@Story("BLUESKY-261 LW Account Backend - Reject Responsible/Settlement party/Ownership move for same values (i.e. no "
    + "change)")
public class UpdateIotDeviceWithSameAttributeValuesTest extends BaseApiTest {

  @Test(description = "Verify node's update reject request with the same responsible (Negative)", groups = "smoke")
  public void updateRegisteredNodeWithSameResponsibleTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with same responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    //request rejection check
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);
  }

  @Test(description = "Verify node's update reject request with the same status (Negative)", groups = "smoke")
  public void updateRegisteredNodeWithSameStatusTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with same responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setNodeStatus("ACTIVE")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    //request rejection check
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);
  }

  @Test(description = "Verify node's update reject request with the same status and check if other attributes are not "
      + "updated (Negative)", groups = "smoke")
  public void updateRegisteredNodeWithSameStatusAndOtherNewAttributesTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with same responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setNodeStatus("ACTIVE")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    //Update node with the same status
    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Node query to db
    String nodeQuery = String.format("select * from t_lwa_node where external_id = '%s' and end_date = dbo.mtmaxdate()",
        lwaUpdated.getExternalId());

    //Assert values in DB are not changed
    Assert.assertEquals(DBHelper.getValueFromDB(nodeQuery, "responsible_id"), lwa.getResponsibleId(),
        "Responsible is incorrectly changed: ");

    BaseAssertions.verifyErrorResponse(response, 428, "Failed to validate the "
        + "request. Reason: Invalid request: Requested node status should not be same as exist node status");
  }

  @Test(description = "Verify node's update reject request with the same responsible for different matching "
      + "node_type+External ID (Negative)", groups = "smoke")
  public void updateRegisteredNodeWithSameResponsibleForDifferentMatchingTest() {

    //Create responsible for camera
    AccountEntity cameraResponsible = new AccountEntityManager().createAccountEntity();
    Response cameraResponsibleResponse =
        AccountEntityServiceMethods.createAccountEntityRequest(cameraResponsible);
    Integer cameraResponsibleId = getAccountId(cameraResponsibleResponse);

    //Create responsible for airsensor
    AccountEntity airsensorResponsible = new AccountEntityManager().createAccountEntity();
    Response airsensorResponsibleResponse =
        AccountEntityServiceMethods.createAccountEntityRequest(airsensorResponsible);
    Integer airsensorResponsibleId = getAccountId(airsensorResponsibleResponse);

    //LWA Camera registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(cameraResponsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //LWA Airsensor registration
    LightWeightAccount.Node node2 = createDefaultNode()
        .setNodeType("AIRSENSOR")
        .setAttributes(new HashMap<String, Object>() {{
          put("msisdn", "value1");
          put("oem", "11");
          put("location", "12356");
          put("model", "1234");
        }});

    LightWeightAccount lwa2 = createDefaultLightWeightAccount(node2)
        .setResponsibleId(airsensorResponsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa2).then().statusCode(200);

    //Update camera with same responsible
    LightWeightAccount lwaCameraUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(cameraResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    //request rejection check
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaCameraUpdated).then().statusCode(428);

    //Update airsensor with new responsible
    LightWeightAccount lwaAirsensorUpdated = new LightWeightAccount()
        .setExternalId(lwa2.getExternalId())
        .setResponsibleId(cameraResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType("AIRSENSOR"));

    //request rejection check
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaAirsensorUpdated).then().statusCode(200);
  }

  @Test(description = "Verify node's update successfully with the responsible that was once assigned", groups = "smoke")
  public void updateRegisteredNodeWithSameResponsibleOneMoreTimeTest() {

    //Create responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Create new responsible
    AccountEntity responsibleNew = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsibleNew);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with same responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    //Update responsible
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(200);

    //Update lwa with responsible that was set previously
    lwaUpdated
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    //Update responsible once again
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(200);
  }

  @Test(description = "Verify node's update reject request with the same settlement (Negative)", groups = "smoke")
  public void updateRegisteredNodeWithSameSettlementTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Create settlement
    AccountEntity settlement = new AccountEntityManager().createAccountEntity();
    Response settlementResponse = AccountEntityServiceMethods.createAccountEntityRequest(settlement);
    Integer settlementId = getAccountId(settlementResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(settlementId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with same settlement
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(settlementId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    //request rejection check
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);
  }

  @Test(description = "Verify node's update reject request with the same settlement new responsible and new method of "
      + "sale (Negative)", groups = "smoke")
  public void updateRegisteredNodeWithSameSettlementAndNewOtherAttributesTest() throws Exception {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Create settlement
    AccountEntity settlement = new AccountEntityManager().createAccountEntity();
    Response settlementResponse = AccountEntityServiceMethods.createAccountEntityRequest(settlement);
    Integer settlementId = getAccountId(settlementResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(settlementId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with same settlement
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(settlementId.toString())
        .setResponsibleId(settlementId.toString())
        .setOwnershipMode(OwnershipModes.RENT)
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(
            new LightWeightAccount.Node().setNodeType(node.getNodeType()).setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "34556789");
              put("oem", "newOem");
            }}));

    //request rejection check
    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    BaseAssertions.verifyErrorResponse(response, 428,
        String.format("Failed to validate the request. Reason: Invalid request: current settlement party is %s",
            settlementId.toString()));

    //Node query to db
    String nodeQuery = String.format("select * from t_lwa_node where external_id = '%s' and end_date = dbo.mtmaxdate()",
        lwaUpdated.getExternalId());

    //Assert values in DB are not changed
    Map<String, Object> queryResults = DBHelper.getResultMap(nodeQuery);
    Assert.assertEquals(queryResults.get("vmsisdn"), "value1");
    Assert.assertEquals(queryResults.get("json_attributes"),
        new ObjectMapper().writeValueAsString(lwa.getNode().getAttributes()));
  }

  @Test(description = "Verify node's update reject request with the same method of sale, new responsible and new "
      + "settlement (Negative)", groups = "smoke")
  public void updateRegisteredNodeWithSameOwnershipModeAndNewOtherAttributesTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Create settlement
    AccountEntity settlement = new AccountEntityManager().createAccountEntity();
    Response settlementResponse = AccountEntityServiceMethods.createAccountEntityRequest(settlement);
    Integer settlementId = getAccountId(settlementResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(settlementId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with same settlement
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(responsibleId.toString())
        .setResponsibleId(settlementId.toString())
        .setOwnershipMode(OwnershipModes.OWNER)
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    //request rejection check
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);
  }

  @Test(description = "Verify node's update reject request with the same responsible, new method of sale and new "
      + "settlement (Negative)", groups = "smoke")
  public void updateRegisteredNodeWithSameResponsibleAndNewOtherAttributesTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //Create settlement
    AccountEntity settlement = new AccountEntityManager().createAccountEntity();
    Response settlementResponse = AccountEntityServiceMethods.createAccountEntityRequest(settlement);
    Integer settlementId = getAccountId(settlementResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(settlementId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with same settlement
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(responsibleId.toString())
        .setResponsibleId(responsibleId.toString())
        .setOwnershipMode(OwnershipModes.RENT)
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    //request rejection check
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);
  }

  @Test(description = "Verify node's update reject request with the same method of sale (Negative)", groups = "smoke")
  public void updateRegisteredNodeWithSameOwnershipModeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with same settlement
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setOwnershipMode(lwa.getOwnershipMode())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    //request rejection check
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);
  }

  @Test(description = "Verify node's update reject request with the same method of sale (Positive)", groups = "smoke")
  public void updateRegisteredNodeWithNewOwnershipModeTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with same settlement
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setOwnershipMode(OwnershipModes.RENT)
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    String nodeQuery = String.format("select * from t_lwa_node where external_id = '%s' and end_date = dbo.mtmaxdate()",
        lwaUpdated.getExternalId());

    //Successfully change method of sale
    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(200);
    Assert.assertEquals(DBHelper.getValueFromDB(nodeQuery, "ownership_mode"), "Rent",
        "Ownership mode is not updated");
  }

  @Test(description = "Verify node's update reject request with the same responsible and check in DB other attributes "
      + "(Negative)", groups = "smoke")
  public void updateRegisteredNodeWithSameResponsibleCheckOtherAttributesInDbTest() throws Exception {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with same responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(responsibleId.toString())
        .setOwnershipMode(OwnershipModes.RENT)
        .setOrderId("updatedOrder")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "valueUpdated");
            }}));

    //request rejection check
    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Query for DB
    String nodeQuery = String.format("select * from t_lwa_node where external_id = '%s' and end_date = dbo.mtmaxdate()",
        lwaUpdated.getExternalId());

    //Getting results of a query
    Map<String, Object> queryResults = DBHelper.getResultMap(nodeQuery);

    //Check attributes
    Assert
        .assertEquals(queryResults.get("ownership_mode"), StringUtils.capitalize(lwa.getOwnershipMode().toLowerCase()),
            "incorrect method of sale: ");
    Assert.assertEquals(queryResults.get("order_id"), lwa.getOrderId(),
        "Order id is incorrect: ");
    Assert.assertEquals(queryResults.get("vmsisdn"), node.getAttributes().get("msisdn"),
        "Msisdn is incorrect");

    //System.out.println(new ObjectMapper().writeValueAsString(lwa.getNode().getAttributes()));
    Assert.assertEquals(queryResults.get("json_attributes"),
        new ObjectMapper().writeValueAsString(lwa.getNode().getAttributes()),
        "Json attributes are incorrect: ");

    //Check if response is rejected
    BaseAssertions.verifyErrorResponse(response, 428, String.format("Failed to validate the request. "
        + "Reason: Invalid request: current responsible is %s", responsibleId.toString()));
  }
}
