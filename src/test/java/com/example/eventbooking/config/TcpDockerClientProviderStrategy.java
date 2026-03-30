package com.example.eventbooking.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.TransportConfig;

import java.net.URI;
import java.time.Duration;

public class TcpDockerClientProviderStrategy extends DockerClientProviderStrategy {

    @Override
    public TransportConfig getTransportConfig() {
        return TransportConfig.builder()
                .dockerHost(URI.create("tcp://localhost:2375"))
                .build();
    }

    @Override
    public String getDescription() {
        return "tcp://localhost:2375";
    }

    @Override
    protected boolean isApplicable() {
        return true;
    }

    @Override
    public DockerClient getDockerClient() {
        var config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:2375")
                .withDockerTlsVerify(false)
                .build();

        var httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(URI.create("tcp://localhost:2375"))
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}
