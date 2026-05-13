package com.example.moviesapp_jaouhari;

public class Cinema {
    public final String name;
    public final String address;
    public final double lat;
    public final double lng;
    public final double rating;

    public Cinema(String name, String address, double lat, double lng, double rating) {
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.rating = rating;
    }
}
