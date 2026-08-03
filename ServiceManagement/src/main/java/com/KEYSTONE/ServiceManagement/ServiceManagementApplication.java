package com.KEYSTONE.ServiceManagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServiceManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceManagementApplication.class, args);
    }

}
