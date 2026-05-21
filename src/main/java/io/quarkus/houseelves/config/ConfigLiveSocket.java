package io.quarkus.houseelves.config;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import jakarta.inject.Inject;

@WebSocket(path = "/api/live")
public class ConfigLiveSocket {

    @Inject
    ObjectMapper mapper;

    @Inject
    ProjectDiscoveryService discoveryService;

    @OnOpen
    public String onOpen() {
        try {
            return mapper.writeValueAsString(Map.of(
                    "type", "connected",
                    "projects", discoveryService.discoverAll()));
        } catch (Exception e) {
            Log.warnf("Failed to build initial state: %s", e.getMessage());
            return "{\"type\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}
