package spring.study.favorite.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.board.entity.Board;
import spring.study.favorite.entity.Favorite;
import spring.study.member.entity.Member;
import spring.study.favorite.repository.FavoriteRepository;

import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;

    @Transactional
    public Favorite save(Member member, Board board) {
        Favorite favorite = Favorite.builder()
                .board(board)
                .member(member)
                .build();

        return favoriteRepository.save(favorite);
    }

    public List<Favorite> findByBoard(Board board) {
        return favoriteRepository.findByBoard(board);
    }

    public List<Favorite> findByMember(Member member) {
        return favoriteRepository.findByMember(member);
    }

    public Favorite findByMemberAndBoard(Member member, Board board) {
        return favoriteRepository.findByMemberAndBoard(member, board);
    }

    public Boolean existFavorite(Member member, Board board) {
        return favoriteRepository.existsByMemberAndBoard(member, board);
    }

    public List<Favorite> getFavorites(Board board, int cursor, int limit) {
        return favoriteRepository.findByBoard(
                board,
                PageRequest.of(cursor, limit, Sort.by("id").descending())
        );
    }

    public long countFavorites(Board board) {
        return favoriteRepository.countByBoard(board);
    }

    public HashMap<Long, Long> countFavorites(List<Board> boardList) {
        HashMap<Long, Long> map = new HashMap<>();

        for (Board board : boardList)
            map.put(board.getId(), favoriteRepository.countByBoard(board));

        return map;
    }

    @Transactional
    public void deleteById(Favorite favorite, Member member, Board board) {
        favoriteRepository.deleteById(favorite.getId());
    }

    @Transactional
    public void deleteByMember(Member member) {
        favoriteRepository.deleteByMember(member);
    }

    @Transactional
    public void deleteByBoard(Board board) {
        favoriteRepository.deleteByBoard(board);
    }
}
