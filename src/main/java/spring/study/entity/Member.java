package spring.study.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@NoArgsConstructor
@Getter
@Setter
@DynamicUpdate
@Entity(name = "member")
public class Member extends BasetimeEntity implements UserDetails {
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @NotNull
    @Column(unique = true)
    private String email;

    @NotNull
    private String pwd;

    @NotNull
    private String name;

    @NotNull
    private Role role;

    @NotNull
    private String profile;
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @JsonIgnore
    @OneToMany(mappedBy = "member", fetch = FetchType.EAGER)
    private List<Board> board = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "member", fetch = FetchType.EAGER)
    private List<Comment> comment = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "follower", fetch = FetchType.EAGER)
    private List<Follow> follower = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "following", fetch = FetchType.EAGER)
    private List<Follow> following = new ArrayList<>();

    @Builder
    public Member(Long id, String email, String pwd, String name, Role role, LocalDateTime lastLoginTime, String profile) {
        this.id = id;
        this.email = email;
        this.pwd = pwd;
        this.name = name;
        this.role = role;
        this.lastLoginTime = lastLoginTime;
        this.profile = profile;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> roles = new HashSet<>();
        roles.add(new SimpleGrantedAuthority(role.getValue()));
        return roles;
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

    public void addBoard(Board board) {
        board.addMember(this);
    }

    public void removeBoard(Board b) {
        board.remove(b);
    }

    public void addComment(Comment comment) {
        comment.addMember(this);
    }

    public void removeComment(List<Comment> list) {
        for (Comment c : list) {
            comment.remove(c);
        }
    }

    public void addFollower(Follow follower) {
        follower.addFollower(this);
    }

    public void addFollowing(Follow following) {
        following.addFollowing(this);
    }

    public void changeProfile(String profile) {
        this.profile = profile;
    }

    public void changePwd(String pwd) {
        this.pwd = pwd;
    }

    public void changeLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
}
