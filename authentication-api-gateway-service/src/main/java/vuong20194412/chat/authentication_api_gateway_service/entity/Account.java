package vuong20194412.chat.authentication_api_gateway_service.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.Instant;
import java.util.Objects;

@Entity(name = "account")
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String fullname;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @JsonProperty("is_enabled")
    @Column(name = "is_enabled")
    private boolean isEnabled;

    @JsonProperty("is_not_locked")
    @Column(name = "is_non_locked")
    private boolean isNonLocked;

    private String gmail;

    @JsonProperty("facebook_id")
    private String facebook;

    @JsonProperty("linkedin_id")
    private String linkedin;

    private String outlook;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Account() {
        super();
        this.isEnabled = true;
        this.isNonLocked = true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id,
                this.password,
                this.email,
                this.fullname,
                this.userId,
                this.isEnabled,
                this.isNonLocked,
                this.gmail,
                this.facebook,
                this.linkedin,
                this.outlook);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Account o)) // pattern matching feature for instanceof, type checking + casting in one
            return false;             // introduced in java 14, officially included in java 17.
        return (Objects.equals(this.id, o.id)
                && Objects.equals(this.password, o.password)
                && Objects.equals(this.email, o.email)
                && Objects.equals(this.fullname, o.fullname)
                && Objects.equals(this.userId, o.userId)
                && Objects.equals(this.isEnabled, o.isEnabled)
                && Objects.equals(this.isNonLocked, o.isNonLocked)
                && Objects.equals(this.gmail, o.gmail)
                && Objects.equals(this.facebook, o.facebook)
                && Objects.equals(this.linkedin, o.linkedin)
                && Objects.equals(this.outlook, o.outlook)
        );
    }

    @Override
    public String toString() {
        return String.format("%s\n\tAccount[id=%s, password=%s, email=%s, fullname=%s, userId=%s, isEnabled=%s, isNonLocked=%s, gmail=%s, facebook=%s, linkedin=%s, outlook=%s]",
                super.toString(),
                this.id,
                this.password != null ? "hidden" : null,
                this.email,
                this.fullname,
                this.userId,
                this.isEnabled,
                this.isNonLocked,
                this.gmail,
                this.facebook,
                this.linkedin,
                this.outlook);
    }

    public static record AccountRecord(Long id, String password, String email, String fullname, Long userId,
                                       Boolean isEnabled, Boolean isNonLocked, String gmail, String facebook,
                                       String linkedin, String outlook) {

        public static AccountRecord createSimpleRecord(String password, String email, String fullname) {
            return new Account.AccountRecord(null, password, email, fullname, null, null, null, null, null, null, null);
        }

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    /**
     * @param encodedPassword need to encode it in advance
     */
    public void setPassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * @return lowerCaseEmail
     */
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean isNonLocked() {
        return isNonLocked;
    }

    public void setNonLocked(boolean nonLocked) {
        isNonLocked = nonLocked;
    }

    public String getGmail() {
        return gmail;
    }

    public void setGmail(String gmail) {
        this.gmail = gmail;
    }

    public String getFacebook() {
        return facebook;
    }

    public void setFacebook(String facebook) {
        this.facebook = facebook;
    }

    public String getLinkedin() {
        return linkedin;
    }

    public void setLinkedin(String linkedin) {
        this.linkedin = linkedin;
    }

    public String getOutlook() {
        return outlook;
    }

    public void setOutlook(String outlook) {
        this.outlook = outlook;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    //@PrePersist
    private void setCreatedAt() {
        this.createdAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void setUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    //@PrePersist
    //@PreUpdate
    private void ensureLowerCaseEmail() {
        this.email = this.email.trim().toLowerCase();
    }

    @PrePersist
    private void prePersist() {
        ensureLowerCaseEmail();
        setCreatedAt();
        System.out.println("BEFORE PERSIST: " + this);
    }

    @PreUpdate
    private void preUpdate() {
        ensureLowerCaseEmail();
        setUpdatedAt();
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

}

