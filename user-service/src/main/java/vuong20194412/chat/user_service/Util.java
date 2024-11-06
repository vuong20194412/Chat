package vuong20194412.chat.user_service;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
class Util {

    static boolean isNotValidEmailAddress(@NonNull String email) {
        // pattern based on owasp.org/www-community/OWASP_Validation_Regex_Repository
        // and reference RFC 822 and RFC 5322 // String c = "[a-zA-Z0-9_+&*-]+";
        // String pattern = String.join(c, "^", "(?:\\.", ")*@(?:", "\\.)+[a-zA-z]{2,}");
        String pc = "([a-zA-Z0-9]|([a-zA-Z0-9][_+&*-][a-zA-Z0-9]))+";
        String dc = "([a-zA-Z0-9]|([a-zA-Z0-9]-[a-zA-Z0-9]))+";
        String pattern =  "^" + pc + "(?:\\." + pc + ")*@(?:" + dc + "\\.)+[a-zA-z]{2,}$";
        return !Pattern.matches(pattern, email);
    }

}
