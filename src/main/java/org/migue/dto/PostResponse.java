package org.migue.dto;

import java.util.List;

public class PostResponse {
        public Long id;
        public String title;
        public String body;
        public String authorName;
        public String authorEmail;
        public List<CommentDto> comments;
}
