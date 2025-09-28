package org.migue.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.migue.dto.PostDto;
import java.util.List;

@Path("/posts")
@RegisterRestClient(configKey="post-api")
public interface PostClient {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<PostDto> getPosts();

    @DELETE
    @Path("/{id}")
    Response deletePost(@PathParam("id") Long postId);
}
