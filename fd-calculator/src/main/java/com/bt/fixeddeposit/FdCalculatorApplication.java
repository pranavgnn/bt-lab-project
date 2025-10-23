package com.bt.fixeddeposit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FdCalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FdCalculatorApplication.class, args);
    }
}
