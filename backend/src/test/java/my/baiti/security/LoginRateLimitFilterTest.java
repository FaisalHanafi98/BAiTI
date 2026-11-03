package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginRateLimitFilterTest {

  @Test
  void unknownLengthBodyIsStillBoundedBeforeTheApplicationReceivesIt() throws Exception {
    var request =
        new MockHttpServletRequest("POST", "/api/auth/login") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    request.setContent("x".repeat(9000).getBytes(StandardCharsets.UTF_8));
    var response = new MockHttpServletResponse();
    new LoginRateLimitFilter(new LoginRateLimiter(5, 30))
        .doFilter(
            request,
            response,
            (input, output) -> {
              throw new AssertionError("Oversized body reached the application");
            });
    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentType()).startsWith("application/problem+json");
  }

  @Test
  void boundedBodyIsPreservedForTheJsonReader() throws Exception {
    var request = new MockHttpServletRequest("POST", "/api/auth/login");
    byte[] body = "{\"synthetic\":\"value\"}".getBytes(StandardCharsets.UTF_8);
    request.setContent(body);
    var response = new MockHttpServletResponse();
    new LoginRateLimitFilter(new LoginRateLimiter(5, 30))
        .doFilter(
            request,
            response,
            (input, output) -> assertThat(input.getInputStream().readAllBytes()).isEqualTo(body));
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
