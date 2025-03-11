package com.estate.parser.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
public class AdEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String sourceId;
    private String sourceCode;
    private String sourceLink;

    @Enumerated(EnumType.STRING)
    private Type type;
    private String price;

    private String city;
    private String location;
    private String address;

    private String bedrooms;
    private String size;

    private String details;

    private LocalDateTime lastModified;

    @CreationTimestamp
    private OffsetDateTime created;

    @UpdateTimestamp
    private OffsetDateTime updated;

    @Getter
    @AllArgsConstructor
    public enum Type {
        APARTMENT_FOR_SALE("Apartment For Sale"),
        APARTMENT_LONG_TERM_RENTAL("Apartment Long Term Rental"),
        HOUSE_FOR_SALE("House For Sale"),
        HOUSE_LONG_TERM_RENTAL("House Long Term Rental"),
        LAND_FOR_SALE("Land For Sale"),
        LAND_LONG_TERM_RENTAL("Land Long Term Rental"),
        RESIDENTIAL_FOR_SALE("Residential Lot For Sale"),
        RESIDENTIAL_LONG_TERM_RENTAL("Residential Long Term Rental"),
        COMMERCIAL_FOR_SALE("Commercial For Sale"),
        COMMERCIAL_LONG_TERM_RENTAL("Commercial Long Term Rental"),
        GARAGE_FOR_SALE("Garage For Sale"),
        GARAGE_LONG_TERM_RENTAL("Garage Long Term Rental"),
        OTHER("Other");

        private final String desc;

        public static List<Type> allBy(String s) {
            var types = Arrays.stream(values()).filter(t -> t.desc.contains(s)).collect(Collectors.toList());
            types.add(OTHER);
            return types;
        }
    }
}
