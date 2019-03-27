package io.github.hamelmoon.spring.cloud.sleuth.zipkin.kafka.reporter;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.cloud.sleuth.zipkin2.ZipkinAutoConfiguration;
import org.springframework.cloud.sleuth.zipkin2.ZipkinProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.ReporterMetrics;
import zipkin2.reporter.Sender;
import zipkin2.reporter.kafka11.KafkaSender;

@Configuration
@ConditionalOnMissingBean({Sender.class})
@ConditionalOnBean({KafkaProperties.class})
@AutoConfigureBefore({ZipkinAutoConfiguration.class})
@AutoConfigureAfter({KafkaAutoConfiguration.class})
@ConditionalOnProperty(
    value = {"spring.zipkin.sender.type"},
    havingValue = "kafka"
)
@Import(ZipkinKafkaReporterConfig.class)
public class ZipkinKafkaReporterAutoConfigure {
  @Bean(ZipkinAutoConfiguration.REPORTER_BEAN_NAME)
  Reporter<Span> kafkaReporter(
      ReporterMetrics reporterMetrics,
      ZipkinProperties zipkin,
      ZipkinKafkaReporterConfig kafkaSenderConfig
  ) {
    return AsyncReporter.builder(kafkaSender(kafkaSenderConfig))
        .queuedMaxSpans(1000)
        .messageTimeout(zipkin.getMessageTimeout(), TimeUnit.SECONDS)
        .metrics(reporterMetrics)
        .build(zipkin.getEncoder());
  }

  @Bean(ZipkinAutoConfiguration.SENDER_BEAN_NAME)
  Sender kafkaSender(ZipkinKafkaReporterConfig kafkaSenderConfig) {
    Map<String, Object> senderProperties = kafkaSenderConfig.getProducer().buildProperties();

    String bootstraps = String.join(",", kafkaSenderConfig.getProducer().getBootstrapServers());

    return KafkaSender.newBuilder()
        .topic(kafkaSenderConfig.getTopic())
        .overrides(senderProperties)
        .bootstrapServers(bootstraps)
        .build();
  }
}
