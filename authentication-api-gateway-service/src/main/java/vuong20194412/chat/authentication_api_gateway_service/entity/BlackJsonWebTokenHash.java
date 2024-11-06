package vuong20194412.chat.authentication_api_gateway_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity(name = "blackJwtHash")
@Table(name = "black_jwt_hashes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"encoded_hash", "issued_at_time", "expiration_time"})
})
public class BlackJsonWebTokenHash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encoded_hash", nullable = false)
    private String encodedHash; // encoded base64 url

    @Column(name = "issued_at_time", nullable = false) // issued at time of token
    private Long issuedAtTime;

    @Column(name = "expiration_time", nullable = false) // expiration time of token
    private Long expirationTime; // erasableTime

    public String getEncodedHash() {
        return encodedHash;
    }

    public void setEncodedHash(String encodedHash) {
        this.encodedHash = encodedHash;
    }

    public Long getIssuedAtTime() {
        return issuedAtTime;
    }

    public void setIssuedAtTime(Long issuedAtTime) {
        this.issuedAtTime = issuedAtTime;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Long expirationTime) {
        this.expirationTime = expirationTime;
    }

    @PrePersist
    @PreUpdate
    private void checkTimes() {
        if (this.expirationTime <= this.issuedAtTime)
            throw new RuntimeException("issuedAtTime must be smaller than expirationTime");
    }

}
