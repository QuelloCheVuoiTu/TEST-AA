package it.vroom.abruno.pistekartitalia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories; // ⭐ Import necessario

@SpringBootApplication
@ComponentScan(basePackages = "it.vroom.abruno")
@EnableMongoRepositories(basePackages = "it.vroom.abruno.repository")
public class PisteKartItaliaApplication {
    public static void main(String[] args) {
        SpringApplication.run(PisteKartItaliaApplication.class, args);
    }
}