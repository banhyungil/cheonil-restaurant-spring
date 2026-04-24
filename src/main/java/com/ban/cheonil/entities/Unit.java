package com.ban.cheonil.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "m_unit")
public class Unit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Short seq;

    @Size(max = 40)
    @NotNull
    @Column(name = "nm", nullable = false, length = 40)
    private String nm;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_unit_cnt", nullable = false)
    private Boolean isUnitCnt;


}