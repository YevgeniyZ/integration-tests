package bluesky.restapi.methods;

import bluesky.restapi.methods.customerMethods.BaseApiMethod;
import bluesky.restapi.methods.customerMethods.EndPoints;
import bluesky.restapi.models.UserCapabilities;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class UserCapabilityServiceMethods extends BaseApiMethod {

  @Step("Add user capability api request")
  public Response addUserCapabilityRequest(UserCapabilities userCapabilities) {

    return given()
        .spec(requestSpecification)
        .body(userCapabilities)
        .when()
        .post(EndPoints.getAddUserCapabilityUrl(), userCapabilities.getUserId(), userCapabilities.getCapabilityTypeId())
        .then().extract().response();
  }

  @Step("Delete user capability api request")
  public Response deleteUserCapabilityRequest(UserCapabilities userCapabilities) {

    return given()
        .spec(requestSpecification)
        .when()
        .delete(EndPoints.getDeleteUserCapabilityUrl(), userCapabilities.getUserId(), userCapabilities.getCapabilityTypeId())
        .then().extract().response();
  }
}
