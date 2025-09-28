package org.migue.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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

    @DELETE
    @Path("/{id}")
    public Response deletePost(@PathParam("id") Long postId) {
        Response response = postService.deletePost(postId);

        if (response.getStatus() == 200 || response.getStatus() == 204) {
            return Response.noContent().build();
        }
        if (response.getStatus() == 404) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.BAD_GATEWAY).build();
    }
}
