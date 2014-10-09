package com.rackspace.papi.service.proxy.httpcomponent;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.CommonHttpHeader;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.http.proxy.common.AbstractRequestProcessor;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Enumeration;

/**
 * Process a request to copy over header values, query string parameters, and
 * request body as necessary.
 */
class HttpComponentRequestProcessor extends AbstractRequestProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(HttpComponentRequestProcessor.class);

    private static final String ENCODING = "UTF-8";
    private final HttpServletRequest sourceRequest;
    private final URI targetHost;
    private final boolean rewriteHostHeader;
    private boolean isConfiguredChunked;

    public HttpComponentRequestProcessor(HttpServletRequest request, URI host, boolean rewriteHostHeader,
                                         boolean isConfiguredChunked) {
        this.sourceRequest = request;
        this.targetHost = host;
        this.rewriteHostHeader = rewriteHostHeader;
        this.isConfiguredChunked = isConfiguredChunked;
    }

    private void setRequestParameters(URIBuilder builder) throws URISyntaxException {
        Enumeration<String> names = sourceRequest.getParameterNames();

        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (StringUtilities.isBlank(name)) {
                continue;
            }
            String[] values = sourceRequest.getParameterValues(name);

            for (String value : values) {
                try {
                    builder.addParameter(name, URLDecoder.decode(value, ENCODING));
                } catch (IllegalArgumentException iae) {
                    LOG.warn("URL parameter could not be decoded, passing it as-is.", iae);
                    builder.addParameter(name, value);
                } catch (UnsupportedEncodingException ex) {
                    LOG.warn("URL parameter could not be decoded, passing it as-is.", ex);
                    builder.addParameter(name, value);
                }
            }
        }
    }

    public URI getUri(String target) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(target);
        setRequestParameters(builder);
        return builder.build();
    }

    /**
     * Scan header values and manipulate as necessary. Host header, if provided, may need to be updated.
     *
     * @param headerName
     * @param headerValue
     * @return
     */
    private String processHeaderValue(String headerName, String headerValue) {
        String result = headerValue;

        // In case the proxy host is running multiple virtual servers,
        // rewrite the Host header to ensure that we get content from
        // the correct virtual server
        if (rewriteHostHeader && headerName.equalsIgnoreCase(CommonHttpHeader.HOST.toString())) {
            result = targetHost.getHost() + ":" + targetHost.getPort();
        }

        return result;
    }

    /**
     * Copy header values from source request to the http method.
     *
     * @param method
     */
    private void setHeaders(HttpRequestBase method) {
        final Enumeration<String> headerNames = sourceRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();

            if (excludeHeader(headerName)) {
                continue;
            }

            final Enumeration<String> headerValues = sourceRequest.getHeaders(headerName);

            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                method.addHeader(headerName, processHeaderValue(headerName, headerValue));
            }
        }
    }

    /**
     * Process a base http request. Base http methods will not contain a message body.
     *
     * @param method
     * @return
     */
    public HttpRequestBase process(HttpRequestBase method) {
        setHeaders(method);
        return method;
    }

    /**
     * Process an entity enclosing http method. These methods can handle a request body.
     *
     * @param method
     * @return
     * @throws IOException
     */
    public HttpRequestBase process(HttpEntityEnclosingRequestBase method) throws IOException {
        final int contentLength = getEntityLength();
        setHeaders(method);
        method.setEntity(new InputStreamEntity(sourceRequest.getInputStream(), contentLength));
        return method;
    }

    private int getEntityLength() throws IOException {

        if (StringUtilities.nullSafeEqualsIgnoreCase(sourceRequest.getHeader("transfer-encoding"), "chunked") ||
                isConfiguredChunked) {
            return -1;
        } else {
            return ((MutableHttpServletRequest) sourceRequest).getRealBodyLength();
        }
    }

}
