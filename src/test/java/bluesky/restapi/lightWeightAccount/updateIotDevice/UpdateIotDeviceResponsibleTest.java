package bluesky.restapi.lightWeightAccount.updateIotDevice;

import bluesky.restapi.base.BaseApiTest;
import bluesky.restapi.helpers.DBHelper;
import bluesky.restapi.helpers.DateTimeHelper;
import bluesky.restapi.managers.AccountEntityManager;
import bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods;
import bluesky.restapi.methods.customerMethods.LightWeightAccountServiceMethods;
import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.AccountState;
import bluesky.restapi.models.AccountStatus;
import bluesky.restapi.models.LightWeightAccount;
import bluesky.restapi.models.LightWeightAccount.Node;
import bluesky.restapi.models.NodeStatus;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.HashMap;

import static bluesky.restapi.base.BaseAssertions.verifyErrorResponse;
import static bluesky.restapi.base.BaseAssertions.verifyLwaFields;
import static bluesky.restapi.base.BaseAssertions.verifyNodeAttributes;
import static bluesky.restapi.base.BaseAssertions.verifyNodeFields;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultLightWeightAccount;
import static bluesky.restapi.managers.LightWeightAccountManager.createDefaultNode;
import static bluesky.restapi.methods.customerMethods.AccountEntityServiceMethods.getAccountId;
import static org.hamcrest.Matchers.hasKey;

@Story("BLUESKY-43 LW Account Backend - Update IOT Device Responsible (Move)")
public class UpdateIotDeviceResponsibleTest extends BaseApiTest {

  private static String MT_MAX_DATE;

  @BeforeClass
  public void initialize() {

    MT_MAX_DATE = DateTimeHelper.getMaxDateValueDb();
  }

  @Test(description = "Verify node's update responsible successfully", groups = "smoke")
  public void updateRegisteredNodeWithNewResponsiblePositiveTest() {

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

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert responsible is updated for lwa
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwaUpdated.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }});

    response.then().body("$", hasKey("startDate"));
  }

  @Issue("https://agile.ecb.ericssondevops.com/browse/BLUESKY-365")
  @Test(description = "Verify node's update responsible successfully after previous attributes update", groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleAfterUpdateNegativeTest() {

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
    LightWeightAccount lwaUpdatedAttributes = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "updatedValueMSISDN");
              put("imsi", "updatedValueIMSI");
            }}));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedAttributes);

    DateTimeHelper.waitForSeconds(2);
    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible after update attributes
    LightWeightAccount lwaUpdatedResponsible = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedResponsible);

    //Assert responsible is updated for lwa
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwaUpdatedResponsible.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", lwaUpdatedAttributes.getNode().getAttributes().get("imsi"));
      put("msisdn", lwaUpdatedAttributes.getNode().getAttributes().get("msisdn"));
    }});

    response.then().body("$", hasKey("startDate"));
  }

  @Test(description = "Verify node's update responsible successfully after previous attributes update in the past "
      + "(Positive)", groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleAfterUpdateInThePastPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new attributes
    LightWeightAccount lwaUpdatedAttributes = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "updatedValueMSISDN");
              put("imsi", "updatedValueIMSI");
            }}));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedAttributes);

    DateTimeHelper.waitForSeconds(2);
    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible after update attributes
    LightWeightAccount lwaUpdatedResponsible = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedResponsible);

    //Assert responsible is updated for lwa
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwaUpdatedResponsible.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", lwaUpdatedAttributes.getNode().getAttributes().get("imsi"));
      put("msisdn", lwaUpdatedAttributes.getNode().getAttributes().get("msisdn"));
    }});

    response.then().body("$", hasKey("startDate"));
  }

  @Test(description = "Verify node's update responsible successfully after previous attributes update with different "
      + "time stamp", groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleAfterUpdateWithTimeStampPositiveTest() {

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
    LightWeightAccount lwaUpdatedAttributes = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node()
            .setNodeType(node.getNodeType())
            .setAttributes(new HashMap<String, Object>() {{
              put("msisdn", "updatedValueMSISDN");
              put("imsi", "updatedValueIMSI");
            }}));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedAttributes);

    DateTimeHelper.waitForSeconds(2);
    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible after update attributes
    LightWeightAccount lwaUpdatedResponsible = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedResponsible);

    //Assert responsible is updated for lwa
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwaUpdatedResponsible.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", lwaUpdatedAttributes.getNode().getAttributes().get("imsi"));
      put("msisdn", lwaUpdatedAttributes.getNode().getAttributes().get("msisdn"));
    }});

    response.then().body("$", hasKey("startDate"));
  }

  @Test(description = "Verify node's update rejected if the responsible in archived status (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleArchivedStatusNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update responsible status in DB
    DBHelper.updateAccountStatusInDB(newResponsibleId, AccountStatus.ARCHIVED);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Update request will be rejected
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: responsible is ineffective on the requested date");
  }

  @Test(description = "Verify node's update rejected if the responsible in archived status (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewSettlementArchivedStatusNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update responsible status in DB
    DBHelper.updateAccountStatusInDB(newResponsibleId, AccountStatus.ARCHIVED);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Update request will be rejected
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: settlement party is ineffective on the requested "
            + "date");

  }

  @Test(description = "Verify node updates rejected if the old responsible in archived status (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithOldResponsibleIncorrectStatusNegativeTest() {

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
        .setStartDate(DateTimeHelper.getDatePlusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert responsible's update will be rejected
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: responsible is ineffective on the requested date");
  }

  @Test(description = "Verify node updates successfully if the old responsible not in archived status", groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleValidStatusPositiveTest() {

    //Create responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2));
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Change Responsible status to Archived
    AccountEntityManager.updateAccountStatusToArchived(responsibleId);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 1))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert responsible is updated for lwa
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwaUpdated.getResponsibleId());
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
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }});
  }

  @Test(description = "Verify node's update rejected if the old responsible in archived status (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithOldResponsibleArchivedStatusNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update responsible status in DB
    DBHelper.updateAccountStatusInDB(responsibleId, AccountStatus.ARCHIVED);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Update request will be rejected
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: responsible is ineffective on the requested date");
  }

  @Test(description = "Verify node's update rejected if the old responsible in suspended status (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithOldResponsibleSuspendedStatusNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update responsible status in DB
    DBHelper.updateAccountStatusInDB(responsibleId, AccountStatus.SUSPENDED);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Update request will be performed
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwaUpdated.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("startDate", lwaUpdated.getStartDate());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }});
  }

  @Test(description = "Verify node's successfully updated if the responsible in suspended status", groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleSuspendedStatusPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update responsible status in DB
    DBHelper.updateAccountStatusInDB(newResponsibleId, AccountStatus.SUSPENDED);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Update request will be performed
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwaUpdated.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("startDate", lwaUpdated.getStartDate());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }});
  }

  @Test(description = "Verify node's successfully updated if the responsible in pending final bill status",
      groups = "smoke")
  public void updateRegisteredNodeWithNewResponsiblePendingFinalBillStatusPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update responsible status in DB
    DBHelper.updateAccountStatusInDB(newResponsibleId, AccountStatus.PENDING_FINAL_BILL);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Update request will be performed
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwaUpdated.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("startDate", lwaUpdated.getStartDate());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }});
  }

  @Test(description = "Verify node's successfully updated if the responsible in closed status", groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleClosedStatusPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update responsible status in DB
    DBHelper.updateAccountStatusInDB(newResponsibleId, AccountStatus.CLOSED);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Update request will be performed
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwaUpdated.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("startDate", lwaUpdated.getStartDate());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }});
  }

  @Test(description = "Verify node's successfully updated if the responsible in pending approval status",
      groups = "smoke")
  public void updateRegisteredNodeWithNewResponsiblePendingApprovalStatusPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update responsible status in DB
    DBHelper.updateAccountStatusInDB(newResponsibleId, AccountStatus.PENDING_APPROVAL);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Update request will be performed
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwaUpdated.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("startDate", lwaUpdated.getStartDate());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }});
  }

  @Test(description = "Verify node's update rejected if namespace is in system_user for responsible (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleInSystemNamespaceNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId("system_user/mt")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: responsible is ineffective on the requested date");
  }

  @Test(description = "Verify node's update rejected if namespace is in system_user for settlement (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewSettlementInSystemNamespaceNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId("system_user/mt")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428, "Failed to validate the request. Reason: "
        + "settlement party is ineffective on the requested date");
  }

  @Test(description = "Verify node's update rejected if user name is incorrect for responsible (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleIncorrectUserNameNetNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId("blablaUsername")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid responsible");
  }

  @Test(description = "Verify node's update rejected if user name is incorrect for settlement (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewSettlementIncorrectUserNameNetNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId("blablaUsername")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid settlement party");
  }

  @Test(description = "Verify node's update rejected if user is not in metranet for responsible (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleNotInMetraNetNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId("2147483647")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: responsible is ineffective on the requested date");
  }

  @Test(description = "Verify node's update rejected if user is not in metranet for settlement (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewSettlementNotInMetraNetNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId("2147483647")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: settlement party is ineffective on the "
            + "requested date");
  }

  @Test(description = "Verify node's update rejected if user has invalid value for responsible (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleHasInvalidValueNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId("21474836470")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 400,
        "Failed to execute bad request. Reason: responsible is not a valid integer: 21474836470");
  }

  @Test(description = "Verify node's update rejected if user has invalid value for settlemnt (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewSettlementHasInvalidValueNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId("21474836470")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 400, "Failed to execute bad request. Reason: "
        + "settlement party is not a valid integer: 21474836470");
  }

  @Test(description = "Verify node's update rejected if user has has incorrect namespace (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleIncorrectNamespaceNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId("authIncorrect/anonymous")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: responsible is ineffective on the requested date");
  }

  @Test(description = "Verify node's update rejected if user has has incorrect namespace for settlement (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewSettlementIncorrectNamespaceNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId("authIncorrect/anonymous")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428, "Failed to validate the request. Reason: "
        + "settlement party is ineffective on the requested date");
  }

  @Test(description = "Verify node's update rejected if user has has incorrect username (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleIncorrectUserNameNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId("mt/anonymousIncorrect")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: responsible is ineffective on the requested date");
  }

  @Test(description = "Verify node's update rejected if user has has incorrect username for settlement (NegativeTest)",
      groups = "smoke")
  public void updateRegisteredNodeWithNewSettlementIncorrectUserNameNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId("mt/anonymousIncorrect")
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428, "Failed to validate the request. Reason: "
        + "settlement party is ineffective on the requested date");
  }

  @Test(enabled = false, description = "Verify that the request is rejected when there is already a Responsible change "
      + "for this day (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleInTheSameDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    DateTimeHelper.waitForSeconds(3);
    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid request: responsible/ already changed for "
            + "this day");
  }

  @Test(enabled = false, description = "Verify that the request is rejected when there is already a Settlement change "
      + "for this day (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithNewSettlementInTheSameDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    DateTimeHelper.waitForSeconds(2);
    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid request: settlement party already changed "
            + "for this day");
  }

  @Test(description = "Verify that the request is rejected when the timestamp of the request is before any start/update "
      + "date of the node (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithTimestampBeforeTheStartDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid request: start date must be after last update");
  }

  @Test(description = "Verify that the request is rejected when the timestamp of the request is before any start/update "
      + "date of the node updating settlement (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeSettlementWithTimestampBeforeTheStartDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getYesterdayDay(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid request: start date must be after last "
            + "update");
  }

  @Test(enabled = false, description = "Verify that the node status will be updated to 'Terminated'", groups = "smoke")
  public void updateRegisteredNodeStatusToTerminatedPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new status terminated
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.TERMINATED);

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus);

    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwa.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwaUpdatedwithTerminatedStatus.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("startDate", lwaUpdatedwithTerminatedStatus.getStartDate());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }});
  }

  @Test(enabled = false, description = "Verify that the node status will be updated to 'Suspended'", groups = "smoke")
  public void updateRegisteredNodeStatusToSuspendedPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new status suspended
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.SUSPENDED);

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus);

    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwa.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwaUpdatedwithTerminatedStatus.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("startDate", lwaUpdatedwithTerminatedStatus.getStartDate());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }});
  }

  @Test(enabled = false, description = "Verify that the node status will be updated to 'Reserved'", groups = "smoke")
  public void updateRegisteredNodeStatusToReservedPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update lwa with new status suspended
    LightWeightAccount lwaUpdatedwithTerminatedStatus = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()))
        .setNodeStatus(NodeStatus.RESERVED);

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdatedwithTerminatedStatus);

    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwa.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwaUpdatedwithTerminatedStatus.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }});
  }

  @Test(description = "Verify that the request is rejected when the node is Terminated for responsible update "
      + "(NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithStatusTerminatedNegativeTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update node status to 'terminated' in DB
    String query =
        String.format("update dbo.t_lwa_node set status = 'Terminated' where external_id = '%s'", lwa.getExternalId());
    DBHelper.runSQLQuery(query);

    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update terminated lwa with new status
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setResponsibleId(newResponsibleId.toString())
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Node status in Terminated state");
  }

  @Test(description = "Verify that the request is rejected when the node is Terminated for update Settlement "
      + "(NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeSettlementWithStatusTerminatedNegativeTest() throws SQLException {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Update node status to 'terminated' in DB
    String query =
        String.format("update dbo.t_lwa_node set status = 'Terminated' where external_id = '%s'", lwa.getExternalId());
    DBHelper.runSQLQuery(query);

    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update terminated lwa with new status
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setSettlementId(newResponsibleId.toString())
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Node status in Terminated state");
  }

  @Test(description = "Verify that the request is rejected when the timestamp of the request is before the node is "
      + "created (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithDateBeforeNodeIsCreatedNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_CUSTOMER_API, 4));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getCurrentDate(DateTimeHelper.DATE_PATTERN_DEFAULT_START));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_CUSTOMER_API, 4));
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert responsible is not updated for lwa
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid request: start date must be after last "
            + "update");
  }

  @Test(description = "Verify that the request is rejected when the timestamp of the request is before any start/update "
      + "date of the node (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithDateBeforeNodeIsUpdatedNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 4));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 4));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_CUSTOMER_API, 4));

    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_CUSTOMER_API, 3))
        .setNode(new Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(200);

    LightWeightAccount lwaUpdated1 = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_CUSTOMER_API, 4))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated1);

    //Assert responsible is not updated for lwa
    verifyErrorResponse(response, 428,
        "Failed to validate the request. Reason: Invalid request: start date must be after last "
            + "update");
  }

  @Test(description = "Verify that the change can be made in the past.", groups = "smoke")
  public void updateRegisteredNodeWithDateInThePastPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 4));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 3));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 4));

    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert responsible is updated for lwa in the past date
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwaUpdated.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("startDate", lwaUpdated.getStartDate());
      put("settlementId", lwa.getSettlementId());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }});
  }

  @Test(description = "Verify that the change can be made in the past for settlement.", groups = "smoke")
  public void updateRegisteredNodeWithSettlementDateInThePastPositiveTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_CUSTOMER_API, 4));
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_CUSTOMER_API, 3));

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity()
        .setVtStart(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 4));
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getDateMinusDays(DateTimeHelper.DATE_PATTERN_DEFAULT_START, 2))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert responsible is updated for lwa in the past date
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwa.getResponsibleId());
      put("externalId", lwa.getExternalId());
      put("nodeStatus", lwa.getNodeStatus());
      put("ownershipMode", lwa.getOwnershipMode());
      put("startDate", lwaUpdated.getStartDate());
      put("settlementId", lwaUpdated.getSettlementId());
      put("orderId", lwa.getOrderId());
    }});

    verifyNodeFields(response, new HashMap<String, Object>() {{
      put("nodeType", node.getNodeType());
    }});

    verifyNodeAttributes(response, new HashMap<String, Object>() {{
      put("oem", node.getAttributes().get("oem"));
      put("location", node.getAttributes().get("location"));
      put("model", node.getAttributes().get("model"));
      put("imsi", node.getAttributes().get("imsi"));
      put("msisdn", node.getAttributes().get("msisdn"));
    }});
  }

  @Test(description = "Verify node's settlement updated successfully with existent settlement id", groups = "smoke")
  public void updateRegisteredNodeWithNewSettlementPositiveTest() {

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

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert responsible is updated for lwa
    verifyLwaFields(response, new HashMap<String, Object>() {{
      put("responsibleId", lwa.getResponsibleId());
      put("settlementId", lwaUpdated.getSettlementId());
    }});
  }

  @Test(description = "Verify node's update responsible successfully and checked in DB", groups = "smoke")
  public void updateRegisteredNodeWithNewResponsibleFromDBPositiveTest() throws SQLException {

    //Create responsible
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

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Query for DB
    String query = String.format("select responsible_id from t_lwa_node where external_id = '%s' and end_date =  "
        + "'%s'", lwaUpdated.getExternalId(), MT_MAX_DATE);
    String updatedAccountId = DBHelper.getValueFromDB(query, "responsible_id");

    //Assert responsible is updated for lwa
    Assert.assertEquals(updatedAccountId, newResponsibleId.toString(), "Responsible is not updated in DB");
  }

  @Issue("BLUESKY-763")
  @Test(description = "Verify node's update responsible successfully and checked in DB", groups = "smoke")
  public void updateRegisteredNodeWithNewSettlementFromDBPositiveTest() throws SQLException {

    //Create responsible
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

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Query for DB
    String query = String.format("select settlement_id from t_lwa_node where external_id = '%s' and end_date = "
        + "'%s'", lwaUpdated.getExternalId(), MT_MAX_DATE);

    String updatedSettlementId = DBHelper.getValueFromDB(query, "settlement_id");

    //Assert responsible is updated for lwa
    Assert.assertEquals(updatedSettlementId, newResponsibleId.toString(), "Settlement is not updated in DB");
  }

  @Test(description = "Verify node's update responsible successfully and history is checked in DB", groups = "smoke")
  public void updateRegisteredNodeHistoryStayFromDBPositiveTest() throws SQLException {

    //Create responsible
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

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Query for DB
    String query = String.format("select responsible_id from t_lwa_node where external_id = '%s' and end_date = '%s'",
        lwaUpdated.getExternalId(), MT_MAX_DATE);
    String updatedAccountId = DBHelper.getValueFromDB(query, "responsible_id");

    //Assert payer is updated for lwa
    Assert.assertEquals(updatedAccountId, newResponsibleId.toString(), "Payer is not updated in DB");
  }

  @Test(description = "Verify node's update responsible successfully and history for settlement is checked in DB",
      groups = "smoke")
  public void updateRegisteredNodeHistoryStayForSettlementFromDBPositiveTest() throws SQLException {

    //Create responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(lwa.getExternalId())
        .setSettlementId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Query for DB
    String query = String.format("select settlement_id from t_lwa_node where external_id = '%s' and end_date = "
        + "'%s'", lwaUpdated.getExternalId(), MT_MAX_DATE);
    String updatedSettlementId = DBHelper.getValueFromDB(query, "settlement_id");

    //Assert payer is updated for lwa
    Assert.assertEquals(updatedSettlementId, newResponsibleId.toString(), "Settlement is not updated in DB");
  }

  @Test(description = "Verify node's will not be updated without external id (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithoutExternalIdNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert responsible is not updated if ExternalId is missed
    verifyErrorResponse(response, 400, "Failed to execute bad request. Reason: "
        + "Missing input parameter: External id");
  }

  @Test(description = "Verify node's will not be updated without external id (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeExternalIdIsEmptyNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId("")
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert responsible is not updated if ExternalId is missed
    verifyErrorResponse(response, 400, "Failed to execute bad request. Reason: "
        + "Missing input parameter: External id");
  }

  @Test(description = "Verify node's will not be updated without node type (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithoutNodeTypeNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(newResponsibleId.toString())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node());

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);
    verifyErrorResponse(response, 400, "Failed to execute bad request. Reason: "
        + "Missing input parameter: Node Type");
  }

  @Test(description = "Verify node's will not be updated with empty node type (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeNodeTypeIsEmptyNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(newResponsibleId.toString())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate(DateTimeHelper.getTomorrowDate(DateTimeHelper.DATE_PATTERN_CUSTOMER_API))
        .setNode(new Node().setNodeType(""));

    LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated).then().statusCode(400);
  }

  @Test(description = "Verify node's will not be updated with empty start date (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeStartDateIsEmptyNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(newResponsibleId.toString())
        .setResponsibleId(newResponsibleId.toString())
        .setStartDate("")
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert responsible is not updated if ExternalId is missed
    verifyErrorResponse(response, 400, "Failed to execute bad request. Reason: "
        + "Missing input parameter: Start date");
  }

  @Test(description = "Verify node's will not be updated with missed start date (NegativeTest)", groups = "smoke")
  public void updateRegisteredNodeWithoutStartDateNegativeTest() {

    //Create new responsible
    AccountEntity responsible = new AccountEntityManager().createAccountEntity();
    Response responsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(responsible);
    Integer responsibleId = getAccountId(responsibleResponse);

    //LWA registration
    Node node = createDefaultNode();

    LightWeightAccount lwa = createDefaultLightWeightAccount(node)
        .setResponsibleId(responsibleId.toString())
        .setSettlementId(responsibleId.toString());

    LightWeightAccountServiceMethods.newNodeRegistration(lwa).then().statusCode(200);

    //Create new responsible
    AccountEntity newResponsible = new AccountEntityManager().createAccountEntity();
    Response newResponsibleResponse = AccountEntityServiceMethods.createAccountEntityRequest(newResponsible);
    Integer newResponsibleId = getAccountId(newResponsibleResponse);

    //Update lwa with new responsible
    LightWeightAccount lwaUpdated = new LightWeightAccount()
        .setExternalId(newResponsibleId.toString())
        .setResponsibleId(newResponsibleId.toString())
        .setNode(new Node().setNodeType(node.getNodeType()));

    Response response = LightWeightAccountServiceMethods.updateNodeRegistration(lwaUpdated);

    //Assert responsible is not updated if ExternalId is missed
    verifyErrorResponse(response, 400, "Failed to execute bad request. Reason: "
        + "Missing input parameter: Start date");
  }
}
