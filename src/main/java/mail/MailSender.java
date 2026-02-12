package mail;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.util.Properties;

public class MailSender {

    private static final String EMAIL =
            System.getenv("MAIL_USER");

    private static final String PASSWORD =
            System.getenv("MAIL_PASS");

    public static void send(String to, String code) {

        if (EMAIL == null || PASSWORD == null) {
            throw new RuntimeException(
                    "Mail credentials not configured"
            );
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(
                props,
                new Authenticator() {
                    protected PasswordAuthentication
                    getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                EMAIL,
                                PASSWORD
                        );
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(to)
            );
            message.setSubject("Verify your account");
            message.setText(
                    "Your verification code: " + code
            );

            Transport.send(message);

            System.out.println("Email sent!");

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
