package com.kanakani;

import com.kanakani.model.Ballot;
import com.kanakani.model.Gender;
import com.kanakani.model.IdeaStatus;
import com.kanakani.model.Resolution;
import com.kanakani.security.AliasGenerator;
import com.kanakani.security.EnvKeyProvider;
import com.kanakani.security.OtpService;
import com.kanakani.security.PhoneVault;
import com.kanakani.security.PostCrypto;
import com.kanakani.security.RateLimiter;
import com.kanakani.security.SessionTokens;
import com.kanakani.security.SlipGuard;
import com.kanakani.service.ActivityPoints;
import com.kanakani.service.IdeaRules;
import com.kanakani.service.ReceiptRules;
import com.kanakani.service.TruthScore;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Plain-Java checks for the security and rule classes. No test framework needed:
 *   javac -d out -sourcepath java tests/com/kanakani/SelfTest.java && java -cp out com.kanakani.SelfTest
 * Exits with a non-zero code if anything fails.
 */
public final class SelfTest {

    private static int failures = 0;
    private static int checks = 0;

    private static void check(boolean ok, String name) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL  " + name);
        }
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        return b;
    }

    public static void main(String[] args) {
        phoneVault();
        postCrypto();
        otp();
        slipGuard();
        aliases();
        rateLimiter();
        sessions();
        receiptRules();
        truthScore();
        points();
        gender();
        ideaRules();
        languages();
        System.out.println(checks + " checks, " + failures + " failed");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void phoneVault() {
        check(PhoneVault.normalizeIndian("98300 12345").equals("+919830012345"), "normalise plain number");
        check(PhoneVault.normalizeIndian("+91 98300-12345").equals("+919830012345"), "normalise +91 number");
        check(PhoneVault.normalizeIndian("098300 12345").equals("+919830012345"), "normalise leading zero");
        boolean rejected = false;
        try {
            PhoneVault.normalizeIndian("12345");
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check(rejected, "reject short number");
        PhoneVault v1 = new PhoneVault(randomBytes(32));
        PhoneVault v2 = new PhoneVault(randomBytes(32));
        String t = v1.token("98300 12345");
        check(t.length() == 64 && t.matches("[0-9a-f]+"), "token is 64 hex chars");
        check(t.equals(v1.token("+91 9830012345")), "same number, same token");
        check(!t.equals(v2.token("98300 12345")), "different key, different token");
        check(!t.contains("9830012345"), "token does not contain the number");
        check(PhoneVault.sameToken(t, t) && !PhoneVault.sameToken(t, v2.token("98300 12345")), "constant-time compare");
    }

    private static void postCrypto() {
        String keys = "1:" + Base64.getEncoder().encodeToString(randomBytes(32));
        PostCrypto c = new PostCrypto(new EnvKeyProvider(keys));
        String id = UUID.randomUUID().toString();
        PostCrypto.Sealed s = c.seal("The Friday cake is real.", id);
        check(c.open(s, id).equals("The Friday cake is real."), "seal then open");
        check(!new String(s.ciphertext()).contains("Friday"), "ciphertext hides the text");
        PostCrypto.Sealed s2 = c.seal("The Friday cake is real.", id);
        check(!java.util.Arrays.equals(s.ciphertext(), s2.ciphertext()), "fresh IV each time");
        boolean wrongAad = false;
        try {
            c.open(s, UUID.randomUUID().toString());
        } catch (IllegalStateException e) {
            wrongAad = true;
        }
        check(wrongAad, "ciphertext moved to another post fails");
        byte[] tampered = s.ciphertext().clone();
        tampered[0] ^= 1;
        boolean tamperCaught = false;
        try {
            c.open(new PostCrypto.Sealed(s.keyId(), s.iv(), tampered), id);
        } catch (IllegalStateException e) {
            tamperCaught = true;
        }
        check(tamperCaught, "tampered ciphertext fails");
        EnvKeyProvider rotated = new EnvKeyProvider(keys + ",2:" + Base64.getEncoder().encodeToString(randomBytes(32)));
        check(rotated.currentKeyId() == 2, "highest key id is current");
    }

    private static void otp() {
        OtpService o = new OtpService(randomBytes(32));
        String code = o.newCode();
        check(code.matches("\\d{6}"), "code is 6 digits");
        String mac = o.mac("tokenA", code);
        check(o.matches("tokenA", code, mac), "correct code matches");
        check(!o.matches("tokenB", code, mac), "code does not work for another number");
        check(!o.matches("tokenA", "000000".equals(code) ? "111111" : "000000", mac), "wrong code fails");
        check(!o.matches("tokenA", "12ab56", mac), "non-digit code fails");
    }

    private static void slipGuard() {
        check(SlipGuard.check("Cake on Friday, bring a fork").isEmpty(), "clean text passes");
        check(SlipGuard.check("mail me at raj@corp.com").contains("an email address"), "catches email");
        check(SlipGuard.check("call 98300 12345 now").contains("a phone number"), "catches phone");
        check(SlipGuard.check("see https://x.example/y").contains("a link"), "catches link");
        check(SlipGuard.check("my id is EMP12345").contains("what looks like an employee ID"), "catches employee id");
    }

    private static void aliases() {
        AliasGenerator g = new AliasGenerator();
        boolean shapeOk = true;
        for (int i = 0; i < 200; i++) {
            if (!g.next().matches("[A-Z][a-z]+ [A-Z][a-z]+ #\\d{3,4}")) {
                shapeOk = false;
            }
        }
        check(shapeOk, "alias shape");
    }

    private static void rateLimiter() {
        RateLimiter r = new RateLimiter();
        Duration w = Duration.ofMinutes(10);
        check(r.allow("k", 3, w) && r.allow("k", 3, w) && r.allow("k", 3, w), "first 3 allowed");
        check(!r.allow("k", 3, w), "4th blocked");
        check(r.allow("other", 3, w), "other key unaffected");
    }

    private static void sessions() {
        String t = SessionTokens.newToken();
        check(t.length() >= 43, "token has 256 bits");
        check(SessionTokens.hash(t).length() == 64 && !SessionTokens.hash(t).equals(t), "hash differs from token");
        check(!t.equals(SessionTokens.newToken()), "tokens are unique");
    }

    private static void receiptRules() {
        Instant now = Instant.parse("2026-10-11T12:00:00Z");
        Instant opens = now.minus(Duration.ofHours(25));
        check(ReceiptRules.authorMayOpenVote(true, opens, now, false), "author may open after 24h");
        check(!ReceiptRules.authorMayOpenVote(true, now.plus(Duration.ofHours(1)), now, false), "too early");
        check(!ReceiptRules.authorMayOpenVote(false, opens, now, false), "opinions get no receipt");
        check(!ReceiptRules.authorMayOpenVote(true, opens, now, true), "cannot open twice");

        UUID author = UUID.randomUUID();
        check(!ReceiptRules.mayVote(author, author, 100), "author cannot vote");
        check(!ReceiptRules.mayVote(UUID.randomUUID(), author, 6), "new account cannot vote");
        check(ReceiptRules.mayVote(UUID.randomUUID(), author, 7), "7-day account can vote");

        Instant opened = now.minus(Duration.ofDays(1));
        check(ReceiptRules.decide(2, 1, opened, now) == Resolution.PENDING, "below quorum stays pending");
        check(ReceiptRules.decide(4, 1, opened, now) == Resolution.CAME_TRUE, "4 of 5 is a two-thirds majority");
        check(ReceiptRules.decide(1, 4, opened, now) == Resolution.BUSTED, "community can bust a rumour");
        check(ReceiptRules.decide(3, 2, opened, now) == Resolution.PENDING, "3 of 5 is not enough");
        check(ReceiptRules.decide(4, 2, opened, now) == Resolution.CAME_TRUE, "4 of 6 is exactly two thirds");
        check(ReceiptRules.decide(3, 3, opened, now) == Resolution.PENDING, "a tie stays pending");
        check(ReceiptRules.decide(3, 3, now.minus(Duration.ofDays(15)), now) == Resolution.UNCLEAR, "stale tie becomes unclear");
        check(ReceiptRules.decide(0, 0, now.minus(Duration.ofDays(15)), now) == Resolution.UNCLEAR, "nobody voted becomes unclear");
        check(ReceiptRules.decide(10, 0, now.minus(Duration.ofDays(30)), now) == Resolution.CAME_TRUE, "clear majority wins even late");
        check(ReceiptRules.majorityNeeded(5) == 4 && ReceiptRules.majorityNeeded(6) == 4 && ReceiptRules.majorityNeeded(7) == 5, "majority maths");
    }

    private static void truthScore() {
        TruthScore a = new TruthScore(20, 18);
        check(a.percent() == 90, "percent");
        check(a.isOracle(), "oracle at 90% of 20");
        check(!new TruthScore(4, 4).isOracle(), "4 rumours is too few for oracle");
        check(!new TruthScore(2, 2).onBoard(), "2 settled is not on the board");
        check(new TruthScore(3, 3).onBoard(), "3 settled is on the board");
        check(new TruthScore(20, 18).wilsonLowerBound() > new TruthScore(3, 3).wilsonLowerBound(), "many good guesses beat 3 lucky ones");
        check(new TruthScore(0, 0).wilsonLowerBound() == 0.0, "no data, no score");
        boolean bad = false;
        try {
            new TruthScore(2, 3);
        } catch (IllegalArgumentException e) {
            bad = true;
        }
        check(bad, "hits cannot exceed settled");
    }

    private static void points() {
        check(ActivityPoints.compute(38, 610, 420, 112) == 38 * 5 + 610 * 2 + 420 + 112, "activity points");
        check(ActivityPoints.compute(38, 610, 420, 112) == 1942, "matches the prototype's top user");
    }

    private static void gender() {
        check(Gender.fromForm("Non-binary") == Gender.NON_BINARY, "gender label to enum");
        check(Gender.fromForm("woman") == Gender.WOMAN, "gender case");
        check(Gender.fromForm("???") == Gender.NOT_SHARED, "unknown gender is not shared");
        check(Gender.fromForm(null) == Gender.NOT_SHARED, "null gender is not shared");
    }

    private static void ideaRules() {
        check(IdeaRules.decideAtClose(9, 0) == IdeaStatus.EXPIRED, "9 voters is not enough");
        check(IdeaRules.decideAtClose(6, 4) == IdeaStatus.APPROVED, "10 voters, more yes: approved");
        check(IdeaRules.decideAtClose(5, 5) == IdeaStatus.REJECTED, "a tie is not a majority");
        check(IdeaRules.decideAtClose(2, 8) == IdeaStatus.REJECTED, "more no: rejected");
        check(IdeaRules.decideAtClose(40, 0) == IdeaStatus.APPROVED, "unanimous yes");
        check(IdeaRules.titleOk("Dark mode") && !IdeaRules.titleOk("hey") && !IdeaRules.titleOk("   ab   "), "title length");
        check(!IdeaRules.titleOk("x".repeat(81)) && !IdeaRules.titleOk(null), "title too long or missing");
        boolean bad = false;
        try {
            IdeaRules.decideAtClose(-1, 3);
        } catch (IllegalArgumentException e) {
            bad = true;
        }
        check(bad, "negative votes rejected");
        check(Ballot.fromForm("YES") == Ballot.YES && Ballot.fromForm("NO") == Ballot.NO && Ballot.fromForm("maybe") == null, "ballot parsing");
        check(IdeaStatus.APPROVED.isComingSoon() && IdeaStatus.BUILDING.isComingSoon() && !IdeaStatus.SHIPPED.isComingSoon(), "coming soon statuses");
    }

    /** Every language file must have exactly the same keys as the English one, and no empty text. Run from the project root. */
    private static void languages() {
        try {
            java.util.Properties en = load("i18n/messages.properties");
            check(!en.isEmpty(), "English text file loads");
            for (String code : new String[] {"bn", "hi"}) {
                java.util.Properties p = load("i18n/messages_" + code + ".properties");
                check(p.stringPropertyNames().equals(en.stringPropertyNames()), code + " has the same keys as English");
                boolean empty = false;
                boolean ownScript = false;
                for (String k : p.stringPropertyNames()) {
                    empty |= p.getProperty(k).isBlank();
                    ownScript |= p.getProperty(k).codePoints().anyMatch(cp -> cp > 0x0900);
                }
                check(!empty, code + " has no empty text");
                check(ownScript, code + " is written in its own script");
            }
        } catch (java.io.IOException e) {
            check(false, "language files readable: " + e.getMessage());
        }
    }

    private static java.util.Properties load(String path) throws java.io.IOException {
        java.util.Properties p = new java.util.Properties();
        try (java.io.Reader r = java.nio.file.Files.newBufferedReader(java.nio.file.Path.of(path), java.nio.charset.StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }
}
