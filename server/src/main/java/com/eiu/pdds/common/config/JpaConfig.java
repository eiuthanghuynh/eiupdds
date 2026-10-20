package com.eiu.pdds.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Configuration enabling Spring Data JPA auditing for entity timestamps.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
