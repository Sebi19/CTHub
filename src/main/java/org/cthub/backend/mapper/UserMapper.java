package org.cthub.backend.mapper;

import org.cthub.backend.dto.auth.UserDto;
import org.cthub.backend.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Both are derived helpers rather than bean properties on the entity, so map them explicitly.
    @Mapping(target = "profileComplete", expression = "java(user.isProfileComplete())")
    @Mapping(target = "passwordSet", expression = "java(user.hasPassword())")
    UserDto toUserDto(User user);
}
