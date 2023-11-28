package com.kcompany.notification.entity.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.Blob;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    @Column(name = "ID")
    private int id;

    @Column(name = "NAME")
    public String name;

    @Column(name = "TEMPLATE")
    public Blob template;
}
