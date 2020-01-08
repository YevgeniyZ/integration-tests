package bluesky.restapi.helpers;


import bluesky.restapi.methods.customerMethods.EndPoints;
import io.restassured.http.ContentType;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static bluesky.restapi.helpers.FileProcessingHelper.getProperty;
import static io.restassured.RestAssured.given;


public class TokenAccessHelper {

  private static Map<String, String> queryParameters = new HashMap<String, String>(){{
    put("client_id", ClientRegistrationHelper.getClientId());
    put("client_secret", ClientRegistrationHelper.getClientSecret());
    put("grant_type","password");
    try {
      put("username", getProperty("username"));
      put("password", getProperty("password"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    put("override","false");
    put("scope","read write");
  }};

  public static String getAuthorizationToken() {

    //Second step - get an access token
    return given().log().ifValidationFails()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .queryParams(queryParameters)
            .body("").log().ifValidationFails()
            .when()
            .post(EndPoints.getAccessTokenUrl())
            .jsonPath().get("access_token");
  }

  public static String getCustomAuthToken(String softwareId, String userName, String password){
   HashMap<String, String> client_secret = ClientRegistrationHelper.getRegistrationData(softwareId);

    queryParameters.put("client_id", client_secret.get("client_id"));
    queryParameters.put("client_secret", client_secret.get("client_secret"));
    queryParameters.put("username", userName);
    queryParameters.put("password", password);

    return given().log().ifValidationFails()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .queryParams(queryParameters)
            .body("").log().ifValidationFails()
            .when().log().everything(true)
            .post(EndPoints.getAccessTokenUrl()).then().log().everything(true).extract()
            .jsonPath().get("access_token");
  }
}