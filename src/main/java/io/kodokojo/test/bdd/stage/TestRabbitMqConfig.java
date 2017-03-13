package io.kodokojo.test.bdd.stage;

import io.kodokojo.commons.config.RabbitMqConfig;
import io.kodokojo.test.DockerService;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isBlank;

public class TestRabbitMqConfig implements RabbitMqConfig {

    private final DockerService rabbitMq;

    private final String microServiceName;

    public TestRabbitMqConfig(DockerService rabbitMq, String microServiceName) {
        requireNonNull(rabbitMq, "rabbitMq must be defined.");
        if (isBlank(microServiceName)) {
            throw new IllegalArgumentException("microServiceName must be defined.");
        }
        this.rabbitMq = rabbitMq;
        this.microServiceName = microServiceName;
    }

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
        return microServiceName;
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
}
