package com.kanakani.security;

/**
 * DEVELOPMENT ONLY. Prints the code to the server log instead of sending an SMS.
 * Never enable this in production: it writes the one-time code (and the number) to a log file.
 */
public final class ConsoleSmsGateway implements OtpService.SmsGateway {

    @Override
    public void send(String e164Number, String message) {
        System.out.println("[DEV SMS] to " + e164Number + ": " + message);
    }
}
