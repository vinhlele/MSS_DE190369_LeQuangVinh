package com.fudn.movieservice;

import com.fudn.movieservice.controller.MovieController;
import com.fudn.movieservice.controller.ShowtimeController;
import com.fudn.movieservice.dto.MovieRequest;
import com.fudn.movieservice.dto.MovieResponse;
import com.fudn.movieservice.dto.ShowtimeRequest;
import com.fudn.movieservice.dto.ShowtimeResponse;
import com.fudn.movieservice.exception.ApiException;
import com.fudn.movieservice.exception.GlobalExceptionHandler;
import com.fudn.movieservice.model.AgeRating;
import com.fudn.movieservice.model.MovieStatus;
import com.fudn.movieservice.model.ShowtimeStatus;
import com.fudn.movieservice.service.MovieService;
import com.fudn.movieservice.service.ShowtimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Mock
    private ShowtimeService showtimeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MovieController(movieService), new ShowtimeController(showtimeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void contextLoads() {
    }

    // ================= MOVIE =================

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

    // ================= SHOWTIME =================

    @Test
    void searchShowtimes_shouldReturnList() throws Exception {
        ShowtimeResponse response = new ShowtimeResponse("s1", "m1", "Galaxy Quest", "r1", "Room 01",
                8, 10, LocalDateTime.of(2026, 12, 20, 19, 0), LocalDateTime.of(2026, 12, 20, 21, 5),
                BigDecimal.valueOf(95000), ShowtimeStatus.SCHEDULED);
        when(showtimeService.search(eq("m1"), eq(LocalDate.of(2026, 12, 20))))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/showtimes")
                        .param("movieId", "m1")
                        .param("date", "2026-12-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].showtimeId").value("s1"))
                .andExpect(jsonPath("$[0].movieTitle").value("Galaxy Quest"))
                .andExpect(jsonPath("$[0].roomName").value("Room 01"));
    }

    @Test
    void getShowtimeById_shouldReturnShowtime() throws Exception {
        ShowtimeResponse response = new ShowtimeResponse("s1", "m1", "Galaxy Quest", "r1", "Room 01",
                8, 10, LocalDateTime.of(2026, 12, 20, 19, 0), LocalDateTime.of(2026, 12, 20, 21, 5),
                BigDecimal.valueOf(95000), ShowtimeStatus.SCHEDULED);
        when(showtimeService.getById("s1")).thenReturn(response);

        mockMvc.perform(get("/api/showtimes/s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showtimeId").value("s1"))
                .andExpect(jsonPath("$.roomName").value("Room 01"));
    }

    @Test
    void createShowtime_validRequest_shouldReturn201() throws Exception {
        ShowtimeResponse response = new ShowtimeResponse("s1", "m1", "Galaxy Quest", "r1", "Room 01",
                8, 10, LocalDateTime.of(2026, 12, 20, 19, 0), LocalDateTime.of(2026, 12, 20, 21, 5),
                BigDecimal.valueOf(95000), ShowtimeStatus.SCHEDULED);
        when(showtimeService.create(any(ShowtimeRequest.class))).thenReturn(response);

        String json = """
                {
                    "movieId": "m1",
                    "roomId": "r1",
                    "startTime": "2026-12-20T19:00:00",
                    "ticketPrice": 95000
                }
                """;

        mockMvc.perform(post("/api/showtimes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.showtimeId").value("s1"))
                .andExpect(jsonPath("$.ticketPrice").value(95000));
    }

    @Test
    void updateShowtime_validRequest_shouldReturn200() throws Exception {
        ShowtimeResponse response = new ShowtimeResponse("s1", "m1", "Galaxy Quest", "r1", "Room 01",
                8, 10, LocalDateTime.of(2026, 12, 20, 20, 0), LocalDateTime.of(2026, 12, 20, 22, 5),
                BigDecimal.valueOf(100000), ShowtimeStatus.SCHEDULED);
        when(showtimeService.update(eq("s1"), any(ShowtimeRequest.class))).thenReturn(response);

        String json = """
                {
                    "movieId": "m1",
                    "roomId": "r1",
                    "startTime": "2026-12-20T20:00:00",
                    "ticketPrice": 100000
                }
                """;

        mockMvc.perform(put("/api/showtimes/s1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showtimeId").value("s1"))
                .andExpect(jsonPath("$.ticketPrice").value(100000));
    }

    @Test
    void cancelShowtime_shouldReturn204() throws Exception {
        doNothing().when(showtimeService).cancel("s1");

        mockMvc.perform(delete("/api/showtimes/s1"))
                .andExpect(status().isNoContent());
    }
}