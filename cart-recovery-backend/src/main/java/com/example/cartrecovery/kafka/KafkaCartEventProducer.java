package com.example.cartrecovery.kafka;


import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaCartEventProducer {

    private static final String TOPIC="abandoned-cart-events";

    private final KafkaTemplate<String, AbandonedCartEvent> kafkaTemplate;

    public KafkaCartEventProducer(
            KafkaTemplate<String, AbandonedCartEvent> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendAbandonedCartEvent(AbandonedCartEvent cartEvent){

        kafkaTemplate.send(
                TOPIC,
                String.valueOf(cartEvent.getCartId()),
                cartEvent
        );
        System.out.println("kafa Abandoned cart published for cart" +cartEvent.getCartId());
    }
}
