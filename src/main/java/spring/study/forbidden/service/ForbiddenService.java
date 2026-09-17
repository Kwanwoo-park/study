package spring.study.forbidden.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.forbidden.dto.ForbiddenRequestDto;
import spring.study.forbidden.dto.ForbiddenResponseDto;
import spring.study.forbidden.entity.Forbidden;
import spring.study.forbidden.entity.Risk;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.repository.ForbiddenRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ForbiddenService {
    private final ForbiddenRepository forbiddenRepository;

    @Transactional
    public Forbidden save(Forbidden forbidden) {
        return forbiddenRepository.save(forbidden);
    }

    @Transactional
    public Forbidden save(ForbiddenRequestDto requestDto) {
        return forbiddenRepository.save(requestDto.toEntity());
    }

    @Transactional(readOnly = true)
    public List<Forbidden> findAll() {
        return forbiddenRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ForbiddenResponseDto> findByWord(String word) {
        return forbiddenRepository.findByWordContaining(word).stream().map(ForbiddenResponseDto::new).toList();
    }

    @Transactional(readOnly = true)
    public List<ForbiddenResponseDto> findByRisk(Risk risk) {
        return forbiddenRepository.findByRisk(risk).stream().map(ForbiddenResponseDto::new).toList();
    }

    @Transactional(readOnly = true)
    public int findWordList(Status status, String content) {
        return forbiddenRepository.findByStatus(status).stream()
                .filter(word -> content.contains(word.getWord()))
                .mapToInt(word -> word.getRisk().getValue())
                .max()
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public List<ForbiddenResponseDto> findByStatus(Status status) {
        return forbiddenRepository.findByStatus(status).stream().map(ForbiddenResponseDto::new).toList();
    }

    @Transactional(readOnly = true)
    public List<ForbiddenResponseDto> findByStatusNot(Status status) {
        return forbiddenRepository.findByStatusNot(status).stream().map(ForbiddenResponseDto::new).toList();
    }

    @Transactional(readOnly = true)
    public Boolean existWord(String word) {
        return forbiddenRepository.existsByWord(word);
    }

    public int updateStatus(Status status, List<Long> idList) {
        return forbiddenRepository.updateStatusInIdList(status, idList);
    }
}
