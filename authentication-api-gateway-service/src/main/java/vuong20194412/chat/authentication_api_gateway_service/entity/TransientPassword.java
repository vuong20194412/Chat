package vuong20194412.chat.authentication_api_gateway_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.Random;

@Entity(name = "TransientPassword")
@Table(name = "transient_passwords")
public class TransientPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "new_password", nullable = false)
    private String encodedNewPassword;

    @Column(name = "erasable_time", nullable = false)
    private Long erasableTime;

    @Column(name = "random_code", nullable = false)
    private String randomCode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEncodedNewPassword() {
        return encodedNewPassword;
    }

    public void setEncodedNewPassword(String encodedNewPassword) {
        this.encodedNewPassword = encodedNewPassword;
    }

    public Long getErasableTime() {
        return erasableTime;
    }

    public void setErasableTime(Long erasableTime) {
        this.erasableTime = erasableTime;
    }

    public String getRandomCode() {
        return randomCode;
    }

    @PrePersist
    private void setRandomCode() {
        Random random = new Random();
        this.randomCode = String.format("%s%s%s%s%s%s",
                random.nextInt(0, 10),
                random.nextInt(0, 10),
                random.nextInt(0, 10),
                random.nextInt(0, 10),
                random.nextInt(0, 10),
                random.nextInt(0, 10));
    }

}
