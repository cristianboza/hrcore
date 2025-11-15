package com.example.hrcore.controller;

import com.example.hrcore.dto.UserDto;
import com.example.hrcore.entity.User;
import com.example.hrcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userRepository.findAll()
                .stream()
                .map(UserDto::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(UserDto.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        User user = User.builder()
                .email(userDto.getEmail())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .phone(userDto.getPhone())
                .department(userDto.getDepartment())
                .role(User.UserRole.valueOf(userDto.getRole()))
                .build();
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(savedUser));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setEmail(userDto.getEmail());
                    user.setFirstName(userDto.getFirstName());
                    user.setLastName(userDto.getLastName());
                    user.setPhone(userDto.getPhone());
                    user.setDepartment(userDto.getDepartment());
                    user.setRole(User.UserRole.valueOf(userDto.getRole()));
                    User updatedUser = userRepository.save(user);
                    return ResponseEntity.ok(UserDto.from(updatedUser));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
