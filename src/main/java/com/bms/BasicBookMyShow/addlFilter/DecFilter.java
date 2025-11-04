package com.bms.BasicBookMyShow.addlFilter;

import com.bms.BasicBookMyShow.model.Show;

import java.util.List;

public interface DecFilter {
    List<Show> apply(List<Show> shows);
}
