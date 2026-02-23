package dev.lunov.p2p_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class P2pServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(P2pServerApplication.class, args);
    }

}
