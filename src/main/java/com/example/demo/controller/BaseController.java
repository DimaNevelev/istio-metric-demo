package com.example.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.io.IOException;


@RestController
@EnableSwagger2
@RequestMapping("/api/v1")
@AllArgsConstructor
@Slf4j
public class BaseController {

    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping("local")
    public ObjectNode getLocal() {
        log.info("Received local call");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode obj = mapper.createObjectNode();
        obj.put("local", 1);
        return obj;
    }

    @GetMapping("internal")
    public ObjectNode getInternal() {
        log.info("Received internal call");
        if (discoveryClient.getInstances("service-2") != null &&
                discoveryClient.getInstances("service-2").size() == 0) {
            throw new IllegalStateException("no instances of service-2 were found");
        }
        ServiceInstance instance = discoveryClient.getInstances("service-2").get(0);
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(instance.getUri().toString() + "/api/v1/local")
                .build();
        ObjectNode node = getResponseBody(okHttpClient, request);
        node.put("internal", 1);
        return node;
    }

    @GetMapping("external")
    public ObjectNode getExternal() {
        log.info("Received external call");
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://httpbin.org/get")
                .build();
        ObjectNode node = getResponseBody(okHttpClient, request);
        node.put("external", 1);
        return node;
    }

    private ObjectNode getResponseBody(OkHttpClient okHttpClient, Request request) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        Call call = okHttpClient.newCall(request);
        log.info("Executing the call");
        try (Response response = call.execute();
             ResponseBody responseBody = response.body()) {
            if(responseBody != null) {
                node.put("response", responseBody.string());
                return node;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        node.put("response", "Failed to get body");
        return node;
    }

}
