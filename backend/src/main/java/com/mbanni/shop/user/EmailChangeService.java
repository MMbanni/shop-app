package com.mbanni.shop.user;
        
        import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.login.LoginRateLimiter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

        @Service
public class EmailChangeService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordsEncoder;
    private final LoginRateLimiter limiter;
    private final ObjectProvider<JavaMailSender> senders;
    private final Clock clock;
    private final String from;

            public EmailChangeService(UserRepository users, PasswordEncoder passwords, LoginRateLimiter limiter,
            ObjectProvider<JavaMailSender> senders, Clock clock, @Value("${account.mail.from:}") String from) {
                this.userRepository = users;
                this.passwordsEncoder = passwords;
                this.limiter = limiter;
                this.senders = senders;
                this.clock = clock;
                this.from = from;
            }

            @Transactional
    public void request(long id, String email, String password, String ip) {
                User user = locked(id);
                limiter.checkAllowed(user.getEmail(), ip);
                if (!passwordsEncoder.matches(password, user.getPassword())) throw invalid("Check your current password.");
                String target = email.strip().toLowerCase(Locale.ROOT);
                if (target.equals(user.getEmail()) || userRepository.existsByEmail(target)) throw invalid("Choose a different email.");
                JavaMailSender sender = senders.getIfAvailable();
                if (sender == null || from.isBlank()) throw invalid("Email delivery is unavailable.");
                String code = UUID.randomUUID().toString();
                user.setPendingEmail(target, hash(code), clock.instant().plusSeconds(900));
                send(sender, user.getEmail(), "Email change requested",
                                "A change to your shop login email was requested. If this was unexpected, contact the shop.");
                send(sender, target, "Verify your shop email", "Paste this code into My account within 15 minutes: "  code);
            }

            @Transactional
    public void confirm(long id, String code) {
                User user = locked(id);
                if (user.verificationExpires() == null || !clock.instant().isBefore(user.verificationExpires())
                                || !hash(code.strip()).equals(user.verificationHash())) throw invalid("Invalid or expired code.");
                if (userRepository.existsByEmail(user.pendingEmail())) throw invalid("That email is already used.");
                user.setEmail(user.pendingEmail());
                user.setPendingEmail(null, null, null);
            }

            private User locked(long id) {
                return userRepository.findByIdForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            }
    private BusinessException invalid(String message) { return new BusinessException(ErrorCode.ILLEGAL_OPERATION, message); }
    private String hash(String code) {
                try {
                        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8)));
                    } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
            }
    private void send(JavaMailSender sender, String to, String subject, String body) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                sender.send(message);
            }
}