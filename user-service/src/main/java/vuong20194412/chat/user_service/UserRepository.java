package vuong20194412.chat.user_service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = "SELECT new vuong20194412.chat.user_service.UserRecord(u.id, u.email, u.fullname, u.gender, u.utcBirthday) " +
            "FROM user u " // user is name entity
    )
    List<UserRecord> findUserAll();

    UserRecord findUserById(Long id);

//    @Query(value = "SELECT new UserRecord(u.id, u.email, u.fullname, u.gender, u.birthday) " +
//            "FROM user u " + // user is name entity
//            "WHERE u.id = :id AND u.email = :email")
//    UserRecord findUserByIdAndEmail(@Param(value = "id") Long id, @Param(value = "email") String email);

    UserRecord findUserByIdAndEmail(Long id, String email);

    boolean existsByEmail(String email);
}


