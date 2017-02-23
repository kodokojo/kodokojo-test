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
package io.kodokojo.test.bdd.stage;


import com.github.dockerjava.api.DockerClient;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.AfterScenario;
import com.tngtech.jgiven.annotation.Hidden;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;
import com.tngtech.jgiven.annotation.ScenarioState;
import io.kodokojo.commons.config.MicroServiceConfig;
import io.kodokojo.commons.config.RabbitMqConfig;
import io.kodokojo.commons.config.RedisConfig;
import io.kodokojo.commons.config.VersionConfig;
import io.kodokojo.commons.event.DefaultEventBuilderFactory;
import io.kodokojo.commons.event.EventBuilderFactory;
import io.kodokojo.commons.event.EventBus;
import io.kodokojo.commons.event.JsonToEventConverter;
import io.kodokojo.commons.model.ServiceInfo;
import io.kodokojo.commons.rabbitmq.RabbitMqConnectionFactory;
import io.kodokojo.commons.rabbitmq.RabbitMqEventBus;
import io.kodokojo.test.DockerService;
import io.kodokojo.test.DockerTestApplicationBuilder;
import io.kodokojo.test.DockerTestSupport;
import io.kodokojo.test.MicroServiceTesterMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ApplicationGiven<SELF extends ApplicationGiven<?>> extends Stage<SELF> implements DockerTestApplicationBuilder, RabbitMqConnectionFactory, JsonToEventConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationGiven.class);

    private static final Map<String, String> USER_PASSWORD = new HashMap<>();

    static {
        USER_PASSWORD.put("jpthiery", "jpascal");
    }

    @ProvidedScenarioState
    public DockerTestSupport dockerTestSupport;

    @ProvidedScenarioState
    DockerClient dockerClient;

    @ProvidedScenarioState
    String redisHost;

    @ProvidedScenarioState
    int redisPort;

    @ProvidedScenarioState
    DockerService rabbitMq;

    @ProvidedScenarioState(resolution = ScenarioState.Resolution.NAME)
    Module eventBusModule;

    @ProvidedScenarioState(resolution = ScenarioState.Resolution.NAME)
    Module redisModule;

    @ProvidedScenarioState
    MicroServiceTesterMock microServiceTesterMock;

    public SELF all_middlewares_are_ready(@Hidden DockerTestSupport dockerTestSupport, @Hidden MicroServiceConfig microServiceConfig) {
        this.dockerTestSupport = dockerTestSupport;
        event_bus_is_available(dockerTestSupport, microServiceConfig);
        redis_is_started(dockerTestSupport);
        return self();
    }

    public SELF event_bus_is_available(@Hidden DockerTestSupport dockerTestSupport, @Hidden MicroServiceConfig microServiceConfig) {
        this.dockerTestSupport = dockerTestSupport;
        rabbitMq = startRabbitMq(dockerTestSupport).get();

        RabbitMqConfig rabbitMqConfig = new RabbitMqConfig() {
            @Override
            public String host() {
                return rabbitMq.getHost();
            }

            @Override
            public Integer port() {
                return rabbitMq.getPort();
            }

            @Override
            public String businessExchangeName() {
                return "kodokojo.business";
            }

            @Override
            public String serviceQueueName() {
                return microServiceConfig.name();
            }

            @Override
            public String broadcastExchangeName() {
                return "kodokojo.broadcast";
            }

            @Override
            public String deadLetterExchangeName() {
                return "deadEx";
            }

            @Override
            public String deadLetterQueueName() {
                return "deadQu";
            }

            @Override
            public String login() {
                return null;
            }

            @Override
            public String password() {
                return null;
            }

            @Override
            public Integer maxRedeliveryMessageCount() {
                return null;
            }

            @Override
            public String virtualHost() {
                return "/";
            }
        };
        eventBusModule = new AbstractModule() {
            @Override
            protected void configure() {
                //
            }

            @Singleton
            @Provides
            EventBuilderFactory provideEventBuilderFactory() {
                return new DefaultEventBuilderFactory(microServiceConfig);
            }

            @Provides
            @Singleton
            RabbitMqConfig provideRabbitMqConfig() {
                return rabbitMqConfig;
            }


            @Provides
            @Singleton
            RabbitMqEventBus provideRabbitMqEventBus(RabbitMqConfig rabbitMqConfig, EventBuilderFactory eventBuilderFactory, VersionConfig versionConfig) {
                ServiceInfo serviceInfo = new ServiceInfo(microServiceConfig.name(), microServiceConfig.uuid(), versionConfig.version(), versionConfig.gitSha1(), versionConfig.branch());
                RabbitMqEventBus rabbitMqEventBus = new RabbitMqEventBus(rabbitMqConfig, ApplicationGiven.this, ApplicationGiven.this, microServiceConfig, serviceInfo);
                rabbitMqEventBus.connect();
                return rabbitMqEventBus;
            }

            @Provides
            @Singleton
            EventBus provideEventBus(RabbitMqEventBus rabbitMqEventBus) {
                return rabbitMqEventBus;
            }

        };
        microServiceTesterMock = MicroServiceTesterMock.getInstance(rabbitMqConfig);
        microServiceTesterMock.connect();
        return self();
    }

    public SELF redis_is_started(@Hidden DockerTestSupport dockerTestSupport) {
        this.dockerTestSupport = dockerTestSupport;
        DockerService service = startRedis(dockerTestSupport).get();
        redisHost = service.getHost();
        redisPort = service.getPortDefinition().getContainerPort();

        redisModule = new AbstractModule() {
            @Override
            protected void configure() {
                //
            }

            @Provides
            @Singleton
            RedisConfig provideRedisConfig() {
                return new RedisConfig() {
                    @Override
                    public String host() {
                        return redisHost;
                    }

                    @Override
                    public Integer port() {
                        return redisPort;
                    }

                    @Override
                    public String password() {
                        return null;
                    }
                };
            }
        };

        return self();
    }


    @AfterScenario
    public void tear_down() {
        /*
        if (httpEndpoint != null) {
            httpEndpoint.stop();
            httpEndpoint = null;
        }
        */

    }

}
