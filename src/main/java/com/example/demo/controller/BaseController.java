package com.example.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


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
        ObjectNode obj = new ObjectMapper().createObjectNode();
        obj.put("local", 1);
        return obj;
    }

    @GetMapping("internal")
    public ObjectNode getInternal(@RequestHeader(value="tenant", defaultValue="unknown") String tenant) {
        log.info("Received internal call");
        if (discoveryClient.getInstances("service-2") != null &&
                discoveryClient.getInstances("service-2").size() == 0) {
            throw new IllegalStateException("no instances of service-2 were found");
        }
        ServiceInstance instance = discoveryClient.getInstances("service-2").get(0);
        discoveryClient.getServices()
                .forEach(_instance -> log.info("Instance {}: {}", _instance, findInstanceInfo(_instance)));
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(instance.getUri().toString() + "/api/v1/local")
                .addHeader("tenant", tenant)
                .build();
        ObjectNode node = getResponseBody(okHttpClient, request);
        node.put("internal", 1);
        return node;
    }

    @PostMapping("local")
    public ObjectNode postLocal(@RequestBody String body) {
        if (body == null) {
            body = "";
        }
        log.info("Received local post of {} bytes", body.getBytes().length);
        ObjectNode obj = new ObjectMapper().createObjectNode();
        obj.put("local", 1);
        return obj;
    }

    @GetMapping("instance/{instanceName}")
    public ObjectNode getInstance(@PathVariable String instanceName) {
        log.info("Received instance call for {}", instanceName);
        String instanceInfo = findInstanceInfo(instanceName);
        log.info("Instance info: {}", instanceInfo);
        ObjectNode node = new ObjectMapper().createObjectNode();
        node.put("instance", instanceInfo);
        return node;
    }

    @GetMapping("instances")
    public ObjectNode getInstances() {
        log.info("Received get instances");
        log.info("Instances info: {}", Arrays.toString(discoveryClient.getServices().toArray()));
        ObjectNode node = new ObjectMapper().createObjectNode();
        node.put("instances", Arrays.toString(discoveryClient.getServices().toArray()));
        return node;
    }

    @GetMapping("external")
    public ObjectNode getExternal(@RequestHeader(value="tenant", defaultValue="unknown") String tenant) {
        log.info("Received external call");
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://httpbin.org/get")
                .addHeader("tenant", tenant)
                .build();
        ObjectNode node = getResponseBody(okHttpClient, request);
        node.put("external", 1);
        return node;
    }

    @GetMapping("namespace")
    public ObjectNode getNamespace(@RequestHeader(value="tenant", defaultValue="unknown") String tenant) {
        log.info("Received namespace call");
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://service-3.n2.svc.cluster.local:8080/api/v1/local")
                .addHeader("tenant", tenant)
                .build();
        ObjectNode node = getResponseBody(okHttpClient, request);
        node.put("namespace", 1);
        return node;
    }

    private String findInstanceInfo(String instance) {
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(instance);
        if (serviceInstances.isEmpty()) {
            return "Not Found";
        }
        ServiceInstance serviceInstance = serviceInstances.get(0);
        return "Host: " + serviceInstance.getHost() + "\n" +
                "Port: " + serviceInstance.getPort() + "\n" +
                "ServiceId: " + serviceInstance.getServiceId() + "\n" +
                "URI: " + serviceInstance.getUri() + "\n" +
                "Metadata: " + serviceInstance.getMetadata().toString() + "\n" +
                "InstanceId: " + serviceInstance.getInstanceId() + "\n" +
                "Scheme: " + serviceInstance.getScheme() + "\n";
    }

    private ObjectNode getResponseBody(OkHttpClient okHttpClient, Request request) {
        ObjectNode node = new ObjectMapper().createObjectNode();
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
