package spring.study.member.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AccountStatusConverter implements AttributeConverter<AccountStatus, String> {
    @Override
    public String convertToDatabaseColumn(AccountStatus attribute) {
        return (attribute == null ? AccountStatus.ACTIVE : attribute).name();
    }

    @Override
    public AccountStatus convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return AccountStatus.ACTIVE;
        }
        return AccountStatus.valueOf(value);
    }
}
