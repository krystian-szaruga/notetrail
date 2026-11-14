package dev.maxaure.notetrail.user.application.service;

import dev.maxaure.notetrail.user.application.port.in.FindUserUseCase;
import dev.maxaure.notetrail.user.application.port.out.UserRepository;

import java.util.UUID;

public class FindUserService implements FindUserUseCase {

    private final UserRepository userRepository;

    public FindUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean existsById(UUID userId) {
        return userRepository.existsById(userId);
    }
}
