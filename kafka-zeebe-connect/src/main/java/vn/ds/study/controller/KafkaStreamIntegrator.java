package vn.ds.study.controller;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.backoff.FixedBackOff;

import com.fasterxml.jackson.databind.JsonNode;

import vn.ds.study.model.TicketRequest;
import vn.ds.study.service.TicketService;

@Component
@Configuration
public class KafkaStreamIntegrator {

	private static final Logger logger = LoggerFactory
	        .getLogger(KafkaStreamIntegrator.class);
	@Value("${sleep.time.second:5}")
	private int sleepTime;

	private static Random rd = new Random();

	private static void sleepInSecond(int second) {
		try {
			TimeUnit.SECONDS.sleep(rd.nextInt(second));
		} catch (InterruptedException ex) {
		}
	}

	@Autowired
	private TicketService approvalService;

	@Transactional
	@Bean
	public Function<JsonNode, JsonNode> process() {
		return e -> {
			logger.info("Received event {}", e.toString());

			if (sleepTime > 0) {
				sleepInSecond(sleepTime);
			}
			TicketRequest rq = TicketRequest.from("1", "Facilitites", 1, 5);
			approvalService.approve(rq);

			logger.info("Sent event {}", e.toString());
			return e;
		};
	}

	@Bean
	public ListenerContainerCustomizer<AbstractMessageListenerContainer<byte[], byte[]>> customizer() {
		// Disable retry in the AfterRollbackProcessor
		return (container, destination, group) -> container
		        .setAfterRollbackProcessor(
		                new DefaultAfterRollbackProcessor<byte[], byte[]>(
		                        (record, exception) -> System.out.println(
		                                "Discarding failed record: " + record),
		                        new FixedBackOff(0L, 0)));
	}
}
