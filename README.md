# Prueba API – Agregador de Posts de JSONPlaceholder

Servicio REST construido con Quarkus 3 que consume los recursos públicos de [JSONPlaceholder](https://jsonplaceholder.typicode.com) para exponer un catálogo de posts enriquecido con información de usuarios y comentarios, además de permitir el borrado de publicaciones.

---

## Requisitos previos

- **Java 21** (configurado vía `maven.compiler.release=21`).
- No necesitas Maven instalado: el proyecto incluye el wrapper `./mvnw`.
- Acceso a internet para contactar la API pública de JSONPlaceholder durante la ejecución.

---

## Ejecución y empaquetado

| Escenario                              | Comando                                   | Descripción |
|----------------------------------------|-------------------------------------------|-------------|
| Desarrollo en caliente                 | `./mvnw quarkus:dev`                      | Levanta el servicio en `http://localhost:8080` con hot-reload. |
| Suite de pruebas                       | `./mvnw test`                             | Ejecuta los tests de `src/test/java/org/migue/PostResourceTest.java`. |
| Empaquetado JVM                        | `./mvnw clean package`                    | Genera el artefacto `target/quarkus-app/`. |
| Ejecución del runner empaquetado       | `java -jar target/quarkus-app/quarkus-run.jar` | Arranca la aplicación compilada. |
| Imagen nativa (opcional, requiere GraalVM) | `./mvnw clean package -Dnative`          | Construye un binario nativo en `target/`. |

---

## Documentación OpenAPI y Swagger

- **Esquema OpenAPI:** `http://localhost:8080/q/openapi`
- **Swagger UI:** `http://localhost:8080/q/swagger-ui`

Configuración relevante (`src/main/resources/application.properties`):

```properties
# Path del contrato OpenAPI
quarkus.smallrye-openapi.path=/q/openapi

# Habilitar Swagger UI
quarkus.swagger-ui.always-include=true
quarkus.swagger-ui.path=/q/swagger-ui
```
* Para acceder a la documentacion swagger, despues de levantar el proyecto vamos a:
- `http://localhost:8080/q/swagger-ui` 

Arquitectura y organización del código
Se optó por una arquitectura limpia y orientada a responsabilidades claras:

## Paquete	Rol principal
- org.migue.resource:	Recursos REST (controladores JAX-RS). PostResource expone los endpoints /posts.


- org.migue.service:	Lógica de negocio. PostService orquesta llamadas a APIs externas, aplica caché y compone la respuesta.


- org.migue.client:	Clientes REST (MicroProfile Rest Client) para posts, users y comments de JSONPlaceholder.


- org.migue.dto:	Data Transfer Objects para mapear estructuras remotas (PostDto, UserDto, CommentDto) y la respuesta enriquecida (PostResponse).


- org.migue.exception:	Excepciones personalizadas (ExternalServiceException, ResourceNotFoundException) para mapear errores de negocio a HTTP.

No se incorporó capa de dominio ni persistencia dado que los datos provienen exclusivamente del servicio externo; la prioridad fue mantener un flujo simple y desacoplado.

## Endpoints expuestos
### Listar posts con comentarios y autores
```declarative
curl -s "http://localhost:8080/posts?authorId=1&search=qui&limit=2" | jq
```
```
Response: (200 OK)
[
  {
    "id": 1,
    "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
    "body": "quia et suscipit\nsuscipit recusandae consequuntur expedita et cum...",
    "authorName": "Leanne Graham",
    "authorEmail": "Sincere@april.biz",
    "comments": [
      {
        "id": 1,
        "postId": 1,
        "name": "id labore ex et quam laborum",
        "email": "Eliseo@gardner.biz",
        "body": "laudantium enim quasi est quidem magnam voluptate ipsam..."
      }
    ]
  }
]
```
Posibles códigos de respuesta:

200 OK cuando existen resultados.

400 Bad Request si los parámetros violan las validaciones.

404 Not Found si no se encuentran posts.

500 Internal Server Error ante problemas inesperados con el servicio externo.

### Borrar un post por ID
```declarative
curl -X DELETE "http://localhost:8080/posts/1"
```
Respuestas esperadas:

204 No Content si JSONPlaceholder reporta 200/204.

404 Not Found cuando el post no existe.

400 Bad Request para IDs inválidos.

502 Bad Gateway si el proveedor responde con errores 5xx.

500 Internal Server Error ante fallos no controlados.

### Manejo de errores
Las validaciones de entrada se realizan en el recurso (PostResource) y generan BadRequestException (400) cuando corresponda.

ResourceNotFoundException encapsula los 404 del proveedor externo para responder adecuadamente al cliente.

ExternalServiceException centraliza fallos en dependencias externas y se traduce en errores 5xx controlados.

El logging se implementó tanto con org.jboss.logging.Logger (en recursos) como con org.slf4j.Logger (en servicios) para rastrear eventos y excepciones.

## Decisiones técnicas destacadas
- Arquitectura limpia y ligera: Al no existir almacenamiento local ni reglas de dominio complejas, se prescindió de capas adicionales. Esto simplifica el mantenimiento y mantiene las dependencias entre paquetes bien delimitadas.

- Caché concurrente de usuarios: PostService utiliza un ConcurrentHashMap<Long, CompletableFuture<UserDto>> para almacenar en memoria las búsquedas de autores durante una misma petición, evitando solicitudes duplicadas a JSONPlaceholder y reduciendo la latencia.

- Ejecución paralela con CompletableFuture: Los comentarios y autores se obtienen de manera asíncrona (CompletableFuture.supplyAsync) usando un Executor inyectado, de modo que la composición de la respuesta no quede atada a llamadas secuenciales.

- Tolerancia a fallos: Fallos parciales (por ejemplo, ausencia de comentarios o errores recuperables en usuarios) no bloquean la entrega de cada post; se rellenan datos por defecto y se registran trazas para su análisis.

- Documentación integrada: Swagger UI y el contrato OpenAPI están siempre disponibles para facilitar la exploración y el testing manual de los endpoints.

## Pruebas automatizadas
El proyecto incluye pruebas de integración sobre PostResource que cubren escenarios exitosos, validaciones, propagación de errores y sanitización de parámetros (src/test/java/org/migue/PostResourceTest.java).
Las pruebas estan hechas con Mocks, asi no hacemos llamadas reales a la API de JSONPlaceholder.

Para ejecutarlas:
```
./mvnw test

```