package com.example.hrcore.mapper;

import com.example.hrcore.dto.FeedbackDto;
import com.example.hrcore.dto.NamedUserDto;
import com.example.hrcore.dto.PageResponse;
import com.example.hrcore.entity.Feedback;
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
public abstract class FeedbackMapper {

    @Autowired
    protected UserRepository userRepository;

    @Mapping(target = "fromUser", expression = "java(getNamedUser(feedback.getFromUserId()))")
    @Mapping(target = "toUser", expression = "java(getNamedUser(feedback.getToUserId()))")
    public abstract FeedbackDto toDto(Feedback feedback);

    public abstract List<FeedbackDto> toDtoList(List<Feedback> feedbacks);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "toUser", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "fromUserId", source = "fromUser.id")
    @Mapping(target = "toUserId", source = "toUser.id")
    public abstract Feedback toEntity(FeedbackDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "toUser", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "fromUserId", ignore = true)
    @Mapping(target = "toUserId", ignore = true)
    public abstract void updateEntityFromDto(FeedbackDto dto, @MappingTarget Feedback entity);

    public PageResponse<FeedbackDto> toPageResponse(Page<Feedback> page) {
        if (page == null) {
            return PageResponse.<FeedbackDto>builder()
                    .content(List.of())
                    .page(0)
                    .size(0)
                    .totalElements(0L)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();
        }

        return PageResponse.<FeedbackDto>builder()
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
