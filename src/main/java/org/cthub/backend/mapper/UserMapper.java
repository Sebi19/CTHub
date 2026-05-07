package org.cthub.backend.mapper;

import org.cthub.backend.dto.auth.UserDto;
import org.cthub.backend.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toUserDto(User user);
}
