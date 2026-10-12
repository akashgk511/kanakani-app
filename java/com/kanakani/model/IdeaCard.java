package com.kanakani.model;

import java.util.List;
import java.util.UUID;

/** One suggested improvement with its tally, ready for the page. */
public record IdeaCard(UUID id, String alias, Gender gender, String title, String body, IdeaStatus status,
                       int yes, int no, Ballot myVote, boolean mine, boolean votingOpen,
                       String ago, String endsIn, String versionLabel, List<IdeaComment> comments) {

    public UUID getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public Gender getGender() {
        return gender;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public IdeaStatus getStatus() {
        return status;
    }

    public int getYes() {
        return yes;
    }

    public int getNo() {
        return no;
    }

    public int getTotal() {
        return yes + no;
    }

    /** Share of yes votes, 0 to 100, for the progress bar. */
    public int getYesPercent() {
        return yes + no == 0 ? 0 : Math.round(100f * yes / (yes + no));
    }

    public boolean isMine() {
        return mine;
    }

    public boolean isVotingOpen() {
        return votingOpen;
    }

    public boolean isVotedYes() {
        return myVote == Ballot.YES;
    }

    public boolean isVotedNo() {
        return myVote == Ballot.NO;
    }

    public String getAgo() {
        return ago;
    }

    public String getEndsIn() {
        return endsIn;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public List<IdeaComment> getComments() {
        return comments;
    }
}
