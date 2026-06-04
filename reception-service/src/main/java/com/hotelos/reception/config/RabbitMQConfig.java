package com.hotelos.reception.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "hotelos.exchange";
    public static final String ROOM_RELEASED_QUEUE = "reception.room-released";
    public static final String ROOM_RELEASED_KEY = "event.room.released";
    public static final String ROOM_STATUS_KEY = "event.room.status";
    public static final String GUEST_CHECKED_IN_KEY = "event.guest.checkedin";
    public static final String GUEST_CHECKED_OUT_KEY = "event.guest.checkedout";

    @Bean
    public TopicExchange hotelosExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
