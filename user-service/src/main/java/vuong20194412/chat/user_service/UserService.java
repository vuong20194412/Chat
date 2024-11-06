package vuong20194412.chat.user_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
class UserService {

    private final UserRepository repository;

    @Autowired
    UserService(UserRepository repository) {
        this.repository = repository;
    }

    List<UserRecord> getAllUsers() {
        return repository.findUserAll();
    }

    UserRecord getUser(Long id) {
        UserRecord userRecord = repository.findUserById(id);
        if (userRecord == null)
            throw new UserNotFoundException(id);

        return userRecord;
    }

    User createUser(UserRecord userRecord) {
        Map<UserUnprocessableEntityException.Type, List<String>> errors = new HashMap<>();

        String email = userRecord.email() != null ? userRecord.email().trim().toLowerCase() : "";
        if (Util.isNotValidEmailAddress(email)) {
            errors.put(UserUnprocessableEntityException.Type.INVALID_EMAIL, new ArrayList<>());
            errors.get(UserUnprocessableEntityException.Type.INVALID_EMAIL).add("email");
        }
        else if (repository.existsByEmail(email)) {
            errors.put(UserUnprocessableEntityException.Type.EXISTED_EMAIL, new ArrayList<>());
            errors.get(UserUnprocessableEntityException.Type.EXISTED_EMAIL).add("email");
        }

        String fullname = userRecord.fullname() != null ? userRecord.fullname().trim() : "";
        if (fullname.isBlank()) {
            errors.put(UserUnprocessableEntityException.Type.MISSING_FIELD, new ArrayList<>());
            errors.get(UserUnprocessableEntityException.Type.MISSING_FIELD).add("fullname");
        }

        if (!errors.isEmpty())
            throw new UserUnprocessableEntityException(errors);

        User user = new User();

        user.setEmail(email);
        user.setFullname(fullname);
        user.setGender(userRecord.gender());
        user.setUtcBirthday(userRecord.utcBirthday());

        return user;
    }

    User updateUser(Long id, User user) {
        User currentUser = repository.findById(id).orElse(null);
        if (currentUser == null)
            throw new UserNotFoundException(id);

        Map<UserUnprocessableEntityException.Type, List<String>> errors = new HashMap<>();

        String email;
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            email = currentUser.getEmail();
        }
        else {
            email = user.getEmail().trim().toLowerCase();
            if (!user.getEmail().equals(currentUser.getEmail())) {
                if (Util.isNotValidEmailAddress(email)) {
                    errors.put(UserUnprocessableEntityException.Type.INVALID_EMAIL, new ArrayList<>());
                    errors.get(UserUnprocessableEntityException.Type.INVALID_EMAIL).add("email");
                } else if (repository.existsByEmail(email)) {
                    errors.put(UserUnprocessableEntityException.Type.EXISTED_EMAIL, new ArrayList<>());
                    errors.get(UserUnprocessableEntityException.Type.EXISTED_EMAIL).add("email");
                }
            }
        }

        String fullname = user.getFullname() != null ? user.getFullname().trim() : currentUser.getFullname();

        if (!errors.isEmpty())
            throw new UserUnprocessableEntityException(errors);

        currentUser.setEmail(email);
        currentUser.setFullname(fullname);
        currentUser.setGender(user.getGender() != null ? user.getGender() : currentUser.getGender());
        currentUser.setUtcBirthday(user.getUtcBirthday() != null ? user.getUtcBirthday() : currentUser.getUtcBirthday());

        return currentUser;
    }

    User saveUser(@NonNull User user) {
        if (user.getCreatedAt() == null) {
            UserRecord userRecord = repository.findUserById(user.getId());
            if (userRecord != null) {
                try {
                    Field field = User.class.getDeclaredField("createdAt");
                    field.setAccessible(true);
                    field.set(user, userRecord.createdAt());
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

            }
        }
        return repository.save(user);
    }

    void deleteUser(Long id) {
        repository.deleteById(id);
    }

}
