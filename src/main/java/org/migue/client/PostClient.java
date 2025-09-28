package org.migue.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.migue.dto.PostDto;
import java.util.List;

@Path("/posts")
@RegisterRestClient(configKey="post-api")
public interface PostClient {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<PostDto> getPosts();
}
