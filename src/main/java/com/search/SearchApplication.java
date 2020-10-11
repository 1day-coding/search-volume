package com.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

@SpringBootApplication
@EnableHystrix
public class SearchApplication
{
    public static void main(String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }
}
