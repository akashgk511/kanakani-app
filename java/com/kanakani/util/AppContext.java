package com.kanakani.util;

import com.kanakani.dao.FlagDao;
import com.kanakani.dao.IdeaDao;
import com.kanakani.dao.JobDao;
import com.kanakani.dao.OtpDao;
import com.kanakani.dao.PostDao;
import com.kanakani.dao.RankDao;
import com.kanakani.dao.ReceiptDao;
import com.kanakani.dao.SessionDao;
import com.kanakani.dao.UserDao;
import com.kanakani.security.ConsoleSmsGateway;
import com.kanakani.security.EnvKeyProvider;
import com.kanakani.security.OtpService;
import com.kanakani.security.PhoneVault;
import com.kanakani.security.PostCrypto;
import com.kanakani.security.RateLimiter;
import com.kanakani.service.AuthService;
import com.kanakani.service.FeedService;
import com.kanakani.service.IdeaService;
import com.kanakani.service.RankService;
import com.kanakani.service.ReceiptService;

import java.util.Base64;

/**
 * Builds the services once and hands them to the servlets. It fails fast at startup if a secret
 * is missing, so the app never runs half-configured.
 *
 * Secrets come from environment variables or -D system properties:
 *   KANAKANI_PHONE_KEY   base64, at least 32 bytes   (keys phone numbers into tokens)
 *   KANAKANI_OTP_KEY     base64, at least 32 bytes   (keys one-time code MACs)
 *   KANAKANI_POST_KEYS   "1:base64,2:base64", 32 bytes each (seals post text)
 *   KANAKANI_SMS         "console" for development only. Leave unset in production and plug in a real gateway.
 */
public final class AppContext {

    private static volatile AppContext instance;

    public final AuthService auth;
    public final FeedService feed;
    public final ReceiptService receipts;
    public final RankService ranks;
    public final JobDao jobs;
    public final IdeaService ideas;
    public final FlagDao flags;
    public final RateLimiter limiter = new RateLimiter();
    public final byte[] csrfKey;

    private AppContext() {
        PhoneVault vault = new PhoneVault(decode(setting("KANAKANI_PHONE_KEY")));
        byte[] otpKey = decode(setting("KANAKANI_OTP_KEY"));
        OtpService otp = new OtpService(otpKey);
        PostCrypto crypto = new PostCrypto(new EnvKeyProvider(setting("KANAKANI_POST_KEYS")));

        OtpService.SmsGateway sms;
        if ("console".equalsIgnoreCase(optional("KANAKANI_SMS"))) {
            sms = new ConsoleSmsGateway();
        } else {
            throw new IllegalStateException("No SMS gateway configured. Set KANAKANI_SMS=console for development, "
                + "or add a real OtpService.SmsGateway implementation.");
        }

        this.csrfKey = otpKey.clone(); // CSRF tokens are derived with a key that is already secret; see CsrfFilter
        this.auth = new AuthService(new UserDao(), new SessionDao(), new OtpDao(), vault, otp, sms, limiter);
        this.feed = new FeedService(new PostDao(), crypto);
        this.receipts = new ReceiptService(new ReceiptDao());
        this.ranks = new RankService(new RankDao(), crypto);
        this.jobs = new JobDao();
        this.ideas = new IdeaService(new IdeaDao(), limiter);
        this.flags = new FlagDao();
    }

    public static AppContext get() {
        AppContext c = instance;
        if (c == null) {
            synchronized (AppContext.class) {
                c = instance;
                if (c == null) {
                    c = new AppContext();
                    instance = c;
                }
            }
        }
        return c;
    }

    private static String optional(String name) {
        String v = System.getenv(name);
        return v != null ? v : System.getProperty(name);
    }

    private static String setting(String name) {
        String v = optional(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing setting " + name);
        }
        return v;
    }

    private static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64.trim());
    }
}
