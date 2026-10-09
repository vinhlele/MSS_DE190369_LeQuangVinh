package com.fudn.movieservice.service;

import com.fudn.movieservice.dto.ShowtimeRequest;
import com.fudn.movieservice.dto.ShowtimeResponse;
import com.fudn.movieservice.exception.ApiException;
import com.fudn.movieservice.model.*;
import com.fudn.movieservice.repository.MovieRepository;
import com.fudn.movieservice.repository.RoomRepository;
import com.fudn.movieservice.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final MovieService movieService;
    private final RoomService roomService;

    public ShowtimeResponse getById(String id) {
        Showtime s = find(id);
        return ShowtimeResponse.from(s, movieService.find(s.getMovieId()), roomService.find(s.getRoomId()));
    }

    public ShowtimeResponse create(ShowtimeRequest request) {
        Movie movie = movieService.find(request.movieId());
        CinemaRoom room = roomService.find(request.roomId());

        LocalDateTime endTime = request.startTime().plusMinutes(movie.getDurationMinutes());

        Showtime showtime = new Showtime();
        showtime.setMovieId(movie.getMovieId());
        showtime.setRoomId(room.getRoomId());
        showtime.setStartTime(request.startTime());
        showtime.setEndTime(endTime);
        showtime.setTicketPrice(request.ticketPrice());
        showtime.setShowtimeStatus(ShowtimeStatus.SCHEDULED);

        return ShowtimeResponse.from(showtimeRepository.save(showtime), movie, room);
    }

    public ShowtimeResponse update(String id, ShowtimeRequest request) {
        Showtime showtime = find(id);
        if (showtime.getShowtimeStatus() == ShowtimeStatus.CANCELLED) {
            throw ApiException.badRequest("Cannot update a cancelled showtime");
        }

        Movie movie = movieService.find(request.movieId());
        CinemaRoom room = roomService.find(request.roomId());

        LocalDateTime endTime = request.startTime().plusMinutes(movie.getDurationMinutes());

        showtime.setMovieId(movie.getMovieId());
        showtime.setRoomId(room.getRoomId());
        showtime.setStartTime(request.startTime());
        showtime.setEndTime(endTime);
        showtime.setTicketPrice(request.ticketPrice());

        return ShowtimeResponse.from(showtimeRepository.save(showtime), movie, room);
    }

    public void cancel(String id) {
        Showtime showtime = find(id);
        showtime.setShowtimeStatus(ShowtimeStatus.CANCELLED);
        showtimeRepository.save(showtime);
    }

    public Showtime find(String id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Showtime not found with id: " + id));
    }

    public List<ShowtimeResponse> toResponses(List<Showtime> showtimes) {
        Map<String, Movie> movies = movieRepository
                .findAllById(showtimes.stream().map(Showtime::getMovieId).distinct().toList())
                .stream().collect(Collectors.toMap(Movie::getMovieId, Function.identity()));
        Map<String, CinemaRoom> rooms = roomRepository
                .findAllById(showtimes.stream().map(Showtime::getRoomId).distinct().toList())
                .stream().collect(Collectors.toMap(CinemaRoom::getRoomId, Function.identity()));
        return showtimes.stream()
                .map(s -> ShowtimeResponse.from(s, movies.get(s.getMovieId()), rooms.get(s.getRoomId())))
                .toList();
    }
}