package vuong20194412.chat.authentication_api_gateway_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import vuong20194412.chat.authentication_api_gateway_service.repository.BlackJsonWebTokenHashRepository;

import java.time.Instant;

@Configuration
@EnableScheduling
class SchedulerConfig {

    private final BlackJsonWebTokenHashRepository blackTokenHashRepository;

    @Autowired
    SchedulerConfig(BlackJsonWebTokenHashRepository blackTokenHashRepository) {
        this.blackTokenHashRepository = blackTokenHashRepository;
    }

    @Scheduled(fixedRateString = "#{${BLACKLIST_TOKEN_PERIOD} * 1000}")
    public void deleteExpiredJwt() {
        blackTokenHashRepository.deleteAllByExpirationTimeLessThanEqual(Instant.now().getEpochSecond());
    }

}
