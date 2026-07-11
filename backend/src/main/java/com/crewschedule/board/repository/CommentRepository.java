package com.crewschedule.board.repository;

import com.crewschedule.board.domain.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 게시글의 모든 댓글(대댓글 포함)을 id 오름차순. 서비스에서 parent별로 그룹핑. */
    @Query("select c from Comment c join fetch c.author left join fetch c.parent "
            + "where c.post.id = :postId order by c.id asc")
    List<Comment> findAllByPostIdWithAuthor(@Param("postId") Long postId);
}
