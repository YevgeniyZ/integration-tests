package bluesky.restapi.methods.customerMethods;

import bluesky.restapi.base.BaseApiTest;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class BaseApiMethod {

  public static RequestSpecification requestSpecificationForCustomMethod = new RequestSpecBuilder()
      .addFilter(new AllureRestAssured())
      .setContentType(ContentType.JSON)
      .setAccept(ContentType.JSON)
      .build();

  public static RequestSpecification requestSpecification = given()
      .filter(new AllureRestAssured())
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .auth().oauth2(BaseApiTest.token).log().uri().and().log().method()
      .when().log().body(true)
      .then().log().all(true).request();
}
