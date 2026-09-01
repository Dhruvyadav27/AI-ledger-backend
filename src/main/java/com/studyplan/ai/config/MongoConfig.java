package com.studyplan.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Turns on @CreatedDate / @LastModifiedDate support for MongoDB documents.
 * Without this, the @CreatedDate field on User (and later on other models)
 * stays null - Spring Data needs auditing explicitly enabled.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
