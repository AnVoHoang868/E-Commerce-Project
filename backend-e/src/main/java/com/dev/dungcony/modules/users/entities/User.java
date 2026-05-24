package com.dev.dungcony.modules.users.entities;

import com.dev.dungcony.commons.entities.BaseEntity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "tbl_users")
public class User extends BaseEntity implements Persistable<UUID> {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Column(name = "f_name")
    private String firstName;

    @Column(name = "l_name")
    private String lastName;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "acc_id")
    private Integer accountId;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Receiver> receivers = new ArrayList<>();
}
