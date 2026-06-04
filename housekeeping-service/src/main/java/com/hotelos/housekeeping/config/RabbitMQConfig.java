package com.hotelos.housekeeping.config;

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
    public static final String ROOM_RELEASED_QUEUE = "housekeeping.room-released";
    public static final String ROOM_RELEASED_KEY = "event.room.released";
    public static final String ROOM_STATUS_KEY = "event.room.status";

    @Bean
    public TopicExchange hotelosExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Queue roomReleasedQueue() {
        return QueueBuilder.durable(ROOM_RELEASED_QUEUE).build();
    }

    @Bean
    public Binding roomReleasedBinding(Queue roomReleasedQueue, TopicExchange hotelosExchange) {
        return BindingBuilder.bind(roomReleasedQueue).to(hotelosExchange).with(ROOM_RELEASED_KEY);
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
