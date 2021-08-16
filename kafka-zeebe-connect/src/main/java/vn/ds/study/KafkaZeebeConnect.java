/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vn.ds.study;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.SocketUtils;

import io.camunda.zeebe.spring.client.EnableZeebeClient;

@SpringBootApplication
@EnableZeebeClient
@EnableScheduling
public class KafkaZeebeConnect {

	private static final Logger logger = LoggerFactory
	        .getLogger(KafkaZeebeConnect.class);

	public static void main(String[] args) {
		int port = SocketUtils.findAvailableTcpPort(8000, 10000);
		System.setProperty("server.port", String.valueOf(port));
		logger.info("Random Server Port is set to {}.", port);
		SpringApplication.run(KafkaZeebeConnect.class, args);
	}

	
}
