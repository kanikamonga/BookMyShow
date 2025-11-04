package com.bms.BasicBookMyShow.filter;

import com.bms.BasicBookMyShow.model.Show;

import java.util.List;

public interface Filter {
    List<Show> filter(List<Show> shows);
}
