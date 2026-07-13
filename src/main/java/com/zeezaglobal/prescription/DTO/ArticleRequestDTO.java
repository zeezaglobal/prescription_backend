package com.zeezaglobal.prescription.DTO;

import com.zeezaglobal.prescription.Entities.Article;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleRequestDTO {

    private String title;

    private String summary;

    private String content;

    private Article.ArticleStatus status;
}
