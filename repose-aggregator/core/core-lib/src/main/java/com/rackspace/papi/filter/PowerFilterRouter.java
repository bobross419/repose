package com.rackspace.papi.filter;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 *
 * @author ddaley
 */
public interface PowerFilterRouter {

    void route(MutableHttpServletRequest servletRequest, MutableHttpServletResponse servletResponse) throws IOException, ServletException, URISyntaxException;
    void initialize(ReposeCluster domain, Node localhost, ServletContext context, String defaultDst) throws PowerFilterChainException;
    
}
