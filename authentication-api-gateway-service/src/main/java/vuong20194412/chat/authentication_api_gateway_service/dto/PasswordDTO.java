package vuong20194412.chat.authentication_api_gateway_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PasswordDTO(
        @JsonProperty("password") String rawPassword,
        @JsonProperty("new_password") String rawNewPassword,
        @JsonProperty("repeated_new_password") String rawRepeatedNewPassword) {
}
