package com.crewschedule.board.repository;

import com.crewschedule.board.domain.Post;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p from Post p join fetch p.author where p.crew.id = :crewId "
            + "and (:beforeId is null or p.id < :beforeId) order by p.id desc")
    List<Post> findPage(
            @Param("crewId") Long crewId, @Param("beforeId") Long beforeId, Pageable pageable);
}
