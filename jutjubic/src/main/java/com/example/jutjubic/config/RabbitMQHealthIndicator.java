package com.example.jutjubic.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator za RabbitMQ konekciju.
 * 
 * Ovaj health indicator omogućava graceful degradation:
 * - Ako je RabbitMQ nedostupan, aplikacija i dalje radi (DOWN status ne sprečava aplikaciju)
 * - Load balancer može da vidi status MQ i da donese odluku o rutiranju
 * - Health check pokazuje da li je messaging dostupan, ali ne blokira aplikaciju
 */
@Component
public class RabbitMQHealthIndicator implements HealthIndicator {

    private final ConnectionFactory connectionFactory;

    public RabbitMQHealthIndicator(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Proverava zdravlje RabbitMQ konekcije.
     * Ako konekcija nije moguća, vraća DOWN status ali aplikacija nastavlja da radi.
     */
    @Override
    public Health health() {
        try {
            // Pokušaj da otvoriš kanal - ako uspe, RabbitMQ je dostupan
            var connection = connectionFactory.createConnection();
            var channel = connection.createChannel(false);
            
            if (channel != null && channel.isOpen()) {
                channel.close();
                connection.close();
                return Health.up()
                        .withDetail("rabbitmq", "Available")
                        .withDetail("connection", "Established")
                        .build();
            }
            
            return Health.down()
                    .withDetail("rabbitmq", "Channel not available")
                    .build();
                    
        } catch (Exception e) {
            // Graceful degradation: RabbitMQ nije dostupan, ali aplikacija nastavlja
            // Load balancer vidi ovaj status, ali replika ostaje u upotrebi za ostale operacije
            return Health.down()
                    .withDetail("rabbitmq", "Not available")
                    .withDetail("error", e.getMessage())
                    .withDetail("degradation", "Application functional without messaging")
                    .build();
        }
    }
}
