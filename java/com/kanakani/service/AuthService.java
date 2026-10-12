package com.kanakani.service;

import com.kanakani.dao.OtpDao;
import com.kanakani.dao.SessionDao;
import com.kanakani.dao.UserDao;
import com.kanakani.model.Gender;
import com.kanakani.model.User;
import com.kanakani.security.AliasGenerator;
import com.kanakani.security.OtpService;
import com.kanakani.security.PhoneVault;
import com.kanakani.security.RateLimiter;
import com.kanakani.security.SessionTokens;
import com.kanakani.util.DataAccessException;
import com.kanakani.util.UserFacingException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Sign up and sign in with a phone number and a one-time code.
 *
 * What touches the number: the SMS provider (to deliver the code) and this server's memory for
 * the length of one request. What is stored: only the keyed token of the number.
 */
public final class AuthService {

    public record Login(User user, String sessionToken, boolean isNewUser) { }

    private static final Duration SESSION_LIFETIME = Duration.ofDays(30);
    private static final int MAX_CODES_PER_WINDOW = 3;
    private static final Duration CODE_WINDOW = Duration.ofMinutes(10);

    private final UserDao users;
    private final SessionDao sessions;
    private final OtpDao otps;
    private final PhoneVault vault;
    private final OtpService otp;
    private final OtpService.SmsGateway sms;
    private final AliasGenerator aliases = new AliasGenerator();
    private final RateLimiter limiter;

    public AuthService(UserDao users, SessionDao sessions, OtpDao otps, PhoneVault vault, OtpService otp,
                       OtpService.SmsGateway sms, RateLimiter limiter) {
        this.users = users;
        this.sessions = sessions;
        this.otps = otps;
        this.vault = vault;
        this.otp = otp;
        this.sms = sms;
        this.limiter = limiter;
    }

    /** Sends a code. Limited to 3 per number per 10 minutes, checked both in memory and in the database. */
    public void requestCode(String rawPhone) {
        String e164;
        try {
            e164 = PhoneVault.normalizeIndian(rawPhone);
        } catch (IllegalArgumentException e) {
            throw new UserFacingException(e.getMessage());
        }
        String token = vault.token(e164);
        if (!limiter.allow("otp:" + token, MAX_CODES_PER_WINDOW, CODE_WINDOW)
                || otps.countSince(token, Instant.now().minus(CODE_WINDOW)) >= MAX_CODES_PER_WINDOW) {
            throw new UserFacingException("Too many codes requested. Try again in a few minutes.");
        }
        String code = otp.newCode();
        otps.create(token, otp.mac(token, code), Instant.now().plus(OtpService.TTL));
        sms.send(e164, "Your Kanakani code is " + code + ". It works for 5 minutes. Do not share it.");
    }

    /** Checks the code, creates the account on first use, and starts a session. */
    public Login verify(String rawPhone, String code, Gender genderIfNew) {
        String token;
        try {
            token = vault.token(rawPhone);
        } catch (IllegalArgumentException e) {
            throw new UserFacingException(e.getMessage());
        }
        OtpDao.Challenge ch = otps.latest(token).orElseThrow(() -> new UserFacingException("Ask for a new code first."));
        if (ch.consumed() || Instant.now().isAfter(ch.expiresAt())) {
            throw new UserFacingException("That code has expired. Ask for a new one.");
        }
        if (ch.attempts() >= OtpService.MAX_ATTEMPTS) {
            throw new UserFacingException("Too many wrong tries. Ask for a new code.");
        }
        if (!otp.matches(token, code, ch.mac())) {
            otps.addAttempt(ch.id());
            throw new UserFacingException("That code is not right.");
        }
        otps.consume(ch.id());

        boolean isNew = false;
        Optional<User> existing = users.findByPhoneToken(token);
        UUID userId;
        if (existing.isPresent()) {
            userId = existing.get().id();
        } else {
            userId = createWithFreshAlias(token, genderIfNew);
            isNew = true;
        }
        users.touchToday(userId);

        String sessionToken = SessionTokens.newToken();
        sessions.create(SessionTokens.hash(sessionToken), userId, Instant.now().plus(SESSION_LIFETIME));
        User user = users.findById(userId).orElseThrow(() -> new UserFacingException("This account is not available."));
        return new Login(user, sessionToken, isNew);
    }

    private UUID createWithFreshAlias(String token, Gender gender) {
        for (int attempt = 0; attempt < 8; attempt++) {
            try {
                return users.create(token, aliases.next(), gender);
            } catch (DataAccessException e) {
                if (!e.isUniqueViolation()) {
                    throw e;
                }
                // Alias already taken (or a parallel signup for the same number). Try another alias.
                Optional<User> raced = users.findByPhoneToken(token);
                if (raced.isPresent()) {
                    return raced.get().id();
                }
            }
        }
        throw new UserFacingException("Could not find you a free secret name. Please try again.");
    }

    public Optional<User> userForSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Optional.empty();
        }
        Optional<User> u = sessions.findUser(SessionTokens.hash(sessionToken));
        u.ifPresent(user -> users.touchToday(user.id()));
        return u;
    }

    public void logout(String sessionToken) {
        if (sessionToken != null) {
            sessions.delete(SessionTokens.hash(sessionToken));
        }
    }

    /** Panic wipe: deletes the account and everything that belongs to it. */
    public void deleteAccount(User user) {
        users.deleteAccount(user.id());
    }
}
