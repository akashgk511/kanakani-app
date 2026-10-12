package com.kanakani.model;

/**
 * One line on a ranking page. For post rankings, {@code detail} is a short snippet of the post.
 * The get... methods exist because JSP expression language reads JavaBean getters.
 */
public record RankRow(int rank, String alias, Gender gender, long value, String detail, boolean oracle) {

    public int getRank() {
        return rank;
    }

    public String getAlias() {
        return alias;
    }

    public Gender getGender() {
        return gender;
    }

    public long getValue() {
        return value;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isOracle() {
        return oracle;
    }
}
