package it.andrewfly.dnsclient.model;

import lombok.Getter;

@Getter
public enum DNSClass {
    IN(1),    // Internet → l'unica utile a quanto pare
    CS(2),    // CSNET    → obsoleta
    CH(3),    // Chaos    → obsoleta
    HS(4),    // Hesiod   → obsoleta
    ANY(255); // wildcard → usata in alcune query speciali

    private final int value;

    DNSClass(int value) { this.value = value; }

    public static DNSClass fromValue(int value) {
        for (DNSClass qc : values())
            if (qc.value == value) return qc;
        throw new IllegalArgumentException("Unknown QClass: " + value);
    }
}
