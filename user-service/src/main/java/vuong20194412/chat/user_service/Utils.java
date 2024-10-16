package vuong20194412.chat.user_service;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
class Utils {

    static boolean isValidEmailAddress(@NonNull String email) {
        // pattern based on owasp.org/www-community/OWASP_Validation_Regex_Repository
        String c = "[a-zA-Z0-9_+&*-]+";
        String pattern = String.join(c, "^", "(?:\\.", ")*@(?:", "\\.)+[a-zA-z]{2,}");
        return Pattern.matches(pattern, email);
    }
}
