package my.baiti.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import my.baiti.common.ApiError;
import org.springframework.web.filter.OncePerRequestFilter;

public final class LoginRateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_AUTH_BODY = 8192;
  private final LoginRateLimiter limits;

  public LoginRateLimitFilter(LoginRateLimiter limits) {
    this.limits = limits;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    boolean login = path.equals("/api/auth/login");
    boolean refresh = path.equals("/api/auth/refresh");
    if (request.getMethod().equals("POST") && (login || refresh)) {
      if (!limits.allowIp(request.getRemoteAddr(), refresh)) {
        ApiError.write(request, response, 429, "Too many requests");
        return;
      }
      if (request.getContentLengthLong() > MAX_AUTH_BODY) {
        ApiError.write(request, response, 413, "Request too large");
        return;
      }
      byte[] body = request.getInputStream().readNBytes(MAX_AUTH_BODY + 1);
      if (body.length > MAX_AUTH_BODY) {
        ApiError.write(request, response, 413, "Request too large");
        return;
      }
      chain.doFilter(new BoundedBody(request, body), response);
      return;
    }
    chain.doFilter(request, response);
  }

  private static final class BoundedBody extends HttpServletRequestWrapper {
    private final byte[] body;

    private BoundedBody(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body;
    }

    @Override
    public int getContentLength() {
      return body.length;
    }

    @Override
    public long getContentLengthLong() {
      return body.length;
    }

    @Override
    public BufferedReader getReader() {
      return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public ServletInputStream getInputStream() {
      var bytes = new ByteArrayInputStream(body);
      return new ServletInputStream() {
        @Override
        public int read() {
          return bytes.read();
        }

        @Override
        public int read(byte[] target, int offset, int length) {
          return bytes.read(target, offset, length);
        }

        @Override
        public boolean isFinished() {
          return bytes.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
          throw new IllegalStateException("Authentication bodies use synchronous reads");
        }
      };
    }
  }
}
