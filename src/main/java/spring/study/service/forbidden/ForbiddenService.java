package spring.study.service.forbidden;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.dto.forbidden.ForbiddenRequestDto;
import spring.study.dto.forbidden.ForbiddenResponseDto;
import spring.study.entity.forbidden.Forbidden;
import spring.study.entity.forbidden.Risk;
import spring.study.entity.forbidden.Status;
import spring.study.repository.forbidden.ForbiddenRepository;

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

    public Forbidden findByWord(String word) {
        return forbiddenRepository.findByWord(word);
    }

    public List<ForbiddenResponseDto> findByRisk(Risk risk) {
        return forbiddenRepository.findByRisk(risk).stream().map(ForbiddenResponseDto::new).toList();
    }

    public List<ForbiddenResponseDto> findByStatus(Status status) {
        return forbiddenRepository.findByStatus(status).stream().map(ForbiddenResponseDto::new).toList();
    }

    public List<ForbiddenResponseDto> findByStatusNot(Status status) {
        return forbiddenRepository.findByStatusNot(status).stream().map(ForbiddenResponseDto::new).toList();
    }

    public int updateToExamine(List<Long> idList) {
        return forbiddenRepository.updateStatusInIdList(Status.EXAMINE, idList);
    }

    public int updateToApproval(List<Long> idList) {
        return forbiddenRepository.updateStatusInIdList(Status.APPROVAL, idList);
    }
}
