package swyp12.team9.server.domain.category.model;

import jakarta.persistence.*;
import lombok.*;
import swyp12.team9.server.global.common.entity.BaseEntity;

/**
 * 고정 카테고리 (탐색 탭용)
 * - 경제/시사, 뷰티/패션, 요리/식품, 운동/건강, 인문/지식, 직장/자기개발, 홈/리빙
 */
@Entity
@Table(name = "categories")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @Column(name = "category_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @Builder
    public Category(String name) {
        this.name = name;
    }
}
