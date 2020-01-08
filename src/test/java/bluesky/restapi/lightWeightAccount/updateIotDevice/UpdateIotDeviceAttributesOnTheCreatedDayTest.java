package bluesky.restapi.lightWeightAccount.updateIotDevice;

import bluesky.restapi.helpers.DBHelper;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.NodeStatus;
import bluesky.restapi.models.OwnershipModes;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static bluesky.restapi.helpers.DateTimeHelper.getCurrentDate;
import static bluesky.restapi.helpers.DateTimeHelper.getDatePlusDays;
import static bluesky.restapi.helpers.DateTimeHelper.getTomorrowDate;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;

@Story("BLUESKY-702 - LW Account Backend - Restrict IOT Device updates for (first) Created Day")
public class UpdateIotDeviceAttributesOnTheCreatedDayTest extends BaseUpdateIotDevice {

  private static final String NEW_ORDER_ID = "New Order";

  @Test(description = "Verify request is rejected if update responsible in node date created",
      groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleAtNodeCreatedDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible at node date created
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    Response response =
        LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428)
            .extract().response();

    Assert.assertTrue(response.getBody().jsonPath().get("message").toString()
        .contains("Responsible: Initial day"), "Error message is incorrect: ");
  }

  @Test(description = "Verify request is rejected if update responsible in node past date created", groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleAtNodeCreatedPastDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible at node date created
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    Response response =
        LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428)
            .extract().response();

    Assert.assertTrue(response.getBody().jsonPath().get("message").toString().contains(" Reason: Cannot Update "
        + "Responsible: Initial day"));
  }

  @Test(description = "Verify request is rejected if update responsible in node future date created", groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleAtNodeCreatedFutureDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    DateTimeHelper.waitForSeconds(1);
    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    Response response =
        LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428)
            .extract().response();

    Assert.assertTrue(
        response.getBody().jsonPath().get("message").toString().contains("Cannot Update Responsible: Initial day"));
  }

  @Test(description = "Verify request is rejected if update status in node date created", groups = "smoke")
  public void updateRegisteredNodeWithStatusAtNodeCreatedDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible at node date created
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setNodeStatus(NodeStatus.SUSPENDED)
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    Response response =
        LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428)
            .extract().response();

    Assert.assertTrue(
        response.getBody().jsonPath().get("message").toString().contains("Cannot Update Node Status: Initial day"));
  }

  @Test(description = "Verify that the API request is rejected if update settlementId at date created with earlier timestamp",
      groups = "smoke")
  public void updateRegisteredNodeWithStatusAtNodeCreatedDateBeforeTimestampNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible at node date created
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setNodeStatus(NodeStatus.SUSPENDED)
        .setStartDate(DateTimeHelper.getDateMinusSeconds(
            getCurrentDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API),
            DateTimeHelper.DATE_PATTERN_CUSTOMER_API, 2))
        .setNode(new LightWeightAccount.Node()
            .setNodeType(node.getNodeType()));

    Response response =
        LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428)
            .extract().response();

    Assert.assertTrue(
        response.getBody().jsonPath().get("message").toString().contains("start date must be after last update"));
  }

  @Test(description = "Verify request is rejected if update OwnershipMode in node date created", groups = "smoke")
  public void updateRegisteredNodeWithOwnershipModeAtNodeCreatedDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible at node date created
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setOwnershipMode(OwnershipModes.RENT)
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    Response response =
        LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428)
            .extract().response();

    Assert.assertTrue(response.getBody().jsonPath().get("message").toString().contains(" Ownership Mode: Initial day"));
  }

  @Test(description = "Verify request is rejected if update Settlement in node date created", groups = "smoke")
  public void updateRegisteredNodeWithSettlementAtNodeCreatedDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible at node date created
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(responsibleId.toString())
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);
  }

  @Test(description = "Verify request is rejected if update node fields in node date created", groups = "smoke")
  public void updateRegisteredNodeWithNodeFieldsAtNodeCreatedDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity responsible1 = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse1 = AccountEntityServiceMethods.createAccountEntityRequest(responsible1);
    Integer responsibleId1 = getAccountId(responsibleResponse1);

    //Update lwa with new responsible at node date created
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(responsibleId.toString())
        .setOwnershipMode(OwnershipModes.RENT)
        .setResponsibleId(responsibleId1.toString())
        .setNodeStatus(NodeStatus.SUSPENDED)
        .setOrderId("54321")
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428);

    String query = String.format("select ownership_mode,responsible_id,status,settlement_id,order_id from t_lwa_node "
        + "where external_id = '%s' and end_date = '%s'", lwaUpdated.getExternalId(), MT_MAX_DATE);

    Map<String, Object> actualDBValues = DBHelper.getResultMap(query);
    //Ownership enum saved as Owner. So have to do this: StringUtils.capitalize(lwa.getOwnershipMode().toLowerCase())
    Assert.assertEquals(actualDBValues.get("ownership_mode"),
        StringUtils.capitalize(lwa.getOwnershipMode().toLowerCase()));
    Assert.assertEquals(actualDBValues.get("responsible_id").toString(), lwa.getResponsibleId());
    Assert.assertEquals(actualDBValues.get("status"), StringUtils.capitalize(lwa.getNodeStatus().toLowerCase()));
    Assert.assertEquals(actualDBValues.get("settlement_id"), lwa.getSettlementId());
    Assert.assertEquals(actualDBValues.get("order_id").toString(), lwa.getOrderId());
  }

  @Test(description = "Verify that the API request is rejected if update settlementId, ownershipMode with Guiding keys "
      + "status at date created", groups = "smoke")
  public void updateRegisteredNodeWithNodeAttributesAtNodeCreatedDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible at node date created
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setOwnershipMode(OwnershipModes.RENT)
        .setNodeStatus(NodeStatus.SUSPENDED)
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "valueUpdated");
            }}));

    Response response =
        LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(428)
            .extract().response();

    Assert.assertTrue(response.getBody().jsonPath().get("message").toString().contains(" Ownership Mode: Initial day"));

    String query = String.format("select ownership_mode,responsible_id,status,settlement_id,vmsisdn from t_lwa_node "
        + "where external_id = '%s' and end_date = '%s'", lwaUpdated.getExternalId(), MT_MAX_DATE);

    Map<String, Object> actualDBValues = DBHelper.getResultMap(query);
    Assert.assertEquals(actualDBValues.get("ownership_mode"),
        StringUtils.capitalize(lwa.getOwnershipMode().toLowerCase()), "Incorrect OwnershipMode");
    Assert.assertEquals(String.valueOf(actualDBValues.get("responsible_id")), lwa.getResponsibleId());
    Assert.assertEquals(actualDBValues.get("status"),
        StringUtils.capitalize(lwa.getNodeStatus().toLowerCase()), "Incorrect Status");
    Assert.assertEquals(actualDBValues.get("vmsisdn"),
        lwa.getNode().getAttributes().get("msisdn"), "Incorrect msisdn");
  }

  @Test(description = "Verify that the API request is successfully created if status, ownershipMode, responsible, "
      + "settlement not in request ", groups = "smoke")
  public void updateRegisteredNodeWithGuidingAttributesAtCreatedDatePositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setOrderId("54321")
        .setStartDate(getDatePlusDays(DateTimeHelper.DATE_PATTERN_CUSTOMER_API, 2))
        .setNode(new LightWeightAccount.Node()
            .setNodeType(node.getNodeType())
            .setAttributes(updatedAttributes));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(200);

    String query = String.format("select * from t_lwa_node where external_id = '%s' and end_date = '%s'",
        lwaUpdated.getExternalId(), MT_MAX_DATE);

    Map<String, Object> actualDBValues = DBHelper.getResultMap(query);

    Assert.assertEquals(actualDBValues.get("vmsisdn"),
        lwaUpdated.getNode().getAttributes().get("msisdn"), "Incorrect msisdn: ");

    Assert.assertEquals(actualDBValues.get("vimsi"),
        lwaUpdated.getNode().getAttributes().get("imsi"), "Incorrect imsi: ");

    Assert.assertEquals(actualDBValues.get("order_id"),
        lwaUpdated.getOrderId(), "Incorrect orderId: ");
  }

  @Test(description = "Verify request is accepted if update OrderId in node same date created", groups = "smoke")
  public void updateRegisteredNodeWithOrderIdAtNodeCreatedDatePositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    LightWeightAccount.Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible at node date created
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setOrderId(NEW_ORDER_ID)
        .setStartDate(getCurrentDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new LightWeightAccount.Node().setNodeType(node.getNodeType()));

    Response response =
        LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(200)
            .extract().response();

    Assert.assertTrue(response.getBody().asString().contains(NEW_ORDER_ID));
  }
}
