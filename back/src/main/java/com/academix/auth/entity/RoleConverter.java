package com.academix.auth.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class RoleConverter implements AttributeConverter<Role, Integer> {

	@Override
	public Integer convertToDatabaseColumn(Role attribute) {
		return attribute == null ? Role.STUDENT.getCode() : attribute.getCode();
	}

	@Override
	public Role convertToEntityAttribute(Integer dbData) {
		return Role.fromCode(dbData);
	}
}