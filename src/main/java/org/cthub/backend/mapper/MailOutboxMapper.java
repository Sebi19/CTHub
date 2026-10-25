package org.cthub.backend.mapper;

import org.cthub.backend.dto.mail.MailOutboxEntryDto;
import org.cthub.backend.mail.outbox.MailOutboxEntry;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MailOutboxMapper {
    MailOutboxEntryDto toDto(MailOutboxEntry entry);
}
