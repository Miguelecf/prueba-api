package org.migue.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.migue.dto.PostResponse;
import org.migue.service.PostService;

import java.util.List;

@Path("/posts")
public class PostResource {

    @Inject
    PostService postService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<PostResponse> getAllPosts(){
        return postService.getPostsWithDetails();
    }


}
