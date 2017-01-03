/**
 * Kodo Kojo - ${project.description}
 * Copyright © 2017 Kodo Kojo (infos@kodokojo.io)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.kodokojo.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.*;
import io.kodokojo.commons.config.MicroServiceConfig;
import io.kodokojo.commons.config.RabbitMqConfig;
import io.kodokojo.commons.config.VersionConfig;
import io.kodokojo.commons.event.*;
import io.kodokojo.commons.model.ServiceInfo;
import io.kodokojo.commons.rabbitmq.RabbitMqConnectionFactory;
import io.kodokojo.commons.rabbitmq.RabbitMqEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class MicroServiceTesterMock extends RabbitMqEventBus implements JsonToEventConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroServiceTesterMock.class);
    private final RabbitMqConfig rabbitMqConfig;

    private List<EventReceiveCallback> callbacks;
    private Channel channel;


    public static MicroServiceTesterMock getInstance(RabbitMqConfig rabbitMqConfig) {
        requireNonNull(rabbitMqConfig, "rabbitMqConfig must be defined.");
        MicroServiceConfig microServiceConfig = new MicroServiceConfig() {
            String uuid = UUID.randomUUID().toString();

            @Override
            public String name() {
                return "mock";
            }

            @Override
            public String uuid() {
                return uuid;
            }
        };

        VersionConfig versionConfig = new VersionConfig() {
            @Override
            public String version() {
                return "1.0.0";
            }

            @Override
            public String gitSha1() {
                return "abcd1234";
            }

            @Override
            public String branch() {
                return "test";
            }

        };
        return new MicroServiceTesterMock(rabbitMqConfig, microServiceConfig, new RabbitMqConnectionFactory() {
        }, new JsonToEventConverter() {
        }, new DefaultEventBuilderFactory(microServiceConfig), new ServiceInfo(microServiceConfig.name(), microServiceConfig.uuid(), versionConfig.version(), versionConfig.gitSha1(), versionConfig.branch()));
    }

    public MicroServiceTesterMock(RabbitMqConfig rabbitMqConfig, MicroServiceConfig microServiceConfig, RabbitMqConnectionFactory rabbitMqConnectionFactory, JsonToEventConverter converter, EventBuilderFactory eventBuilderFactory, ServiceInfo serviceInfo) {
        super(rabbitMqConfig, rabbitMqConnectionFactory, converter, microServiceConfig, serviceInfo);
        this.rabbitMqConfig = rabbitMqConfig;
        requireNonNull(rabbitMqConfig, "rabbitMqConfig must be defined.");
        this.callbacks = new ArrayList<>();
    }

    @Override
    public void connect() {
        super.connect();

        try {

            channel = connection.createChannel();
            connection.addShutdownListener(cause -> LOGGER.error("RAbbitMq Shutdown !", cause));
            String from = "mock";

            DefaultConsumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    Event event = converter(new String(body, "UTF-8"));
                    Gson gson = new GsonBuilder().registerTypeAdapter(Event.class, new GsonEventSerializer()).setPrettyPrinting().create();
                    LOGGER.debug("Receive : \n{}", gson.toJson(event));
                    List<EventReceiveCallback> callbackList = new ArrayList<>(MicroServiceTesterMock.this.callbacks);
                    callbackList.forEach(callback -> {
                        callback.receiveEvent(event, MicroServiceTesterMock.this.eventBuilderFactory, MicroServiceTesterMock.this);
                    });
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            };


            channel.queueDeclare("mock", true, false, false, null);
            channel.basicConsume("mock", false, consumer);
            channel.queueDeclare("mock-local", true, false, false, null);
            channel.basicConsume("mock-local", false, consumer);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Event request(Event event, int duration, TimeUnit timeUnit) throws InterruptedException {
        throw new java.lang.UnsupportedOperationException("Unable to make a request from a Mock service.");
    }


    public void addCallback(EventReceiveCallback callback) {
        requireNonNull(callback, "callback must be defined.");
        this.callbacks.add(callback);
    }

    public interface EventReceiveCallback {

        void receiveEvent(Event event, EventBuilderFactory eventBuilderFactory, EventBus eventBus);

    }

}
