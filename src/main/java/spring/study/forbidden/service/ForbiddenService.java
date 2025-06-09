package spring.study.forbidden.service;

import jakarta.transaction.Transactional;
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

    public List<Forbidden> findAll() {
        return forbiddenRepository.findAll();
    }

    public List<ForbiddenResponseDto> findByWord(String word) {
        return forbiddenRepository.findByWord(word).stream().map(ForbiddenResponseDto::new).toList();
    }

    public List<ForbiddenResponseDto> findByRisk(Risk risk) {
        return forbiddenRepository.findByRisk(risk).stream().map(ForbiddenResponseDto::new).toList();
    }

    public int findWordList(Status status, String content) {
        List<Forbidden> wordList = forbiddenRepository.findByStatus(status);

        for (Forbidden word : wordList) {
            if (content.contains(word.getWord())) {
                switch (word.getRisk()) {
                    case HIGH -> {
                        return 3;
                    }
                    case MIDDLE -> {
                        return 2;
                    }
                    case LOW -> {
                        return 1;
                    }
                }
            }
        }

        return 0;
    }

    public List<ForbiddenResponseDto> findByStatus(Status status) {
        return forbiddenRepository.findByStatus(status).stream().map(ForbiddenResponseDto::new).toList();
    }

    public List<ForbiddenResponseDto> findByStatusNot(Status status) {
        return forbiddenRepository.findByStatusNot(status).stream().map(ForbiddenResponseDto::new).toList();
    }

    public Boolean existWord(String word) {
        return forbiddenRepository.existsByWord(word);
    }

    public int updateStatus(Status status, List<Long> idList) {
        return forbiddenRepository.updateStatusInIdList(status, idList);
    }
}
