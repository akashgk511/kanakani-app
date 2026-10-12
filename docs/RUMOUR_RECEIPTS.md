# Rumour Receipts

No other gossip app lets a rumour be checked later. Here a writer can mark a post as a **checkable rumour**.
Later the community settles it, and the writer builds a public **truth score**.

## Flow
1. Author posts and ticks "This is a rumour that can be checked later". Only these posts can get receipts (database check `only_rumours_get_receipts`).
2. After 24 hours the author may open a vote, saying whether they think it came true or was busted. The author does **not** decide the result.
3. Other people vote CAME_TRUE or BUSTED for up to 14 days.
4. Result needs at least 5 voters, each at least 7 days old on the app, and two thirds of voters agreeing (`majorityNeeded = (2*total+2)/3`).
5. No quorum after 14 days: the post ends as UNCLEAR. UNCLEAR changes nobody's score.

## Score
Truth score uses the Wilson lower bound of (came true) out of (settled), so one lucky rumour cannot top the board.
The **Oracle** badge goes to writers with enough settled rumours and a high bound. Rules live in `ReceiptRules.java` and `TruthScore.java`, with the same conditions repeated in the SQL of `ReceiptDao` and `db/02_views_rankings.sql`.

## Abuse cases
| Attack | Defence now | Later |
|---|---|---|
| Author settles own post | Not possible; author only opens the vote and cannot vote | |
| Fresh fake accounts vote | Voters need 7 days of use; one vote per person per post | Device and phone-token limits |
| Friends team up | Quorum and two-thirds rule make it costly | Collusion detection (same voters always agreeing) |
| Voting on an obvious rumour too early | 24 hour minimum age | |
| Rumour about a named person | Slip-guard, reports, topic rules | Moderation queue and legal review |

## Limits
Voters judge from what they know, not from proof. "Came true" means the community agrees, not that it is a fact.
