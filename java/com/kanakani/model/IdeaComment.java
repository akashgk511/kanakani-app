package com.kanakani.model;

/** A voter's optional comment. Only the secret name is shown. */
public record IdeaComment(String alias, Ballot vote, String text, String ago) {
    public String getAlias() {
        return alias;
    }

    public Ballot getVote() {
        return vote;
    }

    public String getText() {
        return text;
    }

    public String getAgo() {
        return ago;
    }
}
