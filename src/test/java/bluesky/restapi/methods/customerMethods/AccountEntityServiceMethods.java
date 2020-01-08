package bluesky.restapi.methods.customerMethods;

import bluesky.restapi.models.AccountEntity;
import bluesky.restapi.models.AccountState;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;


public class AccountEntityServiceMethods extends BaseApiMethod{


    @Step("Create account entity api request")
    public static Response createAccountEntityRequest(AccountEntity accountEntity){
        return given()
                .spec(requestSpecification)
                .body(accountEntity)
                .when().post(EndPoints.getCreateAccountEntityUrl())
                .then().extract().response();
    }

    @Step("Create account state api request")
    public static Response createAccountStateRequest(AccountState accountState){
        return given()
                .spec(requestSpecification)
                .body(accountState)
                .when().post(EndPoints.getCreateAccountState())
                .then().extract().response();
    }

    @Step("Update account status api request")
    public static Response accountStateUpdateRequest(AccountState accountState){
        return  given()
                .spec(requestSpecification)
                .body(accountState)
                .when().put(EndPoints.getCreateAccountState())
                .then().extract().response();

    }

    @Step("Get accountId value")
    public static Integer getAccountId(Response response){
        Integer accountId = response.jsonPath().get("accountId");
        Allure.addAttachment("Value", "text/plain", accountId.toString());
        return accountId;
    }

}


