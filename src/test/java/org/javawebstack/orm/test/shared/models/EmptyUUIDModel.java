package org.javawebstack.orm.test.shared.models;

import org.javawebstack.orm.Model;
import org.javawebstack.orm.annotation.Column;

import java.util.UUID;

public class EmptyUUIDModel extends Model {
    @Column
    UUID uuid;
}
