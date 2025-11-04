package com.bms.BasicBookMyShow.addlFilter;


import com.bms.BasicBookMyShow.model.Show;

import java.util.List;

public class DecoratorFilterManager implements DecFilter {
    private final DecFilter decFilter;

    public DecoratorFilterManager(DecFilter decFilter) {
        this.decFilter = decFilter;
    }

    @Override
    public List<Show> apply(List<Show> shows) {
        return decFilter.apply(shows);
    }
}
