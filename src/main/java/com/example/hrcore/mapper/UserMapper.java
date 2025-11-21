package com.example.hrcore.mapper;

import com.example.hrcore.dto.NamedUserDto;
import com.example.hrcore.dto.UserDto;
import com.example.hrcore.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "manager", source = "manager", qualifiedByName = "toNamedDtoOrNull")
    UserDto toDto(User user);

    List<UserDto> toDtoList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "directReports", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUserFromDto(UserDto dto, @MappingTarget User user);

    NamedUserDto toNamedDto(User user);

    List<NamedUserDto> toNamedDtoList(List<User> users);

    @Named("toNamedDtoOrNull")
    default NamedUserDto toNamedDtoOrNull(User user) {
        return user != null ? toNamedDto(user) : null;
    }
}
