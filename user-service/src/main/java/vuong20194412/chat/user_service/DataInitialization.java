package vuong20194412.chat.user_service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Date;

@Configuration
class DataInitialization {

    @Bean
    CommandLineRunner initializeData(UserService service) {
        try {
            // With strategy = GenerationType.IDENTITY, increase id auto
            // With database hsqldb
            // if not set id -> create with id = current max id + 1
            // if set id > current max id -> create with id = current max id + 1
            // if set id <= current max id and still existed id -> update with this id
            // if set id <= 0 -> create with id = current max id + 1
            // if set id <= current max id and not still existed id -> create with id = current max id + 1
            User user1 = new User("emailxxx.1@v.vn", "fullname1xx", User.Gender.MALE, Date.from(Instant.now()));
            user1.setId(2L); // although table have yet not user id = 2, after save user1 still use id = 1, not 2
            User user2 = new User("emailxxx.2@v.vn", "fullname2xx", User.Gender.FEMALE, Date.from(Instant.now()));
            // If not set id (current max id = 1) -> after save auto set id 2
            User user3 = new User("emailxxx.3@v.vn", "fullname3xx", User.Gender.FEMALE, Date.from(Instant.now()));
            user3.setId(1L); // user1.getId() = 1 -> after save update user1
            User user4 = new User("emailxxx.4@v.vn", "fullname4xx", User.Gender.MALE, Date.from(Instant.now()));
            // If not set id (current max id = 2) -> after save auto set id 3
            User user5 = new User("emailxxx.5@v.vn", "fullname5xx", User.Gender.FEMALE, Date.from(Instant.now()));
            user5.setId(2L); // although remove user2, (current max id = 3) after save user5 still use id = 4, not 2
            User user6 = new User("emailxxx.6@v.vn", "fullname6xx", null, null);
            user6.setId(0L);

            return args -> {
                service.saveUser(user1);
                //repository.flush();
                service.saveUser(user2);
                //repository.flush();
                service.saveUser(user3);
                //repository.flush();
                service.deleteUser(2L);
                service.saveUser(user4);
                service.saveUser(user5);
                service.saveUser(user6);
                // flush or not flush -> same result
            };
        } catch (Exception e) {
            return args -> {};
        }
    }
}
