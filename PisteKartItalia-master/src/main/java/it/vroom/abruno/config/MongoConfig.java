package it.vroom.abruno.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {

    @Bean
    public MongoClient mongoClient() {
        // Per MongoDB Atlas
        return MongoClients.create("mongodb+srv://abruno19:GbAe654321!@cluster0.b50e9di.mongodb.net/");

        // Per MongoDB locale
        // return MongoClients.create("mongodb://localhost:27017");
    }
}