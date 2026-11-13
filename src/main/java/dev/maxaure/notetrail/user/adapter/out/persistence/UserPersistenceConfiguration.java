package dev.maxaure.notetrail.user.adapter.out.persistence;

import dev.maxaure.notetrail.user.application.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class UserPersistenceConfiguration {

    @Bean
    UserRepository userRepository(UserJpaRepository userJpaRepository) {
        return new UserPersistenceAdapter(userJpaRepository);
    }
}
