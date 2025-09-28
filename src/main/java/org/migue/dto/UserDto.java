// dto/UserDto.java
package org.migue.dto;

public class UserDto {
    public Long id;
    public String name;
    public String username;
    public String email;
    public Address address;
    public String phone;
    public String website;
    public Company company;

    public static class Address {
        public String street;
        public String suite;
        public String city;
        public String zipcode;
        public Geo geo;
    }

    public static class Geo {
        public String lat;
        public String lng;
    }

    public static class Company {
        public String name;
        public String catchPhrase;
        public String bs;
    }
}
