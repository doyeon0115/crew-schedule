package com.crewschedule.board.domain;

import com.crewschedule.common.entity.BaseTimeEntity;
import com.crewschedule.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 게시글/댓글 이모지 반응. post_id 또는 comment_id 중 하나만 세팅(XOR, DB CHECK). */
@Getter
@Entity
@Table(name = "reactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 20)
    private String emoji;

    @Builder
    private Reaction(Post post, Comment comment, User user, String emoji) {
        this.post = post;
        this.comment = comment;
        this.user = user;
        this.emoji = emoji;
    }

    public static Reaction forPost(Post post, User user, String emoji) {
        return Reaction.builder().post(post).user(user).emoji(emoji).build();
    }

    public static Reaction forComment(Comment comment, User user, String emoji) {
        return Reaction.builder().comment(comment).user(user).emoji(emoji).build();
    }
}
