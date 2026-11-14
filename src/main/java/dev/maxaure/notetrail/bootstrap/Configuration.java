package dev.maxaure.notetrail.bootstrap;

import org.springframework.context.annotation.Bean;

import java.time.Clock;

@org.springframework.context.annotation.Configuration
class Configuration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
