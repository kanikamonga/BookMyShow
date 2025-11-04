package com.bms.BasicBookMyShow.sorter;

import com.bms.BasicBookMyShow.model.Show;

import java.util.List;

public class SortManager {

    private final SortingStrategy sortingStrategy;

    public SortManager(SortingStrategy sortingStrategy) {
        this.sortingStrategy = sortingStrategy;
    }

    public List<Show> getSortedShows(List<Show> shows){
        return sortingStrategy.sort(shows);
    }
}
