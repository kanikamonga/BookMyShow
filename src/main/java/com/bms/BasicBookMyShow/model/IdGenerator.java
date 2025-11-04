package com.bms.BasicBookMyShow.model;

import java.util.UUID;

public class IdGenerator {
    public static String generateId(){
        return UUID.randomUUID().toString();

    }

}
