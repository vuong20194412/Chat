package vuong20194412.chat.user_service;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/user")
class UserController {

    private final UserRepository repository;

    UserController(UserRepository repository) {
        this.repository = repository;
    }

    /**
     * Aggregate root
     * @return Iterable
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:} curl -v localhost:8100/api/user
     */
    @GetMapping({"", "/"})
    CollectionModel<EntityModel<UserRecord>> getAll() {
        List<EntityModel<UserRecord>> entityUserRecords = repository.findUserAll()
                .stream()
                .map(this::toModel)
                .toList();

        return CollectionModel.of(entityUserRecords,
                linkTo(methodOn(UserController.class).getAll()).withSelfRel());
    }

    /**
     * New
     * @param user from body
     * @return Object
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:} curl -v -X POST localhost:8100/api/user -H "content-type:application/json" -d "{\"email\": \"testemail@v.vn\", \"fullname\": \"test_fullname\", \"utcBirthday\": \"2024-10-01\", \"gender\": \"MALE\"}"
     */
    @PostMapping({"", "/"})
    ResponseEntity<EntityModel<User>> create(@RequestBody User user) { // be ensured user is not null // can have id in user
        if (!Utils.isValidEmailAddress(user.getEmail()))
            throw new UserUnprocessableEntityException(UserUnprocessableEntityException.Type.INVALID_EMAIL, "email");

        if (user.getFullname() == null || user.getFullname().isBlank())
            throw new UserUnprocessableEntityException(UserUnprocessableEntityException.Type.MISSING_FIELD, "fullname");

        if (user.getGender() != null && Arrays.stream(User.Gender.values()).noneMatch(gender -> gender == user.getGender()))
            throw new UserUnprocessableEntityException(UserUnprocessableEntityException.Type.VALUE_UNAVAILABLE, "gender");

        // Can have id value in user, so need to set id null because create not update
        user.setId(null);
        user.setEmail(user.getEmail().trim());
        user.setFullname(user.getFullname().trim());

        if (repository.existsByEmail(user.getEmail()))
            throw new UserUnprocessableEntityException(UserUnprocessableEntityException.Type.EXISTED_EMAIL, "email");

        EntityModel<User> entityUserRecord = toModel(repository.save(user));

        return ResponseEntity
                .created(entityUserRecord.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(entityUserRecord);
    }

    /**
     * One
     * @param id from path
     * @return Object
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:} curl -v localhost:8100/api/user/1
     */
    @GetMapping({"/{id}", "/{id}/"})
    EntityModel<UserRecord> get(@PathVariable Long id) {
        UserRecord userRecord = repository.findUserById(id);
        if (userRecord == null)
            throw new UserNotFoundException(id);

        return toModel(userRecord);
    }

    /**
     * Change
     * @param user from body
     * @param id from path
     * @return Object
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:} curl -v -X PUT localhost:8100/api/user/1 -H "content-type:application/json" -d "{\"email\": \"rtestemail@v.vn\", \"fullname\": \"r_test_fullname\", \"utcBirthday\": \"2024-10-02\", \"gender\": \"FEMALE\"}"
     */
    @PutMapping({"/{id}", "/{id}/"})
    ResponseEntity<EntityModel<User>> replace(@RequestBody User user, @PathVariable Long id) { // be ensured user is not null // can have id in user
        return repository.findById(id).map(currentUser -> {
            if (user.getEmail() != null) {
                user.setEmail(user.getEmail().trim());
                if (!currentUser.getEmail().equals(user.getEmail())
                        && Utils.isValidEmailAddress(user.getEmail())
                        && !repository.existsByEmail(user.getEmail())) {
                    currentUser.setEmail(user.getEmail());
                }
            }

            if (user.getFullname() != null && !user.getFullname().isBlank()) {
                currentUser.setFullname(user.getFullname().trim());
            }

            if (user.getGender() != null && Arrays.stream(User.Gender.values()).anyMatch(gender -> gender == user.getGender())) {
                currentUser.setGender(user.getGender());
            }

            EntityModel<User> entityUser = toModel(repository.save(currentUser));

            return ResponseEntity.accepted().header("Location", entityUser.getRequiredLink(IanaLinkRelations.SELF).toUri().toString()).body(entityUser);
        })
        .orElseThrow(() -> new UserNotFoundException(id));
    }

    /**
     * Remove
     * @param id from path
     * @return Object
     * @apiNote Remember remove spaces in path url when curl. {@code @HTTP_CURL_test:} curl -v -X DELETE localhost:8100/api/user/2
     */
    @DeleteMapping({"/{id}", "/{id}/"})
    ResponseEntity<?> delete(@PathVariable Long id) {
        repository.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * EntityModel.of UserRecord and links
     * @param userRecord constant
     * @return Object
     */
    EntityModel<UserRecord> toModel(UserRecord userRecord) {
        return EntityModel.of(userRecord,
                linkTo(methodOn(UserController.class).get(userRecord.id())).withSelfRel(),
                linkTo(methodOn(UserController.class).getAll()).withRel("user_list"));
    }

    /**
     * EntityModel.of User and links
     * @param user not constant
     * @return Object
     */
    EntityModel<User> toModel(User user) {
        return EntityModel.of(user,
                linkTo(methodOn(UserController.class).get(user.getId())).withSelfRel(),
                linkTo(methodOn(UserController.class).getAll()).withRel("user_list"));
    }

}
