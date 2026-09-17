package spring.study.favorite.service;

import org.springframework.transaction.annotation.Transactional;
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

    @Transactional(readOnly = true)
    public List<Favorite> findByBoard(Board board) {
        return favoriteRepository.findByBoard(board);
    }

    @Transactional(readOnly = true)
    public List<Favorite> findByMember(Member member) {
        return favoriteRepository.findByMember(member);
    }

    @Transactional(readOnly = true)
    public Favorite findByMemberAndBoard(Member member, Board board) {
        return favoriteRepository.findByMemberAndBoard(member, board);
    }

    @Transactional(readOnly = true)
    public Boolean existFavorite(Member member, Board board) {
        return favoriteRepository.existsByMemberAndBoard(member, board);
    }

    @Transactional(readOnly = true)
    public List<Favorite> getFavorites(Board board, int cursor, int limit) {
        return favoriteRepository.findByBoard(
                board,
                PageRequest.of(cursor, limit, Sort.by("id").descending())
        );
    }

    @Transactional(readOnly = true)
    public long countFavorites(Board board) {
        return favoriteRepository.countByBoard(board);
    }

    @Transactional(readOnly = true)
    public HashMap<Long, Long> countFavorites(List<Board> boardList) {
        HashMap<Long, Long> map = new HashMap<>();
        boardList.forEach(board -> map.put(board.getId(), 0L));
        if (boardList.isEmpty()) return map;
        favoriteRepository.countByBoardIds(boardList.stream().map(Board::getId).toList())
                .forEach(row -> map.put((Long) row[0], (Long) row[1]));

        return map;
    }

    @Transactional(readOnly = true)
    public List<Long> findLikedBoardIds(Member member, List<Board> boards) {
        if (boards.isEmpty()) return List.of();
        return favoriteRepository.findLikedBoardIds(member, boards.stream().map(Board::getId).toList());
    }

    @Transactional
    public void deleteById(Favorite favorite) {
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
