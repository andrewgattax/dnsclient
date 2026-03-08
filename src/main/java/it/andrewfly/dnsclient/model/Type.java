package it.andrewfly.dnsclient.model;

import lombok.Getter;

@Getter
public enum Type {
    A(1), NS(2), CNAME(5), MX(15), TXT(16), AAAA(28);

    private final int value;

    Type(int value) { this.value = value; }

    public static Type fromValue(int value) {
        for (Type type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown Type value: " + value);
    }
}
