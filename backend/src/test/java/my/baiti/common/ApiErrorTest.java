package my.baiti.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class ApiErrorTest {

  @Test
  void errorsHaveTheBlueprintShapeAndDoNotEchoQueryCredentials() throws Exception {
    var request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.setQueryString("synthetic-private-query=never-echo");
    var response = new MockHttpServletResponse();
    ApiError.write(request, response, 401, "Authentication failed");
    var json = JsonMapper.builder().build().readTree(response.getContentAsString());
    assertThat(json.get("type").asString()).isEqualTo("about:blank");
    assertThat(json.get("status").asInt()).isEqualTo(401);
    assertThat(json.get("detail").asString()).isEqualTo("Authentication failed");
    assertThat(json.get("instance").asString()).isEqualTo("/api/auth/login");
    assertThat(json.get("errors").isArray()).isTrue();
    assertThat(response.getContentAsString()).doesNotContain("never-echo");
    assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
  }

  @Test
  void fieldErrorsAndRateLimitHeadersUseTheSameWriter() throws Exception {
    var response = new MockHttpServletResponse();
    ApiError.write(
        new MockHttpServletRequest("POST", "/api/auth/login"),
        response,
        429,
        "Too many requests",
        List.of(new ApiError.InvalidField("email", "Invalid value")));
    assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    assertThat(response.getContentAsString())
        .contains("\"field\":\"email\"")
        .doesNotContain("password");
  }
}
