package spring.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.repository.TestRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class TestService {
    private final TestRepository testRepository;

    public List<Object[]> getCommentWithMember(String name) { return testRepository.getBoardWithMember(name); }
}
