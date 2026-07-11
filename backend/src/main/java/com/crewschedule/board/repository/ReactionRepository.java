package com.crewschedule.board.repository;

import com.crewschedule.board.domain.Reaction;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByPostIdAndUserIdAndEmoji(Long postId, Long userId, String emoji);

    Optional<Reaction> findByCommentIdAndUserIdAndEmoji(Long commentId, Long userId, String emoji);

    @Query("select r from Reaction r join fetch r.user where r.post.id = :postId")
    List<Reaction> findAllByPostIdWithUser(@Param("postId") Long postId);

    @Query("select r from Reaction r join fetch r.user where r.comment.id in :commentIds")
    List<Reaction> findAllByCommentIdInWithUser(@Param("commentIds") Collection<Long> commentIds);
}
