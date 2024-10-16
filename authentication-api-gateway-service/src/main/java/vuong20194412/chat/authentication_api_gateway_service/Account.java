package vuong20194412.chat.authentication_api_gateway_service;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "account")
@Table(name = "accounts")
class Account {

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

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JsonWebToken> tokens;

    @Column(name = "is_enabled")
    private boolean isEnabled;

    @Column(name = "is_non_locked")
    private boolean isNonLocked;

    public Account() {
        super();
        this.isEnabled = true;
        this.isNonLocked = true;
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

    public void setPassword(String hashedPassword) {
        this.password = hashedPassword;
    }

    /**
     * @return lowerCaseEmail
     */
    public String getEmail() {
        return email;
    }

    /**
     * Perform lower case email and set it
     *
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<JsonWebToken> getTokens() {
        return tokens;
    }

    public void setTokens(List<JsonWebToken> tokens) {
        if (this.tokens == null) {
            this.tokens = new ArrayList<>();
        }
        this.tokens.clear();
        this.tokens.addAll(tokens);
    }

    public void addToken(JsonWebToken token) {
        if (this.tokens == null) {
            this.tokens = new ArrayList<>();
        }
        this.tokens.add(token);
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

}

record AccountDTO(String password, String email, String fullname){}
