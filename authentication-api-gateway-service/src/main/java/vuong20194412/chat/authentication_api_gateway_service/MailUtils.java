package vuong20194412.chat.authentication_api_gateway_service;

import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

@Component
public class MailUtils {

    private String stmpHost;
    private String password;
    private String username;

    public MailUtils() {
        loadSetting();
    }

    public void loadSetting() {
        Map<String, Object> setting = SettingConfig.readSetting();
        stmpHost = (String) setting.get("mail_smtp_host");
        password = (String) setting.get("mail_password");
        username = (String) setting.get("mail_username");
        if (stmpHost == null || password == null || username == null) {
            throw new RuntimeException("Not found email setting in setting.json in resources");
        }
    }

    public void setStmpHost(String stmpHost) {
        this.stmpHost = stmpHost;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isValidEmailAddress(@NonNull String email) {
        // pattern based on owasp.org/www-community/OWASP_Validation_Regex_Repository
        // and reference RFC 822 and RFC 5322
        String pc = "([a-zA-Z0-9]|([a-zA-Z0-9][_+&*-][a-zA-Z0-9]))+";
        String dc = "([a-zA-Z0-9]|([a-zA-Z0-9]-[a-zA-Z0-9]))+";
        String pattern =  "^" + pc + "(?:\\." + pc + ")*@(?:" + dc + "\\.)+[a-zA-z]{2,}$";
        //System.out.println(Pattern.compile(pattern));
        return Pattern.matches(pattern, email);
    }

    /**
     *
     * @param recipients recipients separate by ,
     * @param subject title
     * @param text html content
     * @throws AddressException exist invalid email recipient address
     */
    public void sendHtml(@NonNull String recipients, @NonNull String subject, @NonNull String text) throws AddressException {
        Address address;
        try {
            address = new InternetAddress(username);
        } catch (AddressException e) {
            System.out.println(e.getClass());
            throw new RuntimeException(e);
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", stmpHost);
        properties.put("mail.smtp.port", "587"); // for tls

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Address[] recipientAddresses = InternetAddress.parse(recipients);
        if (recipientAddresses.length == 0)
            throw new AddressException();

        Message message = new MimeMessage(session);

        try {
            message.setFrom(address);
            message.setRecipients(Message.RecipientType.TO, recipientAddresses);
            message.setSubject(encodeWithUTF8AndBase64(subject));
            //message.setText(encodeWithUTF8AndBase64(text));
            message.setContent(text, "text/html");

            Transport.send(message);

            System.out.println(String.format("send to %s OK.", recipients));
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private String encodeWithUTF8AndBase64(String text) {
        try {
            // B is Base64 encoding
            return MimeUtility.encodeText(text, StandardCharsets.UTF_8.displayName(), "B");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
