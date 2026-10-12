package com.kanakani.model;

import java.util.UUID;

/**
 * What the app knows about a person. Note what is missing: number, name, email, address.
 * The get... methods exist because JSP expression language reads JavaBean getters.
 */
public record User(UUID id, String alias, Gender gender, int days) {

    public String getAlias() {
        return alias;
    }

    public Gender getGender() {
        return gender;
    }

    public int getDays() {
        return days;
    }
}
