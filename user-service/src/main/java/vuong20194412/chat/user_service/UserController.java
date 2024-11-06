package vuong20194412.chat.user_service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/user")
class UserController {

    private final UserService service;

    private final HttpServletRequest request;

    @Autowired
    UserController(UserService service, HttpServletRequest request) {
        this.service = service;
        this.request = request;
    }

    /**
     * Aggregate root
     * @return Iterable - http code 200 if success
     * @apiNote Remember remove spaces in path url when curl.
     * {@code @HTTP_CURL_test:} curl -v localhost:8100/api/user
     */
    @GetMapping({"", "/"})
    CollectionModel<EntityModel<UserRecord>> getAll() {
        return CollectionModel
                .of(
                    service.getAllUsers().stream().map(this::toModel).toList(),
                    linkTo(methodOn(UserController.class).getAll()).withSelfRel()
                );
    }

    /**
     * New
     * @param userRecord from body
     * @return Object - contains http code 200 if success
     * @apiNote Remember remove spaces in path url when curl.
     * {@code @HTTP_CURL_test:} curl -v -X POST localhost:8100/api/user -H "content-type:application/json" -H "X-USER-SERVICE-TOKEN:x" -d "{\"email\": \"testemail@v.vn\", \"fullname\": \"test_fullname\", \"utc_birthday\": \"2024-10-01\", \"gender\": \"MALE\"}"
     */
    @PostMapping({"", "/"})
    ResponseEntity<EntityModel<User>> create(@RequestBody UserRecord userRecord) { // be ensured user is not null // can have id in user
        System.out.println(userRecord + "\n" + "X-Forwarded-Host: " + request.getHeader("X-Forwarded-Host"));
        if (request.getHeader("X-USER-SERVICE-TOKEN") == null)
            throw new UserUnauthorizedException("Could not create new user");

        User user = service.createUser(userRecord);

        EntityModel<User> entityUser = toModel(service.saveUser(user));

        URI location;
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedHost == null && forwardedProto == null)
            location = entityUser.getRequiredLink(IanaLinkRelations.SELF).toUri();
        else {
            UriComponents uriComponents = getServletUriComponentsBuilder(forwardedProto, forwardedHost, request.getHeader("X-Forwarded-Port")).build();

            location = getServletUri(ServletUriComponentsBuilder.fromUriString(entityUser.getRequiredLink(IanaLinkRelations.SELF).getHref()), uriComponents);
        }

        return ResponseEntity
                .created(location)
                .body(entityUser);
    }

    /**
     * One
     * @param id from path
     * @return Object - http code 201 if success
     * @apiNote Remember remove spaces in path url when curl.
     * {@code @HTTP_CURL_test:} curl -v localhost:8100/api/user/1
     */
    @GetMapping({"/{id}", "/{id}/"})
    EntityModel<UserRecord> get(@PathVariable Long id) {
        return toModel(service.getUser(id));
    }

    /**
     * Change
     * @param user from body
     * @param id from path
     * @return Object - http code 200 if success
     * @apiNote Remember remove spaces in path url when curl.
     * {@code @HTTP_CURL_test:} curl -v -X PUT localhost:8100/api/user/1 -H "content-type:application/json" -H "X-USER-ID:1" -d "{\"email\": \"rtestemail@v.vn\", \"fullname\": \"r_test_fullname\", \"utc_birthday\": \"2024-10-02\", \"gender\": \"FEMALE\"}"
     */
    @PutMapping({"/{id}", "/{id}/"})
    ResponseEntity<EntityModel<User>> replace(@RequestBody User user, @PathVariable Long id) { // be ensured user is not null // can have id in user
        System.out.println(user + "\n" + "X-Forwarded-Host: " + request.getHeader("X-Forwarded-Host"));
        if (!String.valueOf(id).equals(String.valueOf(request.getHeader("X-USER-ID"))))
            throw new UserUnauthorizedException();

        if (request.getHeader("X-USER-SERVICE-TOKEN") == null) {
            user.setEmail(null);
            user.setFullname(null);
        }

        User currentUser = service.updateUser(id, user);

        EntityModel<User> entityUser = toModel(service.saveUser(currentUser));

        URI location;
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedHost == null && forwardedProto == null)
            location = entityUser.getRequiredLink(IanaLinkRelations.SELF).toUri();
        else {
            UriComponents uriComponents = getServletUriComponentsBuilder(forwardedProto, forwardedHost, request.getHeader("X-Forwarded-Port")).build();

            location = getServletUri(ServletUriComponentsBuilder.fromUriString(entityUser.getRequiredLink(IanaLinkRelations.SELF).getHref()), uriComponents);
        }

        return ResponseEntity
                .ok()
                .header("Location", location.toString())
                .body(entityUser);
    }

    /**
     * Remove
     * @param id from path
     * @return Object - http code 204 if success
     * @apiNote Remember remove spaces in path url when curl.
     * {@code @HTTP_CURL_test:} curl -v -X DELETE localhost:8100/api/user/2 -H "X-USER-ID:2" -H "X-USER-SERVICE-TOKEN:x"
     */
    @DeleteMapping({"/{id}", "/{id}/"})
    ResponseEntity<?> delete(@PathVariable Long id) {
        if (!String.valueOf(id).equals(request.getHeader("X-USER-ID")))
            throw new UserUnauthorizedException();

        if (request.getHeader("X-USER-SERVICE-TOKEN") == null)
            throw new UserUnauthorizedException("Could not delete user");

        service.deleteUser(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * EntityModel.of UserRecord and links
     * @param userRecord constant
     * @return Object
     */
    EntityModel<UserRecord> toModel(UserRecord userRecord) {
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedHost == null && forwardedProto == null)
            return EntityModel.of(userRecord,
                    linkTo(methodOn(UserController.class).get(userRecord.id())).withSelfRel(),
                    linkTo(methodOn(UserController.class).getAll()).withRel("user_list"));

        UriComponents uriComponents = getServletUriComponentsBuilder(forwardedProto, forwardedHost, request.getHeader("X-Forwarded-Port")).build();

        return EntityModel.of(userRecord
                ,Link.of(getServletUri(linkTo(methodOn(UserController.class).get(userRecord.id())).toUriComponentsBuilder(), uriComponents).toString(), "self")
                ,Link.of(getServletUri(linkTo(methodOn(UserController.class).getAll()).toUriComponentsBuilder(), uriComponents).toString(), "user_list")
        );

    }

    /**
     * EntityModel.of User and links
     * @param user not constant
     * @return Object
     */
    EntityModel<User> toModel(User user) {
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedHost == null && forwardedProto == null)
            return EntityModel.of(user,
                    linkTo(methodOn(UserController.class).get(user.getId())).withSelfRel(),
                    linkTo(methodOn(UserController.class).getAll()).withRel("user_list"));

        UriComponents uriComponents = getServletUriComponentsBuilder(forwardedProto, forwardedHost, request.getHeader("X-Forwarded-Port")).build();

        return EntityModel.of(user
                ,Link.of(getServletUri(linkTo(methodOn(UserController.class).get(user.getId())).toUriComponentsBuilder(), uriComponents).toString(), "self")
                ,Link.of(getServletUri(linkTo(methodOn(UserController.class).getAll()).toUriComponentsBuilder(), uriComponents).toString(), "user_list")
        );

    }

    private URI getServletUri(UriComponentsBuilder  uriComponentsBuilder, UriComponents uriComponents) {
        return uriComponentsBuilder
                .scheme(uriComponents.getScheme())
                .host(uriComponents.getHost())
                .port(uriComponents.getPort())
                .build()
                .toUri();
    }

    private ServletUriComponentsBuilder getServletUriComponentsBuilder(String forwardedProto, String forwardedHost, String forwardedPort) {
        ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentContextPath();
        try {
            if (forwardedProto != null) {
                builder.scheme(forwardedProto);
            }
            if (forwardedHost != null) {
                builder.host(forwardedHost);
                builder.port(forwardedPort);
            }
            if (forwardedPort != null) {
                builder.port(forwardedPort);
            }
            return builder;
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("X-Forwarded-Host ERROR");
        }
    }

}
