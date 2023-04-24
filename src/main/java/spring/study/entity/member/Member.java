package spring.study.entity.member;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import spring.study.entity.BasetimeEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

@EqualsAndHashCode(of = {"id"})
@NoArgsConstructor
@Getter
@Setter
@Entity
public class Member extends BasetimeEntity implements UserDetails {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String pwd;
    private String name;
    private LocalDateTime lastLoginTime;

    @Builder
    public Member(Long id, String email, String pwd, String name, LocalDateTime lastLoginTime) {
        this.id = id;
        this.email = email;
        this.pwd = pwd;
        this.name = name;
        this.lastLoginTime = lastLoginTime;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collectors = new ArrayList<>();

        collectors.add(() -> {
            return "계정별 등록할 권한";
        });

        return collectors;
    }

    @Override
    public String getPassword() {
        return this.pwd;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPwd() {
        return pwd;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }
}
