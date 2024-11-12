package vuong20194412.chat.user_service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/user")
class UserController {

    private final UserService service;

    private final HttpServletRequest request;

    private ServletUriComponentsBuilder uriComponentsBuilder;

    @Autowired
    UserController(UserService service, HttpServletRequest request) {
        this.service = service;
        this.request = request;
    }

    /**
     * Aggregate root
     *
     * @return Iterable - http code 200 if success
     * @apiNote Remember remove spaces in path url when curl.
     * {@code @HTTP_CURL_test:} curl -v localhost:8100/api/user
     */
    @GetMapping({"", "/"})
    CollectionModel<EntityModel<UserRecord>> getAll() {
        setUriComponentsBuilder();

        Link selfLink;
        if (uriComponentsBuilder == null)
            selfLink = linkTo(methodOn(UserController.class).getAll()).withSelfRel();
        else {
            String path = linkTo(methodOn(UserController.class).getAll()).toUriComponentsBuilder().build().getPath();
            selfLink = Link.of(uriComponentsBuilder.replacePath(path).toUriString(), "self");
        }

        return CollectionModel.of(service
                .getAllUsers()
                .stream()
                .map(this::toModel).toList(), selfLink);
    }

    /**
     * New
     *
     * @param userRecord from body
     * @return Object - contains http code 200 if success
     * @apiNote Remember remove spaces in path url when curl.
     * {@code @HTTP_CURL_test:} curl -v -X POST localhost:8100/api/user -H "content-type:application/json" -H "X-USER-SERVICE-TOKEN:x" -d "{\"email\": \"testemail@v.vn\", \"fullname\": \"test_fullname\", \"utc_birthday\": \"2024-10-01\", \"gender\": \"MALE\"}"
     */
    @PostMapping({"", "/"})
    ResponseEntity<EntityModel<User>> create(@RequestBody UserRecord userRecord) { // be ensured user is not null // can have id in user
        setUriComponentsBuilder();

        System.out.println(userRecord);
        if (request.getHeader("X-USER-SERVICE-TOKEN") == null)
            throw new UserUnauthorizedException("Could not create new user");

        User user = service.createUser(userRecord);

        EntityModel<User> entityUser = toModel(service.saveUser(user));

        Link link = entityUser.getRequiredLink(IanaLinkRelations.SELF);

        if (uriComponentsBuilder == null)
            return ResponseEntity
                    .created(link.toUri())
                    .body(entityUser);

        try {
            String path = ServletUriComponentsBuilder.fromUriString(link.getHref()).build().getPath();
            URI location = uriComponentsBuilder.replacePath(path != null ? path : "").build().toUri();
            return ResponseEntity
                    .created(location)
                    .body(entityUser);
        } catch (Exception ex) {
            return ResponseEntity
                    .created(link.toUri())
                    .body(entityUser);
        }
    }

    /**
     * One
     *
     * @param id from path
     * @return Object - http code 201 if success
     * @apiNote Remember remove spaces in path url when curl.
     * {@code @HTTP_CURL_test:} curl -v localhost:8100/api/user/1
     */
    @GetMapping({"/{id}", "/{id}/"})
    EntityModel<UserRecord> get(@PathVariable Long id) {
        setUriComponentsBuilder();

        return toModel(service.getUser(id));
    }

    /**
     * Change
     *
     * @param user from body
     * @param id   from path
     * @return Object - http code 200 if success
     * @apiNote Remember remove spaces in path url when curl.
     * {@code @HTTP_CURL_test:} curl -v -X PUT localhost:8100/api/user/1 -H "content-type:application/json" -H "X-USER-ID:1" -d "{\"email\": \"rtestemail@v.vn\", \"fullname\": \"r_test_fullname\", \"utc_birthday\": \"2024-10-02\", \"gender\": \"FEMALE\"}"
     */
    @PutMapping({"/{id}", "/{id}/"})
    ResponseEntity<EntityModel<User>> replace(@RequestBody User user, @PathVariable Long id) { // be ensured user is not null // can have id in user
        setUriComponentsBuilder();

        System.out.println(user);
        if (!String.valueOf(id).equals(String.valueOf(request.getHeader("X-USER-ID"))))
            throw new UserUnauthorizedException();

        if (request.getHeader("X-USER-SERVICE-TOKEN") == null) {
            user.setEmail(null);
            user.setFullname(null);
        }

        User currentUser = service.updateUser(id, user);

        EntityModel<User> entityUser = toModel(service.saveUser(currentUser));

        Link link = entityUser.getRequiredLink(IanaLinkRelations.SELF);

        if (uriComponentsBuilder == null)
            return ResponseEntity
                    .ok()
                    .header("Location", link.toUri().toString())
                    .body(entityUser);

        try {
            String path = ServletUriComponentsBuilder.fromUriString(link.getHref()).build().getPath();
            String location = uriComponentsBuilder.replacePath(path != null ? path : "").toUriString();
            return ResponseEntity
                    .ok()
                    .header("Location", location)
                    .body(entityUser);
        } catch (Exception ex) {
            return ResponseEntity
                    .ok()
                    .header("Location", link.toUri().toString())
                    .body(entityUser);
        }
    }

    /**
     * Remove
     *
     * @param id from path
     * @return Object - http code 204 if success
     * @apiNote Remember remove spaces in path url when curl.
     * {@code @HTTP_CURL_test:} curl -v -X DELETE localhost:8100/api/user/2 -H "X-USER-ID:2" -H "X-USER-SERVICE-TOKEN:x"
     */
    @DeleteMapping({"/{id}", "/{id}/"})
    ResponseEntity<?> delete(@PathVariable Long id) {
        //setUriComponentsBuilder();

        if (!String.valueOf(id).equals(request.getHeader("X-USER-ID")))
            throw new UserUnauthorizedException();

        if (request.getHeader("X-USER-SERVICE-TOKEN") == null)
            throw new UserUnauthorizedException("Could not delete user");

        service.deleteUser(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * EntityModel.of UserRecord and links
     *
     * @param userRecord constant
     * @return Object
     */
    @NonNull
    EntityModel<UserRecord> toModel(@NonNull UserRecord userRecord) {
        if (uriComponentsBuilder == null)
            return EntityModel.of(userRecord,
                    linkTo(methodOn(UserController.class).get(userRecord.id())).withSelfRel(),
                    linkTo(methodOn(UserController.class).getAll()).withRel("user_list"));

        try {
            String onePath = linkTo(methodOn(UserController.class).get(userRecord.id())).toUriComponentsBuilder().build().getPath();
            String allPath = linkTo(methodOn(UserController.class).getAll()).toUriComponentsBuilder().build().getPath();

            EntityModel<UserRecord> entityModel = EntityModel.of(userRecord);

            if (onePath != null)
                entityModel.add(Link.of(uriComponentsBuilder.replacePath(onePath).toUriString(), "self"));
            if (allPath != null)
                entityModel.add(Link.of(uriComponentsBuilder.replacePath(allPath).toUriString(), "user_list"));

            return entityModel;

        } catch (Exception ex) {
            return EntityModel.of(userRecord,
                    linkTo(methodOn(UserController.class).get(userRecord.id())).withSelfRel(),
                    linkTo(methodOn(UserController.class).getAll()).withRel("user_list"));
        }
    }

    /**
     * EntityModel.of User and links
     *
     * @param user not constant
     * @return Object
     */
    @NonNull
    EntityModel<User> toModel(@NonNull User user) {
        if (uriComponentsBuilder == null)
            return EntityModel.of(user,
                    linkTo(methodOn(UserController.class).get(user.getId())).withSelfRel(),
                    linkTo(methodOn(UserController.class).getAll()).withRel("user_list"));

        try {
            String onePath = linkTo(methodOn(UserController.class).get(user.getId())).toUriComponentsBuilder().build().getPath();
            String allPath = linkTo(methodOn(UserController.class).getAll()).toUriComponentsBuilder().build().getPath();

            EntityModel<User> entityModel = EntityModel.of(user);

            if (onePath != null)
                entityModel.add(Link.of(uriComponentsBuilder.replacePath(onePath).toUriString(), "self"));
            if (allPath != null)
                entityModel.add(Link.of(uriComponentsBuilder.replacePath(allPath).toUriString(), "user_list"));

            return entityModel;

        } catch (Exception ex) {
            return EntityModel.of(user,
                    linkTo(methodOn(UserController.class).get(user.getId())).withSelfRel(),
                    linkTo(methodOn(UserController.class).getAll()).withRel("user_list"));
        }
    }

    private void setUriComponentsBuilder() {
        String forwardedHost = this.request.getHeader("X-Forwarded-Host");
        String forwardedPort = this.request.getHeader("X-Forwarded-Port");
        String forwardedProto = this.request.getHeader("X-Forwarded-Proto");
        System.out.println("X-Forwarded-Host: " + forwardedHost +
                " | X-Forwarded-Port: " + forwardedPort +
                " | X-Forwarded-Proto: " + forwardedProto);
        if (forwardedHost == null && forwardedProto == null && forwardedPort == null)
            this.uriComponentsBuilder = null;
        else
            this.uriComponentsBuilder = getForwardedServletUriComponentsBuilder(forwardedProto, forwardedHost, forwardedPort);
    }

    @NonNull
    private ServletUriComponentsBuilder getForwardedServletUriComponentsBuilder(@Nullable String forwardedProto,
                                                                                @Nullable String forwardedHost,
                                                                                @Nullable String forwardedPort) throws IllegalArgumentException {
        ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentContextPath();
        try {
            if (forwardedProto != null)
                builder.scheme(forwardedProto.split(",")[0]);

            if (forwardedHost != null) {
                String firstForwardedHost = forwardedHost.split(",")[0];
                int colonIndex = firstForwardedHost.indexOf(":");
                if (colonIndex > 0 && colonIndex == firstForwardedHost.lastIndexOf(":")) {
                    builder.host(firstForwardedHost.substring(0, colonIndex));
                    builder.port(firstForwardedHost.substring(colonIndex + 1));
                } else {
                    builder.host(firstForwardedHost);
                    if (forwardedPort != null) {
                        builder.port(forwardedPort.split(",")[0]);
                    } else {
                        builder.port(null);
                    }
                }
            } else if (forwardedPort != null)
                builder.port(forwardedPort.split(",")[0]);

            return builder;
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
            throw new IllegalArgumentException("X-Forwarded-* ERROR");
        }
    }

}
