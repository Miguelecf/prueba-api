package org.migue;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.migue.dto.PostResponse;
import org.migue.service.PostService;
import org.mockito.Mockito;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class PostResourceTest {

    @InjectMock
    PostService postService;

    // Datos de prueba
    private PostResponse createMockPost(Long id, String title, String authorName) {
        PostResponse post = new PostResponse();
        post.setId(id);
        post.setTitle(title);
        post.setAuthorName(authorName);
        return post;
    }

    private List<PostResponse> createMockPosts() {
        return Arrays.asList(
                createMockPost(1L, "Test Post 1", "Author 1"),
                createMockPost(2L, "Test Post 2", "Author 2")
        );
    }

    // Tests para GET /posts - Casos exitosos
    @Test
    void testGetAllPosts_Success() {
        // Arrange
        List<PostResponse> mockPosts = createMockPosts();
        Mockito.when(postService.getPostsWithDetails()).thenReturn(mockPosts);

        // Act & Assert
        given()
                .when().get("/posts")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].id", equalTo(1))
                .body("[0].title", equalTo("Test Post 1"))
                .body("[0].authorName", equalTo("Author 1"))
                .body("[1].id", equalTo(2))
                .body("[1].title", equalTo("Test Post 2"));
    }

    @Test
    void testGetAllPosts_WithValidQueryParams() {
        // Arrange
        List<PostResponse> mockPosts = Collections.singletonList(
                createMockPost(1L, "Filtered Post", "Author 1")
        );
        Mockito.when(postService.getPostsWithDetails()).thenReturn(mockPosts);

        // Act & Assert
        given()
                .queryParam("authorId", 1)
                .queryParam("search", "test")
                .queryParam("limit", 10)
                .queryParam("offset", 0)
                .when().get("/posts")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("Filtered Post"));
    }

    // Tests para GET /posts - Casos de error
    @Test
    void testGetAllPosts_EmptyResult() {
        // Arrange
        Mockito.when(postService.getPostsWithDetails()).thenReturn(Collections.emptyList());

        // Act & Assert
        given()
                .when().get("/posts")
                .then()
                .statusCode(404);
    }

    @Test
    void testGetAllPosts_NullResult() {
        // Arrange
        Mockito.when(postService.getPostsWithDetails()).thenReturn(null);

        // Act & Assert
        given()
                .when().get("/posts")
                .then()
                .statusCode(404);
    }

    @Test
    void testGetAllPosts_InvalidAuthorId() {
        given()
                .queryParam("authorId", -1)
                .when().get("/posts")
                .then()
                .statusCode(400);
    }

    @Test
    void testGetAllPosts_InvalidLimitZero() {
        given()
                .queryParam("limit", 0)
                .when().get("/posts")
                .then()
                .statusCode(400);
    }

    @Test
    void testGetAllPosts_InvalidLimitTooHigh() {
        given()
                .queryParam("limit", 501)
                .when().get("/posts")
                .then()
                .statusCode(400);
    }

    @Test
    void testGetAllPosts_InvalidOffset() {
        given()
                .queryParam("offset", -1)
                .when().get("/posts")
                .then()
                .statusCode(400);
    }

    @Test
    void testGetAllPosts_SearchTooLong() {
        String longSearch = "a".repeat(201);

        given()
                .queryParam("search", longSearch)
                .when().get("/posts")
                .then()
                .statusCode(400);
    }

    @Test
    void testGetAllPosts_ServiceException() {
        // Arrange
        Mockito.when(postService.getPostsWithDetails())
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        given()
                .when().get("/posts")
                .then()
                .statusCode(500);
    }

    // Tests para DELETE /posts/{id} - Casos exitosos
    @Test
    void testDeletePost_Success() {
        // Arrange
        Response mockResponse = Response.noContent().build();
        Mockito.when(postService.deletePost(1L)).thenReturn(mockResponse);

        // Act & Assert
        given()
                .when().delete("/posts/1")
                .then()
                .statusCode(204);
    }

    @Test
    void testDeletePost_SuccessWith200() {
        // Arrange
        Response mockResponse = Response.ok().build();
        Mockito.when(postService.deletePost(1L)).thenReturn(mockResponse);

        // Act & Assert
        given()
                .when().delete("/posts/1")
                .then()
                .statusCode(204);
    }

    // Tests para DELETE /posts/{id} - Casos de error
    @Test
    void testDeletePost_NotFound() {
        // Arrange
        Response mockResponse = Response.status(Response.Status.NOT_FOUND).build();
        Mockito.when(postService.deletePost(999L)).thenReturn(mockResponse);

        // Act & Assert
        given()
                .when().delete("/posts/999")
                .then()
                .statusCode(404);
    }

    @Test
    void testDeletePost_InvalidIdZero() {
        given()
                .when().delete("/posts/0")
                .then()
                .statusCode(400);
    }

    @Test
    void testDeletePost_InvalidNegativeId() {
        given()
                .when().delete("/posts/-1")
                .then()
                .statusCode(400);
    }

    @Test
    void testDeletePost_ClientErrorFromService() {
        // Arrange
        Response mockResponse = Response.status(Response.Status.BAD_REQUEST).build();
        Mockito.when(postService.deletePost(2L)).thenReturn(mockResponse);

        // Act & Assert
        given()
                .when().delete("/posts/2")
                .then()
                .statusCode(400);
    }

    @Test
    void testDeletePost_ServerErrorFromService() {
        // Arrange
        Response mockResponse = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        Mockito.when(postService.deletePost(3L)).thenReturn(mockResponse);

        // Act & Assert
        given()
                .when().delete("/posts/3")
                .then()
                .statusCode(502);
    }

    @Test
    void testDeletePost_NullResponseFromService() {
        // Arrange
        Mockito.when(postService.deletePost(4L)).thenReturn(null);

        // Act & Assert
        given()
                .when().delete("/posts/4")
                .then()
                .statusCode(500);
    }

    @Test
    void testDeletePost_ServiceException() {
        // Arrange
        Mockito.when(postService.deletePost(5L))
                .thenThrow(new RuntimeException("Delete error"));

        // Act & Assert
        given()
                .when().delete("/posts/5")
                .then()
                .statusCode(500);
    }

    // Test adicional para verificar el comportamiento con parámetros de búsqueda procesados
    @Test
    void testGetAllPosts_SearchWithSpecialCharacters() {
        // Arrange
        List<PostResponse> mockPosts = Collections.singletonList(
                createMockPost(1L, "Clean Post", "Author")
        );
        Mockito.when(postService.getPostsWithDetails()).thenReturn(mockPosts);

        // Act & Assert - Los caracteres especiales deberían ser limpiados
        given()
                .queryParam("search", "test<script>alert('xss')</script>")
                .when().get("/posts")
                .then()
                .statusCode(200);
    }
}