package spring.study.dto.search;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.Role;

@Getter
@Setter
@NoArgsConstructor
public class SearchRequestDto {
    private Long id;
    private String email;
    private String password;
    private String name;
    private Role role;
    private String profile;
}
