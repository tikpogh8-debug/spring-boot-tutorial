package com.tutorial.reactive;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("SPORT")
public record Sport(@Id Integer id, String name) {

    public static Sport named(String name) {
        return new Sport(null, name);
    }
}
