package com.rackspace.papi.components.datastore.distributed;

import org.openrepose.commons.utils.encoding.EncodingProvider;
import org.openrepose.commons.utils.proxy.RequestProxyService;

public class ClusterConfiguration {

    private RequestProxyService proxyService;
    private EncodingProvider encodingProvider;
    private ClusterView clusterView;

    public ClusterConfiguration(RequestProxyService proxyService, EncodingProvider encodingProvider, ClusterView clusterView) {
        this.proxyService = proxyService;
        this.encodingProvider = encodingProvider;
        this.clusterView = clusterView;
    }

    public RequestProxyService getProxyService() {
        return proxyService;
    }

    public EncodingProvider getEncodingProvider() {
        return encodingProvider;
    }

    public ClusterView getClusterView() {
        return clusterView;
    }
}
