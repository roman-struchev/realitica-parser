package com.realitica.parser.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Entity
@Getter
@Setter
public class Stun {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String realiticaId;
    private String type;
    private String price;
    private String district;
    private String address;
    private String location;
    private String bedrooms;
    private String livingArea;
    private String moreInfo;
    private Date lastModified;
    private String link;
}
