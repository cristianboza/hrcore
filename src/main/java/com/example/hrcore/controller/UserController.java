package com.example.hrcore.controller;

import com.example.hrcore.dto.UserDto;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.UserRole;
import com.example.hrcore.mapper.UserMapper;
import com.example.hrcore.repository.UserRepository;
import com.example.hrcore.security.annotation.RequireAuthenticated;
import com.example.hrcore.security.annotation.RequireManagerOrAbove;
import com.example.hrcore.security.annotation.RequireSuperAdmin;
import com.example.hrcore.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthenticationService authenticationService;

    @RequireManagerOrAbove
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Fetching all users by {}", currentUser.getEmail());
        
        List<UserDto> users = userMapper.toDtoList(userRepository.findAll());
        return ResponseEntity.ok(users);
    }

    @RequireAuthenticated
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable UUID id, Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Fetching user {} by {}", id, currentUser.getEmail());
        
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @RequireSuperAdmin
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto, Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Creating user {} by {}", userDto.getEmail(), currentUser.getEmail());
        
        User user = User.builder()
                .email(userDto.getEmail())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .phone(userDto.getPhone())
                .department(userDto.getDepartment())
                .role(UserRole.fromString(userDto.getRole()).orElse(UserRole.EMPLOYEE))
                .build();
        User savedUser = userRepository.save(user);
        log.info("User created with ID: {}", savedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDto(savedUser));
    }

    @RequireManagerOrAbove
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID id, 
            @RequestBody UserDto userDto,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Updating user {} by {}", id, currentUser.getEmail());
        
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        userMapper.updateUserFromDto(userDto, user);
        UserRole.fromString(userDto.getRole()).ifPresent(user::setRole);
        
        User updatedUser = userRepository.save(user);
        log.info("User {} updated successfully", id);
        return ResponseEntity.ok(userMapper.toDto(updatedUser));
    }

    @RequireSuperAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id, Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Deleting user {} by {}", id, currentUser.getEmail());
        
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        log.info("User {} deleted successfully", id);
        return ResponseEntity.noContent().build();
    }
}
