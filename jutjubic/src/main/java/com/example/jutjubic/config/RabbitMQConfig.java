package com.example.jutjubic.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ konfiguracija za message queue funkcionalnost.
 * 
 * Klaster napomene:
 * - Sve replike aplikacije koriste isti RabbitMQ instance
 * - Ako RabbitMQ padne, aplikacija nastavlja da radi (graceful degradation)
 * - Circuit breaker štiti od kaskadnih padova
 */
@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.host:localhost}")
    private String host;

    @Value("${spring.rabbitmq.port:5672}")
    private int port;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.password:guest}")
    private String password;

    /**
     * Queue za video event notifikacije (JSON format)
     */
    @Bean
    public Queue uploadJsonQueue() {
        return new Queue("upload.json", true); // durable queue
    }

    /**
     * Queue za video event notifikacije (Protobuf format)
     */
    @Bean
    public Queue uploadProtoQueue() {
        return new Queue("upload.proto", true); // durable queue
    }

    /**
     * Legacy queue za video events (zadržano zbog kompatibilnosti)
     */
    @Bean
    public Queue videoEventsQueue() {
        return new Queue("video.events", true); // durable queue
    }

    /**
     * Connection factory sa timeout postavkama za resilience
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        
        // Timeout i recovery settings za klaster resilience
        factory.setConnectionTimeout(5000);
        factory.setRequestedHeartBeat(30);
        
        return factory;
    }

    /**
     * RabbitTemplate za slanje poruka sa JSON konverterom
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
