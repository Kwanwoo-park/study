package spring.study.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.study.chat.entity.ChatImageUpload;
import spring.study.chat.repository.ChatImageUploadRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatImageUploadService {
    private final ChatImageUploadRepository uploads;

    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(String id, Long memberId, Long roomId) {
        uploads.save(new ChatImageUpload(id, memberId, roomId, LocalDateTime.now().plusHours(1)));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ChatImageUpload lock(String id) {
        return uploads.findForUpdateById(id);
    }

    @Transactional
    public boolean markReady(String id) {
        ChatImageUpload upload = uploads.findForUpdateById(id);
        return upload != null && upload.markReady(LocalDateTime.now());
    }

    @Transactional
    public void abandon(String id) {
        ChatImageUpload upload = uploads.findForUpdateById(id);
        if (upload != null) upload.abandon(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<String> findExpiredIds() {
        return uploads.findExpiredIds(LocalDateTime.now(), PageRequest.of(0, 20));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(ChatImageUpload upload) {
        uploads.delete(upload);
    }
}
