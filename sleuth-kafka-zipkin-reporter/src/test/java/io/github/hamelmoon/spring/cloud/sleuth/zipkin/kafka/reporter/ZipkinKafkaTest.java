package io.github.hamelmoon.spring.cloud.sleuth.zipkin.kafka.reporter;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.zipkin2.ZipkinAutoConfiguration;
import zipkin2.Endpoint;
import zipkin2.Span;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.kafka11.KafkaSender;

public class ZipkinKafkaTest {
  private final WebApplicationContextRunner contextRunner =
      new WebApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  ZipkinAutoConfiguration.class,
                  KafkaAutoConfiguration.class,
                  ZipkinKafkaReporterAutoConfigure.class,
                  TraceAutoConfiguration.class));

  public static final Endpoint FRONTEND = Endpoint.newBuilder()
      .serviceName("frontend")
      .ip("127.0.0.1")
      .build();
  public static final Endpoint BACKEND = Endpoint.newBuilder()
      .serviceName("backend")
      .ip("192.168.99.101")
      .port(9000)
      .build();

  public static final Span CLIENT_SPAN = Span.newBuilder()
      .traceId("7180c278b62e8f6a216a2aea45d08fc9")
      .parentId("6b221d5bc9e6496c")
      .id("5b4185666d50f68b")
      .name("get")
      .kind(Span.Kind.CLIENT)
      .localEndpoint(FRONTEND)
      .remoteEndpoint(BACKEND)
      .timestamp(1472470996199000L)
      .duration(207000L)
      .addAnnotation(1472470996238000L, "foo")
      .putTag("http.path", "/api")
      .putTag("clnt/finagle.version", "6.45.0")
      .build();

  @Test
  public void kafkaSenderShouldNotFoundByDefault() {
    this.contextRunner.run(
        context -> {
          assertThat(context).doesNotHaveBean(KafkaSender.class);
        });
  }

  @Test
  public void zipkinKafkaSenderConfigShouldBeFoundWithDefaultValues() {
    this.contextRunner
        .withPropertyValues("spring.zipkin.sender.type=kafka")
        .run(
            context -> {
              assertThat(context).hasSingleBean(ZipkinKafkaReporterConfig.class);
              assertThat(context.getBean(ZipkinKafkaReporterConfig.class))
                  .extracting(ZipkinKafkaReporterConfig::getProducer)
                  .isNotNull()
                  .extracting("bootstrapServers")
                  .isNotNull()
                  .hasSize(1)
                  .first()
                  .asList()
                  .contains("localhost:9092")
                  .hasSize(1);
            });
  }

  @Test
  public void zipkinKafkaSenderConfigShouldBeFoundWithCustomValues() {
    this.contextRunner
        .withPropertyValues(
            "spring.zipkin.sender.type=kafka",
            "spring.zipkin.kafka.topic=zipkin-test",
            "spring.zipkin.kafka.producer.bootstrapServers=localhost:9092")
        .run(
            context -> {
              assertThat(context).hasSingleBean(ZipkinKafkaReporterConfig.class);
              assertThat(context.getBean(ZipkinKafkaReporterConfig.class))
                  .hasFieldOrPropertyWithValue("topic", "zipkin-test")
                  .extracting(ZipkinKafkaReporterConfig::getProducer)
                  .isNotNull()
                  .extracting("bootstrapServers")
                  .isNotNull()
                  .hasSize(1)
                  .first()
                  .asList()
                  .contains("localhost:9092")
                  .hasSize(1);
            });
  }

  @Test
  public void kafkaSenderShouldBeFoundWithProperties() {
    this.contextRunner
        .withPropertyValues(
            "spring.zipkin.sender.type=kafka",
            "spring.zipkin.kafka.topic=zipkin-test",
            "spring.zipkin.kafka.producer.bootstrapServers=localhost:9092")
        .run(
            context -> {
              assertThat(context).hasSingleBean(KafkaSender.class);
              assertThat(context.getBean(KafkaSender.class).toString())
                  .contains("bootstrapServers=localhost:9092");
            });
  }

  @Test
  public void kafkaSendersendsSpansToTopic() {

    this.contextRunner
        .withPropertyValues(
            "spring.zipkin.sender.type=kafka",
            "spring.zipkin.kafka.topic=zipkin-test",
            "spring.zipkin.kafka.producer.bootstrapServers=localhost:9092")
        .run(
            context -> {
              SpanBytesEncoder bytesEncoder = SpanBytesEncoder.JSON_V2;
              assertThat(context).hasSingleBean(KafkaSender.class);
              assertThat(context.getBean(KafkaSender.class).sendSpans(
                  Stream.of(CLIENT_SPAN).map(bytesEncoder::encode).collect(toList())));
            });
  }



}
