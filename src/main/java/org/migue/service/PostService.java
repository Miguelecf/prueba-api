package org.migue.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.migue.client.CommentClient;
import org.migue.client.UserClient;
import org.migue.client.PostClient;
import org.migue.dto.PostDto;
import org.migue.dto.PostResponse;

import java.util.ArrayList;
import java.util.List;


@ApplicationScoped
public class PostService {

    @RestClient
    PostClient postClient;


    @RestClient
    CommentClient commentClient;

    @RestClient
    UserClient userClient;

    public List<PostResponse> getPostsWithDetails(){
        List<PostDto> posts = postClient.getPosts();
        List<PostResponse> postResponses = new ArrayList<>();

        return posts.stream().map(post -> {
            PostResponse postResponse = new PostResponse();
            postResponse.id = post.id;
            postResponse.title = post.title;
            postResponse.body = post.body;

            // Fetch comments for the post
            postResponse.comments = commentClient.getComments(post.id);

            // Fetch user details for the author
            var user = userClient.getUser(post.userId);
            postResponse.authorName = user.name;
            postResponse.authorEmail = user.email;

            return postResponse;
        }).toList();
    }


}
