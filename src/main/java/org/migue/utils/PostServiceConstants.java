package org.migue.utils;

/**
 * Constantes utilizadas en PostService para evitar valores mágicos.
 */
public final class PostServiceConstants {

    private PostServiceConstants() {
        // Constructor privado para evitar instanciación
    }

    public static final String DEFAULT_AUTHOR_NAME = "desconocido";
    public static final String DEFAULT_AUTHOR_EMAIL = "desconocido";
    public static final int EXTERNAL_CALL_TIMEOUT_MS = 5000;
    public static final int MAX_POSTS_LIMIT = 1000;
    public static final int SUCCESS_DELETE_STATUS_200 = 200;
    public static final int SUCCESS_DELETE_STATUS_204 = 204;
    public static final int NOT_FOUND_STATUS = 404;
}
