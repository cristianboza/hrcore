package com.example.hrcore.mapper;

import com.example.hrcore.dto.AbsenceRequestDto;
import com.example.hrcore.dto.NamedUserDto;
import com.example.hrcore.dto.PageResponse;
import com.example.hrcore.entity.AbsenceRequest;
import com.example.hrcore.repository.UserRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class AbsenceRequestMapper {

    @Autowired
    protected UserRepository userRepository;

    @Mapping(target = "user", expression = "java(getNamedUser(entity.getUserId()))")
    @Mapping(target = "approver", expression = "java(getNamedUser(entity.getApproverId()))")
    @Mapping(target = "createdBy", expression = "java(getNamedUser(entity.getCreatedById()))")
    public abstract AbsenceRequestDto toDto(AbsenceRequest entity);

    public abstract List<AbsenceRequestDto> toDtoList(List<AbsenceRequest> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "requestCreator", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "approverId", source = "approver.id")
    @Mapping(target = "createdById", expression = "java(dto.getCreatedBy() != null ? dto.getCreatedBy().getId() : null)")
    public abstract AbsenceRequest toEntity(AbsenceRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "requestCreator", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdById", ignore = true)
    public abstract void updateEntityFromDto(AbsenceRequestDto dto, @MappingTarget AbsenceRequest entity);

    public PageResponse<AbsenceRequestDto> toPageResponse(Page<AbsenceRequest> page) {
        if (page == null) {
            return PageResponse.<AbsenceRequestDto>builder()
                    .content(List.of())
                    .page(0)
                    .size(0)
                    .totalElements(0L)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();
        }

        return PageResponse.<AbsenceRequestDto>builder()
                .content(toDtoList(page.getContent()))
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    protected NamedUserDto getNamedUser(java.util.UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(u -> NamedUserDto.builder()
                        .id(u.getId())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .email(u.getEmail())
                        .build())
                .orElse(null);
    }
}
