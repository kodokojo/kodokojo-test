package io.kodokojo.test.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.kodokojo.commons.config.MicroServiceConfig;
import io.kodokojo.commons.config.RabbitMqConfig;
import io.kodokojo.commons.event.*;
import io.kodokojo.commons.model.ServiceInfo;
import io.kodokojo.commons.rabbitmq.RabbitMqConnectionFactory;
import io.kodokojo.test.MicroServiceTesterMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RabbitMqListerner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqListerner.class);

    public static void main(String[] args) {

        String uuid = UUID.randomUUID().toString();
        MicroServiceTesterMock mock = new MicroServiceTesterMock(new RabbitMqConfig() {

            @Override
            public String host() {
                return "192.168.1.17";
            }

            @Override
            public int port() {
                return 5672;
            }

            @Override
            public String businessExchangeName() {
                return "kodokojo.business";
            }

            @Override
            public String serviceQueueName() {
                return "fake";
            }

            @Override
            public String broadcastExchangeName() {
                return "kodokojo.broadcast";
            }

            @Override
            public String login() {
                return null;
            }

            @Override
            public String password() {
                return null;
            }
        },
                new MicroServiceConfig() {

                    @Override
                    public String name() {
                        return "fake";
                    }

                    @Override
                    public String uuid() {
                        return uuid;
                    }
                },
                new RabbitMqConnectionFactory() {

                },
                new JsonToEventConverter() {
                },
                (EventBuilderFactory) () -> new EventBuilder().setFrom("fake"),
                new ServiceInfo("fake", uuid, "1.0.0", "abcd", "test")
        );
        Gson gson = new GsonBuilder().registerTypeAdapter(Event.class, new GsonEventSerializer()).setPrettyPrinting().create();

        mock.connect();
    }


}
