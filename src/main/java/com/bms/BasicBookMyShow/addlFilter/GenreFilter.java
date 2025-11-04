package com.bms.BasicBookMyShow.addlFilter;

import com.bms.BasicBookMyShow.filter.Filter;
import com.bms.BasicBookMyShow.model.Show;

import java.util.List;
import java.util.stream.Collectors;

public class GenreFilter extends DecoratorFilterManager {
    private final String genre;

    public GenreFilter(String  genre,DecFilter decFilter) {
        super(decFilter);
        this.genre = genre;
    }

    @Override
    public List<Show> apply(List<Show> shows){
        return super.apply(shows).stream()
                .filter(show -> show.getMovie().getGenre().equals(genre))
                .collect(Collectors.toList());
    }
}
