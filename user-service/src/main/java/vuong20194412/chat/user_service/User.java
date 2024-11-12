package vuong20194412.chat.user_service;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.time.Instant;
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

    @JsonProperty(value = "utc_birthday")
    @Temporal(value = TemporalType.DATE)
    @Column(name = "birthday")
    private Date utcBirthday;

    @JsonProperty(value = "created_at")
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Instant createdAt;

    @JsonProperty(value = "updated_at")
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Instant updatedAt;

    enum Gender {
        MALE,
        FEMALE,
    }

    public User() {
        super();
    }

    public User(String lowerCaseEmail, String fullname, Gender gender, Date utcBirthday) {
        this.email = lowerCaseEmail;
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

    public String getEmail() {
        return email;
    }

    /**
     * @param lowerCaseEmail need to lower case it in advance
     */
    public void setEmail(String lowerCaseEmail) {
        this.email = lowerCaseEmail;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    //@PrePersist
    private void setCreatedAt() {
        if (createdAt == null)
            createdAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    //@PreUpdate
    private void setUpdatedAt() {
        updatedAt = Instant.now();
    }

    //@PrePersist
    //@PreUpdate
    private void ensureLowerCaseEmail() {
        email = email.toLowerCase().trim();
    }

    @PrePersist
    private void prePersist() {
        ensureLowerCaseEmail();
        setCreatedAt();
        System.out.println("BEFORE PERSIST: " + this);
    }

    @PreUpdate
    public void preUpdate() {
        ensureLowerCaseEmail();
        setUpdatedAt();
        if (createdAt == null || createdAt.isAfter(updatedAt)) {
            throw new IllegalArgumentException("Updated time must be smaller than Created time. Need call update from service");
        }
        System.out.println("BEFORE UPDATE: " + this);
    }

    @PreRemove
    private void preRemove() {
        System.out.println("BEFORE DELETE: " + this);
    }

    @PostPersist
    @PostUpdate
    private void logAfterSave() {
        System.out.println("AFTER SAVE: " + this);
    }

    @PostRemove
    private void logAfterDelete() {
        System.out.println("AFTER DELETE: " + this);
    }

    @Override
    public int hashCode() {
        //return super.hashCode();
        return Objects.hash(this.id, this.email, this.fullname, this.gender, this.utcBirthday, this.createdAt, this.updatedAt);
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
                && Objects.equals(this.createdAt, o.createdAt)
                && Objects.equals(this.updatedAt, o.updatedAt)
        );
    }

    @Override
    public String toString() {
        return String.format("%s\n\tUser[id=%s, email=%s, fullname=%s, gender=%s, utcBirthday=%s, createdAt=%s, updatedAt=%s]",
                super.toString(),
                this.id,
                this.email,
                this.fullname,
                this.gender,
                this.utcBirthday,
                this.createdAt,
                this.updatedAt);
    }

}

record UserRecord (
        Long id,
        String email,
        String fullname,
        User.Gender gender,
        @JsonProperty(value = "utc_birthday") Date utcBirthday,
        @JsonProperty(value = "created_at") Instant createdAt,
        @JsonProperty(value = "updated_at") Instant updatedAt
){}
