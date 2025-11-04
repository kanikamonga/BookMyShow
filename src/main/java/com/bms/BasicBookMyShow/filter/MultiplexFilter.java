package com.bms.BasicBookMyShow.filter;

import com.bms.BasicBookMyShow.model.Show;

import java.util.List;
import java.util.stream.Collectors;

public class MultiplexFilter implements Filter {
    private final String multiPlexName;

    public MultiplexFilter(String multiPlexName) {
        this.multiPlexName = multiPlexName;
    }
    @Override
    public List<Show> filter(List<Show> shows){
        return shows.stream()
                .filter(show-> show.getScreen().getMultiplex().equals(multiPlexName))
                .collect(Collectors.toList());
    }
}
