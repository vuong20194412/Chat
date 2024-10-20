package vuong20194412.chat.authentication_api_gateway_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import vuong20194412.chat.authentication_api_gateway_service.repository.JsonWebTokenRepository;

import java.time.Instant;

@Configuration
@EnableScheduling
class SchedulerConfig {

//    @Value("${JWT_VALIDITY_PERIOD}")
//    private long jwtValidityPeriod;

    private final JsonWebTokenRepository jwtRepository;

    @Autowired
    SchedulerConfig(JsonWebTokenRepository jwtRepository) {
        this.jwtRepository = jwtRepository;
    }

    @Scheduled(fixedRateString = "#{${JWT_VALIDITY_PERIOD} * 1000}")
    public void deleteExpiredJwt() {
        jwtRepository.deleteByExpirationTimeLessThan(Instant.now().getEpochSecond());
    }

}
