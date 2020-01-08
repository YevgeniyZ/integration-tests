package bluesky.restapi.methods.customerMethods;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static bluesky.restapi.helpers.FileProcessingHelper.getProperty;

public class EndPoints {

  private static String baseUrl;

  static {
    try {
      baseUrl = getProperty("baseApiUrl");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static final String accessTokenUrl = baseUrl + "/ecb-security/uaa/oauth/token";
  private static final String registrationUrl = baseUrl + "/ecb-security/uaa/register";
  private static final String createAccountEntityUrl = baseUrl + "/ecb-customer/svc/account-entity";
  private static final String lightWeightAccountApiBaseUrl = "/ecb-customer/svc/lwa/iot";
  private static final String lightWeightAccRegistrationUrl = lightWeightAccountApiBaseUrl + "/registration";
  private static final String accountStateUrl = baseUrl + "/ecb-customer/svc/account-state";
  private static final String lightWeightAccountBulkUploadUrl = baseUrl + "/ecb-customer/svc/lwa/bulk/iot/upload";
  private static final String retrieveNodeUrl =
      baseUrl + "/ecb-customer/svc/lwa/iot/" + "{externalID}" + "/" + "{nodeType}";
  private static final String downloadProcessedFileUrl =
      baseUrl + "/ecb-customer/svc/lwa/bulk/iot/" + "{RequestId}" + "/download";
  private static final String bulkRetrieveNodeUrl = baseUrl + "/ecb-customer/svc/lwa/searchable";
  private static final String addUserCapabilityUrl =
      baseUrl + "/ecb-foundation/svc/capability/add-user-capability/" + "{account-id}" + "/{cap-type-id}";
  private static final String deleteUserCapabilityUrl = baseUrl + "/ecb-foundation/svc/capability/delete-user"
      + "-capability/" + "{account-id}" + "/{cap-type-id}";

  public static String getBaseUrl() {

    return baseUrl;
  }
  public static String getHostName()  {
    InetAddress ip = null;
    try {
      ip = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    return ip.getCanonicalHostName();
  }

  public static String getAccessTokenUrl() {

    return accessTokenUrl;
  }

  public static String getRegistrationUrl() {

    return registrationUrl;
  }

  static String getCreateAccountEntityUrl() {

    return createAccountEntityUrl;
  }

  static String getLightWeightAccRegistrationUrl() {

    return lightWeightAccRegistrationUrl;
  }

  static String getCreateAccountState() {

    return accountStateUrl;
  }

  public static String getLightWeightAccountBulkUploadUrl() {

    return lightWeightAccountBulkUploadUrl;
  }

  static String getRetrieveNodeUrl() {

    return retrieveNodeUrl;
  }

  public static String getDownloadProcessedFileUrl() {

    return downloadProcessedFileUrl;
  }

  static String getBulkRetrieveNodeUrl() {

    return bulkRetrieveNodeUrl;
  }

  public static String getAddUserCapabilityUrl() {

    return addUserCapabilityUrl;
  }

  public static String getDeleteUserCapabilityUrl() {

    return deleteUserCapabilityUrl;
  }
}
