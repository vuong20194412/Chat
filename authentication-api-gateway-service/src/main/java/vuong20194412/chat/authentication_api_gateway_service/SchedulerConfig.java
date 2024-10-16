package vuong20194412.chat.authentication_api_gateway_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;

@Configuration
@EnableScheduling
class SchedulerConfig {

    private final JsonWebTokenRepository jwtRepository;

    @Autowired
    SchedulerConfig(JsonWebTokenRepository jwtRepository) {
        this.jwtRepository = jwtRepository;
    }

    @Scheduled(fixedRate = 1000 * 3600 * 24 * 7)
    public void deleteExpiredJwt() {
        jwtRepository.deleteByExpirationTimeLessThan(Instant.now().getEpochSecond());
    }

}
