package org.ggolawski.sentry.poc.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.solr.security.AuthenticationPlugin;

public class DummyAuthPluginImpl extends AuthenticationPlugin {
  @Override
  public void init(Map<String, Object> arg0) {
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public boolean doAuthenticate(ServletRequest request, ServletResponse response, FilterChain filterChain) throws Exception {
    HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper((HttpServletRequest) request) {
      @Override
      public Principal getUserPrincipal() {
        String authorization = ((HttpServletRequest) request).getHeader("Authorization");
        if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
          // Authorization: Basic base64credentials
          String base64Credentials = authorization.substring("Basic".length()).trim();
          byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
          String credentials = new String(credDecoded, StandardCharsets.UTF_8);
          // credentials = username:password
          return new BasicUserPrincipal(credentials.split(":", 2)[0]);
        } else {
          return null;
        }
      }
    };
    filterChain.doFilter(wrapper, response);
    return true;
  }
}
