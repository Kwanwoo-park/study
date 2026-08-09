package spring.study.member.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MemberStatusConverter implements AttributeConverter<MemberStatus, String> {
    @Override
    public String convertToDatabaseColumn(MemberStatus attribute) {
        return (attribute == null ? MemberStatus.ACTIVE : attribute).name();
    }

    @Override
    public MemberStatus convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return MemberStatus.ACTIVE;
        }
        return MemberStatus.valueOf(value);
    }
}
