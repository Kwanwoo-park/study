package spring.study.todo.service;

import org.junit.jupiter.api.Test;
import spring.study.member.entity.Member;
import spring.study.member.facade.MemberFacade;
import spring.study.member.service.MemberService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

class TodoWithdrawalTest {
    @Test
    void withdrawalRemovesTodosBeforeDeletingTheManagedMember() throws Exception {
        Map<Class<?>, Object> dependencies = new HashMap<>();
        var constructor = MemberFacade.class.getConstructors()[0];
        Object[] arguments = Arrays.stream(constructor.getParameterTypes())
                .map(type -> dependencies.computeIfAbsent(type, key -> mock(key))).toArray();
        MemberFacade facade = (MemberFacade) constructor.newInstance(arguments);
        MemberService members = (MemberService) dependencies.get(MemberService.class);
        TodoService todos = (TodoService) dependencies.get(TodoService.class);
        Member principal = Member.builder().id(7L).email("owner@example.test").build();
        Member managed = Member.builder().id(7L).email("owner@example.test").build();
        when(members.findById(7L)).thenReturn(managed);

        facade.deleteMember(principal, null);

        var ordered = inOrder(members, todos);
        ordered.verify(members).findById(7L);
        ordered.verify(todos).deleteByMember(managed);
        ordered.verify(members).deleteById(7L);
    }
}
