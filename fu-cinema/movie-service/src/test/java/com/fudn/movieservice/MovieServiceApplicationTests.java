package com.fudn.movieservice;

import com.fudn.movieservice.controller.MovieController;
import com.fudn.movieservice.dto.MovieRequest;
import com.fudn.movieservice.dto.MovieResponse;
import com.fudn.movieservice.exception.ApiException;
import com.fudn.movieservice.exception.GlobalExceptionHandler;
import com.fudn.movieservice.model.AgeRating;
import com.fudn.movieservice.model.MovieStatus;
import com.fudn.movieservice.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MovieServiceApplicationTests {

    @Mock
    private MovieService movieService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MovieController(movieService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void search_shouldReturnMovieList() throws Exception {
        MovieResponse response = new MovieResponse("m1", "Galaxy Quest", "Sci-Fi comedy", "Dean Parisot",
                102, "English", AgeRating.P, LocalDate.of(1999, 12, 25), "g1", "Sci-Fi", MovieStatus.NOW_SHOWING);
        when(movieService.search(eq("Galaxy"), eq("g1"), eq(MovieStatus.NOW_SHOWING)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/movies")
                        .param("keyword", "Galaxy")
                        .param("genreId", "g1")
                        .param("status", "NOW_SHOWING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movieId").value("m1"))
                .andExpect(jsonPath("$[0].title").value("Galaxy Quest"))
                .andExpect(jsonPath("$[0].genreName").value("Sci-Fi"));
    }

    @Test
    void getById_shouldReturnMovie() throws Exception {
        MovieResponse response = new MovieResponse("m1", "Galaxy Quest", "Sci-Fi comedy", "Dean Parisot",
                102, "English", AgeRating.P, LocalDate.of(1999, 12, 25), "g1", "Sci-Fi", MovieStatus.NOW_SHOWING);
        when(movieService.getById("m1")).thenReturn(response);

        mockMvc.perform(get("/api/movies/m1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId").value("m1"))
                .andExpect(jsonPath("$.title").value("Galaxy Quest"));
    }

    @Test
    void getById_notFound_shouldReturn404() throws Exception {
        when(movieService.getById("not-found"))
                .thenThrow(ApiException.notFound("Movie not found with id: not-found"));

        mockMvc.perform(get("/api/movies/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Movie not found with id: not-found"));
    }

    @Test
    void create_validRequest_shouldReturn201() throws Exception {
        MovieResponse response = new MovieResponse("m1", "Galaxy Quest", "Sci-Fi comedy", "Dean Parisot",
                102, "English", AgeRating.P, LocalDate.of(1999, 12, 25), "g1", "Sci-Fi", MovieStatus.NOW_SHOWING);
        when(movieService.create(any(MovieRequest.class))).thenReturn(response);

        String json = """
                {
                    "title": "Galaxy Quest",
                    "description": "Sci-Fi comedy",
                    "director": "Dean Parisot",
                    "durationMinutes": 102,
                    "language": "English",
                    "ageRating": "P",
                    "releaseDate": "1999-12-25",
                    "genreId": "g1",
                    "movieStatus": "NOW_SHOWING"
                }
                """;

        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movieId").value("m1"))
                .andExpect(jsonPath("$.title").value("Galaxy Quest"));
    }

    @Test
    void update_validRequest_shouldReturn200() throws Exception {
        MovieResponse response = new MovieResponse("m1", "Galaxy Quest Updated", "Sci-Fi comedy", "Dean Parisot",
                105, "English", AgeRating.P, LocalDate.of(1999, 12, 25), "g1", "Sci-Fi", MovieStatus.NOW_SHOWING);
        when(movieService.update(eq("m1"), any(MovieRequest.class))).thenReturn(response);

        String json = """
                {
                    "title": "Galaxy Quest Updated",
                    "description": "Sci-Fi comedy",
                    "director": "Dean Parisot",
                    "durationMinutes": 105,
                    "language": "English",
                    "ageRating": "P",
                    "releaseDate": "1999-12-25",
                    "genreId": "g1",
                    "movieStatus": "NOW_SHOWING"
                }
                """;

        mockMvc.perform(put("/api/movies/m1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId").value("m1"))
                .andExpect(jsonPath("$.title").value("Galaxy Quest Updated"))
                .andExpect(jsonPath("$.durationMinutes").value(105));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(movieService).delete("m1");

        mockMvc.perform(delete("/api/movies/m1"))
                .andExpect(status().isNoContent());
    }
}