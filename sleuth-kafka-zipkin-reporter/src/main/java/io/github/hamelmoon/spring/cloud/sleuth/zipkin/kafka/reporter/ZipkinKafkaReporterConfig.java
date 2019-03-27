package io.github.hamelmoon.spring.cloud.sleuth.zipkin.kafka.reporter;

import java.util.Collections;
import lombok.Data;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(
    prefix = "spring.zipkin.kafka"
)
@Data
public class ZipkinKafkaReporterConfig {

  private String topic = "zipkin";

  private KafkaProperties.Producer producer;

  public ZipkinKafkaReporterConfig() {
    this.producer = new KafkaProperties.Producer();
    this.producer.setBootstrapServers(Collections.singletonList("localhost:9092"));
    this.producer.setCompressionType("lz4");
    this.producer.setRetries(0);
    this.producer.setKeySerializer(ByteArraySerializer.class);
    this.producer.setValueSerializer(ByteArraySerializer.class);
    this.producer.setBufferMemory(DataSize.ofMegabytes(32));
    this.producer.setBatchSize(DataSize.ofKilobytes(512));
  }
}
