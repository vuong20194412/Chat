package vuong20194412.chat.user_service;

import jakarta.persistence.*;

import java.util.Date;
import java.util.Objects;

@Entity(name = "user")
@Table(name = "users", catalog = "", schema = "", uniqueConstraints = {}, indexes = {})
class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "")
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String fullname;

    @Enumerated(value = EnumType.STRING)
    @Column(length = 32)
    private Gender gender;

    @Temporal(value = TemporalType.DATE)
    @Column(name = "birthday")
    private Date utcBirthday;

    enum Gender {
        MALE,
        FEMALE,
    }

    public User() {
        super();
    }

    public User(String email, String fullname, Gender gender, Date utcBirthday) {
        this.email = email != null ? email.toLowerCase() : null;
        this.fullname = fullname;
        this.gender = gender;
        this.utcBirthday = utcBirthday;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return lowerCaseEmail
     */
    public String getEmail() {
        return email;
    }

    /**
     * Perform lower case email and set it
     * @param email no need to lower case it in advance
     */
    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase() : null;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Date getUtcBirthday() {
        return utcBirthday;
    }

    public void setUtcBirthday(Date utcBirthday) {
        this.utcBirthday = utcBirthday;
    }

    @Override
    public int hashCode() {
        //return super.hashCode();
        return Objects.hash(this.id, this.email, this.fullname, this.gender, this.utcBirthday);
    }

    @Override
    public boolean equals(Object obj) {
        //return super.equals(obj);
        if (this == obj)
            return true;
        if (!(obj instanceof User o)) // pattern matching feature for instanceof, type checking + casting in one
            return false;             // introduced in java 14, officially included in java 17.
        return (Objects.equals(this.id, o.id)
                && Objects.equals(this.email, o.email)
                && Objects.equals(this.fullname, o.fullname)
                && Objects.equals(this.gender, o.gender)
                && Objects.equals(this.utcBirthday, o.utcBirthday)
        );
    }

    @Override
    public String toString() {
        return String.format("%s:{id:%s, email:%s, fullname:%s, gender:%s, utcBirthday:%s}",
                super.toString(),
                this.id,
                this.email,
                this.fullname,
                this.gender,
                this.utcBirthday);
    }
}

record UserRecord (Long id, String email, String fullname, User.Gender gender, Date utcBirthday){};
