package dev.maxaure.notetrail.bootstrap;

import dev.maxaure.notetrail.item.application.port.out.UserLookup;
import dev.maxaure.notetrail.user.application.port.in.FindUserUseCase;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@org.springframework.context.annotation.Configuration
class Configuration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    UserLookup userLookup(FindUserUseCase findUser) {
        return findUser::existsById;
    }
}
