package com.bms.BasicBookMyShow.filter;

import com.bms.BasicBookMyShow.model.Show;

import java.util.List;
import java.util.stream.Collectors;

public class MovieFilter implements Filter {
    private final String movieTitle;

    public MovieFilter(String movieTitle){
        this.movieTitle = movieTitle;
    }
    @Override
    public List<Show> filter(List<Show> shows){
        return shows.stream()
                .filter(show-> show.getMovie().getTitle().equals(movieTitle))
                .collect(Collectors.toList());
    }
}
