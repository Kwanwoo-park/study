package spring.study.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private String phone;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDateTime birth;

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

//    @JsonIgnore
//    @OneToMany(mappedBy = "member", fetch = FetchType.EAGER)
//    private List<ChatRoomMember> chatRoomMembers = new ArrayList<>();
//
//
//    @ManyToMany(fetch = FetchType.EAGER)
//    @JoinTable(
//            name = "room_member",
//            joinColumns = @JoinColumn(name = "member_id"),
//            inverseJoinColumns = @JoinColumn(name = "room_id")
//    )
//    private List<ChatRoom> room = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "member", fetch = FetchType.EAGER)
    private List<ChatMessage> messages = new ArrayList<>();

    @Builder
    public Member(Long id, String email, String pwd, String name, Role role, LocalDateTime lastLoginTime, String profile, String phone, LocalDateTime birth) {
        this.id = id;
        this.email = email;
        this.pwd = pwd;
        this.name = name;
        this.role = role;
        this.lastLoginTime = lastLoginTime;
        this.profile = profile;
        this.phone = phone;
        this.birth = birth;
    }

    @Transient
    @JsonIgnore
    @JsonDeserialize(as = SimpleGrantedAuthority.class)
    private Set<SimpleGrantedAuthority> authorities;

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

    public void removeFollower(Follow follow) {
        for (Follow f : follower) {
            if (f.getFollowing().email.equals(follow.getFollowing().email)) {
                follower.remove(f);
                break;
            }
        }
    }

    public void addMessage(ChatMessage message) {
        message.addMember(this);
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

    public void changePhone(String phone) {
        this.phone = phone;
    }

    // 회원가입 시 전화번호랑 생년월일 안 받은 사람들 받는 용도
    public void changePhoneAndBirth(String phone, LocalDateTime birth) {
        this.phone = phone;
        this.birth = birth;
    }
}
