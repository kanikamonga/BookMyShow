package com.bms.BasicBookMyShow.sorter;

import com.bms.BasicBookMyShow.model.Show;

import java.util.List;

public interface SortingStrategy {
    List<Show> sort(List<Show> shows);
}
