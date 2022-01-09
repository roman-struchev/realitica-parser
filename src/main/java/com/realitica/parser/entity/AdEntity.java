package com.realitica.parser.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Date;

@Entity
@Getter
@Setter
public class AdEntity {
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

    @CreationTimestamp
    private OffsetDateTime created;

    @UpdateTimestamp
    private OffsetDateTime updated;
}
