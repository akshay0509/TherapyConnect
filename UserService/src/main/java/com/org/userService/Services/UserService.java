package com.org.userService.Services;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.org.userService.Assembler.UserAssembler;
import com.org.userService.Dto.AuthRequest;
import com.org.userService.Dto.AuthResponse;
import com.org.userService.Dto.UserDto;
import com.org.userService.Entity.User;
import com.org.userService.Enum.FailureReason;
import com.org.userService.Repository.UserRepository;

@Service
public class UserService {

	@Autowired
	UserRepository userRepository;
	
	@Autowired
	PasswordEncoder passwordEncoder;
	
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);
	
	private UserAssembler userAssembler = new UserAssembler();
	
	public void createUser(UserDto userDto) {
		logger.debug("inside createUser.");
		User user = userAssembler.assembleDtoToEntity(userDto);
		
		String passwordHash = passwordEncoder.encode(userDto.getPassword());
		user.setPasswordHash(passwordHash);
		
		userRepository.save(user);
		logger.debug("exiting createUser.");
	}
	
	public AuthResponse validateUser(AuthRequest authRequest) {
		User user = userRepository.findByUsername(authRequest.getUsername());
		
		if (user == null) {
            return AuthResponse.failure(FailureReason.INVALID_CREDENTIALS);
        }

        if (!user.isEnabled()) {
            return AuthResponse.failure(FailureReason.ACCOUNT_DISABLED);
        }

        if (user.isAccountLocked()) {
            return AuthResponse.failure(FailureReason.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(authRequest.getPassword(), user.getPasswordHash())) {
            return AuthResponse.failure(FailureReason.INVALID_CREDENTIALS);
        }
		
        return AuthResponse.success(
                user.getUserId(),
                user.getUsername(),
                user.getRoles()
                    .stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet()),
                user.isEnabled(),
                user.isAccountLocked()
        );
		
	}
}
