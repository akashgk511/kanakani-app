package com.kanakani.security;

import java.security.SecureRandom;

/** Makes the funny secret names, like "Sneaky Puchka #482". Uses a secure random source. */
public final class AliasGenerator {

    private static final String[] ADJECTIVES = {
        "Sleepy", "Soggy", "Chatty", "Wobbly", "Spicy", "Dizzy", "Grumpy", "Sneaky", "Giggly", "Moody", "Sparkly", "Crispy"
    };
    private static final String[] NOUNS = {
        "Samosa", "Rasgulla", "Hilsa", "Tram", "Jhalmuri", "Mishti", "Cha", "Puchka", "Rickshaw", "Luchi", "Sandesh", "Kachori"
    };

    private final SecureRandom random = new SecureRandom();

    public String next() {
        return ADJECTIVES[random.nextInt(ADJECTIVES.length)] + " "
             + NOUNS[random.nextInt(NOUNS.length)] + " #"
             + (100 + random.nextInt(9900));
    }
}
