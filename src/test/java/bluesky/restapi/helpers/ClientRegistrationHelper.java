package bluesky.restapi.helpers;

import bluesky.restapi.methods.customerMethods.EndPoints;
import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.response.Response;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;


public class ClientRegistrationHelper {

    private static String clientId;
    private static String clientSecret;


    public static void tokenRegistration(){

        RestAssured.useRelaxedHTTPSValidation();
        Response response = given().log().ifValidationFails()
                .contentType(JSON)
                .body(getRegistrationObject()).log().ifValidationFails()
                .post(EndPoints.getRegistrationUrl())
                .then().statusCode(200).log().ifValidationFails(LogDetail.ALL).extract().response();

        clientId = response.getBody().jsonPath().get("client_id");
        clientSecret = response.getBody().jsonPath().get("client_secret");
    }

    //custom token registration
    static HashMap<String, String> getRegistrationData(String softwareId){

        RestAssured.useRelaxedHTTPSValidation();
        Response response = given().log().ifValidationFails()
                .contentType(JSON)
                .body(getRegistrationObject().setClient_name(softwareId).setSoftware_id(softwareId)).log().ifValidationFails()
                .post(EndPoints.getRegistrationUrl())
                .then().statusCode(200).log().ifValidationFails(LogDetail.ALL)
                .extract().response();

        String clientId = response.getBody().jsonPath().get("client_id");
        String clientSecret = response.getBody().jsonPath().get("client_secret");
        return new HashMap<String, String>(){{
            put("client_id",clientId);
            put("client_secret",clientSecret);
        }};
    }


    private static RegistrationObject getRegistrationObject(){
        return new RegistrationObject()
                .setGrant_types(new ArrayList<>(Arrays.asList("password",
                        "refresh_token")))
                .setMutual_tls_sender_constrained_access_tokens(false)
                .setRegistration_client_uri(EndPoints.getRegistrationUrl())
                .setClient_name("JavaTest")
                .setResponse_types(new ArrayList<>(Collections.singletonList("token")))
                .setScope("read write")
                .setSoftware_id("JavaTest");
    }

    public static class RegistrationObject{

        ArrayList<String> grant_types = new ArrayList<>();
        private boolean mutual_tls_sender_constrained_access_tokens;
        private String registration_client_uri;
        private String client_name;
        ArrayList<String> response_types = new ArrayList<>();
        private String scope;
        private String software_id;

        public ArrayList<String> getGrant_types() {
            return grant_types;
        }

        public RegistrationObject setGrant_types(ArrayList<String> grant_types) {
            this.grant_types = grant_types;
            return this;
        }

        public ArrayList<String> getResponse_types() {
            return response_types;
        }

        public RegistrationObject setResponse_types(ArrayList<String> response_types) {
            this.response_types = response_types;
            return this;
        }

        // Getter Methods

        public boolean getMutual_tls_sender_constrained_access_tokens() {
            return mutual_tls_sender_constrained_access_tokens;
        }

        public String getRegistration_client_uri() {
            return registration_client_uri;
        }

        public String getClient_name() {
            return client_name;
        }

        public String getScope() {
            return scope;
        }

        public String getSoftware_id() {
            return software_id;
        }

        // Setter Methods

        public RegistrationObject setMutual_tls_sender_constrained_access_tokens( boolean mutual_tls_sender_constrained_access_tokens ) {
            this.mutual_tls_sender_constrained_access_tokens = mutual_tls_sender_constrained_access_tokens;
            return this;
        }

        public RegistrationObject setRegistration_client_uri( String registration_client_uri ) {
            this.registration_client_uri = registration_client_uri;
            return this;
        }

        public RegistrationObject setClient_name( String client_name ) {
            this.client_name = client_name;
            return this;
        }

        public RegistrationObject setScope( String scope ) {
            this.scope = scope;
            return this;
        }

        public RegistrationObject setSoftware_id( String software_id ) {
            this.software_id = software_id;
            return this;
        }
    }

    public static String getClientId() {
        return clientId;
    }

    public static String getClientSecret() {
        return clientSecret;
    }
}