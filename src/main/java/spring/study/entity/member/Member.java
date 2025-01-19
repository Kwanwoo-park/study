package spring.study.entity.member;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import spring.study.entity.BasetimeEntity;
import spring.study.entity.board.Board;
import spring.study.entity.chat.ChatMessage;
import spring.study.entity.chat.ChatRoomMember;
import spring.study.entity.comment.Comment;
import spring.study.entity.follow.Follow;

import java.time.LocalDateTime;
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
    @Column(unique = true)
    private String phone;

    @NotNull
    private String birth;

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

    @JsonIgnore
    @OneToMany(mappedBy = "member", fetch = FetchType.EAGER)
    private List<ChatRoomMember> chatRoomMembers= new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "member", fetch = FetchType.EAGER)
    private List<ChatMessage> messages = new ArrayList<>();

    @Builder
    public Member(Long id, String email, String pwd, String name, Role role, LocalDateTime lastLoginTime, String profile, String phone, String birth) {
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
        for (Board b1 : board) {
            if (b1.getId().equals(b.getId())) {
                board.remove(b);
                break;
            }
        }
    }

    public void addComment(Comment comment) {
        comment.addMember(this);
    }

    public void removeComments(List<Comment> list) {
        for (Comment c : list) {
            comment.remove(c);
        }
    }

    public void removeComment(Comment cmt) {
        for (Comment c : comment) {
            if (c.getId().equals(cmt.getId())) {
                comment.remove(c);
                break;
            }
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

    public void addChatRoomMembers(ChatRoomMember chatRoomMember) {
        chatRoomMember.addMember(this);
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
    public void changePhoneAndBirth(String phone, String birth) {
        this.phone = phone;
        this.birth = birth;
    }

    public void changeRole(Role role) {
        this.role = role;
    }
}
