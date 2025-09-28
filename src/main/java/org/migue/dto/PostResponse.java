package org.migue.dto;

import java.util.List;

public class PostResponse {
        public Long id;
        public String title;
        public String body;
        public String authorName;
        public String authorEmail;
        public List<CommentDto> comments;

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public String getTitle() {
                return title;
        }

        public void setTitle(String title) {
                this.title = title;
        }

        public String getBody() {
                return body;
        }

        public void setBody(String body) {
                this.body = body;
        }

        public String getAuthorName() {
                return authorName;
        }

        public void setAuthorName(String authorName) {
                this.authorName = authorName;
        }

        public String getAuthorEmail() {
                return authorEmail;
        }

        public void setAuthorEmail(String authorEmail) {
                this.authorEmail = authorEmail;
        }

        public List<CommentDto> getComments() {
                return comments;
        }

        public void setComments(List<CommentDto> comments) {
                this.comments = comments;
        }
}
