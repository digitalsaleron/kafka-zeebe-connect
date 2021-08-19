package vn.ds.study.controller;

import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.JsonNode;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import vn.ds.study.model.JobInfo;
import vn.ds.study.service.JobService;

@Configuration
public class Kafka2ZeebeIntegrator {

	@Autowired
	private ZeebeClient client;

	@Autowired
	private JobService jobService;

	@Bean
	Consumer<JsonNode> validationResponseConsumer() {
		return r -> {
			JobInfo jobI = jobService.next();
			ActivatedJob job = jobI.getActivatedJob();

			Map<String, Object> variables = job.getVariablesAsMap();
			variables.put("isValid", true);

			client.newCompleteCommand(job.getKey()).variables(variables).send();

		};
	}
}
