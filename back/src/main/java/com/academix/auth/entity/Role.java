package com.academix.auth.entity;

public enum Role {
	ADMINISTRATION(0),
	TEACHER(1),
	STUDENT(2);

	private final int code;

	Role(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static Role fromCode(Integer code) {
		if (code == null) {
			return STUDENT;
		}

		return switch (code) {
			case 0 -> ADMINISTRATION;
			case 1 -> TEACHER;
			case 2 -> STUDENT;
			default -> throw new IllegalArgumentException("Invalid role code: " + code);
		};
	}
}