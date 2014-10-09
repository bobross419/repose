package com.rackspace.papi.service.headers.response;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.commons.utils.servlet.http.RouteDestination;
import com.rackspace.papi.service.headers.common.ViaHeaderBuilder;
import javax.servlet.http.HttpServletRequest;

public interface ResponseHeaderService {
    void updateConfig(ViaHeaderBuilder viaHeaderBuilder, LocationHeaderBuilder locationHeaderBuilder);

    void setVia(MutableHttpServletRequest request, MutableHttpServletResponse response);
    void fixLocationHeader(HttpServletRequest originalRequest, MutableHttpServletResponse response, RouteDestination destination, String destinationLocationUri, String requestedContext);
}
