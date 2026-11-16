package app.lexo;

import app.lexo.common.email.RabbitConfig;
import app.lexo.common.email.EmailTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestPropertySource(properties = {"eureka.client.enabled=false", "management.health.mail.enabled=false",
        "spring.rabbitmq.listener.simple.retry.initial-interval=50ms",
        "spring.rabbitmq.listener.simple.retry.multiplier=1"})
@DisplayName("E-mail queue consumer with a real RabbitMQ broker")
class EmailQueueBehaviorTest {

    private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3-alpine");

    static {
        RABBIT.start();
    }

    @DynamicPropertySource
    static void rabbit(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private RabbitProperties rabbitProperties;

    @BeforeEach
    void emptyDeadLetterQueue() {
        reset(mailSender);
        rabbit.execute(channel -> channel.queuePurge(RabbitConfig.DLQ));
    }

    private static SimpleMailMessage mailTo(String recipient) {
        return argThat((SimpleMailMessage m) -> m != null && m.getTo() != null && m.getTo()[0].equals(recipient));
    }

    private EmailTask task(String recipient) {
        return new EmailTask("CONVITE", recipient, "Assunto " + recipient, "Corpo para " + recipient);
    }

    private void enqueue(EmailTask task) {
        rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, task);
    }

    private Message deadLetterWith(String text) {
        List<Message> found = new ArrayList<>();
        await().atMost(Duration.ofSeconds(30)).until(() -> {
            Message m = rabbit.receive(RabbitConfig.DLQ);
            if (m != null && new String(m.getBody(), StandardCharsets.UTF_8).contains(text)) {
                found.add(m);
            }
            return !found.isEmpty();
        });
        return found.get(0);
    }

    @Test
    @DisplayName("a task on the queue is sent as a plain e-mail from the configured sender")
    void sendsTheEmail() {
        String recipient = UUID.randomUUID() + "@cliente.com";

        enqueue(task(recipient));

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(30_000)).send(sent.capture());
        SimpleMailMessage mail = sent.getValue();
        assertThat(mail.getFrom()).isEqualTo("nao-responda@lexo.app");
        assertThat(mail.getTo()).containsExactly(recipient);
        assertThat(mail.getSubject()).isEqualTo("Assunto " + recipient);
        assertThat(mail.getText()).isEqualTo("Corpo para " + recipient);
    }

    @Test
    @DisplayName("a task the mail server cannot take is retried up to the configured limit and then goes to the dead-letter queue")
    void retriesAServerFailureThenDeadLetters() {
        String recipient = UUID.randomUUID() + "@cliente.com";
        doThrow(new MailSendException("smtp fora do ar")).when(mailSender).send(any(SimpleMailMessage.class));

        enqueue(task(recipient));

        Message dead = deadLetterWith(recipient);
        assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).contains("Corpo para " + recipient);
        verify(mailSender, times(rabbitProperties.getListener().getSimple().getRetry().getMaxAttempts()))
                .send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("a task that can never be sent goes to the dead-letter queue after one attempt")
    void doesNotRetryAFailureThatCannotGoAway() {
        String recipient = "sem-endereco-" + UUID.randomUUID();
        doThrow(new MailParseException("endereco invalido")).when(mailSender).send(mailTo(recipient));

        enqueue(task(recipient));

        deadLetterWith(recipient);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("a message that is not a valid task goes to the dead-letter queue without a send attempt")
    void deadLettersAMessageThatIsNotATask() {
        String marker = UUID.randomUUID().toString();
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        Message garbage = new Message(("{isto nao e json " + marker).getBytes(StandardCharsets.UTF_8), properties);

        rabbit.send(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, garbage);

        deadLetterWith(marker);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("after a failure the consumer keeps sending the next task")
    void keepsGoingAfterAFailure() {
        String failing = UUID.randomUUID() + "@falha.com";
        String good = UUID.randomUUID() + "@cliente.com";
        doThrow(new MailSendException("smtp fora do ar")).when(mailSender)
                .send(mailTo(failing));

        enqueue(task(failing));
        enqueue(task(good));

        verify(mailSender, timeout(30_000)).send(mailTo(good));
        deadLetterWith(failing);
    }
}
