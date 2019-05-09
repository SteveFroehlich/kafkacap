package io.thill.kafkacap.capture;

import io.thill.kafkacap.capture.callback.SendCompleteListener;
import io.thill.kafkacap.capture.populator.DefaultRecordPopulator;
import io.thill.kafkacap.capture.populator.RecordPopulator;
import io.thill.kafkacap.capture.queue.CaptureQueue;
import io.thill.kafkacap.capture.queue.ChronicleCaptureQueue;
import io.thill.kafkacap.capture.queue.MemoryCaptureQueue;
import io.thill.kafkacap.util.clock.Clock;
import io.thill.kafkacap.util.clock.SystemMillisClock;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class BufferedPublisherBuilder<K, V> {

  private CaptureQueue captureQueue;
  private RecordPopulator<K, V> recordPopulator;
  private Properties kafkaProducerProperties;
  private Clock clock = new SystemMillisClock();
  private IdleStrategy idleStrategy = new BackoffIdleStrategy(100, 10, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(100));
  private SendCompleteListener sendCompleteListener;

  /**
   * Used to buffer messages prior to populating and sending to Kafka
   *
   * @param captureQueue
   * @return
   */
  public BufferedPublisherBuilder<K, V> captureQueue(CaptureQueue captureQueue) {
    this.captureQueue = captureQueue;
    return this;
  }

  /**
   * Used to populate the outbound {@link org.apache.kafka.clients.producer.ProducerRecord}
   *
   * @param recordPopulator
   * @return
   */
  public BufferedPublisherBuilder<K, V> recordPopulator(RecordPopulator<K, V> recordPopulator) {
    this.recordPopulator = recordPopulator;
    return this;
  }

  /**
   * The properties used to create the underlying {@link KafkaProducer} to use to send outbound records
   *
   * @param kafkaProducerProperties
   * @return
   */
  public BufferedPublisherBuilder<K, V> kafkaProducerProperties(Properties kafkaProducerProperties) {
    this.kafkaProducerProperties = kafkaProducerProperties;
    return this;
  }

  /**
   * The clock used for latency stats tracking. Defaults to {@link SystemMillisClock}
   *
   * @param clock
   * @return
   */
  public BufferedPublisherBuilder<K, V> clock(Clock clock) {
    this.clock = clock;
    return this;
  }

  /**
   * The idle strategy used when there are no messages to process from the chronicle queue. Defaults to {@link BackoffIdleStrategy}
   *
   * @param idleStrategy
   * @return
   */
  public BufferedPublisherBuilder<K, V> idleStrategy(IdleStrategy idleStrategy) {
    this.idleStrategy = idleStrategy;
    return this;
  }

  /**
   * Optional. Listener to fire events after a {@link org.apache.kafka.clients.producer.ProducerRecord} has been dispatched to the {@link KafkaProducer}
   *
   * @param sendCompleteListener
   * @return
   */
  public BufferedPublisherBuilder<K, V> sendCompleteListener(SendCompleteListener sendCompleteListener) {
    this.sendCompleteListener = sendCompleteListener;
    return this;
  }

  public BufferedPublisher build() {
    if(captureQueue == null) {
      throw new IllegalArgumentException("captureQueue cannot be null. See " + ChronicleCaptureQueue.class.getName() + " and " + MemoryCaptureQueue.class.getName());
    }

    if(recordPopulator == null) {
      throw new IllegalArgumentException("recordPopulator cannot be null. See " + DefaultRecordPopulator.class.getName());
    }

    if(kafkaProducerProperties == null) {
      throw new IllegalArgumentException("kafkaProducerProperties cannot be null");
    }

    if(clock == null) {
      throw new IllegalArgumentException("clock cannot be null");
    }

    if(idleStrategy == null) {
      throw new IllegalArgumentException("idleStrategy cannot be null");
    }

    final KafkaProducer<K, V> kafkaProducer = new KafkaProducer<K, V>(kafkaProducerProperties);
    final BufferedPublisher publisher = new BufferedPublisher(
            captureQueue, recordPopulator, kafkaProducer, clock, idleStrategy, sendCompleteListener);

    return publisher;
  }

}
