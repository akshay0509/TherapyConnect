package com.org.userService.Services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.gmail.client-id}")
    private String clientId;

    @Value("${google.gmail.client-secret}")
    private String clientSecret;

    @Value("${google.gmail.refresh-token}")
    private String refreshToken;

    @Value("${google.gmail.application-name}")
    private String applicationName;

    @Value("${google.gmail.from}")
    private String fromAddress;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        try {
            send(toEmail, "Password Reset Request", buildResetEmailHtml(resetLink));
            logger.info("Password reset email sent to {}", maskEmail(toEmail));
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", maskEmail(toEmail), e.getMessage());
        }
    }

    public void sendUsernameEmail(String toEmail, String username) {
        try {
            send(toEmail, "Your TherapyConnect Username", buildUsernameEmailHtml(username));
            logger.info("Username reminder email sent to {}", maskEmail(toEmail));
        } catch (Exception e) {
            logger.error("Failed to send username email to {}: {}", maskEmail(toEmail), e.getMessage());
        }
    }

    private void send(String toEmail, String subject, String htmlBody) throws Exception {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRefreshToken(refreshToken)
                .build();

        Gmail gmail = new Gmail.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(applicationName)
                .build();

        byte[] rawMessage = buildRawMimeMessage(toEmail, subject, htmlBody);
        String encoded = Base64.getUrlEncoder().encodeToString(rawMessage);

        Message message = new Message();
        message.setRaw(encoded);
        gmail.users().messages().send("me", message).execute();
    }

    private byte[] buildRawMimeMessage(String toEmail, String subject, String htmlBody) {
        String raw = "From: " + fromAddress + "\r\n"
                + "To: " + toEmail + "\r\n"
                + "Subject: " + subject + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "\r\n"
                + htmlBody;
        return raw.getBytes(StandardCharsets.UTF_8);
    }

    private String buildResetEmailHtml(String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background: #f8fafc; padding: 40px 0; margin: 0;">
                  <div style="max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 12px;
                              border: 1px solid #e2e8f0; overflow: hidden;">
                    <div style="background: #1e293b; padding: 28px 32px;">
                      <h1 style="color: #f0f6fc; font-size: 1.2rem; margin: 0; font-weight: 700;">Password Reset</h1>
                    </div>
                    <div style="padding: 32px;">
                      <p style="color: #475569; font-size: 0.95rem; line-height: 1.7; margin: 0 0 24px;">
                        We received a request to reset the password for your account.
                        Click the button below to choose a new password.
                        This link expires in <strong>30 minutes</strong>.
                      </p>
                      <a href="%s" style="display: inline-block; background: #3b82f6; color: #ffffff;
                         text-decoration: none; padding: 12px 28px; border-radius: 8px;
                         font-size: 0.9rem; font-weight: 600; margin-bottom: 28px;">
                        Reset Password
                      </a>
                      <p style="color: #94a3b8; font-size: 0.82rem; margin: 0; line-height: 1.6;">
                        If you did not request a password reset, please ignore this email.
                        Your account remains secure and no changes have been made.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(resetLink);
    }

    private String buildUsernameEmailHtml(String username) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background: #f8fafc; padding: 40px 0; margin: 0;">
                  <div style="max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 12px;
                              border: 1px solid #e2e8f0; overflow: hidden;">
                    <div style="background: #1e293b; padding: 28px 32px;">
                      <h1 style="color: #f0f6fc; font-size: 1.2rem; margin: 0; font-weight: 700;">Your Username</h1>
                    </div>
                    <div style="padding: 32px;">
                      <p style="color: #475569; font-size: 0.95rem; line-height: 1.7; margin: 0 0 20px;">
                        Here is the username associated with this email address:
                      </p>
                      <div style="background: #f1f5f9; border-radius: 8px; padding: 16px 20px; margin-bottom: 24px;
                                  font-family: monospace; font-size: 1.1rem; color: #1e293b; font-weight: 700;
                                  letter-spacing: 0.02em;">
                        %s
                      </div>
                      <p style="color: #94a3b8; font-size: 0.82rem; margin: 0; line-height: 1.6;">
                        If you did not request this, please ignore this email.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(username);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***@***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
