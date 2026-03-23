package com.bankingservice.api_gateway;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;

@RestController
public class GatewayProxyController {

    private static final String CUSTOMER_GATEWAY_PREFIX = "/api/customers";
    private static final String ACCOUNT_GATEWAY_PREFIX = "/api/accounts";

    private final RestTemplate restTemplate;

    public GatewayProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @RequestMapping(path = {CUSTOMER_GATEWAY_PREFIX, CUSTOMER_GATEWAY_PREFIX + "/**"})
    public ResponseEntity<byte[]> proxyCustomerService(HttpServletRequest request,
                                                       @RequestHeader HttpHeaders headers,
                                                       @RequestBody(required = false) byte[] body) {
        return forward("http://customer-service", CUSTOMER_GATEWAY_PREFIX, request, headers, body);
    }

    @RequestMapping(path = {ACCOUNT_GATEWAY_PREFIX, ACCOUNT_GATEWAY_PREFIX + "/**"})
    public ResponseEntity<byte[]> proxyAccountService(HttpServletRequest request,
                                                      @RequestHeader HttpHeaders headers,
                                                      @RequestBody(required = false) byte[] body) {
        return forward("http://account-service", ACCOUNT_GATEWAY_PREFIX, request, headers, body);
    }

    private ResponseEntity<byte[]> forward(String serviceBaseUrl,
                                           String gatewayPrefix,
                                           HttpServletRequest request,
                                           HttpHeaders incomingHeaders,
                                           byte[] body) {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        URI targetUri = buildTargetUri(serviceBaseUrl, gatewayPrefix, request);
        HttpEntity<byte[]> entity = new HttpEntity<>(body, copyRequestHeaders(incomingHeaders));

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(targetUri, method, entity, byte[].class);
            return new ResponseEntity<>(response.getBody(), copyResponseHeaders(response.getHeaders()), response.getStatusCode());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode())
                    .headers(copyResponseHeaders(ex.getResponseHeaders()))
                    .body(ex.getResponseBodyAsByteArray());
        }
    }

    private URI buildTargetUri(String serviceBaseUrl, String gatewayPrefix, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String downstreamPath = requestUri.length() > gatewayPrefix.length()
                ? requestUri.substring(gatewayPrefix.length())
                : "";

        String targetPath = StringUtils.hasText(downstreamPath) ? gatewayPrefix.substring(4) + downstreamPath : gatewayPrefix.substring(4);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(serviceBaseUrl + targetPath);
        if (StringUtils.hasText(request.getQueryString())) {
            builder.query(request.getQueryString());
        }
        return builder.build(true).toUri();
    }

    private HttpHeaders copyRequestHeaders(HttpHeaders incomingHeaders) {
        HttpHeaders outgoingHeaders = new HttpHeaders();
        incomingHeaders.forEach((name, values) -> {
            if (!HttpHeaders.HOST.equalsIgnoreCase(name) && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
                outgoingHeaders.put(name, new ArrayList<>(values));
            }
        });
        return outgoingHeaders;
    }

    private HttpHeaders copyResponseHeaders(HttpHeaders sourceHeaders) {
        HttpHeaders targetHeaders = new HttpHeaders();
        if (sourceHeaders == null) {
            return targetHeaders;
        }

        sourceHeaders.forEach((name, values) -> {
            if (!HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(name)) {
                targetHeaders.put(name, new ArrayList<>(values));
            }
        });
        return targetHeaders;
    }
}


